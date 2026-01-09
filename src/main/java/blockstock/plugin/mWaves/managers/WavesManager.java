package blockstock.plugin.mWaves.managers;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import blockstock.plugin.mWaves.mWaves;
import blockstock.plugin.mWaves.main.MobSpawnData;
import blockstock.plugin.mWaves.main.WaveData;
import blockstock.plugin.mWaves.managers.Runner.Runner;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WavesManager implements Listener {

    private final mWaves plugin;
    private final Runner runner;
    private final Map<UUID, WaveData> activeMobs = new ConcurrentHashMap<>();
    private final List<WaveData> waves = new ArrayList<>();
    private final BossBarManager bossBarManager;
    private final Set<Integer> highlightTasks = ConcurrentHashMap.newKeySet();
    private int currentWaveIndex = -1;
    private boolean isActive = false;
    private int taskId = -1;
    private boolean isCheckingCompletion = false;

    public WavesManager(@NotNull mWaves plugin, @NotNull Runner runner) {
        this.plugin = plugin;
        this.runner = runner;
        this.bossBarManager = new BossBarManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void loadWaves() {
        plugin.getConfigManager().reloadConfig();
        waves.clear();
        waves.addAll(plugin.getConfigManager().getWaves());
        if (plugin.getConfigManager().getDebug()) {
            plugin.getLogger().info("Waves loaded: " + waves.size());
        }
    }

    public boolean startWaves() {
        if (isActive) {
            plugin.getLogger().warning("Waves are already active!");
            return false;
        }

        loadWaves();

        if (waves.isEmpty()) {
            plugin.getLogger().warning("No waves found to start! Please check your config.yml.");
            return false;
        }

        isActive = true;
        currentWaveIndex = 0;
        startWave(currentWaveIndex);
        return true;
    }

    public boolean startWavesFromWave(int waveNumber) {
        if (isActive) {
            plugin.getLogger().warning("Waves are already active!");
            return false;
        }

        loadWaves();

        if (waves.isEmpty()) {
            plugin.getLogger().warning("No waves found to start! Please check your config.yml.");
            return false;
        }

        int foundIndex = -1;
        for (int i = 0; i < waves.size(); i++) {
            if (waves.get(i).getCount() == waveNumber) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            plugin.getLogger().warning("Wave #" + waveNumber + " not found!");
            return false;
        }

        isActive = true;
        currentWaveIndex = foundIndex;
        startWave(currentWaveIndex);

        if (plugin.getConfigManager().getDebug()) {
            plugin.getLogger().info("Starting waves from Wave #" + waveNumber + " (Index: " + foundIndex + ")");
        }

        return true;
    }

    public void stopWaves() {
        if (!isActive) {
            return;
        }

        isActive = false;
        currentWaveIndex = -1;
        isCheckingCompletion = false;

        bossBarManager.removeBossBar();
        cancelHighlightTasks();

        if (taskId != -1) {
            runner.run(() -> {
                plugin.getServer().getScheduler().cancelTask(taskId);
                taskId = -1;
            });
        }

        runner.run(() -> {
            for (UUID mobId : new ArrayList<>(activeMobs.keySet())) {
                Entity entity = plugin.getServer().getEntity(mobId);
                if (entity != null && !entity.isDead()) {
                    entity.remove();
                }
                activeMobs.remove(mobId);
            }
        });

        if (plugin.getConfigManager().getDebug()) {
            plugin.getLogger().info("Waves have been stopped.");
        }
    }

    private void startWave(int waveIndex) {
        if (waveIndex >= waves.size() || waveIndex < 0) {
            if (plugin.getConfigManager().getDebug()) {
                plugin.getLogger().info("All waves have been completed.");
            }

            if (plugin.getConfigManager().getAutoRestart()) {
                if (plugin.getConfigManager().isWaveMessagesEnabled()) {
                    String message = plugin.getConfigManager().getAllWavesCompleteMessage();
                    broadcastMessage(plugin.getConfigManager().COLORIZER.colorize(message));
                }
                currentWaveIndex = 0;
                startWave(0);
            } else {
                stopWaves();
            }
            return;
        }

        WaveData wave = waves.get(waveIndex);
        currentWaveIndex = waveIndex;

        if (plugin.getConfigManager().getDebug()) {
            plugin.getLogger().info("Starting Wave #" + wave.getCount() + " with " + wave.getMobsCount() + " mobs.");
        }

        if (plugin.getConfigManager().isWaveMessagesEnabled()) {
            try {
                String message = plugin.getConfigManager().getWaveStartMessage()
                        .replace("%wave%", String.valueOf(wave.getCount()));
                broadcastMessage(plugin.getConfigManager().COLORIZER.colorize(message));
            } catch (Exception e) {
                plugin.getLogger().warning("Error sending wave start message: " + e.getMessage());
            }
        }

        if (plugin.getConfigManager().isSoundsEnabled()) {
            try {
                playSound(plugin.getConfigManager().getWaveStartSound(),
                        plugin.getConfigManager().getWaveStartVolume(),
                        plugin.getConfigManager().getWaveStartPitch());
            } catch (Exception e) {
                plugin.getLogger().warning("Error playing wave start sound: " + e.getMessage());
            }
        }

        try {
            bossBarManager.createBossBar(wave.getCount(), wave.getMobsCount(), wave.getCustomTitle());
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating BossBar: " + e.getMessage());
        }

        runner.run(() -> spawnWaveMobs(wave));
    }

    private void spawnWaveMobs(@NotNull WaveData wave) {
        List<Location> coordinates = wave.getCoordinates();
        List<MobSpawnData> mobsData = wave.getMobs();
        int mobsCount = wave.getMobsCount();
        int spawnRadius = plugin.getConfigManager().getSpawnRadius();

        if (coordinates.isEmpty() || mobsData.isEmpty()) {
            plugin.getLogger().warning("Wave #" + wave.getCount() + " has missing data!");
            return;
        }

        double totalChance = mobsData.stream().mapToDouble(MobSpawnData::getChance).sum();
        if (totalChance <= 0) {
            plugin.getLogger().warning("Wave #" + wave.getCount() + " has zero spawn chance for all mobs!");
            return;
        }

        Random random = new Random();
        int spawned = 0;

        for (int i = 0; i < mobsCount; i++) {
            Location baseLocation = coordinates.get(random.nextInt(coordinates.size()));
            Location spawnLoc = baseLocation.clone();

            if (spawnRadius > 0) {
                double offsetX = (random.nextDouble() * 2 - 1) * spawnRadius;
                double offsetZ = (random.nextDouble() * 2 - 1) * spawnRadius;
                spawnLoc.add(offsetX, 0, offsetZ);

                try {
                    int highestY = spawnLoc.getWorld().getHighestBlockYAt(spawnLoc);
                    spawnLoc.setY(highestY + 1);
                } catch (Exception e) {
                    if (plugin.getConfigManager().getDebug()) {
                        plugin.getLogger().warning("Error determining spawn height: " + e.getMessage());
                    }
                }
            }

            MobSpawnData selectedMob = selectMobByChance(mobsData, random, totalChance);
            if (selectedMob != null) {
                spawnMythicMob(selectedMob.getMobName(), spawnLoc, wave);
                spawned++;
            }
        }

        if (plugin.getConfigManager().getDebug()) {
            plugin.getLogger().info("Spawned " + spawned + " mobs for Wave #" + wave.getCount());
        }
    }

    @Nullable
    private MobSpawnData selectMobByChance(@NotNull List<MobSpawnData> mobsData, @NotNull Random random, double totalChance) {
        double roll = random.nextDouble() * totalChance;
        double current = 0;
        for (MobSpawnData mobData : mobsData) {
            current += mobData.getChance();
            if (roll <= current) return mobData;
        }
        return mobsData.isEmpty() ? null : mobsData.get(mobsData.size() - 1);
    }

    private void spawnMythicMob(@NotNull String mobName, @NotNull Location location, @NotNull WaveData wave) {
        try {
            Optional<MythicMob> optionalMob = MythicBukkit.inst().getMobManager().getMythicMob(mobName);
            if (optionalMob.isEmpty()) {
                plugin.getLogger().warning("MythicMob '" + mobName + "' not found.");
                return;
            }

            MythicMob mob = optionalMob.get();
            ActiveMob spawned = mob.spawn(BukkitAdapter.adapt(location), 1.0);

            if (spawned == null) {
                plugin.getLogger().warning("Failed to spawn MythicMob: " + mobName);
                return;
            }

            Entity entity = spawned.getEntity().getBukkitEntity();
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                double healthMultiplier = wave.getHealthMultiplier();
                if (healthMultiplier > 0 && healthMultiplier != 1.0) {
                    healthMultiplier = Math.max(0.01, Math.min(100.0, healthMultiplier));
                    try {
                        double newMaxHealth = livingEntity.getMaxHealth() * healthMultiplier;
                        livingEntity.setMaxHealth(newMaxHealth);
                        livingEntity.setHealth(newMaxHealth);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error applying health multiplier to " + mobName + ": " + e.getMessage());
                    }
                }
                activeMobs.put(entity.getUniqueId(), wave);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error spawning mob '" + mobName + "': " + e.getMessage());
        }
    }

    @EventHandler
    public void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (!isActive) return;

        UUID entityId = event.getEntity().getUniqueId();
        if (activeMobs.containsKey(entityId)) {
            activeMobs.remove(entityId);

            if (plugin.getConfigManager().isSoundsEnabled()) {
                playSound(plugin.getConfigManager().getMobDeathSound(),
                        plugin.getConfigManager().getMobDeathVolume(),
                        plugin.getConfigManager().getMobDeathPitch());
            }

            bossBarManager.updateProgress(activeMobs.size());

            if (plugin.getConfigManager().getDebug()) {
                plugin.getLogger().info("Mob killed. Remaining: " + activeMobs.size());
            }

            runner.run(this::checkWaveCompletion);
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        if (isActive) {
            bossBarManager.addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(@NotNull org.bukkit.event.player.PlayerQuitEvent event) {
        bossBarManager.removePlayer(event.getPlayer());
    }

    private void checkWaveCompletion() {
        if (!isActive || !activeMobs.isEmpty() || isCheckingCompletion) return;

        if (currentWaveIndex < 0 || currentWaveIndex >= waves.size()) return;

        isCheckingCompletion = true;
        WaveData currentWave = waves.get(currentWaveIndex);

        if (plugin.getConfigManager().getDebug()) {
            plugin.getLogger().info("Wave #" + currentWave.getCount() + " cleared. Next wave in " + currentWave.getExceptions() + " seconds.");
        }

        if (plugin.getConfigManager().isWaveMessagesEnabled()) {
            String message = plugin.getConfigManager().getWaveCompleteMessage()
                    .replace("%wave%", String.valueOf(currentWave.getCount()))
                    .replace("%delay%", String.valueOf(currentWave.getExceptions()))
                    .replace("%next_wave%", String.valueOf(currentWave.getCount() + 1));
            broadcastMessage(plugin.getConfigManager().COLORIZER.colorize(message));
        }

        if (plugin.getConfigManager().isSoundsEnabled()) {
            playSound(plugin.getConfigManager().getWaveCompleteSound(),
                    plugin.getConfigManager().getWaveCompleteVolume(),
                    plugin.getConfigManager().getWaveCompletePitch());
        }

        executeRewards(currentWave.getRewards());

        runner.runDelayed(() -> {
            if (!isActive) {
                isCheckingCompletion = false;
                return;
            }
            isCheckingCompletion = false;
            currentWaveIndex++;
            startWave(currentWaveIndex);
        }, currentWave.getExceptions() * 20L);
    }

    private void executeRewards(List<String> rewards) {
        if (rewards == null || rewards.isEmpty()) return;
        for (String command : rewards) {
            if (command == null || command.isEmpty()) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.trim());
        }
    }

    public int getRemainingMobsCount() {
        activeMobs.keySet().removeIf(mobId -> {
            Entity entity = plugin.getServer().getEntity(mobId);
            return entity == null || entity.isDead();
        });
        return activeMobs.size();
    }

    public boolean isActive() { return isActive; }

    public void cancelHighlightTasks() {
        highlightTasks.forEach(Bukkit.getScheduler()::cancelTask);
        highlightTasks.clear();
    }

    private void playSound(String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, volume, pitch));
        } catch (Exception ignored) {}
    }

    private void broadcastMessage(String message) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
    }
}