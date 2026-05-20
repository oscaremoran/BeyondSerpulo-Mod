package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.IntFloatMap;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.UnitCreateEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock;

public class Ragnarok {
    public static final float PROTOCOL_DURATION = 25f * 60f;
    public static final float FEAR_RADIUS = 220f;
    public static final float TRAMPLE_RADIUS = 60f;
    public static final float TRAMPLE_DAMAGE = 220f;
    public static final float TRAMPLE_KNOCKBACK = 6f;
    public static final float PROTOCOL_AOE_RADIUS = 140f;
    public static final float PROTOCOL_AOE_DPS = 240f;
    public static final float DEATH_BLAST_RADIUS = 360f;
    public static final float DEATH_BLAST_DAMAGE = 6000f;
    public static final float ZANTETSUKEN_CHARGE = 10f * 60f;

    // 0 = ready (can cast Protocol), 1 = protocol active, 2 = protocol expired (Zantetsuken available), 3 = charging Zantetsuken
    private static final IntFloatMap protocolState = new IntFloatMap();
    private static final IntFloatMap protocolTimer = new IntFloatMap();
    private static final IntFloatMap zantCharge = new IntFloatMap();
    private static final IntSet announced = new IntSet();

    private static Table hudWidget;
    private static Label hudLabel;

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> {
            protocolState.clear();
            protocolTimer.clear();
            zantCharge.clear();
            announced.clear();
            setupHud();
        });
        Events.on(UnitCreateEvent.class, e -> {
            try {
                if (BossUnits.ragnarok == null || e.unit == null || e.unit.type != BossUnits.ragnarok) return;
                if (announced.add(e.unit.id)) {
                    try { Vars.ui.showInfoToast("[#ff3010]RAGNAROK HAS SPAWNED. PREPARE FOR YOUR DOOM.[]", 4f); } catch (Exception ex) {}
                    try { Sounds.explosion.at(e.unit.x, e.unit.y); } catch (Exception ex) {}
                }
            } catch (Exception ex) {}
        });
        Events.on(UnitDestroyEvent.class, Ragnarok::onUnitDestroyed);
        Events.run(Trigger.update, Ragnarok::tick);
    }

    private static void setupHud() {
        try {
            if (hudWidget != null) { hudWidget.remove(); hudWidget = null; }
            Table wrap = new Table();
            wrap.setFillParent(true);
            wrap.bottom();
            Table card = new Table();
            try { card.background(Tex.button); } catch (Exception ex) {}
            hudLabel = new Label("");
            card.add(hudLabel).pad(8f);
            wrap.add(card).pad(180f).bottom();
            wrap.visible(Ragnarok::shouldShowHud);
            Vars.ui.hudGroup.addChild(wrap);
            hudWidget = wrap;
        } catch (Exception ex) { Log.err("[BeyondSerpulo] Ragnarok.setupHud: " + ex); }
    }

    private static boolean shouldShowHud() {
        try {
            if (Vars.player == null || BossUnits.ragnarok == null) return false;
            Unit u = Vars.player.unit();
            return u != null && !u.dead && u.type == BossUnits.ragnarok;
        } catch (Exception ex) { return false; }
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame() || Vars.state.isPaused()) return;
        if (BossUnits.ragnarok == null) return;

        for (Unit u : Groups.unit) {
            if (u.dead || u.type != BossUnits.ragnarok) continue;
            updateRagnarok(u);
        }

        cleanupDead(protocolState);
        cleanupDead(protocolTimer);
        cleanupDead(zantCharge);

        try {
            Unit pu = Vars.player == null ? null : Vars.player.unit();
            if (pu != null && !pu.dead && pu.type == BossUnits.ragnarok) updateHud(pu);
        } catch (Exception ex) {}
    }

    private static void updateRagnarok(Unit u) {
        int state = (int) protocolState.get(u.id, 0f);

        // Fear aura — applies to all enemies near Ragnarok, always
        if (BossUnits.ragnarokFear != null) {
            for (Unit e : Groups.unit) {
                if (e == u || e.dead || e.team == u.team) continue;
                if (e.dst2(u) <= FEAR_RADIUS * FEAR_RADIUS) e.apply(BossUnits.ragnarokFear, 40f);
            }
        }

        // Trample — damages and knocks back smaller nearby enemies
        for (Unit e : Groups.unit) {
            if (e == u || e.dead || e.team == u.team) continue;
            if (e.hitSize >= u.hitSize * 0.7f) continue;
            float dx = e.x - u.x, dy = e.y - u.y;
            float d2 = dx * dx + dy * dy;
            if (d2 > TRAMPLE_RADIUS * TRAMPLE_RADIUS) continue;
            e.damage(TRAMPLE_DAMAGE * Time.delta / 60f);
            float d = (float) Math.sqrt(d2) + 0.001f;
            e.vel.add(dx / d * TRAMPLE_KNOCKBACK * Time.delta / 60f, dy / d * TRAMPLE_KNOCKBACK * Time.delta / 60f);
        }

        // State machine
        if (state == 1) {
            // Protocol active
            float t = protocolTimer.get(u.id, 0f) - Time.delta;
            protocolTimer.put(u.id, t);
            if (BossUnits.doomsday != null) u.apply(BossUnits.doomsday, 30f);

            // Constant area explosions + lava trail
            if (Mathf.chance(0.35)) {
                float ang = Mathf.random(360f);
                float r = Mathf.random(PROTOCOL_AOE_RADIUS);
                float ex = u.x + Mathf.cosDeg(ang) * r;
                float ey = u.y + Mathf.sinDeg(ang) * r;
                try { Fx.dynamicExplosion.at(ex, ey, 0.4f); } catch (Exception e2) {}
            }
            if (Time.time % 4f < Time.delta) {
                try { Fx.shockwave.at(u.x, u.y, 0f, Color.valueOf("ff3010")); } catch (Exception e2) {}
            }
            try { Damage.damage(u.team, u.x, u.y, PROTOCOL_AOE_RADIUS, PROTOCOL_AOE_DPS * Time.delta / 60f, true); } catch (Exception e2) {}

            // Lava trail under feet
            try {
                Tile tile = Vars.world.tileWorld(u.x, u.y);
                if (tile != null && Mathf.chance(0.25)) {
                    Floor lava = (Floor) mindustry.content.Blocks.slag;
                    if (lava != null && tile.floor() != null && !tile.floor().isLiquid && tile.block() == mindustry.content.Blocks.air) {
                        tile.setFloor(lava);
                    }
                }
            } catch (Exception e2) {}

            if (t <= 0f) {
                protocolState.put(u.id, 2f); // Protocol expired; Zantetsuken available
                protocolTimer.put(u.id, 0f);
                try { Vars.ui.showInfoToast("[#ff3010]Ragnarok Protocol expired. Press [accent]Shift[#ff3010] for Zantetsuken.[]", 3f); } catch (Exception e2) {}
            }
        } else if (state == 3) {
            // Charging Zantetsuken
            float t = zantCharge.get(u.id, 0f) - Time.delta;
            zantCharge.put(u.id, t);
            if (Mathf.chance(0.6)) {
                float ang = Mathf.random(360f);
                float r = Mathf.random(120f);
                try { Fx.smoke.at(u.x + Mathf.cosDeg(ang) * r, u.y + Mathf.sinDeg(ang) * r, 0f, Color.valueOf("ffffff")); } catch (Exception e2) {}
            }
            if (Time.time % 30f < Time.delta) {
                try { Fx.shockwave.at(u.x, u.y, 0f, Color.valueOf("ffffff")); } catch (Exception e2) {}
            }
            if (t <= 0f) {
                executeZantetsuken(u);
            }
        }

        // Player input — re-read state in case it changed earlier in this tick (e.g., Protocol just expired)
        if (Vars.player != null && Vars.player.unit() == u && Core.input != null
                && (Core.input.keyTap(KeyCode.shiftLeft) || Core.input.keyTap(KeyCode.shiftRight))) {
            int curState = (int) protocolState.get(u.id, 0f);
            if (curState == 0) {
                activateProtocol(u);
            } else if (curState == 1 || curState == 2) {
                // Per spec: once Protocol has been used (active or expired), Shift triggers Zantetsuken
                beginZantetsuken(u);
            }
        }
    }

    private static void activateProtocol(Unit u) {
        protocolState.put(u.id, 1f);
        protocolTimer.put(u.id, PROTOCOL_DURATION);
        try {
            Fx.shockwave.at(u.x, u.y, 0f, Color.valueOf("ff3010"));
            Fx.dynamicExplosion.at(u.x, u.y, 0.8f);
            Sounds.explosion.at(u.x, u.y);
        } catch (Exception ex) {}
        try { Vars.ui.showInfoToast("[#ff3010]RAGNAROK PROTOCOL ENGAGED. DOOMSDAY MODE ACTIVE.[]", 3f); } catch (Exception ex) {}
    }

    private static void beginZantetsuken(Unit u) {
        protocolState.put(u.id, 3f);
        zantCharge.put(u.id, ZANTETSUKEN_CHARGE);
        try { Vars.ui.showInfoToast("[#ffffff]ZANTETSUKEN CHARGING — 10 SECONDS TO ABSOLUTE ANNIHILATION.[]", 4f); } catch (Exception ex) {}
        try { Sounds.explosion.at(u.x, u.y); } catch (Exception ex) {}
    }

    private static void executeZantetsuken(Unit u) {
        float x = u.x, y = u.y;
        try { Vars.ui.showInfoToast("[#ffffff]ZANTETSUKEN.[]", 4f); } catch (Exception ex) {}
        try {
            Fx.massiveExplosion.at(x, y);
            Fx.dynamicExplosion.at(x, y, 1.5f);
            Sounds.explosion.at(x, y);
        } catch (Exception ex) {}

        // Wipe every non-core building on the map.
        // Iterate world tiles (not just Groups.build) because some static blocks — walls,
        // storage, power nodes — aren't always reliably killed via b.kill() alone.
        IntSet visited = new IntSet();
        Seq<Building> toKill = new Seq<>();
        try {
            if (Vars.world != null && Vars.world.tiles != null) {
                int w = Vars.world.width(), h = Vars.world.height();
                for (int tx = 0; tx < w; tx++) {
                    for (int ty = 0; ty < h; ty++) {
                        Tile t = Vars.world.tile(tx, ty);
                        if (t == null) continue;
                        Building b = t.build;
                        if (b == null || b.block == null) continue;
                        if (b.block instanceof CoreBlock) continue;
                        if (b.tile != t) continue; // only handle the root tile of multi-tile blocks
                        if (!visited.add(b.id)) continue;
                        toKill.add(b);
                    }
                }
            }
        } catch (Exception ex) { Log.err("[BeyondSerpulo] Zantetsuken scan: " + ex); }

        for (int i = 0; i < toKill.size; i++) {
            Building b = toKill.get(i);
            if (b == null) continue;
            try { b.health = 0f; } catch (Exception ex) {}
            try { b.kill(); } catch (Exception ex) {}
            // Fallback: if it's still alive (some blocks override kill), strip via tile.remove()
            try {
                if (b.tile != null && b.tile.build == b) {
                    b.tile.remove();
                }
            } catch (Exception ex) {}
        }

        // Kill every unit, including Ragnarok itself
        Seq<Unit> uKill = new Seq<>();
        for (Unit other : Groups.unit) {
            if (other == null || other.dead) continue;
            uKill.add(other);
        }
        for (int i = 0; i < uKill.size; i++) {
            try { uKill.get(i).kill(); } catch (Exception ex) {}
        }
    }

    private static void onUnitDestroyed(UnitDestroyEvent e) {
        try {
            Unit u = e.unit;
            if (u == null || BossUnits.ragnarok == null || u.type != BossUnits.ragnarok) return;
            int state = (int) protocolState.get(u.id, 0f);
            // If Ragnarok died DURING Protocol or AFTER it (state 1 or 2), trigger nuclear-level blast.
            // Not triggered if Zantetsuken already executed the kill (that's a planned annihilation).
            if (state == 1 || state == 2) {
                float x = u.x, y = u.y;
                try {
                    Fx.massiveExplosion.at(x, y);
                    Fx.dynamicExplosion.at(x, y, 1.2f);
                    Sounds.explosion.at(x, y);
                } catch (Exception ex) {}
                try { Damage.damage(u.team, x, y, DEATH_BLAST_RADIUS, DEATH_BLAST_DAMAGE, true); } catch (Exception ex) {}
            }
            protocolState.remove(u.id, 0f);
            protocolTimer.remove(u.id, 0f);
            zantCharge.remove(u.id, 0f);
            announced.remove(u.id);
        } catch (Exception ex) { Log.err("[BeyondSerpulo] Ragnarok.onUnitDestroyed: " + ex); }
    }

    private static void updateHud(Unit u) {
        try {
            if (hudLabel == null) return;
            int state = (int) protocolState.get(u.id, 0f);
            String txt;
            if (state == 0) {
                txt = "[#ff3010]RAGNAROK PROTOCOL READY[]  [lightgray]— press [accent]Shift[][]";
            } else if (state == 1) {
                float t = protocolTimer.get(u.id, 0f);
                txt = "[#ff3010]DOOMSDAY MODE ACTIVE[]  " + Strings.fixed(t / 60f, 1) + "s\n[lightgray]Press [accent]Shift[lightgray] to arm Zantetsuken[]";
            } else if (state == 2) {
                txt = "[#ffffff]ZANTETSUKEN ARMED[]  [lightgray]— press [accent]Shift[][lightgray] to annihilate all[]";
            } else {
                float t = zantCharge.get(u.id, 0f);
                txt = "[#ffffff]ZANTETSUKEN CHARGING[]  " + Strings.fixed(t / 60f, 1) + "s";
            }
            hudLabel.setText(txt);
        } catch (Exception ex) {}
    }

    private static void cleanupDead(IntFloatMap m) {
        Seq<Integer> rm = new Seq<>();
        for (var entry : m.entries()) {
            Unit u = Groups.unit.getByID(entry.key);
            if (u == null || u.dead) rm.add(entry.key);
        }
        for (int i = 0; i < rm.size; i++) m.remove(rm.get(i), 0f);
    }
}
