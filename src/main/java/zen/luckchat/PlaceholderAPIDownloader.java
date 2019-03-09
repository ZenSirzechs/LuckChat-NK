package zen.luckchat;
import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.TextFormat;

import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class PlaceholderAPIDownloader {

    public static void checkAndRun(Plugin plugin) {
        Server server = plugin.getServer();

        if (server.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return;
        }

        plugin.getLogger().warning(TextFormat.RED+"PlaceholderAPI not found!");
        plugin.getLogger().info(TextFormat.DARK_GREEN+"Downloading PlaceholderAPI...");

        String placeholderApi = server.getFilePath() + "/plugins/PlaceholderAPI.jar";

        try {
            URL website = new URL("https://sites.google.com/site/downloadcenterpetterim1/PlaceholderAPI.jar");
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(placeholderApi);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
        } catch (Exception e) {
            server.getLogger().logException(e);
            server.getPluginManager().disablePlugin(plugin);
            return;
        }

        plugin.getLogger().info(TextFormat.AQUA+"PlaceholderAPI downloaded successfully!");
        server.getPluginManager().loadPlugin(placeholderApi);
    }
}