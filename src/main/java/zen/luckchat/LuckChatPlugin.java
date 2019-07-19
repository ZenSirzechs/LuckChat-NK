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
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;

public class LuckChatPlugin extends PluginBase implements Listener {

    static LuckPermsApi luckPerms = null;
    static PlaceholderAPI placeholderApi = null;
    static Config config;

    public void onEnable() {
        this.saveDefaultConfig();
        config = this.getConfig();

        // Get LuckPerms API
        try {
            luckPerms = LuckPerms.getApiSafe().orElse(null);
        } catch (Throwable e) {
            // ignore
        }
        if (luckPerms == null) {
            this.getLogger().emergency("Unable to get LuckPerms API! Disabling...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for Placeholder API
        try {
            placeholderApi = PlaceholderAPI.getInstance();
        } catch (Throwable e) {
            // ignore
        }

        if (config.getBoolean("FirstRun")){
            this.getServer().getLogger().info(TextFormat.AQUA+"Configuring first run...");

            for (Group g : luckPerms.getGroups()) {
                String group = g.getName();
                config.set("Chat."+group, "[%name%] >> %msg%");
                config.set("NameTag."+group, "%name%");
                this.getServer().getLogger().info(TextFormat.LIGHT_PURPLE+"Fetching group: " + group);
                config.set("FirstRun", false);
                config.save();
            }
        }
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getScheduler().scheduleDelayedRepeatingTask(this, new NameTag(this), config.getInt("NameTag.update", 20), config.getInt("NameTag.update", 20));
        this.getServer().getLogger().info(TextFormat.AQUA+"Starting chat listener..");
    }

    @EventHandler
    public void onChat(PlayerChatEvent e) {
        Player p = e.getPlayer();
        String name = p.getDisplayName();
        String message = e.getMessage();
        User user = luckPerms.getUser(p.getUniqueId());
        if (user == null) {
            this.getLogger().warning("An error occurred when attempting to retrieve " + p.getName() + "'s user data!");
            return;
        }
        Contexts contexts = luckPerms.getContextManager().getApplicableContexts(p);
        MetaData metaData = user.getCachedData().getMetaData(contexts);

        String prefix = metaData.getPrefix();
        String suffix = metaData.getSuffix();
        suffix = suffix != null ? suffix : "";
        prefix = prefix != null ? prefix : "";

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
