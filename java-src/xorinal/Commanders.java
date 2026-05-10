package xorinal;

import arc.Events;
import arc.struct.ObjectMap;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.util.Align;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.StatusEffects;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.game.SpawnGroup;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.UnitType;
import mindustry.ui.Fonts;

public class Commanders {
    // Per-unit-type commander names. Key = UnitType.name (e.g. "dagger"), value = pool of names.
    // Edit freely — add new entries for any unit type. Falls back to DEFAULT_NAMES if a type isn't listed.
    public static final ObjectMap<String, String[]> NAMES_BY_TYPE = ObjectMap.of(
        // Serpulo ground T1-T5
        "dagger",   new String[]{"Pricker", "Stinger", "Whisper", "Nick"},
        "mace",     new String[]{"Bonebreak", "Thudd", "Crusher", "Ironjaw"},
        "fortress", new String[]{"Bastion", "Warden", "Bulwark", "Siegehead"},
        "scepter",  new String[]{"Lord Rust", "Magnus", "Ironclaw", "Sovereign"},
        "reign",    new String[]{"The Black Sigil", "Tyrant", "Dread Marshal", "Kael the Unending"},

        // Serpulo air
        "flare",    new String[]{"Sparrow", "Wisp", "Flicker"},
        "horizon",  new String[]{"Stormcrow", "Vesper", "Zephyr"},
        "zenith",   new String[]{"Skylord", "Tempest", "Aether"},
        "antumbra", new String[]{"Firebringer", "Pall", "Umbra"},
        "eclipse",  new String[]{"Voidbringer", "Nightfall", "The Last Sun"},

        // Serpulo naval
        "risso",    new String[]{"Reefcutter", "Brine"},
        "minke",    new String[]{"Tidehunter", "Salt"},
        "bryde",    new String[]{"Maelstrom", "Deepkeel"},
        "sei",      new String[]{"Abysswalker", "Trench"},
        "omura",    new String[]{"Leviathan", "The Drowned King"},

        // Serpulo crawlers / support
        "crawler",  new String[]{"Skitter", "Twitch", "Pop"},
        "atrax",    new String[]{"Acidfang", "Drip"},
        "spiroct",  new String[]{"Sporequeen", "Coil"},
        "arkyid",   new String[]{"Eight-Legs", "Widow Marshal"},
        "toxopid",  new String[]{"Venomlord", "The Eight-Crowned"},

        // Erekir
        "stell",    new String[]{"Anvil", "Bolt"},
        "locus",    new String[]{"Storm-Locus", "Spire"},
        "precept",  new String[]{"Prefect", "Praetor"},
        "vanquish", new String[]{"Vanquisher Prime", "Iron Vow"},
        "conquer",  new String[]{"The All-Conqueror", "Final Edict"},

        "merui",    new String[]{"Glide", "Murmur"},
        "cleroi",   new String[]{"Chant", "Choir"},
        "anthicus", new String[]{"Hymn-Bearer", "Cantor"},
        "tecta",    new String[]{"Roof of Sky", "Tecta the Vast"},
        "collaris", new String[]{"Crowned One", "Yokemaster"},

        "elude",    new String[]{"Veil", "Slip"},
        "avert",    new String[]{"Turnaway", "Shun"},
        "obviate",  new String[]{"Erasure", "Forfend"},
        "quell",    new String[]{"Hush", "Silencer"},
        "disrupt",  new String[]{"The Unraveler", "Disjunction"}
    );

    // Used when a unit type has no entry in NAMES_BY_TYPE.
    public static final String[] DEFAULT_NAMES = {
        "[ERROR]", "[ERROR]", "[ERROR]", "[ERROR]", "[ERROR]", "[ERROR]"
    };

    public static final float AURA_RADIUS = 80f;
    public static final float HP_MULT = 1.5f;
    public static final float DMG_MULT = 1.25f;
    public static final float ALLY_DMG_MULT = 1.25f;
    public static final float ALLY_DMG_TAKEN_MULT = 0.80f;
    public static final long ROUT_DURATION_MS = 10_000L;
    public static final int WAVE_THRESHOLD = 10;

    private static UnitType pendingType;

    public static int commanderId = -1;
    public static String commanderName;
    private static long routEndsAt;

    public static void init() {
        Events.on(EventType.WaveEvent.class, e -> onWave());
        Events.on(EventType.UnitSpawnEvent.class, e -> onSpawn(e.unit));
        Events.on(EventType.UnitDestroyEvent.class, e -> onDestroy(e.unit));
        Events.on(EventType.UnitDamageEvent.class, e -> onDamage(e));
        Events.on(EventType.WorldLoadEvent.class, e -> reset());
        Events.on(EventType.StateChangeEvent.class, e -> {
            try { if (e.to == GameState.State.menu) reset(); } catch (Exception ex) {}
        });
        Events.run(EventType.Trigger.update, Commanders::tick);
        Events.run(EventType.Trigger.drawOver, Commanders::draw);
    }

    private static void reset() {
        pendingType = null;
        commanderId = -1;
        commanderName = null;
        routEndsAt = 0L;
    }

    private static void onWave() {
        try {
            pendingType = null;
            if (Vars.state == null || Vars.state.rules == null || Vars.state.rules.spawns == null) return;
            int wave = Vars.state.wave - 1;
            int total = 0;
            UnitType strongest = null;
            float bestHp = 0f;
            for (SpawnGroup g : Vars.state.rules.spawns) {
                if (g == null || g.type == null) continue;
                int n = g.getSpawned(wave);
                if (n <= 0) continue;
                total += n;
                if (g.type.health > bestHp) { bestHp = g.type.health; strongest = g.type; }
            }
            if (total > WAVE_THRESHOLD && strongest != null) {
                pendingType = strongest;
            }
        } catch (Exception ex) {}
    }

    private static void onSpawn(Unit u) {
        try {
            if (pendingType == null) return;
            if (u == null || u.type != pendingType) return;
            if (Vars.player != null && u.team == Vars.player.team()) return;
            promote(u);
            pendingType = null;
        } catch (Exception ex) {}
    }

    private static String pickName(UnitType type) {
        String[] pool = type == null ? null : NAMES_BY_TYPE.get(type.name);
        if (pool == null || pool.length == 0) pool = DEFAULT_NAMES;
        return pool[Mathf.random(pool.length - 1)];
    }

    private static void promote(Unit u) {
        commanderId = u.id;
        commanderName = pickName(u.type);
        u.maxHealth = u.type.health * HP_MULT;
        u.health = u.maxHealth;
        u.damageMultiplier = DMG_MULT;
        if (Vars.ui != null) Vars.ui.showInfoToast("[red]" + commanderName + "[] commands the wave!", 4f);
    }

    private static void onDestroy(Unit u) {
        if (u == null || u.id != commanderId) return;
        startRout();
        commanderId = -1;
        commanderName = null;
    }

    private static void startRout() {
        routEndsAt = System.currentTimeMillis() + ROUT_DURATION_MS;
        if (Vars.ui != null) Vars.ui.showInfoToast("[lime]Commander down — enemy routed![]", 3f);
    }

    private static void onDamage(EventType.UnitDamageEvent e) {
        try {
            if (commanderId < 0 || e == null || e.bullet == null) return;
            Unit victim = e.unit;
            if (victim == null) return;
            if (Vars.player == null) return;
            if (victim.team == Vars.player.team()) return;
            Unit cmd = Groups.unit.getByID(commanderId);
            if (cmd == null || !cmd.isValid()) return;
            if (victim.id == cmd.id) return;
            if (victim.dst2(cmd) > AURA_RADIUS * AURA_RADIUS) return;
            float heal = e.bullet.damage * (1f - ALLY_DMG_TAKEN_MULT);
            if (heal > 0f) victim.heal(heal);
        } catch (Exception ex) {}
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame()) return;

        if (commanderId >= 0) {
            Unit cmd = Groups.unit.getByID(commanderId);
            if (cmd == null || !cmd.isValid()) {
                commanderId = -1;
                commanderName = null;
            } else {
                float target = cmd.type.health * HP_MULT;
                if (Math.abs(cmd.maxHealth - target) > 0.01f) {
                    cmd.maxHealth = target;
                    if (cmd.health > target) cmd.health = target;
                }
                cmd.damageMultiplier = DMG_MULT;

                float radSq = AURA_RADIUS * AURA_RADIUS;
                for (Unit u : Groups.unit) {
                    if (u == null || !u.isValid() || u.id == cmd.id) continue;
                    if (u.team != cmd.team) continue;
                    if (u.dst2(cmd) > radSq) continue;
                    u.damageMultiplier = ALLY_DMG_MULT;
                }
            }
        }

        if (System.currentTimeMillis() < routEndsAt) {
            for (Unit u : Groups.unit) {
                if (u == null || !u.isValid()) continue;
                if (Vars.player != null && u.team == Vars.player.team()) continue;
                u.apply(StatusEffects.freezing, 60f);
            }
            float remTicks = ((routEndsAt - System.currentTimeMillis()) / 1000f) * 60f;
            if (Vars.state.wavetime < remTicks) Vars.state.wavetime = remTicks;
        }
    }

    public static long routMillisRemaining() {
        return Math.max(0L, routEndsAt - System.currentTimeMillis());
    }

    private static void draw() {
        if (Vars.state == null || !Vars.state.isGame()) return;
        if (commanderId < 0) return;
        Unit u = Groups.unit.getByID(commanderId);
        if (u == null || !u.isValid()) return;

        float prevZ = Draw.z();
        Draw.z(Layer.overlayUI);

        float pulse = 0.4f + 0.6f * Mathf.absin(Time.time, 12f, 1f);
        Draw.color(1f, 0.2f, 0.2f, 0.55f * pulse);
        Lines.stroke(1.2f);
        Lines.circle(u.x, u.y, AURA_RADIUS);
        Draw.color();

        Fonts.def.getData().setScale(0.25f);
        Fonts.def.setUseIntegerPositions(false);
        String label = "[red]" + (commanderName == null ? "Commander" : commanderName) + "[]";
        String plain = commanderName == null ? "Commander" : commanderName;
        float w = 0.25f * (plain.length() * 7f + 6f);
        float h = 3.5f;
        float labelY = u.y + u.hitSize / 2f + 14f;
        Draw.color(0f, 0f, 0f, 0.6f);
        Fill.rect(u.x, labelY, w, h);
        Fonts.def.setColor(Color.white);
        Fonts.def.draw(label, u.x, labelY + h / 2f, Align.center);

        Fonts.def.getData().setScale(1f);
        Fonts.def.setColor(Color.white);
        Draw.reset();
        Draw.z(prevZ);
    }
}
