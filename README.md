# NetherChance

A Paper plugin that makes Nether access unpredictable. Once a day, the server rolls to decide whether the gates stay as they are or switch between open and closed.

[Download](https://github.com/Lthewiredlabs/NetherChance/releases/latest) · [What's new in 1.2](#whats-new-in-12) · [Install](#install) · [Basic commands](#basic-commands) · [Configuration](#configuration)

## How it works

By default, the roll happens at midnight in `America/New_York`, with a **15% chance to change the current state**. An open Nether can close, and a closed Nether can open. Otherwise, nothing changes. The state and last roll date are saved, so restarting the server doesn't give it another roll that day.

**Closed gates block portal travel and teleports in both directions.** Commands such as `/home`, `/spawn`, `/back`, and `/tpa` can't carry players across the Nether boundary. Teleports that stay on the same side of that boundary aren't blocked.

## What's new in 1.2

Gate changes now have their own announcements, weather, and sounds:

- **Random phrases:** four opening messages and four closing messages. Each set avoids its last choice when possible, and remembers it across restarts.
- **Opening storm:** cosmetic lightning near Overworld players, followed by 15 minutes of rain. The lightning doesn't damage players, start fires, or affect copper and lightning rods.
- **Clear skies on closing:** rain and thunder stop, with 60 seconds of clear weather before the normal cycle resumes.
- **Sound cues:** thunder when the gates open and a low portal sound when they close. Players in every dimension receive the announcement and sound, including those in the Nether.

You can change the phrases, storm duration, affected worlds, and effect switches in the [configuration](#configuration).

The effects also work with the manual open and close commands. They only play when the gates actually change: repeating a command for the current state or reloading the config won't replay them. Startup doesn't replay them either, though a missed daily roll will still run after startup.

## Install

Requires **Paper 26.2** and **Java 25**.

1. Download `NetherChance-1.2.jar` from the [release page](https://github.com/Lthewiredlabs/NetherChance/releases/tag/v1.2).
2. Stop the server and put the JAR in `plugins/`. If you're upgrading, remove the old NetherChance JAR so there's only one copy.
3. Start the server. The log should show `NetherChance v1.2` enabling.

Keep the `plugins/NetherChance/` folder when upgrading. It holds your settings and saved gate state.

## Basic commands

Type `/netherchance` in chat to check whether the Nether is open and when the daily roll happens. Every command also works with the shorter `/nchance` alias.

| Command | What it does |
| --- | --- |
| `/netherchance` or `/netherchance status` | Shows the current state and daily schedule. |
| `/netherchance open` | Opens the gates immediately. |
| `/netherchance close` | Closes the gates immediately. |
| `/netherchance roll` | Makes an extra roll without using up the daily one. It can leave the gates unchanged. |
| `/netherchance reload` | Applies changes you've made to the configuration. |

Status is available to everyone by default. The other commands require operator status or the `netherchance.admin` permission.

For example, `/nchance close` closes the Nether now, while `/nchance open` reopens it. The regular daily schedule continues either way.

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
