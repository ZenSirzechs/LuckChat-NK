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
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.caching.MetaData;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;

public class LuckChatPlugin extends PluginBase implements Listener {

    static byte hook = 0;
    static PlaceholderAPI placeholderApi = null;
    public static Config config;
    private static String pf = (TextFormat.WHITE + "[ " + TextFormat.AQUA + "Luck" + TextFormat.DARK_AQUA + "Chat" + TextFormat.WHITE + " ]" + TextFormat.DARK_AQUA);

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

            if (hook == 1) {
                for (Group g : LuckPerms.getApi().getGroups()) {
                    String group = g.getName();
                    config.set("Chat." + group, "[%name%] >> %msg%");
                    config.set("NameTag." + group, "%name%");
                    getServer().getLogger().info(pf + TextFormat.LIGHT_PURPLE + " Fetching group: " + group);
                }
            } else {
                for (net.luckperms.api.model.group.Group g : LuckPermsProvider.get().getGroupManager().getLoadedGroups()) {
                    String group = g.getName();
                    config.set("Chat." + group, "[%name%] >> %msg%");
                    config.set("NameTag." + group, "%name%");
                    getServer().getLogger().info(pf + TextFormat.LIGHT_PURPLE + " Fetching group: " + group);
                }
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
    public void onChat(PlayerChatEvent e) {
        String message = e.getMessage();
        Player p = e.getPlayer();
        String name = p.getDisplayName();;

        String prefix = getPrefix(p);
        String suffix = getSuffix(p);
        String perm = getGroup(p);

        String msg = (config.getString("Chat."+perm)
                .replace("%name%", p.getName())
                .replace("%disname%", name)
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix)
                .replace("%group%", perm)
                .replace("%money%", getMoney(p))
                .replace("%faction%", getFaction(p))
                .replace("%device%", getOS(p))
                .replace("%msg%", message));

        if (placeholderApi != null) {
            msg = placeholderApi.translateString(msg, p);
        }

        if (config.getBoolean("ChatAsync")) {
            e.setCancelled();
            String finalMsg = msg;
            
            this.getServer().getScheduler().scheduleTask(() -> {
                for (Player player : this.getServer().getOnlinePlayers().values()) {
                    player.sendMessage(TextFormat.colorize('&', finalMsg));
                }
                Server.getInstance().getLogger().info(TextFormat.colorize('&', finalMsg));
            }, true);

        } else {
            e.setFormat(TextFormat.colorize('&', msg));
        }
    }

    static String getMoney(Player p) {
        try {
            Class.forName("me.onebone.economyapi.EconomyAPI");
            return Double.toString(me.onebone.economyapi.EconomyAPI.getInstance().myMoney(p));
        } catch (Exception ex) {
            return "EconomyAPI not found";
        }
    }

    static String getFaction(Player p) {
        try {
            Class.forName("com.massivecraft.factions.P");
            return com.massivecraft.factions.P.p.getPlayerFactionTag(p);
        } catch (Exception ex) {
            return "Faction not found";
        }
    }
    
    static String getOS(Player p) {
        switch (p.getLoginChainData().getDeviceOS()) {
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

        if (this.getServer().getPluginManager().getPlugin("LuckPerms") != null) {

            try {
                Class.forName("me.lucko.luckperms.LuckPerms");
                this.getServer().getLogger().info(TextFormat.GREEN + "LuckPerms v4 > detected ...");
                hook = 1;
            } catch (ClassNotFoundException e) {
                this.getServer().getLogger().info(TextFormat.GREEN + "LuckPerms v5 < detected...");
            }

        } else {
            this.getServer().getLogger().error(TextFormat.RED + "LuckPerms not found! Disabling ...");
            this.getServer().getPluginManager().disablePlugin(this);
        }

    }

    static String getGroup(Player p) {

        String group = "";

        try {
            if (hook == 1) {
                Class.forName("me.lucko.luckperms.LuckPerms");
                group = LuckPerms.getApi().getUser(p.getUniqueId()).getPrimaryGroup();
            } else {
                Class.forName("net.luckperms.api.LuckPerms");
                group = net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId()).getPrimaryGroup();
            }
        } catch (ClassNotFoundException ignored) {}

        return group;
    }

    static String getPrefix(Player p) {

        String prefix = "";

        try {
            if (hook == 1) {
                Class.forName("me.lucko.luckperms.LuckPerms");
                MetaData metaData = LuckPerms.getApi().getUser(p.getUniqueId()).getCachedData().getMetaData(LuckPerms.getApi().getContextManager().getApplicableContexts(p));
                prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
            } else {
                Class.forName("net.luckperms.api.LuckPerms");
                CachedMetaData metaData = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData(LuckPermsProvider.get().getContextManager().getQueryOptions(p));
                prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
            }
        } catch (ClassNotFoundException ignored) {}

        return prefix;
    }

    static String getSuffix(Player p) {

        String suffix = "";

        try {
            if (hook == 1) {
                Class.forName("me.lucko.luckperms.LuckPerms");
                MetaData metaData = LuckPerms.getApi().getUser(p.getUniqueId()).getCachedData().getMetaData(LuckPerms.getApi().getContextManager().getApplicableContexts(p));
                suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
            } else {
                Class.forName("net.luckperms.api.LuckPerms");
                CachedMetaData metaData = LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData(LuckPermsProvider.get().getContextManager().getQueryOptions(p));
                suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
            }
        } catch (ClassNotFoundException ignored) {}

        return suffix;
    }
}
