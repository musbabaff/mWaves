package blockstock.plugin.mWaves;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import blockstock.plugin.mWaves.main.CommandManager;
import blockstock.plugin.mWaves.main.Expension;
import blockstock.plugin.mWaves.main.WavesManager;
import blockstock.plugin.mWaves.utils.ConfigManager;
import blockstock.plugin.mWaves.utils.GlowingManager;
import blockstock.plugin.mWaves.utils.MessagesManager;
import blockstock.plugin.mWaves.utils.Runner.PaperRunner;
import blockstock.plugin.mWaves.utils.Runner.Runner;
import blockstock.plugin.mWaves.utils.Utils;

@Getter
public final class mWaves extends JavaPlugin {

    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private Utils utils;
    private GlowingManager glowingManager;
    private final Runner runner = new PaperRunner(this);
    private CommandManager commandsManager;
    private WavesManager wavesManager;

    @Override
    public void onEnable() {
        try {
            loadingConfiguration();
            configManager.setupColorizer();
            registerCommands();
            registerExpensions();
            getLogger().info("mWaves has been successfully enabled!");
        } catch (Exception e) {
            getLogger().severe("CRITICAL ERROR DURING STARTUP! Disabling plugin...");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (wavesManager != null) {
            wavesManager.cancelHighlightTasks();

            if (wavesManager.isActive()) {
                wavesManager.stopWaves();
            }
        }
        getLogger().info("mWaves has been safely disabled.");
    }

    private void loadingConfiguration() {
        try {
            configManager = new ConfigManager(this, runner);
            utils = new Utils();
            messagesManager = new MessagesManager(this, runner);
            glowingManager = new GlowingManager(this);
            commandsManager = new CommandManager(this, runner);
            wavesManager = new WavesManager(this, runner);
        } catch (Exception e) {
            getLogger().severe("ERROR LOADING CONFIGURATION! Shutting down...");
            e.printStackTrace();
            throw e;
        }
    }

    private void registerCommands() {
        // plugin.yml içindeki isimle (mWaves) eşleştiğinden emin oluyoruz
        if (getCommand("mWaves") != null) {
            getCommand("mWaves").setExecutor(commandsManager);
        } else {
            getLogger().severe("Command 'mWaves' not found in plugin.yml!");
        }
    }

    private void registerExpensions() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new Expension(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully.");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not function.");
        }
    }

    public void reloadPluginConfig() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                messagesManager.reloadMessages();

                boolean wasActive = wavesManager.isActive();
                if (wasActive) {
                    wavesManager.stopWaves();
                    getLogger().info("Active waves stopped for configuration reload.");
                }

                wavesManager.loadWaves();
                configManager.setupColorizer();

                getLogger().info("Configuration reloaded successfully.");
            } catch (Exception e) {
                getLogger().severe("Error while reloading configuration: " + e.getMessage());
                if (configManager.getDebug()) {
                    e.printStackTrace();
                }
            }
        });
    }
}