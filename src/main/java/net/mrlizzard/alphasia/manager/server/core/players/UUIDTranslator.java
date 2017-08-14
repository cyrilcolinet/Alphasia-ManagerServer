package net.mrlizzard.alphasia.manager.server.core.players;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import org.apache.log4j.Level;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class UUIDTranslator  {

    private final Pattern                               UUID_PATTERN                = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    private final Pattern                               MOJANGIAN_UUID_PATTERN      = Pattern.compile("[a-fA-F0-9]{32}");
    private final Map<String, CachedUUIDEntry>          nameToUuidMap               = new ConcurrentHashMap<>(128, 0.5f, 4);
    private final Map<UUID, CachedUUIDEntry>            uuidToNameMap               = new ConcurrentHashMap<>(128, 0.5f, 4);
    private final AlphaManagerServer                    instance;

    public UUIDTranslator(AlphaManagerServer instance) {
        this.instance = instance;
    }

    private void addToMaps(String name, UUID uuid) {
        // This is why I like LocalDate...

        // Cache the entry for three days.
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_WEEK, 3);

        // Create the entry and populate the local maps
        CachedUUIDEntry entry = new CachedUUIDEntry(name, uuid, calendar);
        nameToUuidMap.put(name.toLowerCase(), entry);
        uuidToNameMap.put(uuid, entry);
    }

    public void persistInfo(String name, UUID uuid, Jedis jedis) {
        addToMaps(name, uuid);
        jedis.hset("cache:uuid", name.toLowerCase(), instance.getGson().toJson(uuidToNameMap.get(uuid)));
        jedis.hset("cache:uuid", uuid.toString(), instance.getGson().toJson(uuidToNameMap.get(uuid)));
    }

    public UUID getUUID(String name, boolean allowMojangCheck) {
        // Check if it exists in the map
        CachedUUIDEntry cachedUUIDEntry = nameToUuidMap.get(name.toLowerCase());

        if (cachedUUIDEntry != null) {
            if (!cachedUUIDEntry.expired()) return cachedUUIDEntry.getUuid();
            else nameToUuidMap.remove(name);
        }

        // Check if we can exit early
        if (UUID_PATTERN.matcher(name).find()) {
            return UUID.fromString(name);
        }

        if (MOJANGIAN_UUID_PATTERN.matcher(name).find()) {
            // Reconstruct the UUID
            return UUIDFetcher.getUUID(name);
        }

        // Let's try Redis.
        try (Jedis jedis = instance.getCacheConnector().getCacheResource()) {
            String stored = jedis.hget("cache:uuid", name.toLowerCase());

            if (stored != null) {
                // Found an entry value. Deserialize it.
                CachedUUIDEntry entry = instance.getGson().fromJson(stored, CachedUUIDEntry.class);

                // Check for expiry:
                if (entry.expired()) {
                    jedis.hdel("cache:uuid", name.toLowerCase());
                } else {
                    nameToUuidMap.put(name.toLowerCase(), entry);
                    uuidToNameMap.put(entry.getUuid(), entry);
                    return entry.getUuid();
                }
            }

            // That didn't work. Let's ask Mojang.
            if (!allowMojangCheck)
                return null;

            Map<String, UUID> uuidMap1;

            try {
                uuidMap1 = new UUIDFetcher(Collections.singletonList(name)).call();
            } catch (Exception e) {
                AlphaManagerServer.log(Level.ERROR, "Unable to fetch UUID from Mojang servers for " + name + ". " + e.getMessage());
                return null;
            }

            for (Map.Entry<String, UUID> entry : uuidMap1.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    persistInfo(entry.getKey(), entry.getValue(), jedis);
                    return entry.getValue();
                }
            }
        } catch (JedisException e) {
            AlphaManagerServer.log(Level.ERROR, "Unable to fetch UUID for " + name + ". " + e.getMessage());
        }

        return null; // Nope, game over!
    }

    public String getName(UUID uuid, boolean allowMojangCheck) {
        // Check if it exists in the map
        CachedUUIDEntry cachedUUIDEntry = uuidToNameMap.get(uuid);

        if (cachedUUIDEntry != null) {
            if (!cachedUUIDEntry.expired()) return cachedUUIDEntry.getName();
            else uuidToNameMap.remove(uuid);
        }

        // Okay, it wasn't locally cached. Let's try Redis.
        try (Jedis jedis = instance.getCacheConnector().getCacheResource()) {
            String stored = jedis.hget("cache:uuid", uuid.toString());
            if (stored != null) {
                // Found an entry value. Deserialize it.
                CachedUUIDEntry entry = instance.getGson().fromJson(stored, CachedUUIDEntry.class);

                // Check for expiry:
                if (entry.expired()) {
                    jedis.hdel("cache:uuid", uuid.toString());
                } else {
                    nameToUuidMap.put(entry.getName().toLowerCase(), entry);
                    uuidToNameMap.put(uuid, entry);
                    return entry.getName();
                }
            }

            if (!allowMojangCheck)
                return null;

            // That didn't work. Let's ask Mojang. This call may fail, because Mojang is insane.
            String name;

            try {
                List<String> names = NameFetcher.nameHistoryFromUuid(uuid);
                name = names.get(names.size() - 1);
            } catch (Exception e) {
                AlphaManagerServer.log(Level.ERROR, "Unable to fetch name from Mojang servers for " + uuid + ". " + e.getMessage());
                return null;
            }

            if (name != null) {
                persistInfo(name, uuid, jedis);
                return name;
            }

            return null;
        } catch (JedisException e) {
            AlphaManagerServer.log(Level.ERROR, "Unable to fetch name for " + uuid + ". " + e.getMessage());
            return null;
        }
    }

    private static class CachedUUIDEntry {

        private final String name;
        private final UUID uuid;
        private final Calendar expiry;

        public boolean expired() {
            return Calendar.getInstance().after(expiry);
        }

        public CachedUUIDEntry(String name, UUID uuid, Calendar expiry) {
            this.name = name;
            this.uuid = uuid;
            this.expiry = expiry;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Calendar getExpiry() {
            return expiry;
        }

    }

}