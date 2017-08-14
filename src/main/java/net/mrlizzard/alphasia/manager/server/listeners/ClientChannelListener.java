package net.mrlizzard.alphasia.manager.server.listeners;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.PubSubConsumer;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

public class ClientChannelListener implements PubSubConsumer {

    @Override
    public void consume(String channel, String message) {
        if(message.startsWith("start")) {
            String[] content = StringUtils.split(message, " ");

            if(content.length < 2)
                return;

            String hostname = content[1];
            AlphaManagerServer.getInstance().getAlphaManagerClientNetwork().heartBeet(hostname);
        } else if(message.startsWith("stop")) {
            String[] content = StringUtils.split(message, " ");

            if(content.length < 2 || content[1].equalsIgnoreCase(AlphaManagerServer.getInstance().getHostname()))
                return;

            String hostname = content[1];
            AlphaManagerServer.log(Level.INFO, "Client manually stopped (" + hostname + ").");
            AlphaManagerServer.getInstance().getAlphaManagerClientNetwork().getClients().remove(hostname);
        } else if(message.startsWith("heartbeat")) {
            String[] content = StringUtils.split(message, " ");

            if(content.length < 2)
                return;

            AlphaManagerServer.getInstance().getAlphaManagerClientNetwork().heartBeet(content[1]);
        }
    }

}
