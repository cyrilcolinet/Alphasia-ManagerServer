package net.mrlizzard.alphasia.manager.server.core.database.handlers;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.database.CachingConnector;
import org.apache.log4j.Level;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class SubscribingThread implements Runnable {

    private final Type                  type;
    private final String[]              channels;
    private final JedisPubSub           pubSub;
    private final CachingConnector      connector;
    private boolean                     enabled     = true;

    public SubscribingThread(Type type, CachingConnector connector, JedisPubSub pubSub, String... channels) {
        this.type = type;
        this.connector = connector;
        this.channels = channels;
        this.pubSub = pubSub;
    }

    @Override
    public void run() {
        while (enabled) {
            try {
                Jedis jedis = connector.getCacheResource();
                //FarosServers.getInstance().getLogger().log(Level.INFO, "Starting subscribing " + ((type == Type.SUBSCRIBE) ? "SUBSCRIBE" : "PSUBSCRIBE") + ". Channels to subscribe on " + StringUtils.join(channels, "; "));

                try {
                    if (type == Type.SUBSCRIBE) jedis.subscribe(pubSub, channels);
                    else jedis.psubscribe(pubSub, channels);
                } catch (Exception e) {
                    AlphaManagerServer.log(Level.ERROR, "Exception during subscription. " + e.getMessage());
                }

                AlphaManagerServer.log(Level.INFO, "Closing subscription. " + (((!enabled) ? "Stopping thread." : "Trying to restart.")));
                jedis.close();
            } catch (Exception e) {
                AlphaManagerServer.log(Level.ERROR, "Exception while getting database: " + e.getMessage() + ". Retrying in 5 seconds.");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    public void disable() {
        enabled = false;
        pubSub.punsubscribe();
        pubSub.unsubscribe();
    }

    public enum Type {
        SUBSCRIBE, PSUBSCRIBE
    }

}
