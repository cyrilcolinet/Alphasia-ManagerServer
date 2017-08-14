package net.mrlizzard.alphasia.manager.server.core.database.networking;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.Console;
import net.mrlizzard.alphasia.manager.server.core.database.Publisher;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.PubSubConsumer;
import net.mrlizzard.alphasia.manager.server.core.database.handlers.SubscribingThread;
import net.mrlizzard.alphasia.manager.server.listeners.ClientChannelListener;
import net.mrlizzard.alphasia.manager.server.listeners.DedicatedServerLoadListener;
import net.mrlizzard.alphasia.manager.server.listeners.ServerChannelListener;
import net.mrlizzard.alphasia.manager.server.tasks.ServerHeartbeetTask;
import net.mrlizzard.alphasia.manager.server.utils.logger.ChatColor;
import org.apache.log4j.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AlphaManagerServerNetwork {

    protected AlphaManagerServer                            instance;

    private final SubscribingThread                         thread;
    private ConcurrentHashMap<String, RemoteServer>         servers         = new ConcurrentHashMap<>();

    public AlphaManagerServerNetwork(AlphaManagerServer instance) {
        this.instance = instance;
        AlphaManagerServer.log(Level.INFO, "Initialized linkage with other servers...");

        if (instance.getCacheConnector().getCacheResource().hsetnx("alphamanager:servers", instance.getHostname(), instance.getIpAddress()) == 0) {
            thread = null;

            AlphaManagerServer.log(Level.ERROR, "This server IP address is already registered in servers instance.");
            System.exit(-1);
        } else {
            instance.getPublisher().publish(new Publisher.PendingMessage("alphamanager.server", "start " + AlphaManagerServer.getInstance().getHostname()));
            instance.getCacheConnector().getCacheResource().hset("alphamanager:servers", AlphaManagerServer.getInstance().getHostname(), AlphaManagerServer.getInstance().getIpAddress());

            // Get all registered servers on map
            Map<String, String> registeredServers = instance.getCacheConnector().getCacheResource().hgetAll("alphamanager:servers");
            registeredServers.entrySet().forEach(entry -> this.heartBeet(entry.getKey()));

            PubSubConsumer minecraftCommands = (channel, message) -> {
                String[] msg = message.split(" ");
                UUID uuid;
                boolean userAuthorized;

                if(msg.length < 2)
                    return;

                try {
                    uuid = UUID.fromString(msg[0]);
                } catch(Exception ignored) {
                    AlphaManagerServer.log(Level.ERROR, msg[0] + " is not a valid UUID. Aborted.");
                    return;
                }

                try {
                    userAuthorized = !instance.getCacheConnector().getCacheResource().sismember("alphamanager:_config:owners", uuid.toString());
                } catch(Exception error) {
                    AlphaManagerServer.log(Level.ERROR, "Error while getting manager owners.");
                    return;
                }

                if(msg.length > 1) {
                    AlphaManagerServer.log(Level.INFO, ChatColor.YELLOW + "Executing remote command by " + instance.getUuidTranslator().getName(uuid, true) + ": " + message.replace(msg[0] + " ", "") + ChatColor.RESET);
                    Console.secureCommand(message.replace(msg[0] + " ", ""), false, userAuthorized);
                }
            };

            instance.getCacheConnector().subscribe("alphamanager.external.command", minecraftCommands);
            instance.getCacheConnector().subscribe("alphamanager.server", new ServerChannelListener());
            instance.getCacheConnector().subscribe("alphamanager.client", new ClientChannelListener());
            //instance.getCacheConnector().subscribe("proxybridge", new ProxyHeartbeatListener());
            //instance.getCacheConnector().subscribe("alphamanager.client.load", new DedicatedServerLoadListener());
            //instance.getCacheConnector().subscribe("servers", new InstanceHeartbeatListener());

            // Init heartbeat
            new ServerHeartbeetTask(this::heartBeet);

            thread = new SubscribingThread(SubscribingThread.Type.SUBSCRIBE, net.mrlizzard.alphasia.manager.server.AlphaManagerServer.getInstance().getCacheConnector(), new Subscriber(this), "alphamanager");
            new Thread(thread).start();
        }
    }

    private void heartBeet() {
        AlphaManagerServer instance = AlphaManagerServer.getInstance();
        instance.getPublisher().publish(new Publisher.PendingMessage("alphamanager.server", "heartbeat " + instance.getHostname()));

        servers.values().stream().filter(server -> !server.isOnline()).forEach(server -> {
            AlphaManagerServer.log(Level.ERROR, ChatColor.RED + "Server \"" + server.getHostname() + "\" detected as offline, will be removed.");
            servers.remove(server.getHostname());
            instance.getCacheConnector().getCacheResource().hdel("alphamanager:servers", server.getHostname());
        });
    }

    public void heartBeet(String hostname) {
        if(this.instance.getHostname().equalsIgnoreCase(hostname))
            return;

        RemoteServer server = servers.get(hostname);

        if(server == null) servers.put(hostname, new RemoteServer(new Date(), hostname));
        else server.heartbeet();
    }

    public void disable() {
        AlphaManagerServer.log(Level.INFO, "Clearing old cache...");
        instance.getPublisher().publish(new Publisher.PendingMessage("alphamanager.server", "stop " + instance.getHostname()));
        instance.getCacheConnector().getCacheResource().hdel("alphamanager:servers", instance.getHostname());
    }

    public ConcurrentHashMap<String, RemoteServer> getServers() {
        return servers;
    }

    public static class RemoteServer {

        private Date        lastHeartbeet;
        private String      hostname;
        private Date        uptime;

        public RemoteServer(Date lastHeartbeet, String hostname) {
            this.lastHeartbeet = lastHeartbeet;
            this.hostname = hostname;
            this.uptime = new Date();
            AlphaManagerServer.log(Level.INFO, "Registered new server entity (" + hostname + ").");
        }

        public void heartbeet() {
            lastHeartbeet = new Date();
        }

        public boolean isOnline() {
            Date date = new Date(System.currentTimeMillis() - 15000); // 15 secondes
            return !(lastHeartbeet == null || lastHeartbeet.before(date));
        }

        public String getHostname() {
            return hostname;
        }

        public Date getLastHeartbeet() {
            return lastHeartbeet;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public void setLastHeartbeet(Date lastHeartbeet) {
            this.lastHeartbeet = lastHeartbeet;
        }

        public Date getUptime() {
            return uptime;
        }
    }

}