package zen.luckchat;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;

public class LuckChatPlugin extends PluginBase implements Listener {

    static PlaceholderAPI placeholderApi = null;
    static LuckPerms api;
    public static Config config;
    private static final String pf = (TextFormat.WHITE + "[ " + TextFormat.AQUA + "Luck" + TextFormat.DARK_AQUA + "Chat" + TextFormat.WHITE + " ]" + TextFormat.DARK_AQUA);

    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        checkAPIHook();

        // Check for Placeholder API
        try {
            placeholderApi = PlaceholderAPI.getInstance();
        } catch (Throwable ignored) {
        }

        if (config.getBoolean("FirstRun", true)) {
            getServer().getLogger().info("\n" + pf + " Configuring first run... \n\n");


            for (Group g : api.getGroupManager().getLoadedGroups()) {
                String group = g.getName();
                config.set("Chat." + group, "[%name%] >> %msg%");
                config.set("NameTag." + group, "%name%");
                getServer().getLogger().info(pf + TextFormat.LIGHT_PURPLE + " Fetching group: " + group);
            }

            config.set("FirstRun", false);
            config.save();
            getServer().getLogger().info(pf + " First run configured. \n\n");
        }

        getServer().getScheduler().scheduleDelayedRepeatingTask(this, new NameTag(this),
                config.getInt("NameTag.update", 20),
                config.getInt("NameTag.update", 20),
                config.getBoolean("NameTag.updateAsync", true));
        getServer().getLogger().info("\n" + pf + " Loaded " + config.getSection("Chat").getKeys().size()
                + " chat formats and " + (config.getSection("NameTag").getKeys().size() - 2)
                + " NameTag formats. \n" + pf + " Starting chat listener.. \n\n");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(PlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        String perm = getGroup(player);

        String msg = (config.getString("Chat."+perm)
                .replace("%%n", "\n")
                .replace("%%r", "\r")
                .replace("%%t", "\t")
                .replace("%name%", player.getName())
                .replace("%disname%", player.getDisplayName())
                .replace("%prefix%", getPrefix(player))
                .replace("%suffix%", getSuffix(player))
                .replace("%group%", perm)
                .replace("%money%", getMoney(player))
                .replace("%faction%", getFaction(player))
                .replace("%device%", getOS(player))
                .replace("%msg%", message));

        if (placeholderApi != null) {
            msg = placeholderApi.translateString(msg, player);
        }

        if (config.getBoolean("ChatAsync")) {
            event.setCancelled();
            String finalMsg = msg;
            
            this.getServer().getScheduler().scheduleTask(() -> {
                for (Player p : this.getServer().getOnlinePlayers().values()) {
                    p.sendMessage(TextFormat.colorize('&', finalMsg));
                }
                Server.getInstance().getLogger().info(TextFormat.colorize('&', finalMsg));
            }, true);

        } else {
            event.setFormat(TextFormat.colorize('&', msg));
        }
    }

    static String getMoney(Player player) {
        try {
            Class.forName("me.onebone.economyapi.EconomyAPI");
            return Double.toString(me.onebone.economyapi.EconomyAPI.getInstance().myMoney(player));
        } catch (Exception ex) {
            return "EconomyAPI not found";
        }
    }

    static String getFaction(Player player) {
        try {
            Class.forName("com.massivecraft.factions.P");
            return com.massivecraft.factions.P.p.getPlayerFactionTag(player);
        } catch (Exception ex) {
            return "Faction not found";
        }
    }
    
    static String getOS(Player player) {
        switch (player.getLoginChainData().getDeviceOS()) {
            case 1:
                return "Android";
            case 2:
                return "iOS";
            case 3:
                return "Mac";
            case 4:
                return "Fire";
            case 5:
                return "Gear VR";
            case 6:
                return "HoloLens";
            case 7:
                return "Windows 10";
            case 8:
                return "Windows";
            case 9:
                return "Dedicated";
            case 10:
                return "tvOS";
            case 11:
                return "PlayStation";
            case 12:
                return "NX";
            case 13:
                return "Xbox";
            default:
                return "Unknown";
        }
    }

    void checkAPIHook() {

        if (this.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            this.getServer().getLogger().error(TextFormat.RED + "LuckPerms not found! Disabling ...");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            api = LuckPermsProvider.get();
        }

    }

    static String getGroup(Player player) {
        return  api.getUserManager().getUser(player.getUniqueId()).getPrimaryGroup();
    }

    static String getPrefix(Player player) {

        CachedMetaData metaData = api.getUserManager()
                .getUser(player.getUniqueId()).getCachedData()
                .getMetaData(api.getContextManager().getQueryOptions(player));

        return metaData.getPrefix() != null ? metaData.getPrefix() : "";
    }

    static String getSuffix(Player player) {

        CachedMetaData metaData = api.getUserManager()
                .getUser(player.getUniqueId()).getCachedData()
                .getMetaData(api.getContextManager().getQueryOptions(player));

        return metaData.getSuffix() != null ? metaData.getSuffix() : "";
    }
}
