package io.github.lthewiredlabs.netherchance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Applies atmosphere once, when the caller has committed an actual gate transition. */
final class NetherAtmosphere {
    private static final long DEFAULT_OPEN_SECONDS = 900L;
    private static final long MAX_OPEN_SECONDS = 86_400L;
    private static final int CLEAR_HOLD_TICKS = 60 * 20;

    private final Server server;
    private final Supplier<FileConfiguration> configuration;
    private final Logger logger;

    private boolean weatherEnabled;
    private boolean lightningEnabled;
    private boolean soundsEnabled;
    private boolean allOverworlds;
    private Set<String> worldNames = Set.of();
    private int openDurationTicks;

    NetherAtmosphere(JavaPlugin plugin) {
        this(plugin.getServer(), plugin::getConfig, plugin.getLogger());
    }

    NetherAtmosphere(Server server, Supplier<FileConfiguration> configuration, Logger logger) {
        this.server = server;
        this.configuration = configuration;
        this.logger = logger;
        reload();
    }

    /** Reads settings only; it never changes weather or replays a transition. */
    void reload() {
        FileConfiguration config = configuration.get();
        weatherEnabled = config.getBoolean("effects.weather.enabled", true);
        lightningEnabled = config.getBoolean("effects.lightning.enabled", true);
        soundsEnabled = config.getBoolean("effects.sounds.enabled", true);

        long configuredSeconds = config.getLong("effects.weather.open-duration-seconds", DEFAULT_OPEN_SECONDS);
        long seconds = Math.max(1L, Math.min(MAX_OPEN_SECONDS, configuredSeconds));
        if (seconds != configuredSeconds) {
            logger.warning("effects.weather.open-duration-seconds must be between 1 and "
                    + MAX_OPEN_SECONDS + "; using " + seconds + ".");
        }
        openDurationTicks = (int) (seconds * 20L);

        Object configuredWorlds = config.get("effects.worlds");
        allOverworlds = configuredWorlds == null
                || configuredWorlds instanceof List<?> names && names.isEmpty();
        Set<String> names = new HashSet<>();
        if (configuredWorlds instanceof List<?> configuredNames) {
            for (Object value : configuredNames) {
                if (value instanceof String name && !name.isBlank()) {
                    names.add(name.trim());
                } else {
                    logger.warning("Ignoring a non-string or blank entry in effects.worlds.");
                }
            }
        } else if (configuredWorlds != null) {
            logger.warning("effects.worlds must be a list; weather and lightning are disabled until corrected.");
        }
        worldNames = Set.copyOf(names);
    }

    void onTransition(boolean open) {
        for (World world : server.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL
                    || !allOverworlds && !worldNames.contains(world.getName())) {
                continue;
            }
            if (weatherEnabled) {
                updateWeather(world, open);
            }
            if (open && lightningEnabled) {
                flashLightning(world);
            }
        }

        if (soundsEnabled) {
            String sound = open ? "minecraft:entity.lightning_bolt.thunder" : "minecraft:block.end_portal.spawn";
            for (Player player : server.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, SoundCategory.WEATHER, 0.7f, open ? 1.0f : 0.6f);
            }
        }
    }

    private void updateWeather(World world, boolean open) {
        // Rain/snow supplies the atmosphere without enabling damaging natural lightning.
        // These setters reset clear-weather time, so apply the closing hold last.
        world.setThundering(false);
        world.setStorm(open);
        world.setWeatherDuration(open ? openDurationTicks : CLEAR_HOLD_TICKS);
        world.setThunderDuration(open ? openDurationTicks + CLEAR_HOLD_TICKS : CLEAR_HOLD_TICKS);
        world.setClearWeatherDuration(open ? 0 : CLEAR_HOLD_TICKS);
    }

    private void flashLightning(World world) {
        Set<Long> struckChunks = new HashSet<>();
        for (Player player : world.getPlayers()) {
            Location playerLocation = player.getLocation();
            int chunkX = playerLocation.getBlockX() >> 4;
            int chunkZ = playerLocation.getBlockZ() >> 4;
            long chunkKey = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
            if (!struckChunks.add(chunkKey) || !world.isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            // One real-looking cosmetic bolt per occupied chunk, with no new chunk loads.
            int x = (chunkX << 4) + 8;
            int z = (chunkZ << 4) + 8;
            double y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1.0;
            // Paper's isEffect path suppresses fire, damage, rod power and copper changes.
            // The vanilla client's bolt thunder is intrinsic, independent of our sound cue.
            world.strikeLightningEffect(new Location(world, x + 0.5, y, z + 0.5));
        }
    }
}
