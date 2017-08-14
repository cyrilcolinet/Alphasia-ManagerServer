package net.mrlizzard.alphasia.manager.server.core.entity;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.StartupMode;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

import java.util.*;

public class SystemEntity {

    private static Map<String, SystemEntity>        systems             = new HashMap<>();

    private String                                  name;
    private int                                     starterPort;
    private List<String>                            plugins             = new ArrayList<>();
    private List<String>                            brutMaps            = new ArrayList<>();
    private Map<String, MapEntity>                  maps;
    private boolean                                 local;
    private int                                     autoIncrementer;
    private boolean                                 opened;
    private int                                     serverCount;

    public SystemEntity(String name, boolean local, int starterPort, String maps, String plugins) {
        this.name = name;
        this.local = local;
        this.starterPort = starterPort;

        if(maps.contains(",")) this.brutMaps.addAll(Arrays.asList(StringUtils.split(maps, ",")));
        else this.brutMaps.add(maps);

        // Même si il y a toujours plusieurs plugins, on sait jamais
        if(plugins.contains(",")) this.plugins.addAll(Arrays.asList(StringUtils.split(plugins, ",")));
        else this.plugins.add(plugins);

        this.plugins.add("AlphaManagerBukkit");
        systems.put(name, this);
        this.loadMaps();
    }

    private void loadMaps() {
        maps = new HashMap<>();

        brutMaps.forEach(mapName -> {
            Map<String , String> map = AlphaManagerServer.getInstance().getCacheConnector().getCacheResource().hgetAll("alphamanager:_config:maps:" + this.name + ":" + mapName.toLowerCase());
            String name = map.get("name");
            MapEntity mapEntity = new MapEntity(this, name, map.get("folder"), Integer.parseInt(map.get("slots")), Integer.parseInt(map.get("free-slots")), Long.parseLong(map.get("ram")), StartupMode.getBy(map.get("startup-mode")));
            AlphaManagerServer.log(Level.INFO, "Added " + name + " world in " + this.name + " system");
            maps.put(name, mapEntity);
        });
    }

    public boolean getLocal() {
        return local;
    }

    public Collection<String> getBrutMaps() {
        return brutMaps;
    }

    public Collection<String> getPlugins() {
        return plugins;
    }

    public int getStarterPort() {
        return starterPort;
    }

    public int getAutoIncrementer() {
        return autoIncrementer;
    }

    public int getServerCount() {
        return serverCount;
    }

    public Map<String, MapEntity> getMaps() {
        return maps;
    }

    public static Map<String, SystemEntity> getSystems() {
        return systems;
    }

    public String getName() {
        return name;
    }

    public boolean isLocal() {
        return local;
    }

    public boolean isOpened() {
        return opened;
    }

}

