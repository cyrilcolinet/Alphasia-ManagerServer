package net.mrlizzard.alphasia.manager.server.core.database.handlers;

public interface PubSubConsumer {

    void consume(String channel, String message);

}
