package net.mrlizzard.alphasia.manager.server.core.database.handlers;

import com.google.common.collect.HashMultimap;
import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.listeners.DebugListener;
import net.mrlizzard.alphasia.manager.server.utils.logger.ChatColor;
import org.apache.log4j.Level;
import redis.clients.jedis.Client;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * MainSubscriber class.
 * @author MrLizzard
 */
public class MainSubscriber extends JedisPubSub {

    protected HashMultimap<String, PubSubConsumer> consumers = HashMultimap.create();
    protected HashMultimap<String, PubSubConsumer> pconsumers = HashMultimap.create();

    public void addConsumer(String chan, PubSubConsumer consummer) {
        this.consumers.put(chan, consummer);
    }

    public void addPConsumer(String chan, PubSubConsumer consummer) {
        this.pconsumers.put(chan, consummer);
    }

    @Override
    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        AlphaManagerServer.log(Level.INFO, "UnSubscribed from pattern " + pattern);
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        AlphaManagerServer.log(Level.INFO, "UnSubscribed from channel " + channel);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        AlphaManagerServer.log(Level.INFO, "Subscribed on channel " + channel);
    }

    @Override
    public void onPSubscribe(String pattern, int subscribedChannels) {
        AlphaManagerServer.log(Level.INFO, "Subscribed on pattern " + pattern);
    }

    @Override
    public void proceed(Client client, String... channels) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                uploadConsumerDeMort();
            }
        }, 1);

        super.proceed(client, channels);
    }

    @Override
    public void proceedWithPatterns(Client client, String... channels) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                uploadConsumerDeMort();
            }
        }, 1);

        super.proceedWithPatterns(client, channels);
    }

    public void uploadConsumerDeMort() {
        consumers.keySet().forEach(this::subscribe);
        pconsumers.keySet().forEach(this::psubscribe);
    }

    @Override
    public void onMessage(String channel, String message) {
        Set<PubSubConsumer> consummers = this.consumers.get(channel);

        if (consummers != null)
            try {
                consummers.forEach(consumer -> {
                    consumer.consume(channel, message);

                    if(DebugListener.enabled)
                        AlphaManagerServer.log(Level.INFO, "Consuming to " + consumer.getClass().getName());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @Override
    public void onPMessage(String pattern, String channel, String message) {
        if (channel.equals("__sentinel__:hello"))
            return;

        if (pattern.equals("*")) {
            if(DebugListener.enabled)
                AlphaManagerServer.log(Level.INFO, ChatColor.GREEN + "{DEBUG: " + channel + "} " + message + ChatColor.RESET);
        } else {
            Set<PubSubConsumer> consummers = this.pconsumers.get(pattern);

            if (consummers != null) {
                try {
                    consummers.forEach(consumer -> {
                        consumer.consume(channel, message);

                        if(DebugListener.enabled)
                            AlphaManagerServer.log(Level.INFO, "Consuming to " + consumer.getClass().getName());
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
