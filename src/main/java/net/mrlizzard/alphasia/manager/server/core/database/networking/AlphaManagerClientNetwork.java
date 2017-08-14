package net.mrlizzard.alphasia.manager.server.core.database.networking;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.tasks.ClientHeartbeetTask;
import net.mrlizzard.alphasia.manager.server.utils.logger.ChatColor;
import org.apache.log4j.Level;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlphaManagerClientNetwork {

    protected AlphaManagerServer                            instance;

    private ConcurrentHashMap<String, RemoteClient>         clients         = new ConcurrentHashMap<>();

    public AlphaManagerClientNetwork(AlphaManagerServer instance) {
        this.instance = instance;
        AlphaManagerServer.log(Level.INFO, "Initialized clients connector...");

        // Get all registered clients on map
        Map<String, String> registeredClients = instance.getCacheConnector().getCacheResource().hgetAll("alphamanager:clients");
        registeredClients.entrySet().forEach(entry -> this.heartBeet(entry.getKey()));

        // Init heartbeet
        new ClientHeartbeetTask(this::heartBeet);
    }

    private void heartBeet() {
        clients.values().stream().filter(client -> !client.isOnline()).forEach(client -> {
            AlphaManagerServer.log(Level.ERROR, ChatColor.RED + "Client \"" + client.getHostname() + "\" (" + (client.isProduction() ? "production" : "dev") + " mode) detected as offline, will be removed.");
            clients.remove(client.getHostname());
            instance.getCacheConnector().getCacheResource().hdel("alphamanager:clients", client.getHostname());
        });
    }

    public void heartBeet(String hostname) {
        RemoteClient client = clients.get(hostname);

        if(client == null) clients.put(hostname, new RemoteClient(new Date(), hostname));
        else client.heartbeet();
    }

    public ConcurrentHashMap<String, RemoteClient> getClients() {
        return clients;
    }

    public static class RemoteClient {

        private Date        lastHeartbeet;
        private String      hostname;
        private boolean     production;
        private Date        uptime;

        public RemoteClient(Date lastHeartbeet, String hostname) {
            this.lastHeartbeet = lastHeartbeet;
            this.hostname = hostname;
            this.production = hostname.startsWith("client");
            this.uptime = new Date();
            AlphaManagerServer.log(Level.INFO, "Registered new client entity (" + hostname + ") in " + (this.production ? "production" : "dev") + " mode.");
        }

        public void heartbeet() {
            lastHeartbeet = new Date();
        }

        public boolean isOnline() {
            Date date = new Date(System.currentTimeMillis() - 15000); // 15 secondes
            return !(lastHeartbeet == null || lastHeartbeet.before(date));
        }

        public Date getLastHeartbeet() {
            return lastHeartbeet;
        }

        public String getHostname() {
            return hostname;
        }

        public void setLastHeartbeet(Date lastHeartbeet) {
            this.lastHeartbeet = lastHeartbeet;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public void setProduction(boolean production) {
            this.production = production;
        }

        public boolean isProduction() {
            return production;
        }

        public Date getUptime() {
            return uptime;
        }
    }

}
