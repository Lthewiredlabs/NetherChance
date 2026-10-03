# Testing NetherChance

## Automated checks

With Java 25 and Gradle 9.1 or newer, run:

```sh
gradle clean check build
```

The `check` task runs three executable test suites:

- `policyTest`: the chance boundary, daily scheduling, and the Nether teleport lock.
- `phraseTest`: both message pools, avoiding repeats, remembered choices, and empty or edited phrase lists.
- `atmosphereTest`: storm duration, weather clearing, world selection, effect switches, sound delivery, and lightning placement in occupied, loaded chunks.

The atmosphere tests use an in-memory server double. They check the calls made to Paper, not how a player sees or hears the result.

## Gameplay checks

Use a separate Paper server with players in the Overworld and Nether.

- Open and close the gates. Check that everyone receives the right phrase and sound. Opening should produce cosmetic lightning near Overworld players, without damage, fire, or changes to copper and lightning rods.
- Check the opening storm's duration and world selection. Closing during a storm should clear rain and thunder immediately, with 60 seconds of clear weather before the normal cycle resumes.
- Repeat a command for the current state, reload the config, and restart when no daily roll is due. None should replay the effects. A roll that leaves the state unchanged should not trigger transition effects.
- Alternate opening and closing, including across a restart. Each pool should avoid its previous phrase when it has more than one distinct choice.
- Try the effect switches and a custom world list. Weather and lightning should stay out of the Nether and End. Added sound cues should still reach players there.
- With the gates closed, check portals and `/home`, `/spawn`, `/back`, and `/tpa` in both directions across the Nether boundary. Same-dimension teleports should still work. Reopen the gates and check that travel resumes.

## Version 1.2 verification

The automated suites pass. Installation and startup were verified on Paper 26.2 with Java 25, including preservation of the existing gate state and last daily-roll date. The client-side visual, sound, and gameplay checks above are still pending.
