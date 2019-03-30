package zen.luckchat;

import cn.nukkit.Player;

import cn.nukkit.utils.TextFormat;
import com.massivecraft.factions.P;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;

import static zen.luckchat.LuckChatPlugin.getMoney;

public class NameTag extends Thread {

    private LuckChatPlugin plugin;

    public NameTag(LuckChatPlugin plugin) {
        this.plugin = plugin;
        setName("NameTag");
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers().values()) {
            String name = p.getDisplayName();
            User user = LuckChatPlugin.luckPerms.getUser(p.getUniqueId());
            if (user == null) {
                plugin.getLogger().warning("An error occurred when attempting to retrieve " + p.getName() + "'s user data!");
                return;
            }
            Contexts contexts = LuckChatPlugin.luckPerms.getContextManager().getApplicableContexts(p);
            MetaData metaData = user.getCachedData().getMetaData(contexts);

            String prefix = metaData.getPrefix();
            String suffix = metaData.getSuffix();
            suffix = suffix != null ? suffix : "";
            prefix = prefix != null ? prefix : "";

            String perm = user.getPrimaryGroup();
            String tag = (LuckChatPlugin.config.getString("NameTag."+perm)
                    .replace("%name%", p.getName())
                    .replace("%disname%", name)
                    .replace("%prefix%", prefix)
                    .replace("%suffix%", suffix)
                    .replace("%group%", perm)
                    .replace("%money%", getMoney(p)));

            if (LuckChatPlugin.placeholderApi != null) {
                tag = LuckChatPlugin.placeholderApi.translateString(tag, p);
            }
            if (LuckChatPlugin.factions != null) {
                String faction = P.p.getPlayerFactionTag(p);
                tag = tag.replace("%faction%", faction);
            } else {
                tag = tag.replace("%faction%", "");
            }
            p.setNameTag(TextFormat.colorize('&', tag));
        }
    }
}
