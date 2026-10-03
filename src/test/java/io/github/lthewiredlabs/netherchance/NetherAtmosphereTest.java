package io.github.lthewiredlabs.netherchance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/** Behavioral checks against a strict in-memory server; never connects to Minecraft. */
public final class NetherAtmosphereTest {
    public static void main(String[] args) {
        openingAndEarlyClosingControlWeather();
        startupAndReloadDoNotReplayEffects();
        onlySelectedOverworldsChange();
        invalidWorldListsDoNotExpandScope();
        durationIsBoundedBeforeTickConversion();
        effectsCanBeDisabledIndependently();
        lightningUsesOnlyOccupiedLoadedChunks();
        System.out.println("All Nether atmosphere tests passed.");
    }

    private static void openingAndEarlyClosingControlWeather() {
        Fixture fixture = new Fixture();
        TestWorld world = fixture.world("world", World.Environment.NORMAL);
        world.thundering = true;
        world.clearTicks = 60_000;
        NetherAtmosphere atmosphere = fixture.atmosphere();

        atmosphere.onTransition(true);
        require(world.raining, "Opening starts a storm");
        require(!world.thundering, "Opening suppresses natural damaging thunder");
        require(world.weatherTicks == 18_000, "Default opening storm lasts fifteen minutes");
        require(world.thunderTicks > world.weatherTicks, "Natural thunder stays off for the entire storm");
        require(world.clearTicks == 0, "Opening removes any existing clear-weather hold");
        require(world.chunkChecks == 0 && world.heightQueries == 0,
                "An empty world needs no chunk or terrain queries");

        // Closing before the opening duration ends must cancel that weather immediately.
        atmosphere.onTransition(false);
        require(!world.raining && !world.thundering, "Closing clears rain and thunder");
        require(world.weatherTicks == 1_200, "Closing replaces the unfinished rain timer");
        require(world.clearTicks == 1_200, "Closing holds clear skies for sixty seconds");
        require(world.strikes.isEmpty(), "Closing never summons lightning");
    }

    private static void startupAndReloadDoNotReplayEffects() {
        Fixture fixture = new Fixture();
        TestWorld world = fixture.world("world", World.Environment.NORMAL);
        TestPlayer player = fixture.player(world, 1, 64, 1);
        NetherAtmosphere atmosphere = fixture.atmosphere();
        fixture.config.set("effects.weather.open-duration-seconds", 1_800L);
        atmosphere.reload();
        require(world.mutations == 0 && player.sounds.isEmpty(), "Startup and reload must not apply effects");
        require(world.chunkChecks == 0, "Reload must not inspect terrain");

        atmosphere.onTransition(true);
        require(world.weatherTicks == 36_000, "The next transition uses the reloaded duration");
        int mutations = world.mutations;
        int sounds = player.sounds.size();
        atmosphere.reload();
        require(world.mutations == mutations && player.sounds.size() == sounds,
                "Reload during a storm must neither restart it nor repeat sounds");
    }

    private static void onlySelectedOverworldsChange() {
        Fixture fixture = new Fixture();
        TestWorld primary = fixture.world("world", World.Environment.NORMAL);
        TestWorld creative = fixture.world("creative", World.Environment.NORMAL);
        TestWorld nether = fixture.world("world_nether", World.Environment.NETHER);
        TestWorld end = fixture.world("world_the_end", World.Environment.THE_END);
        TestPlayer netherPlayer = fixture.player(nether, 0, 64, 0);
        TestPlayer endPlayer = fixture.player(end, 0, 64, 0);
        NetherAtmosphere atmosphere = fixture.atmosphere();
        atmosphere.onTransition(true);
        require(primary.raining && creative.raining, "Defaults cover all NORMAL worlds");
        require(nether.mutations == 0 && end.mutations == 0, "Nether and End weather must never change");
        require(netherPlayer.sounds.size() == 1 && endPlayer.sounds.size() == 1,
                "Players outside the Overworld still hear the transition cue");

        int primaryMutations = primary.mutations;
        fixture.config.set("effects.worlds", List.of(" creative ", "world_nether", "world_the_end"));
        atmosphere.reload();
        atmosphere.onTransition(false);
        require(primary.mutations == primaryMutations, "An excluded Overworld remains untouched");
        require(!creative.raining, "A named NORMAL world receives closing weather");
        require(nether.mutations == 0 && end.mutations == 0,
                "Naming the Nether or End must not allow weather effects there");
        require(netherPlayer.sounds.get(1).equals("minecraft:block.end_portal.spawn"),
                "Closing sound reaches a player trapped in the Nether");
    }

    private static void invalidWorldListsDoNotExpandScope() {
        for (Object configured : List.of("world", List.of(" ", 12), List.of("missing-world"))) {
            Fixture fixture = new Fixture();
            TestWorld world = fixture.world("world", World.Environment.NORMAL);
            fixture.config.set("effects.worlds", configured);
            fixture.atmosphere().onTransition(true);
            require(world.mutations == 0, "An invalid or unmatched allowlist must not affect every world");
        }
    }

    private static void durationIsBoundedBeforeTickConversion() {
        long[] seconds = { 1L, 1_800L, 86_400L, 0L, -10L, Long.MAX_VALUE };
        int[] expectedTicks = { 20, 36_000, 1_728_000, 20, 20, 1_728_000 };
        for (int index = 0; index < seconds.length; index++) {
            Fixture fixture = new Fixture();
            TestWorld world = fixture.world("world", World.Environment.NORMAL);
            fixture.config.set("effects.weather.open-duration-seconds", seconds[index]);
            fixture.atmosphere().onTransition(true);
            require(world.weatherTicks == expectedTicks[index], "Storm duration is valid and cannot overflow");
        }
    }

    private static void effectsCanBeDisabledIndependently() {
        Fixture fixture = new Fixture();
        TestWorld world = fixture.world("world", World.Environment.NORMAL);
        TestPlayer player = fixture.player(world, 1, 64, 1);
        world.loadedChunks.add(chunkKey(0, 0));
        fixture.config.set("effects.weather.enabled", false);
        fixture.config.set("effects.lightning.enabled", false);
        fixture.config.set("effects.sounds.enabled", false);
        NetherAtmosphere atmosphere = fixture.atmosphere();
        atmosphere.onTransition(true);
        atmosphere.onTransition(false);
        require(world.mutations == 0 && player.sounds.isEmpty(), "All effects can be disabled");

        fixture.config.set("effects.sounds.enabled", true);
        atmosphere.reload();
        atmosphere.onTransition(true);
        require(world.mutations == 0 && player.sounds.size() == 1, "Sound cues can run alone");

        fixture.config.set("effects.sounds.enabled", false);
        fixture.config.set("effects.lightning.enabled", true);
        atmosphere.reload();
        atmosphere.onTransition(true);
        require(world.strikes.size() == 1 && !world.raining, "Lightning can run with weather disabled");
        require(player.sounds.size() == 1, "Disabling added sound cues leaves no explicit sound call");

        fixture.config.set("effects.lightning.enabled", false);
        fixture.config.set("effects.weather.enabled", true);
        atmosphere.reload();
        atmosphere.onTransition(true);
        require(world.raining && world.strikes.size() == 1, "Weather can run with lightning disabled");
    }

    private static void lightningUsesOnlyOccupiedLoadedChunks() {
        Fixture fixture = new Fixture();
        TestWorld world = fixture.world("world", World.Environment.NORMAL);
        fixture.player(world, 1, 64, 1);
        fixture.player(world, 15, 70, 15); // Same chunk: one shared flash.
        fixture.player(world, -1, 64, -1); // Negative coordinates must select chunk -1,-1.
        fixture.player(world, 33, 64, 1); // Not loaded: no query or strike permitted.
        world.loadedChunks.add(chunkKey(0, 0));
        world.loadedChunks.add(chunkKey(-1, -1));
        fixture.atmosphere().onTransition(true);
        require(world.strikes.size() == 2 && world.heightQueries == 2,
                "Only unique occupied loaded chunks get lightning and terrain queries");
        require(world.strikes.stream().allMatch(location -> location.getY() == 81),
                "Bolts start just above the surface height");
        require(world.strikes.stream().anyMatch(location -> location.getBlockX() < 0),
                "Negative world coordinates are handled correctly");
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private static <T> T strictProxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (object, method, arguments) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "Test " + type.getSimpleName();
                            case "hashCode" -> System.identityHashCode(object);
                            case "equals" -> object == arguments[0];
                            default -> throw new AssertionError(method);
                        };
                    }
                    return handler.invoke(object, method, arguments);
                }));
    }

    private static final class Fixture {
        final YamlConfiguration config = new YamlConfiguration();
        final List<World> worlds = new ArrayList<>();
        final List<Player> players = new ArrayList<>();
        final Server server = strictProxy(Server.class, (object, method, arguments) -> switch (method.getName()) {
            case "getWorlds" -> worlds;
            case "getOnlinePlayers" -> players;
            default -> throw new AssertionError("Unexpected server call: " + method);
        });

        TestWorld world(String name, World.Environment environment) {
            TestWorld result = new TestWorld(name, environment);
            worlds.add(result.world);
            return result;
        }

        TestPlayer player(TestWorld world, double x, double y, double z) {
            TestPlayer result = new TestPlayer(world.world, x, y, z);
            players.add(result.player);
            world.players.add(result.player);
            return result;
        }

        NetherAtmosphere atmosphere() {
            Logger logger = Logger.getAnonymousLogger();
            logger.setLevel(Level.OFF);
            return new NetherAtmosphere(server, () -> config, logger);
        }
    }

    private static final class TestPlayer {
        final Player player;
        final List<String> sounds = new ArrayList<>();

        TestPlayer(World world, double x, double y, double z) {
            Location location = new Location(world, x, y, z);
            player = strictProxy(Player.class, (object, method, arguments) -> switch (method.getName()) {
                case "getLocation" -> location.clone();
                case "playSound" -> {
                    sounds.add((String) arguments[1]);
                    yield null;
                }
                default -> throw new AssertionError("Unexpected player call: " + method);
            });
        }
    }

    private static final class TestWorld {
        final World world;
        final String name;
        final World.Environment environment;
        final List<Player> players = new ArrayList<>();
        final Set<Long> loadedChunks = new HashSet<>();
        final List<Location> strikes = new ArrayList<>();
        boolean raining;
        boolean thundering;
        int weatherTicks;
        int thunderTicks;
        int clearTicks;
        int mutations;
        int chunkChecks;
        int heightQueries;

        TestWorld(String name, World.Environment environment) {
            this.name = name;
            this.environment = environment;
            world = strictProxy(World.class, this::invoke);
        }

        private Object invoke(Object object, Method method, Object[] arguments) {
            switch (method.getName()) {
                case "getName": return name;
                case "getEnvironment": return environment;
                case "getPlayers": return players;
                case "isChunkLoaded":
                    chunkChecks++;
                    return loadedChunks.contains(chunkKey((int) arguments[0], (int) arguments[1]));
                case "getHighestBlockYAt":
                    int x = (int) arguments[0];
                    int z = (int) arguments[1];
                    require(loadedChunks.contains(chunkKey(x >> 4, z >> 4)), "Terrain queries must not load chunks");
                    heightQueries++;
                    return 80;
                case "strikeLightningEffect":
                    Location strike = (Location) arguments[0];
                    require(loadedChunks.contains(chunkKey(strike.getBlockX() >> 4, strike.getBlockZ() >> 4)),
                            "Cosmetic lightning must not load chunks");
                    strikes.add(strike.clone());
                    break;
                case "setStorm":
                    raining = (boolean) arguments[0];
                    clearTicks = 0; // Match the documented setter side effect.
                    break;
                case "setThundering":
                    thundering = (boolean) arguments[0];
                    clearTicks = 0;
                    break;
                case "setWeatherDuration": weatherTicks = (int) arguments[0]; break;
                case "setThunderDuration": thunderTicks = (int) arguments[0]; break;
                case "setClearWeatherDuration": clearTicks = (int) arguments[0]; break;
                default: throw new AssertionError("Unexpected world call or mutation: " + method);
            }
            mutations++;
            return null;
        }
    }
}
