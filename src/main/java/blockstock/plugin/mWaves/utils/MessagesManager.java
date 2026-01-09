package blockstock.plugin.mWaves.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import blockstock.plugin.mWaves.mWaves;
import blockstock.plugin.mWaves.utils.Runner.Runner;

import java.io.File;

public class MessagesManager {
    private final mWaves plugin;
    private Runner runner;
    private FileConfiguration messagesconfig;
    private File messagesConfigFile;
    private String language;

    public MessagesManager(mWaves plugin, Runner runner) {
        this.runner = runner;
        this.plugin = plugin;
        this.language = plugin.getConfigManager().getLanguage();
        loadFiles();
    }

    private void loadFiles() {
        // Artık mesaj dosyalarını messages_en.yml veya messages_tr.yml olarak arayacak
        messagesConfigFile = new File(plugin.getDataFolder(),
                "messages_" + language + ".yml"
        );

        if (!messagesConfigFile.exists()) {
            // Eğer dosya yoksa varsayılan olarak messages_en.yml kaydedecek
            plugin.saveResource("messages_en.yml", false);
        }
        reloadMessages();
    }

    public void reloadMessages() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (messagesConfigFile == null) {
                messagesConfigFile = new File(plugin.getDataFolder(), "messages_en.yml");
            }
            if (messagesConfigFile.exists()) {
                messagesconfig = YamlConfiguration.loadConfiguration(messagesConfigFile);
            } else {
                plugin.getLogger().warning("Messages file not found: " + messagesConfigFile.getPath());
            }
        });
    }

    private String getMessage(String path, String defaultValue) {
        if (messagesconfig == null) return defaultValue;
        return messagesconfig.getString(path, defaultValue);
    }

    public String nopermission() {
        return getMessage("system.no-permission", "&cYou do not have permission.");
    }
    public String reloadplugin() {
        return getMessage("system.reload-plugin", "&aPlugin reloaded successfully!");
    }
    public String playeronly() {
        // Buradaki anahtarı "player-only" olarak düzelttim (config ile uyumlu olması için)
        return getMessage("system.player-only", "&cThis command is only available to players!");
    }

    public String info(String count, String currentWave) {
        return getMessage("another.info", "&e%count% entities remaining.")
                .replace("%count%", count)
                .replace("%wave%", currentWave);
    }

    public String highlightSuccess(int count, String visibility) {
        return getMessage("highlight.success", "&a%count% mobs highlighted %visibility%! Highlight will vanish in 10 seconds.")
                .replace("%count%", String.valueOf(count))
                .replace("%visibility%", visibility);
    }

    public String highlightVisibilityAdmin() {
        return getMessage("highlight.visibility-admin", "only for you");
    }

    public String highlightVisibilityAll() {
        return getMessage("highlight.visibility-all", "for all players");
    }
}