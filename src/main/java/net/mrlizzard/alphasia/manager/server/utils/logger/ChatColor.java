package net.mrlizzard.alphasia.manager.server.utils.logger;


import net.mrlizzard.alphasia.manager.server.AlphaManagerServer;

public class ChatColor {

    // ChatColor fields
    public static final String      RESET        = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[0m" : "";
    public static final String      BLACK        = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[30m" : "";
    public static final String      RED         = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[31m" : "";
    public static final String      GREEN        = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[32m" : "";
    public static final String      YELLOW       = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[33m" : "";
    public static final String      BLUE         = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[34m" : "";
    public static final String      PURPLE       = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[35m" : "";
    public static final String      CYAN         = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[36m" : "";
    public static final String      WHITE        = !AlphaManagerServer.getInstance().isWindowsOs() ? "\u001B[37m" : "";

}
