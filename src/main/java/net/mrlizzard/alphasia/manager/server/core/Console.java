package net.mrlizzard.alphasia.manager.server.core;

import com.ocpsoft.pretty.time.Duration;
import com.ocpsoft.pretty.time.PrettyTime;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.completer.FileNameCompleter;
import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.database.Publisher;
import net.mrlizzard.alphasia.manager.server.core.database.networking.AlphaManagerClientNetwork;
import net.mrlizzard.alphasia.manager.server.core.database.networking.AlphaManagerServerNetwork;
import net.mrlizzard.alphasia.manager.server.core.entity.MapEntity;
import net.mrlizzard.alphasia.manager.server.core.entity.SystemEntity;
import net.mrlizzard.alphasia.manager.server.listeners.DebugListener;
import net.mrlizzard.alphasia.manager.server.utils.logger.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Level;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Console {

    public Console() {
        try {
            ConsoleReader console = new ConsoleReader();
            console.addCompleter(new FileNameCompleter());
            console.setPrompt("");
            String line;

            while ((line = console.readLine()) != null) {
                secureCommand(line, true, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void secureCommand(String message, boolean display, boolean external) {
        try {
            execute(message, display, external);
        } catch(Exception error) {
            AlphaManagerServer.log(Level.ERROR, "Error in processing command. " + error.getMessage());
        }
    }

    private static void execute(String message, boolean display, boolean external) {
        if(message.equalsIgnoreCase("help")) {
            if(display) {
                AlphaManagerServer.log(Level.INFO, "=~=~=~=~=~=~=~=~=~=~= DEFAULT HELP PAGE =~=~=~=~=~=~=~=~=~=~=");
                AlphaManagerServer.log(Level.INFO, "help - Display help (this page).");
                AlphaManagerServer.log(Level.INFO, "close - Shutdown this faros management service.");
                AlphaManagerServer.log(Level.INFO, "proxylist - Display the proxies list.");
                AlphaManagerServer.log(Level.INFO, "manager - Manage AlphaManager instances. (full-access)");
                AlphaManagerServer.log(Level.INFO, "config - Check configuration of managers. (full-access)");
                AlphaManagerServer.log(Level.INFO, "=~=~=~=~=~=~=~=~=~=~= DEFAULT HELP PAGE =~=~=~=~=~=~=~=~=~=~=");
            }

            return;
        }

        if(message.equalsIgnoreCase("close")) {
            System.exit(-1);
            return;
        }

        // SubCommand manager
        if(message.contains("manager") && !external) {
            List<String> split = Arrays.asList(StringUtils.split(message, " "));
            List<String> subCommands = new ArrayList<>();

            if((split.size() < 2 || split.get(1).toLowerCase().contains("help")) && display) {
                AlphaManagerServer.log(Level.INFO, "=~=~=~=~=~=~=~=~=~=~= MANAGER HELP PAGE =~=~=~=~=~=~=~=~=~=~=");
                AlphaManagerServer.log(Level.INFO, "manager help - Display help (this page).");
                AlphaManagerServer.log(Level.INFO, "manager updateclients - Force update packages and configurations on all clients.");
                AlphaManagerServer.log(Level.INFO, "manager debug - Enable or Disable debug display.");
                AlphaManagerServer.log(Level.INFO, "manager list <servers/clients> - List of the servers/clients registered.");
                AlphaManagerServer.log(Level.INFO, "manager client <clientId> <command> - Send client command. (close,reload,reboot (warn: linux reboot))");
                AlphaManagerServer.log(Level.INFO, "manager owners - See the owners list.");
                AlphaManagerServer.log(Level.INFO, "manager addowner <username> - Add manager owner to the list.");
                AlphaManagerServer.log(Level.INFO, "=~=~=~=~=~=~=~=~=~=~= MANAGER HELP PAGE =~=~=~=~=~=~=~=~=~=~=");
                return;
            }

            if(split.get(1).toLowerCase().contains("updateclients")) {
                AlphaManagerServer.getInstance().getPublisher().publish(new Publisher.PendingMessage("alphamanager.client.all", "update"));

                if(display)
                    AlphaManagerServer.log(Level.INFO, "Force package update sending to all clients.");

                return;
            }

            if(split.get(1).toLowerCase().contains("debug")) {
                DebugListener.enabled = !DebugListener.enabled;

                if(display)
                    AlphaManagerServer.log(Level.INFO, "Debug mode is " + (DebugListener.enabled ? "enabled" : "disabled") + ".");

                return;
            }

            subCommands.addAll(Arrays.asList("servers", "clients"));

            if(split.get(1).toLowerCase().contains("list") && split.size() >= 3 && subCommands.contains(split.get(2).toLowerCase()) && display) {
                PrettyTime prettyTime = new PrettyTime();

                if(split.get(2).equalsIgnoreCase("servers")) {
                    AlphaManagerServerNetwork network = AlphaManagerServer.getInstance().getAlphaManagerServerNetwork();
                    AlphaManagerServer.log(Level.INFO, (network.getServers().size() + 1) + " servers registered. Servers list:");
                    AlphaManagerServer.log(Level.INFO, " - " + AlphaManagerServer.getInstance().getHostname() + ChatColor.GREEN + " (Online)" + ChatColor.RESET + " - Started " + prettyTime.format(AlphaManagerServer.getInstance().getStarted()));

                    network.getServers().values().forEach(server -> {
                        String status = (server.isOnline() ? ChatColor.GREEN + " (Online)" : ChatColor.RED + " (Offline)") + ChatColor.RESET;
                        AlphaManagerServer.log(Level.INFO, " - " + server.getHostname() + status + " - Started " + prettyTime.format(server.getUptime()));
                    });

                    return;
                } else if(split.get(2).equalsIgnoreCase("clients")) {
                    AlphaManagerClientNetwork network = AlphaManagerServer.getInstance().getAlphaManagerClientNetwork();

                    if(network.getClients().size() <= 0) {
                        AlphaManagerServer.log(Level.INFO, "No clients registered.");
                        return;
                    }

                    AlphaManagerServer.log(Level.INFO, network.getClients().size() + " clients registered. Clients list:");

                    network.getClients().values().forEach(client -> {
                        String status = (client.isOnline() ? ChatColor.GREEN + " (Online)" : ChatColor.RED + " (Offline)") + ChatColor.RESET;
                        String[] hostContent = StringUtils.split(client.getHostname().toLowerCase(), ".");
                        String clientId;

                        if(hostContent.length >= 3 && hostContent[hostContent.length - 1].equals("fr") && hostContent[hostContent.length - 2].equals("alphasia")) clientId = hostContent[0];
                        else clientId = client.getHostname().toLowerCase();

                        AlphaManagerServer.log(Level.INFO, " - Id: " + clientId + ", Hostname: " + client.getHostname() + " |" + status + " - Started: " + prettyTime.format(client.getUptime()));
                    });

                    return;
                }
            }

            subCommands.clear();
            subCommands.addAll(Arrays.asList("close", "reload", "reboot"));

            if(split.get(1).toLowerCase().contains("client") && split.size() >= 4 && subCommands.contains(split.get(3).toLowerCase())) {
                String command = split.get(3).toLowerCase(), clientId = split.get(2).toLowerCase();

                AlphaManagerServer.log(Level.INFO, "Sending \"" + command + "\" command to client with id \"" + clientId + "\"");
                AlphaManagerServer.getInstance().getPublisher().publish(new Publisher.PendingMessage("alphamanager.client." + clientId, "command " + command));
                return;
            }

            subCommands.clear();

            if(split.get(1).toLowerCase().contains("owners")) {
                Set<String> owners = AlphaManagerServer.getInstance().getCacheConnector().getCacheResource().smembers("alphamanager:_config:owners");

                if(owners.size() < 1) {
                    AlphaManagerServer.log(Level.INFO, "No owner registered.");
                    return;
                } else AlphaManagerServer.log(Level.INFO, owners.size() + " owners registered. Owners list:");

                for (String stringUUID : owners) {
                    UUID uuid;

                    try {
                        uuid = UUID.fromString(stringUUID);
                    } catch(Exception ignored) {
                        AlphaManagerServer.log(Level.INFO, "Unable to convert string to UUID.");
                        continue;
                    }

                    AlphaManagerServer.log(Level.INFO, " - " + AlphaManagerServer.getInstance().getUuidTranslator().getName(uuid, true) + " (" + uuid.toString() + ")");
                }

                return;
            }

            if(split.get(1).toLowerCase().contains("addowner") && split.size() == 3) {
                String pseudo = split.get(2);
                UUID uuid;

                try {
                    uuid = AlphaManagerServer.getInstance().getUuidTranslator().getUUID(pseudo, true);
                    AlphaManagerServer.getInstance().getCacheConnector().getCacheResource().sadd("alphamanager:_config:owners", uuid.toString());
                    AlphaManagerServer.log(Level.INFO, "Owner \"" + pseudo + "\" registered.");
                } catch(Exception error) {
                    AlphaManagerServer.log(Level.ERROR, "Unable to get UUID for " + pseudo + ".");
                }

                return;
            }
        } else if(external) {
            AlphaManagerServer.log(Level.WARN, "Unauthorized user trying to execute command, cancelled.");
            return;
        }

        // Config subCommand
        if(message.contains("config") && !external) {
            List<String> split = Arrays.asList(StringUtils.split(message, " "));
            List<String> subCommands = new ArrayList<>();

            if((split.size() < 2 || split.get(1).toLowerCase().contains("help")) && display) {
                AlphaManagerServer.log(Level.INFO, "=~=~=~=~=~=~=~=~=~=~= CONFIG HELP PAGE =~=~=~=~=~=~=~=~=~=~=");
                AlphaManagerServer.log(Level.INFO, "config systems - Display list of systems.");
                AlphaManagerServer.log(Level.INFO, "config maps <system> - List of the map for the system.");
                AlphaManagerServer.log(Level.INFO, "config system params <system> - Display parameters for the the system.");
                AlphaManagerServer.log(Level.INFO, "=~=~=~=~=~=~=~=~=~=~= CONFIG HELP PAGE =~=~=~=~=~=~=~=~=~=~=");
                return;
            }

            if(split.get(1).toLowerCase().contains("systems") && split.size() == 2 && display) {
                Map<String, SystemEntity> registeredSystems = SystemEntity.getSystems();
                AlphaManagerServer.log(Level.INFO, registeredSystems.size() + " systems registered. Systems list:");

                registeredSystems.entrySet().forEach(entry -> {
                    SystemEntity systemEntity = entry.getValue();
                    AlphaManagerServer.log(Level.INFO, " - " + entry.getKey() + " | Local mode: " + (systemEntity.isLocal() ? ChatColor.GREEN : ChatColor.RED) + systemEntity.isLocal() + ChatColor.RESET);
                });

                return;
            }

            if(split.get(1).toLowerCase().contains("maps") && display) {
                String system = split.get(2).toLowerCase();

                if(!SystemEntity.getSystems().containsKey(split.get(2).toLowerCase())) {
                    AlphaManagerServer.log(Level.INFO, "System " + system + " does not exists.");
                    return;
                }

                SystemEntity systemEntity = SystemEntity.getSystems().get(system);
                AlphaManagerServer.log(Level.INFO, "Maps list for system \"" + system + "\":");

                systemEntity.getMaps().entrySet().forEach(entry -> {
                    MapEntity mapEntity = entry.getValue();
                    AlphaManagerServer.log(Level.INFO, " => Map \"" + mapEntity.getMapName() + "\"");
                    AlphaManagerServer.log(Level.INFO, "  Folder: " + mapEntity.getFolderName());
                    AlphaManagerServer.log(Level.INFO, "  Slots: " + mapEntity.getSlots());
                    AlphaManagerServer.log(Level.INFO, "  Free Slots: " + mapEntity.getReservedSlotsForBoot());
                    AlphaManagerServer.log(Level.INFO, "  RAM: " + mapEntity.getPhysicalMemory());
                    AlphaManagerServer.log(Level.INFO, "  Startup Mode: " + mapEntity.getMode().toString());
                });

                return;
            }

            if(split.size() >= 3 && split.get(1).toLowerCase().contains("system") && split.get(2).contains("params") && display) {
                String system = split.get(3).toLowerCase();

                if(!SystemEntity.getSystems().containsKey(system)) {
                    AlphaManagerServer.log(Level.INFO, "System " + system + " does not exists.");
                    return;
                }

                SystemEntity systemEntity = SystemEntity.getSystems().get(system);
                AlphaManagerServer.log(Level.INFO, "Parameters for system \"" + system + "\":");
                AlphaManagerServer.log(Level.INFO, " - Plugins: " + StringUtils.join(systemEntity.getPlugins(), ", "));
                AlphaManagerServer.log(Level.INFO, " - Maps: " + StringUtils.join(systemEntity.getBrutMaps(), ", "));
                AlphaManagerServer.log(Level.INFO, " - Local Mode: " + systemEntity.getLocal());
                AlphaManagerServer.log(Level.INFO, " - Starter Port: " + systemEntity.getStarterPort());
                return;
            }
        } else if(external) {
            AlphaManagerServer.log(Level.WARN, "Unauthorized user trying to execute command, cancelled.");
            return;
        }

        /*if(message.equalsIgnoreCase("proxylist")) {
            if(display) {
                Map<String, ProxyEntity> proxies = ProxyEntity.getProxies();

                if (proxies.size() == 0) {
                    AlphaManagerServer.log(Level.INFO, "No proxies is configured yet.");
                } else {
                    AlphaManagerServer.log(Level.INFO, proxies.size() + (proxies.size() > 1 ? " proxies was" : " proxy is") + " configured. Proxies list:");

                    proxies.entrySet().forEach(entry -> {
                        AlphaManagerServer.log(Level.INFO, ChatColor.YELLOW + "(" + entry.getValue().getProxyId() + ") " + entry.getKey() + ChatColor.RESET + " (" + ChatColor.BLUE + "online: " + ChatColor.GREEN + true + ChatColor.RESET + ", " + ChatColor.BLUE + "onlineplayers: " + ChatColor.GREEN + entry.getValue().getOnlinePlayers() + ChatColor.RESET + ")");
                    });
                }
            }

            return;
        }*/

        if(display)
            AlphaManagerServer.log(Level.INFO, "Unknown command. Type \"help\" to display help." + ChatColor.RESET);
    }

}
