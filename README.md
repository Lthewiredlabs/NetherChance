# Nether Chance

Nether Chance is a Paper plugin that turns Nether access into a persistent
daily event. It makes one real-world roll per calendar day:

- If the Nether is open, a successful roll closes it.
- If the Nether is closed, a successful roll opens it.
- The daily toggle chance is 15% by default.
- A failed roll leaves the current state unchanged.
- Closing the Nether blocks every player teleport across its boundary in both
  directions, including portals and commands such as `/home`, `/spawn`,
  `/back`, and `/tpa`.
- Homes and teleports that remain entirely inside the same dimension still work.
- Entity portal travel across the Nether boundary is also blocked.
- Players already in the Nether remain trapped there until it reopens.

The plugin announces startup, the beginning of each daily roll, and the result
in Minecraft chat. Players also receive the current state when they join.

## Requirements

- Paper 26.2
- Java 25

## Installation

1. Build the plugin with `gradle build`.
2. Stop the Minecraft server.
3. Copy `build/libs/NetherChance-1.1.jar` into the server's `plugins` folder.
4. Start the server and confirm `NetherChance v1.1` enables in the log.

Never replace a plugin JAR while Paper is running.

## Commands

- `/netherchance` or `/netherchance status` - show the current state and schedule
- `/netherchance open` - force the Nether open (operators)
- `/netherchance close` - force the Nether closed (operators)
- `/netherchance roll` - run an extra roll without consuming the daily roll (operators)
- `/netherchance reload` - reload configuration (operators)

Alias: `/nchance`

## State and schedule

Runtime configuration is stored in `plugins/NetherChance/config.yml`. Persistent
state is stored in `plugins/NetherChance/state.yml`, including the date of the
last completed daily roll so a server restart cannot cause extra daily rolls.

The default schedule is midnight in `America/New_York`. The chance, schedule,
timezone, startup delay, reveal delay, and join announcement can all be changed
in `config.yml`.

## Build and test

```text
gradle clean check build
```

The dependency-free policy tests verify the exact 15% boundary and the
once-per-calendar-day scheduling rule.

## License

Nether Chance is available under the MIT License.
