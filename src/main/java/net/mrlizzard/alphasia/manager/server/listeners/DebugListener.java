package net.mrlizzard.alphasia.manager.server.listeners;

/**
 * DebugListener class.
 * @author MrLizzard
 */
public class DebugListener {

    public static boolean enabled;

    public DebugListener() {
        //enabled = (FarosClient.getInstance().getInstanceSettingsConfig().getMode().equals(Mode.DEBUG) || FarosServers.getInstance().getInstanceSettingsConfig().getMode().equals(Mode.DEVELOPMENT));
        enabled = true;
    }

}
