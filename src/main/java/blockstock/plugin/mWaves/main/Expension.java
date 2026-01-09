package blockstock.plugin.mWaves.main;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import blockstock.plugin.mWaves.mWaves;

public class Expension extends PlaceholderExpansion implements Relational{

    private final mWaves plugin;

    public Expension(mWaves plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "mWaves";
    }

    @Override
    public @NotNull String getAuthor() {
        return "musbabaff";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("wave")) {
            if (!plugin.getWavesManager().isActive()) {
                return "Inactive";
            }
            int currentWave = plugin.getWavesManager().getCurrentWaveNumber();

            return String.valueOf(currentWave);
        }
        if (params.equalsIgnoreCase("count")) {
            if (!plugin.getWavesManager().isActive()) {
                return "Inactive";
            }
            int remaining = plugin.getWavesManager().getRemainingMobsCount();

            return String.valueOf(remaining);
        }
        return null;
    }
    @Override
    public String onPlaceholderRequest(Player player, Player player1, String s) {
        return "";
    }
}
