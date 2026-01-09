package blockstock.plugin.mWaves.managers.configuration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import blockstock.plugin.mWaves.mWaves;
import blockstock.plugin.mWaves.managers.Runner.Runner;

import java.io.File;

public class MessagesManager {
    private final mWaves plugin;
    private Runner runner;
    private FileConfiguration messagesconfig;
    private File messagesConfigFile;

    public MessagesManager(mWaves plugin, Runner runner) {
        this.runner = runner;
        this.plugin = plugin;
        loadFiles();
    }

    private void loadFiles() {
        // Renamed to messages_en.yml for consistency with the new language
        messagesConfigFile = new File(plugin.getDataFolder(), "messages_en.yml");
        if (!messagesConfigFile.exists()) {
            plugin.saveResource("messages_en.yml", false);
        }
        reloadMessages();
    }

    public void reloadMessages() {
        if (messagesConfigFile == null) {
            messagesConfigFile = new File(plugin.getDataFolder(), "messages_en.yml");
        }
        if (messagesConfigFile.exists()) {
            messagesconfig = YamlConfiguration.loadConfiguration(messagesConfigFile);
        } else {
            plugin.getLogger().warning("Messages file not found: " + messagesConfigFile.getPath());
        }
    }

    private String getMessage(String path, String defaultValue) {
        if (messagesconfig == null) return defaultValue;
        return messagesconfig.getString(path, defaultValue);
    }

    public String nopermission() {
        return getMessage("system.no-permission", "&cYou do not have permission to perform this action.");
    }

    public String reloadplugin() {
        return getMessage("system.reload-plugin", "&aPlugin configuration successfully reloaded.");
    }

    public String playeronly() {
        return getMessage("system.player-only", "&cThis command can only be executed by a player!");
    }

    public String info(String count, String currentWave) {
        return getMessage("another.info", "&eRemaining: &a%count% &emobs on Wave &6#%wave%&e.")
                .replace("%count%", count)
                .replace("%wave%", currentWave);
    }

    public String highlightSuccess(int count, String visibility) {
        return getMessage("highlight.success", "&aHighlighted %count% mobs %visibility%! Glowing will fade in 10 seconds.")
                .replace("%count%", String.valueOf(count))
                .replace("%visibility%", visibility);
    }

    public String highlightVisibilityAdmin() {
        return getMessage("highlight.visibility-admin", "(visible only to you)");
    }

    public String highlightVisibilityAll() {
        return getMessage("highlight.visibility-all", "(visible to everyone)");
    }
}