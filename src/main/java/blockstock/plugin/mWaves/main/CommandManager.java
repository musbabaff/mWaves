package blockstock.plugin.mWaves.main;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import blockstock.plugin.mWaves.mWaves;
import blockstock.plugin.mWaves.utils.Runner.Runner;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor {

    private final mWaves plugin;
    private Runner runner;

    public CommandManager(mWaves plugin, Runner runner) {
        this.plugin = plugin;
        this.runner = runner;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            send(sender, "§cUsage: /mwaves <reload|start|stop|info|force_start|highlight>");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload":
                if (!sender.hasPermission("mwaves.admin")) {
                    send(sender, plugin.getMessagesManager().nopermission());
                    return true;
                }
                try {
                    plugin.reloadPluginConfig();
                    send(sender, plugin.getMessagesManager().reloadplugin());
                } catch (Exception e) {
                    send(sender, "§4§lError reloading configuration: " + e.getMessage());
                    if (plugin.getConfigManager().getDebug()) {
                        e.printStackTrace();
                    }
                }
                return true;
            case "start":
                if (!sender.hasPermission("mwaves.admin")) {
                    send(sender, plugin.getMessagesManager().nopermission());
                    return true;
                }
                if (plugin.getWavesManager().isActive()) {
                    send(sender, "§cWaves are already active! Use /mwaves stop to end them.");
                    return true;
                }
                boolean started = plugin.getWavesManager().startWaves();
                if (started) {
                    send(sender, "§aMob waves started!");
                } else {
                    send(sender, "§cFailed to start waves! Please check config and logs.");
                }
                return true;
            case "stop":
                if (!sender.hasPermission("mwaves.admin")) {
                    send(sender, plugin.getMessagesManager().nopermission());
                    return true;
                }
                if (!plugin.getWavesManager().isActive()) {
                    send(sender, "§cWaves are not active!");
                    return true;
                }
                plugin.getWavesManager().stopWaves();
                send(sender, "§aWaves stopped!");
                return true;
            case "forcestart", "force_start":
                if (!sender.hasPermission("mwaves.admin")) {
                    send(sender, plugin.getMessagesManager().nopermission());
                    return true;
                }
                if (args.length < 2) {
                    send(sender, "§cUsage: /mwaves force_start <wave_number>");
                    return true;
                }
                if (plugin.getWavesManager().isActive()) {
                    send(sender, "§cWaves are already active! Use /mwaves stop to end them.");
                    return true;
                }
                try {
                    int waveNumber = Integer.parseInt(args[1]);
                    boolean forceStarted = plugin.getWavesManager().startWavesFromWave(waveNumber);
                    if (forceStarted) {
                        send(sender, "§aMob waves started from wave #" + waveNumber + "!");
                    } else {
                        send(sender, "§cCould not start waves from wave #" + waveNumber + "! Check config and logs.");
                    }
                } catch (NumberFormatException e) {
                    send(sender, "§cError: '" + args[1] + "' is not a valid number!");
                }
                return true;
            case "info":
                if (!sender.hasPermission(plugin.getConfigManager().getPermissionInfo())) {
                    send(sender, plugin.getMessagesManager().nopermission());
                    return true;
                }
                if (!plugin.getWavesManager().isActive()) {
                    send(sender, "§eWaves are not active.");
                    return true;
                }
                int remaining = plugin.getWavesManager().getRemainingMobsCount();
                int currentWave = plugin.getWavesManager().getCurrentWaveNumber();
                send(sender, plugin.getMessagesManager().info(String.valueOf(remaining), String.valueOf(currentWave)));
                return true;
            case "highlight":
                String highlightMode = plugin.getConfigManager().getHighlightMode();

                if ("ADMIN".equalsIgnoreCase(highlightMode)) {
                    if (!sender.hasPermission("mwaves.admin")) {
                        send(sender, plugin.getMessagesManager().nopermission());
                        return true;
                    }
                } else {
                    if (!sender.hasPermission("mwaves.highlight")) {
                        send(sender, plugin.getMessagesManager().nopermission());
                        return true;
                    }
                }

                if (!(sender instanceof Player)) {
                    send(sender, plugin.getMessagesManager().playeronly());
                    return true;
                }
                if (!plugin.getWavesManager().isActive()) {
                    send(sender, "§cWaves are not active!");
                    return true;
                }

                Player player = (Player) sender;
                List<Entity> mobs = plugin.getWavesManager().getActiveMobEntities();

                if (mobs.isEmpty()) {
                    send(sender, "§cNo active mobs to highlight!");
                    return true;
                }

                try {
                    Player viewer = null;
                    String visibilityMessage;

                    if ("ADMIN".equalsIgnoreCase(highlightMode) && plugin.getGlowingManager().isProtocolLibAvailable()) {
                        viewer = player;
                        visibilityMessage = plugin.getMessagesManager().highlightVisibilityAdmin();
                    } else {
                        viewer = null;
                        visibilityMessage = plugin.getMessagesManager().highlightVisibilityAll();
                    }

                    int highlighted = plugin.getGlowingManager().applyGlowing(mobs, viewer);

                    send(sender, plugin.getMessagesManager().highlightSuccess(highlighted, visibilityMessage));

                    final List<Entity> glowingMobs = new ArrayList<>(mobs);
                    final Player finalViewer = viewer;

                    int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            plugin.getGlowingManager().removeGlowing(glowingMobs, finalViewer);
                        } catch (Exception e) {
                            if (plugin.getConfigManager().getDebug()) {
                                plugin.getLogger().warning("Error while removing mob highlighting: " + e.getMessage());
                            }
                        }
                    }, 200L).getTaskId();

                    plugin.getWavesManager().registerHighlightTask(taskId);

                } catch (Exception e) {
                    send(sender, "§cError highlighting mobs: " + e.getMessage());
                    if (plugin.getConfigManager().getDebug()) {
                        e.printStackTrace();
                    }
                }
                return true;
            default:
                send(sender, "§cUnknown subcommand. Use: /mwaves <reload|start|stop|info|force_start|highlight>");
                return true;
        }
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(plugin.getConfigManager().COLORIZER.colorize(msg));
    }
}