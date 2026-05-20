package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.IntFloatMap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

public class Abyssal {
    public static final float SUFFOCATE_MAX = 10f * 60f;
    public static final float DRAIN_PER_TICK = 1.0f;
    public static final float RECHARGE_PER_TICK = 0.6f;
    public static final float DEEP_HEAL_PER_SEC = 80f;
    public static final float WATER_SLOW_AURA_RADIUS = 110f;
    public static final float CALL_COOLDOWN = 25f * 60f;
    public static final float WHIRLPOOL_TTL = 6f * 60f;
    public static final float WHIRLPOOL_RADIUS = 110f;
    public static final float WHIRLPOOL_DPS = 90f;

    private static final IntFloatMap suffocate = new IntFloatMap();
    private static final IntFloatMap callCooldown = new IntFloatMap();

    private static final Seq<Whirlpool> whirlpools = new Seq<>();

    private static Table hudWidget;
    private static Label hudLabel;

    private static class Whirlpool {
        float x, y, ttl;
        mindustry.game.Team ownerTeam;
    }

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> {
            suffocate.clear();
            callCooldown.clear();
            whirlpools.clear();
            setupHud();
        });
        Events.run(Trigger.update, Abyssal::tick);
    }

    private static void setupHud() {
        try {
            if (hudWidget != null) { hudWidget.remove(); hudWidget = null; }
            Table wrap = new Table();
            wrap.setFillParent(true);
            wrap.top();
            Table card = new Table();
            try { card.background(Tex.button); } catch (Exception ex) {}
            hudLabel = new Label("");
            card.add(hudLabel).pad(8f);
            wrap.add(card).pad(60f).top();
            wrap.visible(Abyssal::shouldShowHud);
            Vars.ui.hudGroup.addChild(wrap);
            hudWidget = wrap;
        } catch (Exception ex) { Log.err("[BeyondSerpulo] Abyssal.setupHud: " + ex); }
    }

    private static boolean shouldShowHud() {
        try {
            if (Vars.player == null || BossUnits.abyssal == null) return false;
            Unit u = Vars.player.unit();
            return u != null && !u.dead && u.type == BossUnits.abyssal;
        } catch (Exception ex) { return false; }
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame() || Vars.state.isPaused()) return;
        if (BossUnits.abyssal == null) return;
        if (Vars.net != null && Vars.net.client()) return;

        // Per-Abyssal updates
        for (Unit u : Groups.unit) {
            if (u.dead || u.type != BossUnits.abyssal) continue;
            updateAbyssal(u);
        }

        // Clean dead from maps
        cleanupDead(suffocate);
        cleanupDead(callCooldown);

        // Update whirlpools
        for (int i = whirlpools.size - 1; i >= 0; i--) {
            Whirlpool w = whirlpools.get(i);
            w.ttl -= Time.delta;
            if (w.ttl <= 0f) { whirlpools.remove(i); continue; }
            applyWhirlpool(w);
        }

        // HUD
        try {
            Unit pu = Vars.player == null ? null : Vars.player.unit();
            if (pu != null && !pu.dead && pu.type == BossUnits.abyssal) updateHud(pu);
        } catch (Exception ex) {}
    }

    private static void updateAbyssal(Unit u) {
        Tile tile = Vars.world.tileWorld(u.x, u.y);
        boolean inWater = false, inDeep = false;
        if (tile != null) {
            Floor f = tile.floor();
            if (f != null && f.isLiquid) {
                inWater = true;
                inDeep = f.isDeep();
            }
        }

        // Suffocation
        float cur = suffocate.get(u.id, SUFFOCATE_MAX);
        if (inWater) {
            cur = Math.min(SUFFOCATE_MAX, cur + RECHARGE_PER_TICK * Time.delta);
        } else {
            cur = Math.max(0f, cur - DRAIN_PER_TICK * Time.delta);
            if (Mathf.chance(0.10)) Fx.smoke.at(u.x + Mathf.range(20f), u.y + Mathf.range(20f), 0f, Color.valueOf("a02020"));
            if (BossUnits.suffocating != null) u.apply(BossUnits.suffocating, 30f);
        }
        suffocate.put(u.id, cur);

        if (cur <= 0f) {
            u.kill();
            return;
        }

        // Deep water heal
        if (inDeep && u.health < u.maxHealth) {
            u.heal(DEEP_HEAL_PER_SEC * Time.delta / 60f);
        }

        // Slow aura — apply to enemies within radius when Abyssal is in water
        if (inWater && BossUnits.abyssalGrasp != null) {
            for (Unit e : Groups.unit) {
                if (e == u || e.dead || e.team == u.team) continue;
                float d2 = e.dst2(u);
                if (d2 > WATER_SLOW_AURA_RADIUS * WATER_SLOW_AURA_RADIUS) continue;
                Tile et = Vars.world.tileWorld(e.x, e.y);
                if (et != null && et.floor() != null && et.floor().isLiquid) {
                    e.apply(BossUnits.abyssalGrasp, 30f);
                }
            }
        }

        // Abyssal Call cooldown — decay only; activation is manual via Shift on player-controlled Abyssal
        float cd = callCooldown.get(u.id, 0f);
        cd = Math.max(0f, cd - Time.delta);
        callCooldown.put(u.id, cd);

        if (Vars.player != null && Vars.player.unit() == u && cd <= 0f
                && Core.input != null && Core.input.keyTap(KeyCode.shiftLeft)) {
            float tx = u.aimX, ty = u.aimY;
            if (tx == 0f && ty == 0f) { tx = u.x; ty = u.y; }
            spawnWhirlpool(tx, ty, u.team);
            callCooldown.put(u.id, CALL_COOLDOWN);
            try { Vars.ui.showInfoToast("[#3a90b0]Abyssal Call summoned.[]", 1.5f); } catch (Exception ex) {}
        }
    }

    private static Unit findEnemyCluster(Unit u) {
        Unit best = null;
        float bestScore = 0f;
        for (Unit e : Groups.unit) {
            if (e == u || e.dead || e.team == u.team) continue;
            float d2 = e.dst2(u);
            if (d2 > 520f * 520f) continue;
            int neighbors = 0;
            for (Unit n : Groups.unit) {
                if (n == e || n.dead || n.team != e.team) continue;
                if (n.dst2(e) < 160f * 160f) neighbors++;
            }
            float score = neighbors - d2 * 0.0001f;
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    private static void spawnWhirlpool(float x, float y, mindustry.game.Team team) {
        Whirlpool w = new Whirlpool();
        w.x = x; w.y = y; w.ttl = WHIRLPOOL_TTL; w.ownerTeam = team;
        whirlpools.add(w);
        try {
            Fx.shockwave.at(x, y, 0f, Color.valueOf("050510"));
            Fx.dynamicExplosion.at(x, y, 0.4f);
        } catch (Exception ex) {}
    }

    private static void applyWhirlpool(Whirlpool w) {
        if (Mathf.chance(0.5)) {
            float ang = Mathf.random(360f);
            float r = Mathf.random(WHIRLPOOL_RADIUS);
            Fx.smoke.at(w.x + Mathf.cosDeg(ang) * r, w.y + Mathf.sinDeg(ang) * r, 0f, Color.valueOf("050510"));
        }
        if (Time.time % 8f < Time.delta) {
            Fx.shockwave.at(w.x, w.y, 0f, Color.valueOf("050510"));
        }

        float pullStrength = 0.20f;
        for (Unit e : Groups.unit) {
            if (e.dead || e.team == w.ownerTeam) continue;
            float dx = w.x - e.x, dy = w.y - e.y;
            float d2 = dx * dx + dy * dy;
            if (d2 > WHIRLPOOL_RADIUS * WHIRLPOOL_RADIUS) continue;
            float d = (float) Math.sqrt(d2) + 0.001f;
            e.vel.add(dx / d * pullStrength, dy / d * pullStrength);
            if (BossUnits.abyssalGrasp != null) e.apply(BossUnits.abyssalGrasp, 30f);
        }

        Damage.damage(w.ownerTeam, w.x, w.y, WHIRLPOOL_RADIUS, WHIRLPOOL_DPS * Time.delta / 60f, true);

        // Drain enemy power gens: small damage to power blocks within radius
        for (Building b : Groups.build) {
            if (b == null || b.team == w.ownerTeam || b.block == null) continue;
            if (!b.block.hasPower) continue;
            float dx = w.x - b.x, dy = w.y - b.y;
            if (dx * dx + dy * dy > WHIRLPOOL_RADIUS * WHIRLPOOL_RADIUS) continue;
            b.damage(0.3f);
        }
    }

    private static void updateHud(Unit u) {
        try {
            if (hudLabel == null) return;
            float cur = suffocate.get(u.id, SUFFOCATE_MAX);
            Tile t = Vars.world.tileWorld(u.x, u.y);
            boolean inWater = t != null && t.floor() != null && t.floor().isLiquid;
            float seconds = cur / 60f;
            String state = inWater ? "[#5fff8f]RECHARGING[]" : "[#ff5050]SUFFOCATING[]";
            String top = state + "  " + (inWater ? "[#5fff8f]" : "[#ff5050]")
                + Strings.fixed(seconds, 1) + "s / 10.0s[]";

            float cd = callCooldown.get(u.id, 0f);
            String call;
            if (cd <= 0f) call = "[#3a90b0]Abyssal Call READY[]  [lightgray]— press [accent]Shift[][]";
            else call = "[lightgray]Abyssal Call cooldown:[]  " + Strings.fixed(cd / 60f, 1) + "s";

            hudLabel.setText(top + "\n" + call);
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
