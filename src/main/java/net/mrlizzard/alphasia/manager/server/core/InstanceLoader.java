package net.mrlizzard.alphasia.manager.server.core;

import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;
import net.mrlizzard.alphasia.manager.server.core.entity.SystemEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

import java.util.*;

public class InstanceLoader {

    private AlphaManagerServer instance;

    public InstanceLoader(AlphaManagerServer instance) {
        this.instance = instance;

        this.loadAllSystems();
    }

    private void loadAllSystems() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                String configurationKey = "alphamanager:_config:";
                String configurationSystems = ((String) instance.getCoreConfiguration().get("systems"));
                List<String> systems = new ArrayList<>();
                systems.addAll(Arrays.asList(StringUtils.split(configurationSystems, ",")));

                systems.forEach(systemName -> {
                    Map<String, String> system = instance.getCacheConnector().getCacheResource().hgetAll(configurationKey + "systems:" + systemName.toLowerCase());

                    try {
                        String name = systemName.toLowerCase();
                        boolean local = Boolean.getBoolean(system.get("local"));
                        int starterPort = Integer.parseInt(system.get("starter-port"));

                        // On l'enregistre
                        AlphaManagerServer.log(Level.INFO, "New system entity registered. (" + name + ")");
                        new SystemEntity(name, local, starterPort, system.get("maps"), system.get("plugins"));
                    } catch(Exception error) {
                        AlphaManagerServer.log(Level.INFO, "Error during getting remote system configuration. " + error.getMessage());
                        return;
                    }
                });
            }
        };

        new Timer().schedule(task, 1000); // On lance l'action après 1 seconde pour être sûr que le système soit prêt
    }

}
