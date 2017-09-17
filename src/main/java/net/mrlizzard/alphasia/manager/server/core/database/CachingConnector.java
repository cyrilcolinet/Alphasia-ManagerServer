package net.mrlizzard.alphasia.manager.server.core.database;

import net.mrlizzard.alphasia.manager.server.core.database.handlers.MainSubscriber;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.PubSubConsumer;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.SubscribingThread;
import redis.clients.jedis.Jedis;

/**
 * ChachingConnector class.
 * @author MrLizzard
 */
public abstract class CachingConnector {

    protected String                password;
    protected int                   database;
    protected MainSubscriber        commandsSubscriber;
    protected SubscribingThread     thread;

    public void subscribe(String channel, PubSubConsumer consummer) {
        commandsSubscriber.subscribe(channel);
        commandsSubscriber.addConsumer(channel, consummer);
    }

    public void psubscribe(String channel, PubSubConsumer consummer) {
        commandsSubscriber.psubscribe(channel);
        commandsSubscriber.addPConsumer(channel, consummer);
    }

    public abstract Jedis getCacheResource();

    public abstract void killConnections();

    public abstract void initiateConnections() throws InterruptedException;

    public abstract MainSubscriber getCommandsSubscriber();

    public abstract void disable();

}
