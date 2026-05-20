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
import mindustry.entities.units.WeaponMount;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Bullet;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

public class Celestial {
    public static final float VEIL_DURATION = 10f * 60f;
    public static final float VEIL_COOLDOWN = 30f * 60f;
    public static final float VEIL_RADIUS = 140f;
    public static final float ALLY_BUFF_RADIUS = 180f;

    private static final IntFloatMap veilActive = new IntFloatMap();
    private static final IntFloatMap veilCooldown = new IntFloatMap();
    private static final Seq<Bullet> bulletRemoveBuf = new Seq<>();

    private static Table hudWidget;
    private static Label hudLabel;

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> {
            veilActive.clear();
            veilCooldown.clear();
            setupHud();
        });
        Events.run(Trigger.update, Celestial::tick);
    }

    private static void setupHud() {
        try {
            if (hudWidget != null) { hudWidget.remove(); hudWidget = null; }
            Table wrap = new Table();
            wrap.setFillParent(true);
            wrap.bottom();
            Table card = new Table();
            try { card.background(mindustry.gen.Tex.button); } catch (Exception ex) {}
            hudLabel = new Label("");
            card.add(hudLabel).pad(8f);
            wrap.add(card).pad(120f).bottom();
            wrap.visible(Celestial::shouldShowHud);
            Vars.ui.hudGroup.addChild(wrap);
            hudWidget = wrap;
        } catch (Exception ex) { Log.err("[BeyondSerpulo] Celestial.setupHud: " + ex); }
    }

    private static boolean shouldShowHud() {
        try {
            if (Vars.player == null || BossUnits.celestial == null) return false;
            Unit u = Vars.player.unit();
            return u != null && !u.dead && u.type == BossUnits.celestial;
        } catch (Exception ex) { return false; }
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame() || Vars.state.isPaused()) return;
        if (BossUnits.celestial == null) return;

        // Decay timers
        decayMap(veilActive);
        decayMap(veilCooldown);

        // Cleanup dead unit entries
        cleanupDead(veilActive);
        cleanupDead(veilCooldown);

        // Player-controlled Celestial: check Shift to activate veil
        try {
            Unit pu = Vars.player == null ? null : Vars.player.unit();
            if (pu != null && !pu.dead && pu.type == BossUnits.celestial) {
                if (Core.input != null && Core.input.keyTap(KeyCode.shiftLeft)
                        && veilActive.get(pu.id, 0f) <= 0f && veilCooldown.get(pu.id, 0f) <= 0f) {
                    activateVeil(pu);
                }
                updateHudLabel(pu);
            }
        } catch (Exception ex) {}

        // Per-active-veil-unit effects (works for any team/unit running a veil)
        for (var entry : veilActive.entries()) {
            float remain = entry.value;
            if (remain <= 0f) continue;
            Unit u = Groups.unit.getByID(entry.key);
            if (u == null || u.dead) continue;

            // Invulnerability: heal back any damage tick by clamping HP to max
            // (healthMultiplier doesn't fully prevent damage; this is the simplest safe approach)
            if (u.health < u.maxHealth) u.health = u.maxHealth;

            // Visual: pulsing aura
            if (Mathf.chance(0.4)) {
                float ang = Mathf.random(360f);
                float r = Mathf.random(VEIL_RADIUS * 0.4f, VEIL_RADIUS);
                Fx.smoke.at(u.x + Mathf.cosDeg(ang) * r, u.y + Mathf.sinDeg(ang) * r, 0f, Color.valueOf("ffe080"));
            }
            if (Time.time % 10f < Time.delta) {
                Fx.shockwave.at(u.x, u.y, 0f, Color.valueOf("ffe080"));
            }

            // Reflect bullets near the unit
            reflectBullets(u);

            // Bless nearby allies (fire-rate boost)
            if (BossUnits.blessed != null) {
                for (Unit ally : Groups.unit) {
                    if (ally == u || ally.dead || ally.team != u.team) continue;
                    if (ally.dst2(u) <= ALLY_BUFF_RADIUS * ALLY_BUFF_RADIUS) {
                        ally.apply(BossUnits.blessed, 30f);
                    }
                }
            }
        }
    }

    private static void activateVeil(Unit u) {
        veilActive.put(u.id, VEIL_DURATION);
        veilCooldown.put(u.id, VEIL_DURATION + VEIL_COOLDOWN);
        try {
            Fx.shockwave.at(u.x, u.y, 0f, Color.valueOf("ffe080"));
            Fx.dynamicExplosion.at(u.x, u.y, 0.5f);
        } catch (Exception ex) {}
        try { Vars.ui.showInfoToast("[#ffe080]Celestial Veil engaged.[]", 1.5f); } catch (Exception ex) {}
    }

    private static void reflectBullets(Unit u) {
        bulletRemoveBuf.clear();
        for (Bullet b : Groups.bullet) {
            if (b == null || b.team == u.team || b.owner == u) continue;
            float dx = b.x - u.x, dy = b.y - u.y;
            float d2 = dx * dx + dy * dy;
            if (d2 > VEIL_RADIUS * VEIL_RADIUS) continue;
            // Re-aim back toward original shooter (if any) and flip team
            try {
                float vx, vy;
                if (b.owner instanceof Unit) {
                    Unit src = (Unit) b.owner;
                    float ang = Mathf.angle(src.x - b.x, src.y - b.y);
                    float speed = Mathf.sqrt(b.vel.x * b.vel.x + b.vel.y * b.vel.y);
                    if (speed < 0.01f) speed = 4f;
                    vx = Mathf.cosDeg(ang) * speed;
                    vy = Mathf.sinDeg(ang) * speed;
                } else {
                    vx = -b.vel.x;
                    vy = -b.vel.y;
                }
                b.vel.set(vx, vy);
                b.team = u.team;
                b.owner = u;
            } catch (Exception ex) {}
        }
    }

    private static void updateHudLabel(Unit u) {
        try {
            if (hudLabel == null) return;
            float active = veilActive.get(u.id, 0f);
            float cd = veilCooldown.get(u.id, 0f);
            String txt;
            if (active > 0f) {
                txt = "[#ffe080]Celestial Veil ACTIVE[]  " + Strings.fixed(active / 60f, 1) + "s";
            } else if (cd > 0f) {
                txt = "[lightgray]Veil cooldown:[]  " + Strings.fixed(cd / 60f, 1) + "s";
            } else {
                txt = "[#ffe080]Veil READY[]  [lightgray]— press [accent]Shift[][]";
            }
            hudLabel.setText(txt);
        } catch (Exception ex) {}
    }

    private static void decayMap(IntFloatMap m) {
        Seq<int[]> updates = new Seq<>();
        for (var entry : m.entries()) {
            float v = entry.value - Time.delta;
            updates.add(new int[]{entry.key, Float.floatToRawIntBits(Math.max(0f, v))});
        }
        for (int i = 0; i < updates.size; i++) {
            int[] kv = updates.get(i);
            m.put(kv[0], Float.intBitsToFloat(kv[1]));
        }
    }

    private static void cleanupDead(IntFloatMap m) {
        Seq<Integer> rm = new Seq<>();
        for (var entry : m.entries()) {
            if (entry.value <= 0f) { rm.add(entry.key); continue; }
            Unit u = Groups.unit.getByID(entry.key);
            if (u == null || u.dead) rm.add(entry.key);
        }
        for (int i = 0; i < rm.size; i++) m.remove(rm.get(i), 0f);
    }
}
