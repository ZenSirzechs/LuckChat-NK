package zen.luckchat;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;

public class LuckChatPlugin extends PluginBase implements Listener {

    static LuckPermsApi luckPerms = null;
    static PlaceholderAPI placeholderApi = null;
    static Config config;
    private static String pf = (TextFormat.WHITE + "[ " + TextFormat.AQUA + "Luck" + TextFormat.DARK_AQUA + "Chat" + TextFormat.WHITE + " ]" + TextFormat.DARK_AQUA);

    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        // Get LuckPerms API
        try {
            luckPerms = LuckPerms.getApiSafe().orElse(null);
        } catch (Throwable e) {
            getLogger().emergency("Unable to get LuckPerms API! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for Placeholder API
        try {
            placeholderApi = PlaceholderAPI.getInstance();
        } catch (Throwable ignored) {
        }

        if (config.getBoolean("FirstRun", true)){
            getServer().getLogger().info("\n" + pf + " Configuring first run... \n\n");
            for (Group g : luckPerms.getGroups()) {
                String group = g.getName();
                config.set("Chat."+group, "[%name%] >> %msg%");
                config.set("NameTag."+group, "%name%");
                getServer().getLogger().info(pf + TextFormat.LIGHT_PURPLE+" Fetching group: " + group);
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
        Player p = e.getPlayer();
        String name = p.getDisplayName();
        String message = e.getMessage();

        User user = luckPerms.getUser(p.getUniqueId());
        if (user == null) {
            getLogger().warning("An error occurred when attempting to retrieve " + p.getName() + "'s user data!");
            return;
        }
        MetaData metaData = user.getCachedData().getMetaData(luckPerms.getContextManager().getApplicableContexts(p));
        String prefix = metaData.getPrefix() != null ? metaData.getPrefix() : "";
        String suffix = metaData.getSuffix() != null ? metaData.getSuffix() : "";
        String perm = user.getPrimaryGroup();

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
        e.setFormat(TextFormat.colorize('&', msg));
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
}
