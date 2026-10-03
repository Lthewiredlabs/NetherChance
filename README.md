# NetherChance

A Paper plugin that makes Nether access unpredictable. Once a day, the server rolls to decide whether the gates stay as they are or switch between open and closed.

[Download the latest release](https://github.com/Lthewiredlabs/NetherChance/releases/latest) · [Default configuration](src/main/resources/config.yml)

## How it works

By default, the roll happens at midnight in `America/New_York`, with a **15% chance to change the current state**. An open Nether can close, and a closed Nether can open. Otherwise, nothing changes. The state and last roll date are saved, so restarting the server doesn't give it another roll that day.

**Closed gates block portal travel and teleports in both directions.** Commands such as `/home`, `/spawn`, `/back`, and `/tpa` can't carry players across the Nether boundary. Teleports that stay on the same side of that boundary aren't blocked.

Opening the gates brings cosmetic lightning and a 15-minute Overworld rainstorm. Closing them clears rain and thunder, followed by a short clear-weather period. The lightning doesn't damage players, start fires, or affect copper and lightning rods.

Each opening or closing picks from its own set of four phrases, avoiding the previous choice when possible. Players hear a sound cue and see the announcement in every dimension. These effects only happen when the gates actually change. Startup and reload don't replay them, though starting the server after a missed daily roll will still run that roll.

## Install

Requires **Paper 26.2** and **Java 25**.

1. Download `NetherChance-1.2.jar` from the [release page](https://github.com/Lthewiredlabs/NetherChance/releases/tag/v1.2).
2. Stop the server and put the JAR in `plugins/`. If you're upgrading, remove the old NetherChance JAR so there's only one copy.
3. Start the server. The log should show `NetherChance v1.2` enabling.

Keep the `plugins/NetherChance/` folder when upgrading. It holds your settings and saved gate state.

## Commands

Use `/netherchance` or the shorter `/nchance`.

| Command | What it does |
| --- | --- |
| `/nchance status` | Shows the current state and daily schedule. Also the default when no command is given. |
| `/nchance open` | Opens the gates. |
| `/nchance close` | Closes the gates. |
| `/nchance roll` | Makes an extra roll without using up the daily one. |
| `/nchance reload` | Reloads the configuration. |

Status is available to everyone by default. The other commands require operator status or the `netherchance.admin` permission.

## Configuration

Edit `plugins/NetherChance/config.yml`, then run `/nchance reload`. The [default config](src/main/resources/config.yml) includes all settings and comments. Older config files use the defaults for any new settings they don't contain.

| Setting | Default |
| --- | --- |
| `chance-percent` | `15` |
| `daily-time` | `"00:00"` |
| `time-zone` | `"America/New_York"` |
| `messages.opening` / `messages.closing` | Four phrases in each list; replace them with your own if you like. |
| `effects.weather.open-duration-seconds` | `900` (15 minutes) |
| `effects.worlds` | `[]` — all loaded Overworld worlds, or a list of specific world names. |
| `effects.weather.enabled` | `true` |
| `effects.lightning.enabled` | `true` |
| `effects.sounds.enabled` | `true` |

Weather follows Minecraft's normal rules: sleeping, commands, and other plugins can end the storm early, and disabling the weather cycle pauses its timer. To silence the effects entirely, turn off both lightning and sounds; lightning bolts have their own thunder sound.

## Building

Use Java 25 and Gradle 9.1 or newer:

```sh
gradle clean check build
```

The JAR is written to `build/libs/NetherChance-1.2.jar`. See [testing notes](docs/testing.md) for the automated checks and gameplay checklist.

## License

[MIT](LICENSE)
