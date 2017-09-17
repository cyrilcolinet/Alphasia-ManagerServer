package net.mrlizzard.alphasia.manager.server.core.database;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.MainSubscriber;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.SubscribingThread;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Level;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class SingleCacheConnector extends CachingConnector {

    private final String        cacheIp;
    protected JedisPool         cachePool;

    public SingleCacheConnector(String cacheIp, String password, int database) {
        this.cacheIp = cacheIp;
        this.password = password;
        this.database = database;

        AlphaManagerServer.log(Level.INFO, "Starting redis (single) caching connection.");

        try {
            initiateConnections();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Jedis getCacheResource() {
        return cachePool.getResource();
    }

    @Override
    public void killConnections() {
        cachePool.destroy();
    }

    @Override
    public void initiateConnections() throws InterruptedException {
        // Préparation de la connexion
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(1024);
        config.setMaxWaitMillis(5000);

        String[] cacheParts = StringUtils.split(cacheIp, ":");
        int cachePort = (cacheParts.length > 1) ? Integer.decode(cacheParts[1]) : 6379;

        if (password == null || password.length() == 0) {
            AlphaManagerServer.log(Level.INFO, "Be careful, redis connection not secured. Data can be compromised.");
            this.cachePool = new JedisPool(config, cacheParts[0], cachePort, 5000, null, database);
        } else {
            AlphaManagerServer.log(Level.INFO, "Good job, your connection is secured, but... SSL is missing.");
            this.cachePool = new JedisPool(config, cacheParts[0], cachePort, 5000, password, database);
        }

        AlphaManagerServer.log(Level.INFO, "Connection initialized on database number " + database + ".");

        this.commandsSubscriber = new MainSubscriber();

        thread = new SubscribingThread(SubscribingThread.Type.PSUBSCRIBE, this, commandsSubscriber, "*");
        new Thread(thread).start();

        while (!commandsSubscriber.isSubscribed())
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        while (!commandsSubscriber.isSubscribed())
            Thread.sleep(100);
    }

    @Override
    public MainSubscriber getCommandsSubscriber() {
        return commandsSubscriber;
    }

    @Override
    public void disable() {
        AlphaManagerServer.log(Level.INFO, "Killing cache subscriptions...");
        commandsSubscriber.unsubscribe();
        commandsSubscriber.punsubscribe();
        AlphaManagerServer.log(Level.INFO, "Closing cache subscriber connection...");
        thread.disable();
        AlphaManagerServer.log(Level.INFO, "Removing cache pools...");
        killConnections();
    }

}
