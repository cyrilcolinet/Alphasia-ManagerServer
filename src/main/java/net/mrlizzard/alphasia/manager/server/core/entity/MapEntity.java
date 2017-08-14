package net.mrlizzard.alphasia.manager.server.core.entity;

import net.mrlizzard.alphasia.manager.server.core.StartupMode;

import java.util.HashMap;

public class MapEntity {

    public HashMap<String, InstanceEntity>      instances 			= new HashMap<>();
    private String 						        mapName;
    private String 						        folderName;
    private int    						        slots;
    private int    						        reservedSlotsForBoot;
    private long   						        physicalMemory;
    private StartupMode   						mode;
    private SystemEntity 						server;
    private int    						        freeSlots;
    private boolean                             online;

    public MapEntity(SystemEntity entity, String mapName, String folderName, int slots, int freeSlotsForBoot, long RAM, StartupMode mode) {
        //super(1000 * 10, mode.getBetweenCheck());
        this.server = entity;
        this.mode = mode;
        this.mapName = mapName;
        this.folderName = folderName;
        this.slots = slots;
        this.reservedSlotsForBoot = freeSlotsForBoot;
        this.physicalMemory = RAM;
        this.online = true;
    }

    public HashMap<String, InstanceEntity> getInstances() {
        return instances;
    }

    public String getMapName() {
        return mapName;
    }

    public int getFreeSlots() {
        return freeSlots;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getSlots() {
        return slots;
    }

    public int getReservedSlotsForBoot() {
        return reservedSlotsForBoot;
    }

    public long getPhysicalMemory() {
        return physicalMemory;
    }

    public StartupMode getMode() {
        return mode;
    }

    public SystemEntity getServer() {
        return server;
    }

}
