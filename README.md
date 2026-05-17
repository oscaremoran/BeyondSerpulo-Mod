# Beyond Serpulo

A Mindustry planet expansion mod adding three custom worlds: **Xorinal** (organic spore world), **Tetra** (frost moon of Serpulo), and **Vantres** (blackened volcanic moon of Erekir). Includes new resources, units, turrets, and a full campaign chain alongside vanilla Serpulo and Erekir tech.

**Minimum Mindustry version:** 154 (v8)

## Installation

### In-game (recommended, once approved)
Open Mindustry → `Mods` → `Browse` → search for **Beyond Serpulo** → `Download`.

### Manual
1. Download the latest `BeyondSerpulo-Mod.zip` from [Releases](https://github.com/oscaremoran/BeyondSerpulo-Mod/releases).
2. Drop the zip into your Mindustry mods directory:
   - **macOS:** `~/Library/Application Support/Mindustry/mods/`
   - **Windows:** `%appdata%/Mindustry/mods/`
   - **Linux:** `~/.local/share/Mindustry/mods/`
3. Restart Mindustry and enable Beyond Serpulo under `Mods`.

## Content

### Items
- Spore Cluster, Bio-Mass, Mycelium Fiber, Mycelium Plate

### Liquids
- Spore Solution

### Units
- Core Scout, Drifter, Sporeling, Myceloid, Speeder, Infector

### Blocks
- Production, distribution, liquid, power, storage, turrets, walls, and unit factories themed around spore/fungal biology.

### Planet
- Xorinal — a full campaign planet with sectors and custom maps.

## Development

Mix of HJSON content and Java extensions under `java-src/beyondserpulo/`. Build with `./build.sh` (requires Mindustry's `desktop.jar` on the path configured in the script).

**Naming note:** the mod's internal id is `xorinal` and many content ids are prefixed `xorinal-*`. This is intentional — the mod was originally called Xorinal and renaming the id would invalidate every existing player save and schematic. The display name, jar artifact, repo, and Java package all use `BeyondSerpulo` / `beyondserpulo`. "Xorinal" now refers only to one of the three planets in the mod.

## License

MIT — see [LICENSE](LICENSE).
