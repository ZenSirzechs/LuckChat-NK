package zen.luckchat;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;

import static zen.luckchat.LuckChatPlugin.*;

public class NameTag extends Thread {

    private final LuckChatPlugin plugin;

    NameTag(LuckChatPlugin plugin) {
        this.plugin = plugin;
        setName("NameTag");
    }

    @Override
    public void run() {
        for (Player player : plugin.getServer().getOnlinePlayers().values()) {

            String perm = LuckChatPlugin.getGroup(player);

            String tag = (LuckChatPlugin.config.getString("NameTag." + perm)
                    .replace("%%n", "\n")
                    .replace("%%r", "\r")
                    .replace("%%t", "\t")
                    .replace("%name%", player.getName())
                    .replace("%disname%", player.getDisplayName())
                    .replace("%prefix%", LuckChatPlugin.getPrefix(player))
                    .replace("%suffix%", LuckChatPlugin.getSuffix(player))
                    .replace("%group%", perm)
                    .replace("%device%", getOS(player))
                    .replace("%faction%", getFaction(player))
                    .replace("%money%", getMoney(player)));

            if (LuckChatPlugin.placeholderApi != null) {
                tag = LuckChatPlugin.placeholderApi.translateString(tag, player);
            }

            if (!player.getNameTag().equals(TextFormat.colorize('&', tag))) {
                player.setNameTag(TextFormat.colorize('&', tag));
            }
        }
    }
}
