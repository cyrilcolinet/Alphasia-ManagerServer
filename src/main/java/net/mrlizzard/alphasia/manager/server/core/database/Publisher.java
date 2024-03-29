package net.mrlizzard.alphasia.manager.server.core.database;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import org.apache.log4j.Level;
import redis.clients.jedis.Jedis;

import java.util.concurrent.LinkedBlockingQueue;

public class Publisher implements Runnable {

    private LinkedBlockingQueue<PendingMessage>     pendingMessages = new LinkedBlockingQueue<>();
    private final CachingConnector                  connector;
    private Jedis                                   jedis;

    public Publisher(CachingConnector connector) {
        this.connector = connector;
    }

    public void publish(PendingMessage message) {
        pendingMessages.add(message);
    }

    @Override
    public void run() {
        fixDatabase();

        while (true) {
            PendingMessage message;

            try {
                message = pendingMessages.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                jedis.close();
                return;
            }

            boolean published = false;

            while (!published) {
                try {
                    jedis.publish(message.getChannel(), message.getMessage());
                    message.runAfter();
                    published = true;
                } catch (Exception e) {
                    fixDatabase();
                }
            }
        }
    }

    private void fixDatabase() {
        try {
            jedis = connector.getCacheResource();
        } catch (Exception e) {
            AlphaManagerServer.log(Level.ERROR, "Can't connect to redis server: " + e.getMessage() + ". Retrying in 5 seconds.");

            try {
                Thread.sleep(5000);
                fixDatabase();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static class PendingMessage {
        private final String channel;
        private final String message;
        private final Runnable callback;

        public PendingMessage(String channel, String message) {
            this.channel = channel;
            this.message = message;
            this.callback = null;
        }

        public PendingMessage(String channel, String message, Runnable callback) {
            this.channel = channel;
            this.message = message;
            this.callback = callback;
        }

        public String getChannel() {
            return channel;
        }

        public String getMessage() {
            return message;
        }

        public void runAfter() {
            try {
                if (callback != null)
                    callback.run();
            } catch (Exception ignored) {}
        }
    }

}
