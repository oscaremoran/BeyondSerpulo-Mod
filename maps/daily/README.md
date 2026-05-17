# Daily Challenge maps

The Daily Challenge feature rotates through five maps named (in `maps/` top-level):

- `daily-1.msav` — mode: SURVIVE_WAVES (highest wave wins)
- `daily-2.msav` — mode: FASTEST_CAPTURE (fastest capture wins)
- `daily-3.msav` — mode: SURVIVE_WAVES
- `daily-4.msav` — mode: FASTEST_CAPTURE
- `daily-5.msav` — mode: SURVIVE_WAVES

Until you create these maps with the in-game editor, the Daily Challenge screen will
show "Coming soon" for any missing day. The mod won't crash; the menu button still works.

Modes are declared in `java-src/xorinal/DailyChallenge.java` — edit the `entries` array
to change which mode each map uses.

NOTE: Mindustry loads .msav files from the top level of `maps/`. Save your daily-N maps
to `maps/daily-1.msav` etc. — this subdirectory is just for documentation.
