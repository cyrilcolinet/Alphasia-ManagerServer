package net.mrlizzard.alphasia.manager.server.tasks;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.utils.CustomThreadedTask;
import org.apache.log4j.Level;

public class ServerHeartbeetTask extends CustomThreadedTask {

    private Runnable runnable;

    public ServerHeartbeetTask(Runnable runnable) {
        super(0, 5000);

        this.runnable = runnable;
        AlphaManagerServer.log(Level.INFO, "Server heartbeat task started !");
    }

    @Override
    public void run() {
        runnable.run();
    }

}
