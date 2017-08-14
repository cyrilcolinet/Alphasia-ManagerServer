package net.mrlizzard.alphasia.manager.server.listeners;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.PubSubConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

public class ServerChannelListener implements PubSubConsumer {

    @Override
    public void consume(String channel, String message) {
        if(message.startsWith("heartbeat")) {
            String[] content = StringUtils.split(message, " ");

            if(content.length < 2)
                return;

            AlphaManagerServer.getInstance().getAlphaManagerServerNetwork().heartBeet(content[1]);
        } else if(message.startsWith("stop")) {
            String[] content = StringUtils.split(message, " ");

            if(content.length < 2 || content[1].equalsIgnoreCase(AlphaManagerServer.getInstance().getHostname()))
                return;

            String hostname = content[1];
            AlphaManagerServer.log(Level.INFO, "Server manually stopped (" + hostname + ").");
            AlphaManagerServer.getInstance().getAlphaManagerServerNetwork().getServers().remove(hostname);
        }
    }

}
