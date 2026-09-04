package io.github.lthewiredlabs.netherchance;

import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NetherChancePlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> SUBCOMMANDS = List.of("status", "open", "close", "roll", "reload");

    private File stateFile;
    private YamlConfiguration stateConfig;
    private boolean netherOpen;
    private LocalDate lastDailyRoll;
    private boolean rollInProgress;

    private int chancePercent;
    private LocalTime dailyTime;
    private ZoneId timeZone;
    private boolean announceOnJoin;
    private long startupDelayTicks;
    private long revealDelayTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        loadState();

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand command = getCommand("netherchance");
        if (command == null) {
            throw new IllegalStateException("netherchance command is missing from plugin.yml");
        }
        command.setExecutor(this);
        command.setTabCompleter(this);

        getServer().getScheduler().runTaskLater(this, this::announceStartup, startupDelayTicks);
        getServer().getScheduler().runTaskTimer(this, this::checkForDailyRoll, startupDelayTicks + 100L, 600L);

        getLogger().info("Nether Chance enabled: Nether is " + stateWord()
                + ", daily toggle chance is " + chancePercent + "% at "
                + dailyTime.format(TIME_FORMAT) + " " + timeZone + ".");
    }

    @Override
    public void onDisable() {
        saveState();
        getLogger().info("Nether Chance disabled; state saved as " + stateWord() + ".");
    }

    private void loadSettings() {
        reloadConfig();
        chancePercent = clamp(getConfig().getInt("chance-percent", 15), 0, 100);
        announceOnJoin = getConfig().getBoolean("announce-on-player-join", true);
        startupDelayTicks = secondsToTicks(getConfig().getLong("startup-announcement-delay-seconds", 15L));
        revealDelayTicks = secondsToTicks(getConfig().getLong("roll-reveal-delay-seconds", 5L));

        String configuredTime = getConfig().getString("daily-time", "00:00");
        try {
            dailyTime = LocalTime.parse(configuredTime, TIME_FORMAT);
        } catch (DateTimeException error) {
            getLogger().warning("Invalid daily-time '" + configuredTime + "'; using 00:00.");
            dailyTime = LocalTime.MIDNIGHT;
        }

        String configuredZone = getConfig().getString("time-zone", "America/New_York");
        try {
            timeZone = ZoneId.of(configuredZone);
        } catch (DateTimeException error) {
            getLogger().warning("Invalid time-zone '" + configuredZone + "'; using America/New_York.");
            timeZone = ZoneId.of("America/New_York");
        }
    }

    private void loadState() {
        stateFile = new File(getDataFolder(), "state.yml");
        stateConfig = YamlConfiguration.loadConfiguration(stateFile);
        netherOpen = stateConfig.getBoolean("nether-open", getConfig().getBoolean("default-open", true));

        String storedDate = stateConfig.getString("last-daily-roll");
        if (storedDate != null && !storedDate.isBlank()) {
            try {
                lastDailyRoll = LocalDate.parse(storedDate);
            } catch (DateTimeException error) {
                getLogger().warning("Ignoring invalid last-daily-roll value in state.yml: " + storedDate);
            }
        }
        saveState();
    }

    private void saveState() {
        if (stateConfig == null || stateFile == null) {
            return;
        }
        stateConfig.set("nether-open", netherOpen);
        stateConfig.set("last-daily-roll", lastDailyRoll == null ? null : lastDailyRoll.toString());
        try {
            stateConfig.save(stateFile);
        } catch (IOException error) {
            getLogger().log(Level.SEVERE, "Could not save Nether Chance state", error);
        }
    }

    private void announceStartup() {
        broadcast(Component.text("Nether Chance has begun! ", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("The Nether is currently " + stateWord() + ". ", stateColor()))
                .append(Component.text("Fate is tested daily at "
                        + dailyTime.format(TIME_FORMAT) + " " + timeZone + ".", NamedTextColor.YELLOW)));
    }

    private void checkForDailyRoll() {
        ZonedDateTime now = ZonedDateTime.now(timeZone);
        if (!rollInProgress && NetherChancePolicy.dailyRollIsDue(
                now.toLocalDate(), now.toLocalTime(), lastDailyRoll, dailyTime)) {
            startRoll(now.toLocalDate(), true, Bukkit.getConsoleSender());
        }
    }

    private void startRoll(LocalDate rollDate, boolean daily, CommandSender initiator) {
        if (rollInProgress) {
            initiator.sendMessage(prefixed(Component.text("A Nether roll is already in progress.", NamedTextColor.RED)));
            return;
        }
        rollInProgress = true;
        broadcast(Component.text("The daily Nether roll has begun... ", NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .append(Component.text("There is a " + chancePercent
                        + "% chance the gates will change.", NamedTextColor.YELLOW)));

        getServer().getScheduler().runTaskLater(this, () -> completeRoll(rollDate, daily), revealDelayTicks);
    }

    private void completeRoll(LocalDate rollDate, boolean daily) {
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        boolean toggled = NetherChancePolicy.shouldToggle(roll, chancePercent);
        if (toggled) {
            netherOpen = !netherOpen;
        }
        if (daily) {
            lastDailyRoll = rollDate;
        }
        rollInProgress = false;
        saveState();

        if (toggled && netherOpen) {
            broadcast(Component.text("Roll: " + roll + "/100 — ", NamedTextColor.YELLOW)
                    .append(Component.text("THE NETHER GATES HAVE OPENED!", NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text(" Portals now work.", NamedTextColor.GRAY)));
        } else if (toggled) {
            broadcast(Component.text("Roll: " + roll + "/100 — ", NamedTextColor.YELLOW)
                    .append(Component.text("THE NETHER GATES HAVE CLOSED!", NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD))
                    .append(Component.text(" No entry or exit until the gates reopen.", NamedTextColor.GRAY)));
        } else {
            broadcast(Component.text("Roll: " + roll + "/100 — ", NamedTextColor.YELLOW)
                    .append(Component.text("The Nether remains " + stateWord() + ".", stateColor())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (netherOpen || event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            return;
        }
        World fromWorld = event.getFrom().getWorld();
        event.setCancelled(true);
        if (fromWorld != null && fromWorld.getEnvironment() == World.Environment.NETHER) {
            event.getPlayer().sendMessage(prefixed(Component.text(
                    "The Nether gates are closed. You are trapped here until they reopen.", NamedTextColor.RED)));
        } else {
            event.getPlayer().sendMessage(prefixed(Component.text(
                    "The Nether gates are closed. Nobody may enter until they reopen.", NamedTextColor.RED)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (netherOpen) {
            return;
        }
        Location destination = event.getTo();
        World fromWorld = event.getFrom().getWorld();
        World destinationWorld = destination == null ? null : destination.getWorld();
        boolean startsInNether = fromWorld != null && fromWorld.getEnvironment() == World.Environment.NETHER;
        boolean endsInNether = destinationWorld != null && destinationWorld.getEnvironment() == World.Environment.NETHER;
        if (startsInNether || endsInNether) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!announceOnJoin) {
            return;
        }
        getServer().getScheduler().runTaskLater(this, () -> event.getPlayer().sendMessage(
                prefixed(Component.text("The Nether is currently " + stateWord() + ". ", stateColor())
                        .append(Component.text("Daily chance to change: " + chancePercent + "%.", NamedTextColor.GRAY)))), 40L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("status")) {
            if (!sender.hasPermission("netherchance.status")) {
                deny(sender);
                return true;
            }
            sendStatus(sender);
            return true;
        }

        if (!sender.hasPermission("netherchance.admin")) {
            deny(sender);
            return true;
        }

        switch (subcommand) {
            case "open" -> setState(true, sender);
            case "close" -> setState(false, sender);
            case "roll" -> startRoll(ZonedDateTime.now(timeZone).toLocalDate(), false, sender);
            case "reload" -> {
                loadSettings();
                sender.sendMessage(prefixed(Component.text("Configuration reloaded.", NamedTextColor.GREEN)));
                sendStatus(sender);
            }
            default -> sender.sendMessage(prefixed(Component.text(
                    "Usage: /" + label + " [status|open|close|roll|reload]", NamedTextColor.YELLOW)));
        }
        return true;
    }

    private void setState(boolean open, CommandSender sender) {
        if (netherOpen == open) {
            sender.sendMessage(prefixed(Component.text(
                    "The Nether is already " + stateWord() + ".", stateColor())));
            return;
        }
        netherOpen = open;
        saveState();
        broadcast(Component.text(sender.getName() + " has forced the Nether gates " + stateWord() + ".", stateColor())
                .decorate(TextDecoration.BOLD));
    }

    private void sendStatus(CommandSender sender) {
        String lastRoll = lastDailyRoll == null ? "never" : lastDailyRoll.toString();
        sender.sendMessage(prefixed(Component.text("Nether: " + stateWord(), stateColor())
                .append(Component.text(" | Daily toggle chance: " + chancePercent + "%", NamedTextColor.GRAY))
                .append(Component.text(" | Schedule: " + dailyTime.format(TIME_FORMAT)
                        + " " + timeZone, NamedTextColor.GRAY))
                .append(Component.text(" | Last daily roll: " + lastRoll, NamedTextColor.GRAY))));
    }

    private void deny(CommandSender sender) {
        sender.sendMessage(prefixed(Component.text("You do not have permission to do that.", NamedTextColor.RED)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> choices = new ArrayList<>();
        for (String choice : SUBCOMMANDS) {
            boolean allowed = choice.equals("status")
                    ? sender.hasPermission("netherchance.status")
                    : sender.hasPermission("netherchance.admin");
            if (allowed && choice.startsWith(prefix)) {
                choices.add(choice);
            }
        }
        return choices;
    }

    private void broadcast(Component message) {
        Component complete = prefixed(message);
        Bukkit.getConsoleSender().sendMessage(complete);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(complete);
        }
    }

    private Component prefixed(Component message) {
        return Component.text("[Nether Chance] ", NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD)
                .append(message.decoration(TextDecoration.BOLD, false));
    }

    private String stateWord() {
        return netherOpen ? "OPEN" : "CLOSED";
    }

    private NamedTextColor stateColor() {
        return netherOpen ? NamedTextColor.GREEN : NamedTextColor.RED;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static long secondsToTicks(long seconds) {
        return Math.max(0L, seconds) * 20L;
    }
}
