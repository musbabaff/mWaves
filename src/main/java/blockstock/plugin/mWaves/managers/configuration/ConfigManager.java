package blockstock.plugin.mWaves.managers.configuration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import blockstock.plugin.mWaves.mWaves;
import blockstock.plugin.mWaves.main.MobSpawnData;
import blockstock.plugin.mWaves.main.WaveData;
import blockstock.plugin.mWaves.managers.Runner.Runner;
import blockstock.plugin.mWaves.managers.color.Colorizer;
import blockstock.plugin.mWaves.managers.color.impl.LegacyAdvancedColorizer;
import blockstock.plugin.mWaves.managers.color.impl.LegacyColorizer;
import blockstock.plugin.mWaves.managers.color.impl.MiniMessageColorizer;
import blockstock.plugin.mWaves.managers.color.impl.VanillaColorizer;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {
    private final mWaves plugin;
    protected FileConfiguration config;
    private File configFile;
    public Colorizer COLORIZER;
    private Runner runner;

    public ConfigManager(mWaves plugin, Runner runner) {
        this.plugin = plugin;
        this.runner = runner;
        loadConfigFiles();
    }

    private void loadConfigFiles() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                plugin.saveResource("config.yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not save config.yml: " + e.getMessage());
                plugin.getLogger().warning("Please create the 'plugins/mWaves/' folder manually and ensure write permissions.");
            }
        }
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
        } else {
            plugin.getLogger().warning("Config file not found at: " + configFile.getPath());
        }
    }

    public boolean getBoolean(String path, boolean def) {
        if (config == null) return def;
        return config.contains(path) ? config.getBoolean(path) : def;
    }

    public String getString(String path, String def) {
        if (config == null) return def;
        return config.contains(path) ? config.getString(path) : def;
    }

    public List<String> getStringList(String path) {
        if (config == null) return new ArrayList<>();
        return config.getStringList(path);
    }

    public boolean getDebug() {
        return getBoolean("debug", true);
    }

    public String getPermissionInfo() {
        return getString("info_command_permission", "mwaves.user");
    }

    public boolean getAutoRestart() {
        return getBoolean("auto_restart", true);
    }

    public int getSpawnRadius() {
        return config != null ? config.getInt("spawn_radius", 5) : 5;
    }

    public double getDefaultHealthMultiplier() {
        return config != null ? config.getDouble("default_health_multiplier", 1.0) : 1.0;
    }

    public boolean isWaveMessagesEnabled() {
        return getBoolean("wave_messages.enabled", true);
    }

    public String getWaveStartMessage() {
        return getString("wave_messages.start", "&e&l>>> &6Wave %wave% has started! &e&l<<<");
    }

    public String getWaveCompleteMessage() {
        return getString("wave_messages.complete", "&a&l>>> &2Wave %wave% cleared! Next wave in %delay% seconds. &a&l<<<");
    }

    public String getAllWavesCompleteMessage() {
        return getString("wave_messages.all_complete", "&6&l>>> All waves completed! Restarting from the first wave... &6&l<<<");
    }

    public boolean isSoundsEnabled() {
        return getBoolean("sounds.enabled", true);
    }

    public String getWaveStartSound() {
        return getString("sounds.wave_start.sound", "ENTITY_ENDER_DRAGON_GROWL");
    }

    public float getWaveStartVolume() {
        return (float) (config != null ? config.getDouble("sounds.wave_start.volume", 1.0) : 1.0);
    }

    public float getWaveStartPitch() {
        return (float) (config != null ? config.getDouble("sounds.wave_start.pitch", 1.0) : 1.0);
    }

    public String getWaveCompleteSound() {
        return getString("sounds.wave_complete.sound", "UI_TOAST_CHALLENGE_COMPLETE");
    }

    public float getWaveCompleteVolume() {
        return (float) (config != null ? config.getDouble("sounds.wave_complete.volume", 1.0) : 1.0);
    }

    public float getWaveCompletePitch() {
        return (float) (config != null ? config.getDouble("sounds.wave_complete.pitch", 1.0) : 1.0);
    }

    public String getMobDeathSound() {
        return getString("sounds.mob_death.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public float getMobDeathVolume() {
        return (float) (config != null ? config.getDouble("sounds.mob_death.volume", 0.5) : 0.5);
    }

    public float getMobDeathPitch() {
        return (float) (config != null ? config.getDouble("sounds.mob_death.pitch", 1.2) : 1.2);
    }

    public boolean isBossBarEnabled() {
        String mode = getBossBarMode();
        return !"NONE".equalsIgnoreCase(mode);
    }

    public String getBossBarMode() {
        if (config != null && !config.contains("bossbar.mode") && config.contains("bossbar.enabled")) {
            boolean enabled = config.getBoolean("bossbar.enabled", true);
            return enabled ? "ALL" : "NONE";
        }
        return getString("bossbar.mode", "ALL").toUpperCase();
    }

    public String getBossBarTitle() {
        return getString("bossbar.title", "&6Wave %wave% &7- &eRemaining: %remaining%/%total%");
    }

    public String getBossBarColor() {
        return getString("bossbar.color", "YELLOW");
    }

    public String getBossBarStyle() {
        return getString("bossbar.style", "SEGMENTED_10");
    }

    public String getHighlightMode() {
        return getString("highlight.mode", "ADMIN").toUpperCase();
    }

    public void setupColorizer() {
        COLORIZER = switch (getString("serializer", "LEGACY").toUpperCase()) {
            case "MINIMESSAGE" -> new MiniMessageColorizer();
            case "LEGACY" -> new LegacyColorizer(plugin);
            case "LEGACY_ADVANCED" -> new LegacyAdvancedColorizer(plugin);
            default -> new VanillaColorizer(plugin);
        };
    }

    @NotNull
    public List<WaveData> getWaves() {
        List<WaveData> waves = new ArrayList<>();
        if (config == null || !config.contains("Waves")) {
            return waves;
        }

        List<?> wavesList = config.getList("Waves");
        if (wavesList == null || wavesList.isEmpty()) {
            return waves;
        }

        for (int i = 0; i < wavesList.size(); i++) {
            try {
                String wavePath = "Waves." + i;
                ConfigurationSection section = config.getConfigurationSection(wavePath);

                if (section == null) {
                    Object waveObj = wavesList.get(i);
                    if (waveObj instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> waveMap = (java.util.Map<String, Object>) waveObj;
                        section = config.createSection(wavePath);
                        for (java.util.Map.Entry<String, Object> entry : waveMap.entrySet()) {
                            section.set(entry.getKey(), entry.getValue());
                        }
                    }
                }

                if (section == null) {
                    plugin.getLogger().warning("Failed to load wave #" + (i + 1));
                    continue;
                }

                int count = section.getInt("count", i + 1);
                List<String> mobsList = section.getStringList("mobs");
                List<String> coordinatesList = section.getStringList("coordinates");
                int mobsCount = section.getInt("mobs-count", 10);
                int exceptions = section.getInt("exceptions", 10);
                double healthMultiplier = section.getDouble("health-multiplier", getDefaultHealthMultiplier());
                String customTitle = section.getString("title", null);
                List<String> rewards = section.getStringList("rewards");

                List<MobSpawnData> mobs = new ArrayList<>();
                for (String mobData : mobsList) {
                    mobs.add(MobSpawnData.parse(mobData));
                }

                List<Location> coordinates = new ArrayList<>();
                List<World> availableWorlds = Bukkit.getWorlds();
                World defaultWorld = availableWorlds.isEmpty() ? null : availableWorlds.get(0);

                if (getDebug() && defaultWorld == null) {
                    plugin.getLogger().warning("Wave " + count + ": No available worlds found on the server!");
                }

                World world = null;
                for (String coordStr : coordinatesList) {
                    World coordWorld = getWorldFromCoordinates(coordStr, defaultWorld);
                    if (coordWorld != null) {
                        world = coordWorld;
                        if (getDebug()) {
                            plugin.getLogger().info("Wave " + count + ": World '" + world.getName() + "' identified from coordinates.");
                        }
                        break;
                    }
                }

                if (world == null) {
                    if (defaultWorld == null) {
                        plugin.getLogger().severe("Wave " + count + ": Could not find a world to spawn mobs! Check world loading. Total worlds: " + availableWorlds.size());
                        continue;
                    }
                    world = defaultWorld;
                    if (getDebug()) {
                        plugin.getLogger().info("Wave " + count + ": Using default world '" + world.getName() + "'");
                    }
                }

                for (String coordStr : coordinatesList) {
                    Location loc = parseLocation(coordStr, world);
                    if (loc != null) coordinates.add(loc);
                }

                if (coordinates.isEmpty()) {
                    plugin.getLogger().warning("Wave " + count + " does not have valid coordinates!");
                    continue;
                }

                if (mobs.isEmpty()) {
                    plugin.getLogger().warning("Wave " + count + " has no mobs defined!");
                    continue;
                }

                waves.add(new WaveData(count, mobs, coordinates, mobsCount, exceptions,
                        healthMultiplier, customTitle, rewards));

                if (getDebug()) {
                    plugin.getLogger().info("Loaded Wave #" + count + " with " + mobsCount + " mobs.");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Error loading Wave #" + (i + 1) + ": " + e.getMessage());
                if (getDebug()) e.printStackTrace();
            }
        }
        return waves;
    }

    private World getWorldFromCoordinates(@NotNull String coordStr, World defaultWorld) {
        try {
            String[] parts = coordStr.split(",");
            if (parts.length == 4) {
                World world = Bukkit.getWorld(parts[0].trim());
                if (world != null) return world;
            }
            return defaultWorld;
        } catch (Exception e) {
            return defaultWorld;
        }
    }

    private Location parseLocation(@NotNull String coordStr, @NotNull World defaultWorld) {
        try {
            String[] parts = coordStr.split(",");
            if (parts.length == 3) {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                return new Location(defaultWorld, x, y, z);
            } else if (parts.length == 4) {
                World world = Bukkit.getWorld(parts[0].trim());
                if (world == null) world = defaultWorld;
                double x = Double.parseDouble(parts[1].trim());
                double y = Double.parseDouble(parts[2].trim());
                double z = Double.parseDouble(parts[3].trim());
                return new Location(world, x, y, z);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Coordinate parsing error: " + coordStr + " - " + e.getMessage());
        }
        return null;
    }
}