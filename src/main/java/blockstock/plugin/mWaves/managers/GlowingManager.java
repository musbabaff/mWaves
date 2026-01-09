package blockstock.plugin.mWaves.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import blockstock.plugin.mWaves.mWaves;

import java.util.*;

public class GlowingManager {

    private final mWaves plugin;
    private final boolean protocolLibAvailable;

    public GlowingManager(@NotNull mWaves plugin) {
        this.plugin = plugin;
        this.protocolLibAvailable = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;

        if (protocolLibAvailable) {
            plugin.getLogger().info("ProtocolLib detected - mob highlights will only be visible to administrators in ADMIN mode.");
        } else {
            plugin.getLogger().info("ProtocolLib not detected - mob highlights will be visible to all players.");
        }
    }

    public boolean isProtocolLibAvailable() {
        return protocolLibAvailable;
    }

    public int applyGlowing(@NotNull List<Entity> mobs, Player viewer) {
        if (!protocolLibAvailable || viewer == null) {
            return applyStandardGlowing(mobs);
        }

        return applyProtocolLibGlowing(mobs, viewer);
    }

    private int applyStandardGlowing(@NotNull List<Entity> mobs) {
        int count = 0;
        for (Entity mob : mobs) {
            if (mob != null && !mob.isDead()) {
                mob.setGlowing(true);
                count++;
            }
        }
        return count;
    }

    private int applyProtocolLibGlowing(@NotNull List<Entity> mobs, @NotNull Player viewer) {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            int count = 0;

            for (Entity mob : mobs) {
                if (mob == null || mob.isDead()) {
                    continue;
                }

                try {
                    PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                    packet.getIntegers().write(0, mob.getEntityId());

                    WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(mob);

                    byte currentFlags = watcher.getByte(0);
                    byte newFlags = (byte) (currentFlags | 0x40);

                    var watcherObject = watcher.getWatchableObject(0);
                    if (watcherObject == null) {
                        if (plugin.getConfigManager().getDebug()) {
                            plugin.getLogger().warning("Could not get watchableObject with index 0 for mob: " + mob.getType());
                        }
                        continue;
                    }

                    var serializer = watcherObject.getWatcherObject().getSerializer();
                    if (serializer == null) {
                        if (plugin.getConfigManager().getDebug()) {
                            plugin.getLogger().warning("Serializer for index 0 is null for mob: " + mob.getType());
                        }
                        continue;
                    }

                    List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();
                    wrappedDataValueList.add(new WrappedDataValue(0, serializer, newFlags));

                    packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);

                    protocolManager.sendServerPacket(viewer, packet);
                    count++;

                } catch (Exception e) {
                    if (plugin.getConfigManager().getDebug()) {
                        plugin.getLogger().warning("Error applying ProtocolLib highlight to " + mob.getType() + ": " + e.getMessage());
                    }
                }
            }

            return count;

        } catch (Exception e) {
            plugin.getLogger().warning("ProtocolLib error: " + e.getMessage());
            if (plugin.getConfigManager().getDebug()) {
                e.printStackTrace();
            }
            return applyStandardGlowing(mobs);
        }
    }

    public void removeGlowing(@NotNull List<Entity> mobs, Player viewer) {
        if (!protocolLibAvailable || viewer == null) {
            removeStandardGlowing(mobs);
        } else {
            removeProtocolLibGlowing(mobs, viewer);
        }
    }

    private void removeStandardGlowing(@NotNull List<Entity> mobs) {
        for (Entity mob : mobs) {
            if (mob != null && !mob.isDead()) {
                mob.setGlowing(false);
            }
        }
    }

    private void removeProtocolLibGlowing(@NotNull List<Entity> mobs, @NotNull Player viewer) {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

            for (Entity mob : mobs) {
                if (mob == null || mob.isDead()) {
                    continue;
                }

                try {
                    PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
                    packet.getIntegers().write(0, mob.getEntityId());

                    WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(mob);

                    byte currentFlags = watcher.getByte(0);
                    byte newFlags = (byte) (currentFlags & ~0x40);

                    var watcherObject = watcher.getWatchableObject(0);
                    if (watcherObject == null) {
                        if (plugin.getConfigManager().getDebug()) {
                            plugin.getLogger().warning("Could not get watchableObject with index 0 for mob: " + mob.getType());
                        }
                        continue;
                    }

                    var serializer = watcherObject.getWatcherObject().getSerializer();
                    if (serializer == null) {
                        if (plugin.getConfigManager().getDebug()) {
                            plugin.getLogger().warning("Serializer for index 0 is null for mob: " + mob.getType());
                        }
                        continue;
                    }

                    List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();
                    wrappedDataValueList.add(new WrappedDataValue(0, serializer, newFlags));

                    packet.getDataValueCollectionModifier().write(0, wrappedDataValueList);

                    protocolManager.sendServerPacket(viewer, packet);

                } catch (Exception e) {
                    if (plugin.getConfigManager().getDebug()) {
                        plugin.getLogger().warning("Error removing ProtocolLib highlight from " + mob.getType() + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("ProtocolLib error during highlight removal: " + e.getMessage());
            if (plugin.getConfigManager().getDebug()) {
                e.printStackTrace();
            }
            removeStandardGlowing(mobs);
        }
    }
}