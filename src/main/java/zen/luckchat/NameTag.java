package zen.luckchat;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;

import static zen.luckchat.LuckChatPlugin.*;

public class NameTag extends Thread {

    private LuckChatPlugin plugin;

    NameTag(LuckChatPlugin plugin) {
        this.plugin = plugin;
        setName("NameTag");
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers().values()) {
            String name = p.getDisplayName();

            String prefix = LuckChatPlugin.getPrefix(p);
            String suffix = LuckChatPlugin.getSuffix(p);
            String perm = LuckChatPlugin.getGroup(p);

            String tag = (LuckChatPlugin.config.getString("NameTag."+perm)
                    .replace("%name%", p.getName())
                    .replace("%disname%", name)
                    .replace("%prefix%", prefix)
                    .replace("%suffix%", suffix)
                    .replace("%group%", perm)
                    .replace("%device%", getOS(p))
                    .replace("%faction%", getFaction(p))
                    .replace("%money%", getMoney(p)));

            if (LuckChatPlugin.placeholderApi != null) {
                tag = LuckChatPlugin.placeholderApi.translateString(tag, p);
            }

            if (!p.getNameTag().equals(TextFormat.colorize('&', tag))) {
                p.setNameTag(TextFormat.colorize('&', tag));
            }
        }
    }
}
