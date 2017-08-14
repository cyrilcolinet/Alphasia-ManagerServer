package net.mrlizzard.alphasia.manager.server.core;

public enum StartupMode {

    VERY_SLOW       (4, 2, 5 * 1000),
    SLOW            (4, 2, 3 * 1000),
    NORMAL          (4, 2, 2000),
    BURST           (4, 2, 1500),
    VERY_BURST      (4, 8, 1000);

    private int  nettyThreads;
    private int  garbageCollectorThreads;
    private long betweenCheck;

    /**
     * Constructor of a mode
     * @param nettyThreads
     * @param garbageCollectorThreads
     * @param betweenCheck
     */
    StartupMode(int nettyThreads, int garbageCollectorThreads, long betweenCheck) {
        this.nettyThreads 				= nettyThreads;
        this.garbageCollectorThreads 	= garbageCollectorThreads;
        this.betweenCheck 				= betweenCheck;
    }

    /**
     * Getting the number of the netty threads
     * @return
     */
    public int getNettyThreads() {
        return this.nettyThreads;
    }

    /**
     * Getting the number of the garbage collector threads
     * @return
     */
    public int getGarbageCollectorThreads() {
        return this.garbageCollectorThreads;
    }

    /**
     * Getting the between check value
     * @return
     */
    public long getBetweenCheck() {
        return this.betweenCheck;
    }

    /**
     * Get mode by string
     * @return
     */
    public static StartupMode getBy(String string) {
        for (StartupMode mode : StartupMode.values())
            if (mode.name().equalsIgnoreCase(string)) return mode;

        return StartupMode.NORMAL;
    }

}
