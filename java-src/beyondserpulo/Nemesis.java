package beyondserpulo;

import arc.Events;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.struct.IntFloatMap;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.UnitTypes;
import mindustry.entities.Damage;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.UnitDestroyEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.type.UnitType;

public class Nemesis {
    private static final float RAGE_HP_FRAC = 0.5f;
    private static final float RAGE_STATUS_REFRESH = 60f;
    private static final float AURA_RADIUS = 96f;
    private static final float AURA_DPS = 60f;
    private static final float DEATH_EXPLOSION_RADIUS = 160f;
    private static final float DEATH_EXPLOSION_DAMAGE = 1200f;
    private static final int DEATH_DRONES_MIN = 4;
    private static final int DEATH_DRONES_MAX = 6;
    private static final float DEATH_DRONE_LIFETIME_TICKS = 60f * 12f;

    private static final IntFloatMap droneTtl = new IntFloatMap();
    private static final IntSet droneIds = new IntSet();
    private static final Seq<Integer> removeBuf = new Seq<>();

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> { droneTtl.clear(); droneIds.clear(); });
        Events.on(UnitDestroyEvent.class, Nemesis::onUnitDestroyed);
        Events.run(Trigger.update, Nemesis::tick);
    }

    private static void onUnitDestroyed(UnitDestroyEvent e) {
        try {
            Unit u = e.unit;
            if (u == null || BossUnits.nemesis == null || u.type != BossUnits.nemesis) {
                // Also handle ghost-drone cleanup if a tracked drone dies
                if (u != null && droneIds.contains(u.id)) {
                    droneIds.remove(u.id);
                    droneTtl.remove(u.id, 0f);
                }
                return;
            }
            float x = u.x, y = u.y;
            try {
                Fx.massiveExplosion.at(x, y);
                Fx.dynamicExplosion.at(x, y);
                Sounds.explosion.at(x, y);
            } catch (Exception ex) {}
            try {
                Damage.damage(u.team, x, y, DEATH_EXPLOSION_RADIUS, DEATH_EXPLOSION_DAMAGE, true);
            } catch (Exception ex) {}

            int count = Mathf.random(DEATH_DRONES_MIN, DEATH_DRONES_MAX);
            UnitType drone = UnitTypes.flare;
            if (drone == null) return;
            for (int i = 0; i < count; i++) {
                float ang = (360f / count) * i + Mathf.random(-10f, 10f);
                float dx = x + Mathf.cosDeg(ang) * 24f;
                float dy = y + Mathf.sinDeg(ang) * 24f;
                Unit d = drone.create(u.team);
                d.set(dx, dy);
                d.rotation = ang;
                d.health = drone.health * 0.6f;
                d.maxHealth = d.health;
                d.add();
                droneIds.add(d.id);
                droneTtl.put(d.id, DEATH_DRONE_LIFETIME_TICKS + Mathf.random(0f, 60f * 3f));
                try { Fx.spawn.at(dx, dy); } catch (Exception ex) {}
            }
        } catch (Exception ex) { Log.err("[BeyondSerpulo] Nemesis.onUnitDestroyed: " + ex); }
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame() || Vars.state.isPaused()) return;
        if (Vars.net != null && Vars.net.client()) return;
        if (BossUnits.nemesis == null) return;

        removeBuf.clear();
        for (var entry : droneTtl.entries()) {
            float ttl = entry.value - Time.delta;
            if (ttl <= 0f) {
                Unit du = Groups.unit.getByID(entry.key);
                if (du != null && !du.dead) du.kill();
                removeBuf.add(entry.key);
            } else {
                droneTtl.put(entry.key, ttl);
            }
        }
        for (int i = 0; i < removeBuf.size; i++) {
            int id = removeBuf.get(i);
            droneTtl.remove(id, 0f);
            droneIds.remove(id);
        }

        for (Unit u : Groups.unit) {
            if (u.dead || u.type != BossUnits.nemesis) continue;
            float frac = u.maxHealth > 0f ? u.health / u.maxHealth : 1f;
            if (frac <= RAGE_HP_FRAC) {
                if (BossUnits.rage != null) u.apply(BossUnits.rage, RAGE_STATUS_REFRESH);
                applyRageAura(u);
            }
        }
    }

    private static void applyRageAura(Unit boss) {
        try {
            if (Mathf.chance(0.10)) {
                Fx.smoke.at(boss.x + Mathf.range(AURA_RADIUS * 0.7f), boss.y + Mathf.range(AURA_RADIUS * 0.7f), 0f, Color.valueOf("ff4040"));
            }
            if (Time.time % 6f < Time.delta) {
                Damage.damage(boss.team, boss.x, boss.y, AURA_RADIUS, AURA_DPS * 0.1f, true);
                Fx.shockwave.at(boss.x, boss.y, 0f, Color.valueOf("ff4040"));
            }
        } catch (Exception ex) {}
    }
}
