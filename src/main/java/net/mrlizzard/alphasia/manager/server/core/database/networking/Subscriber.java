package net.mrlizzard.alphasia.manager.server.core.database.networking;

import redis.clients.jedis.JedisPubSub;

public class Subscriber extends JedisPubSub {

    private final AlphaManagerServerNetwork networkServer;

    public Subscriber(AlphaManagerServerNetwork networkServer) {
        this.networkServer = networkServer;
    }

    @Override
    public void onMessage(String channel, String message) {
        String[] content = message.split(" ");
        System.out.println(message);
    }

}
