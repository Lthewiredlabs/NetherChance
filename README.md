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

## Gate transition effects

Version 1.2 adds effects whenever the gates actually open or close, including
operator changes through `/netherchance open` and `/netherchance close`:

- Each opening chooses a random phrase from the four bundled opening messages;
  each closing chooses from a separate pool of four closing messages. The last
  choice from each pool is saved, so the next opening or closing avoids repeating
  its previous phrase, including after a restart.
- Players in every dimension receive the transition message and a sound cue:
  thunder on opening and a low portal sound on closing. Opening also creates
  cosmetic lightning near players in the selected Overworld worlds without
  dealing damage, starting fires, or triggering lightning-rod and copper
  mechanics.
- Opening starts a 15-minute rainstorm in the selected Overworld worlds, with
  snow in snowy biomes. Natural thunder is disabled during this storm.
- Closing clears rain and thunder in those worlds and keeps the weather clear
  for 60 seconds before normal weather can resume.

Effects happen only when the state changes. A roll that leaves the gates in
their existing state still reports its result without transition effects.
Startup, configuration reload, and an operator command that requests the
existing state do not trigger them.

## Requirements

- Paper 26.2
- Java 25
- Gradle 9.1 or newer when building from source

## Installation

1. Build the plugin with `gradle build`.
2. Stop the Minecraft server.
3. Copy `build/libs/NetherChance-1.2.jar` into the server's `plugins` folder.
4. Start the server and confirm `NetherChance v1.2` enables in the log.

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
The most recently selected opening and closing phrases are also persisted.

The default schedule is midnight in `America/New_York`. The chance, schedule,
timezone, startup delay, reveal delay, and join announcement can all be changed
in `config.yml`.

## Transition settings

The bundled `config.yml` contains these settings. Existing configuration files
that omit the new settings use the bundled defaults.

| Setting | Default | Behavior |
| --- | --- | --- |
| `messages.opening` | Four opening phrases | Random message pool when the gates open. |
| `messages.closing` | Four closing phrases | Separate random message pool when the gates close. |
| `effects.worlds` | `[]` | Empty means every loaded Overworld (`NORMAL`) world; otherwise list the Overworld world names for weather and lightning. |
| `effects.weather.enabled` | `true` | Enable opening rainstorms and closing weather clearing in the selected worlds. |
| `effects.weather.open-duration-seconds` | `900` | Length of the opening rainstorm, limited to 1–86400 seconds. |
| `effects.lightning.enabled` | `true` | Show cosmetic lightning on opening near players in the selected worlds. |
| `effects.sounds.enabled` | `true` | Play added opening and closing sound cues to players in all dimensions. |

The world list controls weather and lightning. Transition messages and the
added sound cue reach players in all dimensions. Minecraft also plays its own
sound for a lightning bolt; to silence the event, disable both lightning and
sounds. Reload changed settings with `/netherchance reload`; reloading does
not replay transition effects.

Weather durations are measured in server ticks (20 ticks per configured second)
and follow Minecraft's weather cycle; the weather is not locked for a wall-clock
duration. Disabling that cycle pauses the weather timer, and sleeping,
administrator commands, or other plugins can change the weather before the
timer ends.

## Build and test

```text
gradle clean check build
```

The dependency-free policy tests verify the exact 15% boundary and the
once-per-calendar-day scheduling rule. Phrase tests cover separate message
pools, non-repeating selection, remembered choices, and configuration fallback.
Atmosphere tests use an in-memory server double to check weather timing and
clearing, world selection, configuration switches, cross-dimension sound
delivery, and lightning placement limited to occupied, loaded chunks.

### Manual gameplay validation

Build checks do not verify the in-game presentation or other plugins' teleport
behavior. Before deployment, use a separate Paper test server to check:

1. Open closed gates and close open gates with players in the Overworld and
   Nether. Check the phrase and opening/closing sound cues in both dimensions,
   plus opening lightning near players in the selected Overworld worlds.
   Confirm that the lightning causes no damage, fire, or lightning-rod/copper
   changes.
2. Confirm that opening starts rain only in the selected Overworld worlds for
   the configured duration, with natural thunder disabled. Confirm that closing
   clears rain and thunder, provides a 60-second clear window, and then allows
   normal weather again.
3. Repeat a command for the current state, produce a roll with no state change,
   reload configuration, and restart with no daily roll due. None should replay
   transition effects.
4. Alternate opening and closing several times, including across a restart.
   Consecutive openings must use different opening phrases, and consecutive
   closings must use different closing phrases when their pools have multiple
   choices.
5. Test each effect switch and the weather world list. With gates closed, check
   portals and `/home`, `/spawn`, `/back`, and `/tpa` across the Nether boundary
   in both directions; check that same-dimension teleports still work and that
   cross-boundary travel resumes when the gates open.

These checks are a validation checklist, not a claim that version 1.2 has been
tested in-game or installed on a live server.

## License

Nether Chance is available under the MIT License.
