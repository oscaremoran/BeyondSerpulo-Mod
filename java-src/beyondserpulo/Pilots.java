package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.math.geom.Point2;
import arc.struct.IntFloatMap;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.content.StatusEffects;
import mindustry.ctype.ContentType;
import mindustry.core.GameState;
import mindustry.entities.Effect;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Entityc;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.gen.WorldLabel;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;

public class Pilots {
    private static final float CONTRIB_THRESHOLD = 0.30f;
    public static final String[] TIER_NAMES = {"Cadet", "Recruit", "Veteran", "Elite"};

    private static final IntMap<IntFloatMap> unitDmg = new IntMap<>();
    private static final IntMap<IntFloatMap> bldgDmg = new IntMap<>();
    private static final IntMap<Block> bldgBlock = new IntMap<>();
    private static final IntMap<Float> bldgHealth = new IntMap<>();

    public static final IntMap<PilotData> byUnit = new IntMap<>();
    public static final Seq<PilotData> roster = new Seq<>();
    public static final Seq<PilotData> hallOfFame = new Seq<>();
    public static final Seq<DownedPilot> downed = new Seq<>();

    public static final long DOWN_DURATION_MS = 30_000L;
    public static final float RESCUE_RADIUS = 32f;

    public static class DownedPilot {
        public PilotData pilot;
        public String unitTypeName;
        public float x, y;
        public long deadline;
    }

    public static int pendingNameUnitId = -1;
    public static PilotData pendingNamePilot;

    private static final IntMap<Long> nextBarkAt = new IntMap<>();
    private static final long BARK_COOLDOWN_MS = 8000L;

    private static final IntMap<float[]> lastUnitPos = new IntMap<>();
    private static final IntMap<String> lastUnitType = new IntMap<>();

    private static final String SAVE_KEY = "xorinal-pilots-v1";
    private static final String DEPLOYED_KEY = "xorinal-pilots-deployed-v1";

    public static class DeployedEntry {
        public String sectorKey;
        public float x, y;
        public String unitTypeName;
        public PilotData pilot;
        public boolean mustSpawn;
    }

    public static class DeployedSave {
        public Seq<DeployedEntry> all = new Seq<>();
    }

    private static final arc.struct.ObjectMap<String, Seq<DeployedEntry>> deployedBySector = new arc.struct.ObjectMap<>();
    private static String lastSectorKey;

    public static void init() {
        PilotSpecials.load();
        load();
        loadDeployed();

        Events.on(EventType.UnitDamageEvent.class, e -> onUnitDamage(e));
        Events.on(EventType.BuildDamageEvent.class, e -> onBuildDamage(e));
        Events.on(EventType.UnitDestroyEvent.class, e -> onUnitDestroy(e));
        Events.on(EventType.BlockDestroyEvent.class, e -> onBlockDestroy(e));
        Events.on(EventType.SectorCaptureEvent.class, e -> { clearSectorBucket(currentSectorKey()); extractAlive(); });
        Events.on(EventType.SectorLoseEvent.class, e -> { clearSectorBucket(currentSectorKey()); killAllDeployed(); });
        Events.on(EventType.StateChangeEvent.class, e -> {
            try { if (e.to == GameState.State.menu) parkInSector(); } catch (Exception ex) {}
        });
        Events.on(EventType.SaveWriteEvent.class, e -> snapshotDeployed());
        Events.on(EventType.WorldLoadEvent.class, e -> Core.app.post(Pilots::reattachOnLoad));
        Events.run(EventType.Trigger.update, Pilots::tick);
        Events.run(EventType.Trigger.afterGameUpdate, Pilots::lockStats);
        Events.on(EventType.SaveLoadEvent.class, e -> { load(); loadDeployed(); });
    }

    private static String currentSectorKey() {
        try {
            var s = Vars.state.rules.sector;
            if (s == null || s.planet == null) return null;
            return s.planet.name + ":" + s.id;
        } catch (Exception ex) { return null; }
    }

    private static void clearSectorBucket(String sk) {
        if (sk == null) return;
        if (deployedBySector.remove(sk) != null) saveDeployed();
    }

    private static void lockStats() {
        if (Vars.state == null || !Vars.state.isGame()) return;
        for (IntMap.Entry<PilotData> e : byUnit.entries()) {
            Unit u = Groups.unit.getByID(e.key);
            if (u == null || !u.isValid()) continue;
            int t = e.value.tier();
            float mult = t == 3 ? 1.35f : t == 2 ? 1.20f : t == 1 ? 1.10f : 1f;
            float target = u.type.health * mult;
            if (Math.abs(u.maxHealth - target) > 0.01f) {
                u.maxHealth = target;
                if (u.health > target) u.health = target;
            }
            u.damageMultiplier = mult;
        }
    }

    private static void onUnitDamage(EventType.UnitDamageEvent e) {
        try {
            Bullet b = e.bullet;
            Unit victim = e.unit;
            if (b == null || victim == null) return;
            Unit shooter = ownerUnit(b);
            if (shooter == null || !shooter.isValid()) return;
            if (Vars.player == null) return;
            if (shooter.team != Vars.player.team()) return;
            if (shooter.team == victim.team) return;
            if (!PilotSpecials.isCombat(shooter.type)) return;

            IntFloatMap m = unitDmg.get(victim.id);
            if (m == null) { m = new IntFloatMap(); unitDmg.put(victim.id, m); }
            m.increment(shooter.id, 0f, b.damage);

            applyVampire(shooter, b.damage);
        } catch (Exception ex) {}
    }

    private static void onBuildDamage(EventType.BuildDamageEvent e) {
        try {
            Bullet b = e.source;
            Building build = e.build;
            if (b == null || build == null || build.tile == null) return;
            Unit shooter = ownerUnit(b);
            if (shooter == null || !shooter.isValid()) return;
            if (Vars.player == null) return;
            if (shooter.team != Vars.player.team()) return;
            if (shooter.team == build.team) return;
            if (!PilotSpecials.isCombat(shooter.type)) return;

            int pos = Point2.pack(build.tile.x, build.tile.y);
            IntFloatMap m = bldgDmg.get(pos);
            if (m == null) { m = new IntFloatMap(); bldgDmg.put(pos, m); }
            m.increment(shooter.id, 0f, b.damage);
            bldgBlock.put(pos, build.block);
            bldgHealth.put(pos, build.maxHealth);

            applyVampire(shooter, b.damage);
        } catch (Exception ex) {}
    }

    private static Unit ownerUnit(Bullet b) {
        Entityc o = b.owner;
        return (o instanceof Unit) ? (Unit) o : null;
    }

    private static void applyVampire(Unit shooter, float dmg) {
        PilotData p = byUnit.get(shooter.id);
        if (p == null || p.tier() < 3) return;
        if (PilotSpecials.of(shooter.type) != PilotSpecials.Special.VAMPIRE) return;
        shooter.heal(dmg * 0.20f);
    }

    private static void onUnitDestroy(EventType.UnitDestroyEvent e) {
        try {
            Unit u = e.unit;
            if (u == null) return;
            IntFloatMap dmg = unitDmg.remove(u.id);
            if (dmg != null && Vars.player != null && u.team != Vars.player.team()) {
                int weight = Math.max(1, Math.round(u.maxHealth));
                awardCredits(dmg, u.maxHealth, weight, u.x, u.y);
            }
            PilotData dead = byUnit.remove(u.id);
            if (dead != null) {
                onPilotDowned(dead, u);
            }
            if (pendingNameUnitId == u.id) {
                pendingNameUnitId = -1;
                pendingNamePilot = null;
            }
        } catch (Exception ex) {}
    }

    private static void onBlockDestroy(EventType.BlockDestroyEvent e) {
        try {
            if (e.tile == null) return;
            int pos = Point2.pack(e.tile.x, e.tile.y);
            IntFloatMap dmg = bldgDmg.remove(pos);
            Block b = bldgBlock.remove(pos);
            Float hp = bldgHealth.remove(pos);
            if (dmg == null || b == null || hp == null) return;
            int weight = Math.max(1, Math.round(hp));
            awardCredits(dmg, hp, weight, e.tile.worldx(), e.tile.worldy());
        } catch (Exception ex) {}
    }

    private static void awardCredits(IntFloatMap dmg, float baseHealth, int weight, float x, float y) {
        if (baseHealth <= 0f) baseHealth = 1f;
        for (IntFloatMap.Entry entry : dmg) {
            if (entry.value / baseHealth < CONTRIB_THRESHOLD) continue;
            Unit shooter = Groups.unit.getByID(entry.key);
            if (shooter == null || !shooter.isValid()) continue;
            if (!PilotSpecials.isCombat(shooter.type)) continue;

            PilotData p = byUnit.get(shooter.id);
            if (p == null) {
                p = new PilotData();
                p.unitTypeName = shooter.type.name;
                p.baseHealth = shooter.type.health;
                p.alive = true;
                p.activeUnitId = shooter.id;
                byUnit.put(shooter.id, p);
            }
            int oldTier = p.tier();
            p.killPoints += weight;
            int newTier = p.tier();
            if (newTier > oldTier) onTierUp(p, shooter, newTier);
        }
    }

    private static void onTierUp(PilotData p, Unit u, int newTier) {
        if (Vars.ui == null) return;
        if (newTier == 1 && p.name == null) {
            pendingNameUnitId = u.id;
            pendingNamePilot = p;
            Vars.ui.showInfoToast(
                "[gold]Unit promotion available![] " + u.type.localizedName + " at ("
                    + (int) (u.x / 8) + ", " + (int) (u.y / 8) + ") — click the [accent]?[] above the unit to name it",
                6f);
        } else {
            String n = (p.name != null) ? p.name : u.type.localizedName;
            Vars.ui.showInfoToast("[gold]" + n + "[] promoted to [accent]" + TIER_NAMES[newTier] + "[]!", 3f);
        }
    }

    public static void openNamePrompt(PilotData p) {
        if (p == null || p.name != null || Vars.ui == null) return;
        UnitType t = lookupType(p.unitTypeName);
        String typeName = t != null ? t.localizedName : "Unit";
        Vars.ui.showTextInput("Name your " + typeName, "Pilot name", "", name -> {
            commitName(p, name);
            if (pendingNamePilot == p) {
                pendingNamePilot = null;
                pendingNameUnitId = -1;
            }
        });
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame()) return;
        String sk = currentSectorKey();
        if (sk != null) lastSectorKey = sk;
        for (IntMap.Entry<PilotData> e : byUnit.entries()) {
            Unit u = Groups.unit.getByID(e.key);
            if (u == null || !u.isValid()) continue;
            lastUnitPos.put(u.id, new float[]{u.x, u.y});
            if (u.type != null) lastUnitType.put(u.id, u.type.name);
            int t = e.value.tier();
            mindustry.type.StatusEffect tierFx = PilotEffects.forTier(t);
            if (tierFx != null) u.apply(tierFx, 60f);
            if (t >= 3) {
                PilotSpecials.Special s = PilotSpecials.of(u.type);
                if (s == PilotSpecials.Special.OVERDRIVE) u.apply(StatusEffects.overdrive, 60f);
                else if (s == PilotSpecials.Special.SPEED) u.apply(StatusEffects.fast, 60f);
                else if (s == PilotSpecials.Special.BOSS) u.apply(StatusEffects.boss, 60f);
                else if (s == PilotSpecials.Special.REGEN && u.health < u.maxHealth) {
                    u.heal(u.maxHealth * 0.10f / 180f);
                }
            }
            maybeBark(u, e.value);
        }
        tickDowned();
    }

    private static void maybeBark(Unit u, PilotData p) {
        try {
            if (p.name == null) return;
            long now = System.currentTimeMillis();
            Long next = nextBarkAt.get(u.id);
            if (next != null && now < next) return;

            if (u.health < u.maxHealth * 0.30f) {
                bark(u, "[#ffaa55]Needs support![]");
                nextBarkAt.put(u.id, now + BARK_COOLDOWN_MS);
                return;
            }

            float ownHp = u.type == null ? u.maxHealth : u.type.health;
            if (ownHp <= 0f) return;
            float searchRange = 160f * 8f;
            float searchSq = searchRange * searchRange;
            Unit best = null;
            float bestDist = searchSq;
            for (Unit other : Groups.unit) {
                if (other == null || !other.isValid()) continue;
                if (other.team == u.team) continue;
                float d = other.dst2(u);
                if (d < bestDist) { bestDist = d; best = other; }
            }
            if (best == null) return;
            float threatHp = best.type == null ? best.maxHealth : best.type.health;
            float ratio = threatHp / ownHp;
            if (ratio > 4f) {
                bark(u, "[#ff5555]In trouble![]");
                nextBarkAt.put(u.id, now + BARK_COOLDOWN_MS);
            } else if (ratio < 1f) {
                bark(u, "[#7fff7f]Doing well[]");
                nextBarkAt.put(u.id, now + BARK_COOLDOWN_MS);
            }
        } catch (Exception ex) {}
    }

    private static void bark(Unit u, String text) {
        try {
            WorldLabel l = WorldLabel.create();
            l.text = text;
            l.set(u.x, u.y + u.hitSize / 2f + 6f);
            l.fontSize = 0.9f;
            l.flags = (byte) (WorldLabel.flagOutline | WorldLabel.flagBackground);
            l.add();
            arc.util.Time.run(90f, l::remove);
        } catch (Exception ex) {}
    }

    private static void extractAlive() {
        for (IntMap.Entry<PilotData> e : byUnit.entries()) {
            PilotData p = e.value;
            if (p.alive && p.name != null && !roster.contains(p, true)) {
                p.activeUnitId = -1;
                roster.add(p);
            }
        }
        for (DownedPilot d : downed) {
            if (d == null || d.pilot == null) continue;
            if (d.pilot.name != null && !roster.contains(d.pilot, true)) {
                d.pilot.alive = true;
                d.pilot.activeUnitId = -1;
                roster.add(d.pilot);
            }
        }
        downed.clear();
        byUnit.clear();
        nextBarkAt.clear();
        lastUnitPos.clear();
        lastUnitType.clear();
        unitDmg.clear(); bldgDmg.clear(); bldgBlock.clear(); bldgHealth.clear();
        save();
    }

    private static void killAllDeployed() {
        Seq<PilotData> dying = new Seq<>();
        for (IntMap.Entry<PilotData> e : byUnit.entries()) dying.add(e.value);
        byUnit.clear();
        nextBarkAt.clear();
        lastUnitPos.clear();
        lastUnitType.clear();
        unitDmg.clear(); bldgDmg.clear(); bldgBlock.clear(); bldgHealth.clear();
        for (PilotData p : dying) {
            p.alive = false;
            onPilotDeath(p);
        }
        for (DownedPilot d : downed) {
            if (d == null || d.pilot == null) continue;
            d.pilot.alive = false;
            onPilotDeath(d.pilot);
        }
        downed.clear();
        save();
    }

    private static void onPilotDowned(PilotData p, Unit u) {
        p.activeUnitId = -1;
        if (p.name == null) {
            p.alive = false;
            onPilotDeath(p);
            return;
        }
        DownedPilot d = new DownedPilot();
        d.pilot = p;
        d.unitTypeName = u.type == null ? p.unitTypeName : u.type.name;
        d.x = u.x; d.y = u.y;
        d.deadline = System.currentTimeMillis() + DOWN_DURATION_MS;
        downed.add(d);
        nextBarkAt.remove(u.id);
        lastUnitPos.remove(u.id);
        lastUnitType.remove(u.id);
        if (Vars.ui != null) Vars.ui.showInfoToast("[red]" + p.name + " is down[]", 3f);
    }

    private static void tickDowned() {
        if (downed.isEmpty()) return;
        long now = System.currentTimeMillis();
        float radSq = RESCUE_RADIUS * RESCUE_RADIUS;
        for (int i = downed.size - 1; i >= 0; i--) {
            DownedPilot d = downed.get(i);
            if (d == null || d.pilot == null) { downed.remove(i); continue; }
            if (Vars.player != null) {
                Unit rescuer = null;
                for (Unit u : Groups.unit) {
                    if (u == null || !u.isValid()) continue;
                    if (u.team != Vars.player.team()) continue;
                    float dx = u.x - d.x, dy = u.y - d.y;
                    if (dx * dx + dy * dy <= radSq) { rescuer = u; break; }
                }
                if (rescuer != null && tryRescue(d)) {
                    downed.remove(i);
                    continue;
                }
            }
            if (now >= d.deadline) {
                downed.remove(i);
                d.pilot.alive = false;
                onPilotDeath(d.pilot);
            }
        }
    }

    private static boolean tryRescue(DownedPilot d) {
        try {
            UnitType type = lookupType(d.unitTypeName);
            if (type == null) return false;
            if (Vars.player == null) return false;
            Unit nu = type.spawn(Vars.player.team(), d.x, d.y);
            if (nu == null) return false;
            d.pilot.alive = true;
            d.pilot.activeUnitId = nu.id;
            byUnit.put(nu.id, d.pilot);
            if (Vars.ui != null) Vars.ui.showInfoToast("[lime]" + d.pilot.name + " rescued![]", 3f);
            return true;
        } catch (Exception ex) { return false; }
    }

    public static int downedCount() { return downed.size; }

    public static long earliestDownedMillisRemaining() {
        if (downed.isEmpty()) return 0L;
        long now = System.currentTimeMillis();
        long best = Long.MAX_VALUE;
        for (DownedPilot d : downed) if (d.deadline < best) best = d.deadline;
        return Math.max(0L, best - now);
    }

    private static void onPilotDeath(PilotData p) {
        roster.remove(p, true);
        if (p.name == null) return;
        hallOfFame.add(p.copy());
        hallOfFame.sort((a, b) -> Integer.compare(b.killPoints, a.killPoints));
        while (hallOfFame.size > 3) hallOfFame.remove(hallOfFame.size - 1);
        if (Vars.ui != null) Vars.ui.showInfoToast("[red]" + p.name + " has fallen.[]", 4f);
        save();
    }

    public static int deployCost(PilotData p) {
        UnitType t = lookupType(p.unitTypeName);
        if (t == null) return 0;
        int base = (int) (t.health * 0.5f);
        return base + (int) (base * (0.25f + 0.25f * p.tier()));
    }

    public static Item deployCostItem() { return Items.graphite; }

    public static UnitType lookupType(String name) {
        if (name == null) return null;
        return Vars.content.<UnitType>getByName(ContentType.unit, name);
    }

    public static float[] openSpotNear(Building core, UnitType type) {
        float[] fallback = core == null ? new float[]{0f, 0f} : new float[]{core.x, core.y};
        if (core == null || type == null) return fallback;
        int cx = core.tileX(), cy = core.tileY();
        for (int r = 1; r <= 14; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    mindustry.world.Tile t = Vars.world.tile(cx + dx, cy + dy);
                    if (t == null) continue;
                    if (t.build != null) continue;
                    if (t.solid()) continue;
                    if (!type.flying && t.floor() != null && t.floor().isDeep()) continue;
                    return new float[]{t.worldx(), t.worldy()};
                }
            }
        }
        return fallback;
    }

    public static boolean tryDeploy(PilotData p) {
        if (p == null || !p.alive || p.activeUnitId != -1) return false;
        if (Vars.state == null || !Vars.state.isGame()) {
            if (Vars.ui != null) Vars.ui.showInfoToast("[red]Must be in a sector to deploy[]", 2f);
            return false;
        }
        UnitType t = lookupType(p.unitTypeName);
        if (t == null) return false;
        Building core = Vars.player == null ? null : Vars.player.team().core();
        if (core == null) {
            if (Vars.ui != null) Vars.ui.showInfoToast("[red]No core to deploy from[]", 2f);
            return false;
        }
        int cost = deployCost(p);
        Item it = deployCostItem();
        if (core.items == null || core.items.get(it) < cost) {
            if (Vars.ui != null) Vars.ui.showInfoToast("[red]Need " + cost + " " + it.localizedName + "[]", 2f);
            return false;
        }
        core.items.remove(it, cost);
        float[] spot = openSpotNear(core, t);
        Unit u = t.spawn(Vars.player.team(), spot[0], spot[1]);
        if (u == null) return false;
        p.activeUnitId = u.id;
        roster.remove(p, true);
        byUnit.put(u.id, p);
        save();
        if (Vars.ui != null) Vars.ui.showInfoToast("[gold]" + p.name + "[] deployed", 2f);
        return true;
    }

    private static void snapshotDeployed() {
        try {
            String sk = currentSectorKey();
            if (sk == null) sk = lastSectorKey;
            if (sk == null) return;
            if (byUnit.isEmpty()) return;
            Seq<DeployedEntry> entries = new Seq<>();
            for (IntMap.Entry<PilotData> e : byUnit.entries()) {
                PilotData p = e.value;
                if (!p.alive) continue;
                Unit u = Groups.unit.getByID(e.key);
                float x, y; String typeName;
                if (u != null && u.isValid()) {
                    x = u.x; y = u.y;
                    typeName = u.type == null ? p.unitTypeName : u.type.name;
                } else {
                    float[] pos = lastUnitPos.get(e.key);
                    if (pos == null) continue;
                    x = pos[0]; y = pos[1];
                    typeName = lastUnitType.get(e.key);
                    if (typeName == null) typeName = p.unitTypeName;
                }
                DeployedEntry de = new DeployedEntry();
                de.sectorKey = sk;
                de.x = x; de.y = y;
                de.unitTypeName = typeName;
                de.pilot = p;
                entries.add(de);
            }
            if (!entries.isEmpty()) {
                deployedBySector.put(sk, entries);
                saveDeployed();
            }
            Log.info("[BeyondSerpulo] snapshotDeployed: " + entries.size + " entries for " + sk);
        } catch (Exception ex) { Log.err("[BeyondSerpulo] snapshotDeployed: " + ex); }
    }

    private static void parkInSector() {
        try {
            String sk = lastSectorKey;
            if (sk == null) sk = currentSectorKey();
            Log.info("[BeyondSerpulo] parkInSector: sector=" + sk + " byUnit=" + byUnit.size);
            if (sk == null) { byUnit.clear(); nextBarkAt.clear(); return; }
            if (byUnit.isEmpty()) return;

            Seq<DeployedEntry> entries = new Seq<>();
            for (IntMap.Entry<PilotData> e : byUnit.entries()) {
                PilotData p = e.value;
                if (!p.alive) continue;
                float x, y;
                String typeName;
                Unit u = Groups.unit.getByID(e.key);
                if (u != null && u.isValid()) {
                    x = u.x; y = u.y;
                    typeName = u.type == null ? p.unitTypeName : u.type.name;
                } else {
                    float[] pos = lastUnitPos.get(e.key);
                    if (pos == null) continue;
                    x = pos[0]; y = pos[1];
                    typeName = lastUnitType.get(e.key);
                    if (typeName == null) typeName = p.unitTypeName;
                }
                DeployedEntry de = new DeployedEntry();
                de.sectorKey = sk;
                de.x = x; de.y = y;
                de.unitTypeName = typeName;
                p.activeUnitId = -1;
                de.pilot = p;
                entries.add(de);
            }
            if (!entries.isEmpty()) {
                deployedBySector.put(sk, entries);
            }
            Log.info("[BeyondSerpulo] parkInSector: wrote " + entries.size + " entries for " + sk);
            byUnit.clear();
            nextBarkAt.clear();
            lastUnitPos.clear();
            lastUnitType.clear();
            unitDmg.clear(); bldgDmg.clear(); bldgBlock.clear(); bldgHealth.clear();
            for (DownedPilot d : downed) {
                if (d == null || d.pilot == null) continue;
                d.pilot.alive = false;
                onPilotDeath(d.pilot);
            }
            downed.clear();
            saveDeployed();
        } catch (Exception ex) { Log.err("[BeyondSerpulo] parkInSector: " + ex); }
    }

    private static void reattachOnLoad() {
        try {
            String sk = currentSectorKey();
            if (sk == null) { Log.info("[BeyondSerpulo] reattach: no sector key"); return; }
            lastSectorKey = sk;
            Seq<DeployedEntry> bucket = deployedBySector.get(sk);
            if (bucket == null || bucket.isEmpty()) { Log.info("[BeyondSerpulo] reattach: no parked pilots for " + sk); return; }
            int totalUnits = 0, friendlyUnits = 0;
            for (Unit u : Groups.unit) {
                if (u == null || !u.isValid()) continue;
                totalUnits++;
                if (Vars.player != null && u.team == Vars.player.team()) friendlyUnits++;
            }
            Log.info("[BeyondSerpulo] reattach: " + bucket.size + " parked pilots for " + sk
                + "; totalUnits=" + totalUnits + " friendly=" + friendlyUnits
                + " playerTeam=" + (Vars.player == null ? "null" : Vars.player.team().name));

            float radSq = (32f * 8f) * (32f * 8f);
            Seq<PilotData> dead = new Seq<>();
            for (DeployedEntry de : bucket) {
                if (de == null || de.pilot == null) continue;

                if (de.mustSpawn) {
                    UnitType type = lookupType(de.unitTypeName);
                    Building core = Vars.player == null ? null : Vars.player.team().core();
                    if (type != null && core != null) {
                        float[] spot = openSpotNear(core, type);
                        Unit nu = type.spawn(Vars.player.team(), spot[0], spot[1]);
                        if (nu != null) {
                            de.pilot.alive = true;
                            de.pilot.activeUnitId = nu.id;
                            byUnit.put(nu.id, de.pilot);
                            continue;
                        }
                    }
                    dead.add(de.pilot);
                    continue;
                }

                Unit best = null;
                float bestDist = radSq;
                Unit anyOfType = null;
                float anyDist = Float.MAX_VALUE;
                int matchCount = 0;
                for (Unit u : Groups.unit) {
                    if (u == null || !u.isValid()) continue;
                    if (Vars.player != null && u.team != Vars.player.team()) continue;
                    if (u.type == null) continue;
                    if (de.unitTypeName != null && !de.unitTypeName.equals(u.type.name)) continue;
                    if (byUnit.containsKey(u.id)) continue;
                    matchCount++;
                    float dx = u.x - de.x, dy = u.y - de.y;
                    float d = dx * dx + dy * dy;
                    if (d < bestDist) { bestDist = d; best = u; }
                    if (d < anyDist) { anyDist = d; anyOfType = u; }
                }
                Log.info("[BeyondSerpulo] reattach: candidates of type=" + de.unitTypeName + " count=" + matchCount
                    + " savedAt=(" + de.x + "," + de.y + ")");
                Unit pick = best != null ? best : anyOfType;
                if (pick != null) {
                    de.pilot.alive = true;
                    de.pilot.activeUnitId = pick.id;
                    byUnit.put(pick.id, de.pilot);
                    Log.info("[BeyondSerpulo] reattach: " + (de.pilot.name == null ? "?" : de.pilot.name)
                        + " -> unit " + pick.id + " (" + pick.type.name + ") dist=" + (float)Math.sqrt(anyDist));
                } else {
                    Log.info("[BeyondSerpulo] reattach: no matching unit for "
                        + (de.pilot.name == null ? "?" : de.pilot.name)
                        + " type=" + de.unitTypeName);
                    dead.add(de.pilot);
                }
            }
            deployedBySector.remove(sk);
            saveDeployed();

            for (PilotData p : dead) {
                p.alive = false;
                onPilotDeath(p);
            }
        } catch (Exception ex) { Log.err("[BeyondSerpulo] reattachOnLoad: " + ex); }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static void saveDeployed() {
        try {
            DeployedSave s = new DeployedSave();
            for (var entry : deployedBySector.entries()) s.all.addAll(entry.value);
            Core.settings.putJson(DEPLOYED_KEY, s);
        } catch (Exception ex) { Log.err("[BeyondSerpulo] saveDeployed: " + ex); }
    }

    private static void loadDeployed() {
        try {
            Object obj = Core.settings.getJson(DEPLOYED_KEY, DeployedSave.class, DeployedSave::new);
            deployedBySector.clear();
            if (!(obj instanceof DeployedSave)) return;
            DeployedSave s = (DeployedSave) obj;
            if (s.all == null) return;
            for (DeployedEntry de : s.all) {
                if (de == null || de.sectorKey == null) continue;
                Seq<DeployedEntry> bucket = deployedBySector.get(de.sectorKey);
                if (bucket == null) { bucket = new Seq<>(); deployedBySector.put(de.sectorKey, bucket); }
                bucket.add(de);
            }
        } catch (Exception ex) { Log.err("[BeyondSerpulo] loadDeployed: " + ex); }
    }

    public static boolean parkPilotForLaunch(PilotData pilot, Unit unit, String destSectorKey) {
        try {
            if (pilot == null || unit == null || destSectorKey == null) return false;
            byUnit.remove(unit.id);
            nextBarkAt.remove(unit.id);

            DeployedEntry de = new DeployedEntry();
            de.sectorKey = destSectorKey;
            de.x = -1f; de.y = -1f;
            de.mustSpawn = true;
            de.unitTypeName = unit.type == null ? pilot.unitTypeName : unit.type.name;
            pilot.activeUnitId = -1;
            pilot.alive = true;
            de.pilot = pilot;

            Seq<DeployedEntry> bucket = deployedBySector.get(destSectorKey);
            if (bucket == null) { bucket = new Seq<>(); deployedBySector.put(destSectorKey, bucket); }
            bucket.add(de);

            unit.remove();
            saveDeployed();
            return true;
        } catch (Exception ex) { Log.err("[BeyondSerpulo] parkPilotForLaunch: " + ex); return false; }
    }

    public static void clearHallOfFame() {
        hallOfFame.clear();
        save();
    }

    public static void commitName(PilotData p, String name) {
        if (p == null || name == null) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        if (trimmed.length() > 24) trimmed = trimmed.substring(0, 24);
        p.name = trimmed;
        save();
    }

    public static class SaveBundle {
        public Seq<PilotData> roster = new Seq<>();
        public Seq<PilotData> hallOfFame = new Seq<>();
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    public static void save() {
        try {
            SaveBundle b = new SaveBundle();
            b.roster.addAll(roster);
            b.hallOfFame.addAll(hallOfFame);
            Core.settings.putJson(SAVE_KEY, b);
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] Pilots.save: " + ex);
        }
    }

    public static void load() {
        try {
            Object obj = Core.settings.getJson(SAVE_KEY, SaveBundle.class, SaveBundle::new);
            if (!(obj instanceof SaveBundle)) return;
            SaveBundle b = (SaveBundle) obj;
            roster.clear(); hallOfFame.clear();
            if (b.roster != null) roster.addAll(b.roster);
            if (b.hallOfFame != null) hallOfFame.addAll(b.hallOfFame);
            for (PilotData p : roster) p.activeUnitId = -1;
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] Pilots.load: " + ex);
        }
    }
}
