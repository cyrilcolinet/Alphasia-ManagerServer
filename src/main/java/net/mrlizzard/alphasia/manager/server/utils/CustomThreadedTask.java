package net.mrlizzard.alphasia.manager.server.utils;

import java.util.Timer;
import java.util.TimerTask;

public abstract class CustomThreadedTask extends TimerTask {

    private static Timer    timer = new Timer();

    private long            starterMs;
    private long	        repeatMs;

    protected CustomThreadedTask(long starterMs, long repeatMs) {
        this.starterMs = starterMs;
        this.repeatMs = repeatMs;
        timer.schedule(this, this.getStarterMs(), this.getRepeatMs());
    }

    protected long getStarterMs() {
        return this.starterMs;
    }

    protected long getRepeatMs() {
        return this.repeatMs;
    }

}