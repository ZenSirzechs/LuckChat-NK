package zen.luckchat;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import com.massivecraft.factions.P;

import java.util.Optional;

public class Main extends PluginBase implements Listener {

    Config config;
    Config groups;

    public void onEnable() {
        this.saveDefaultConfig();
        this.config = this.getConfig();

        //Gonna do this in the next version
        //PlaceholderAPIDownloader.checkAndRun(this);

        groups = new Config(getDataFolder() + "/groups.yml", Config.YAML);
        if (config.getBoolean("FirstRun")){
            this.getServer().getLogger().info(TextFormat.AQUA+"Configuring first run...");
            Optional<LuckPermsApi> api = LuckPerms.getApiSafe();
            for (Group g : api.get().getGroups()) {
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
        LuckPermsApi api = LuckPerms.getApi();
        User user = api.getUser(p.getUniqueId());
        Contexts contexts = api.getContextManager().getApplicableContexts(p);
        MetaData metaData = user.getCachedData().getMetaData(contexts);

        String prefix = metaData.getPrefix();
        String suffix = metaData.getSuffix();
        String sf = suffix != null ? suffix : "";
        String pf = prefix != null ? prefix : "";

        Optional<LuckPermsApi> apiSafe = LuckPerms.getApiSafe();
        for (Group g : apiSafe.get().getGroups()) {
            String perm = api.getUser(p.getUniqueId()).getPrimaryGroup();
            String msg = (groups.getString(perm)
                    .replace("%name%", p.getName())
                    .replace("%disname%", name)
                    .replace("%prefix%", pf)
                    .replace("%suffix%", sf)
                    .replace("%msg%", message));
            if (this.getServer().getPluginManager().getPlugin("Factions") == null){
                e.setFormat(TextFormat.colorize('&', msg).replace("%faction%", ""));
            } else {
                String faction = P.p.getPlayerFactionTag(p);
                e.setFormat(TextFormat.colorize('&', msg).replace("%faction%", faction));
            }
        }
    }
}
