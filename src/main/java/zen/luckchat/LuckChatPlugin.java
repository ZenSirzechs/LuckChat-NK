package zen.luckchat;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.massivecraft.factions.P;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;

public class LuckChatPlugin extends PluginBase implements Listener {

    private LuckPermsApi luckPerms = null;
    private PlaceholderAPI placeholderApi = null;
    private P factions = null;
    private Config config;
    private Config groups;

    public void onEnable() {
        this.saveDefaultConfig();
        this.config = this.getConfig();

        // Get LuckPerms API
        try {
            luckPerms = LuckPerms.getApiSafe().orElse(null);
        } catch (Exception e) {
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
        } catch (Exception e) {
            // ignore
        }

        // Check for Factions
        try {
            factions = P.p;
        } catch (Exception e) {
            // ignore
        }

        groups = new Config(getDataFolder() + "/groups.yml", Config.YAML);
        if (config.getBoolean("FirstRun")){
            this.getServer().getLogger().info(TextFormat.AQUA+"Configuring first run...");

            for (Group g : luckPerms.getGroups()) {
                String group = g.getName();
                groups.set(group, "[%name%] >> %msg%");
                this.getServer().getLogger().info(TextFormat.LIGHT_PURPLE+"Fetching group: " + group);
                config.set("FirstRun", false);
                config.save();
                groups.save();
            }
        }
        this.getServer().getPluginManager().registerEvents(this, this);
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
        String msg = (groups.getString(perm)
                .replace("%name%", p.getName())
                .replace("%disname%", name)
                .replace("%prefix%", prefix)
                .replace("%suffix%", suffix)
                .replace("%money%", getMoney(p))
                .replace("%msg%", message));

        if (placeholderApi != null) {
            msg = placeholderApi.translateString(msg);
        }
        if (factions != null) {
            String faction = P.p.getPlayerFactionTag(p);
            msg = msg.replace("%faction%", faction);
        } else {
            msg = msg.replace("%faction%", "");
        }
        e.setFormat(TextFormat.colorize('&', msg));
    }

    public static String getMoney(Player p) {
        try {
            Class.forName("me.onebone.economyapi.EconomyAPI");
            return Double.toString(me.onebone.economyapi.EconomyAPI.getInstance().myMoney(p));
        } catch (Exception ex) {
            return "EconomyAPI not found";
        }
    }
}
