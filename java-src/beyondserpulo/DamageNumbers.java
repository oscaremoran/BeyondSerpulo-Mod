package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.graphics.Layer;
import mindustry.ui.Fonts;

public class DamageNumbers {
    private static final float LIFETIME = 120f;
    private static final float RISE = 22f;
    private static final float TEXT_SCALE = 0.26f;

    private static final Seq<Pop> pops = new Seq<>();
    private static boolean enabled = true;
    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    static class Pop {
        float x, y, value, age, jx;
        boolean crackling;
        Color color;
        Pop(float x, float y, float value, Color color, boolean crackling) {
            this.x = x; this.y = y; this.value = value; this.color = color;
            this.crackling = crackling;
            this.jx = Mathf.range(4f);
        }
    }

    public static void init() {
        Events.on(EventType.UnitDamageEvent.class, e -> {
            if (!enabled) return;
            if (e.unit == null || e.bullet == null) return;
            float dmg = e.bullet.damage;
            if (dmg <= 0f) return;
            spawn(e.unit.x, e.unit.y + e.unit.hitSize / 2f + 2f, dmg);
        });

        Events.on(EventType.BuildDamageEvent.class, e -> {
            if (!enabled) return;
            if (e.build == null || e.source == null) return;
            float dmg = e.source.damage;
            if (dmg <= 0f) return;
            spawn(e.build.x, e.build.y + e.build.block.size * 4f, dmg);
        });

        Events.run(EventType.Trigger.update, () -> {
            if (Vars.state == null || Vars.state.isPaused()) return;
            for (int i = pops.size - 1; i >= 0; i--) {
                pops.get(i).age += 1f;
                if (pops.get(i).age >= LIFETIME) pops.remove(i);
            }
        });

        Events.run(EventType.Trigger.drawOver, DamageNumbers::draw);
    }

    private static void spawn(float x, float y, float dmg) {
        Color c;
        boolean crackling = false;
        if (dmg <= 50f)        c = Color.white;
        else if (dmg <= 150f)  c = Color.yellow;
        else if (dmg <= 250f)  c = Color.scarlet;
        else { c = Color.scarlet; crackling = true; }
        pops.add(new Pop(x, y, dmg, c, crackling));
    }

    private static void draw() {
        if (pops.isEmpty()) return;
        if (Vars.state == null || !Vars.state.isGame()) return;

        Rect view = Core.camera.bounds(Tmp.r1).grow(64f);
        float prevZ = Draw.z();

        Fonts.def.getData().setScale(TEXT_SCALE);
        Fonts.def.setUseIntegerPositions(false);

        float time = arc.util.Time.time;
        for (Pop p : pops) {
            if (!view.contains(p.x, p.y)) continue;
            float t = p.age / LIFETIME;
            float alpha = 1f - t * t;
            float dy = RISE * t;
            float fx = p.x + p.jx;
            float fy = p.y + dy;

            float r = p.color.r, g = p.color.g, b = p.color.b;
            if (p.crackling) {
                float jitter = Mathf.range(1.2f);
                fx += jitter;
                fy += Mathf.range(1.2f);
                float flash = 0.5f + 0.5f * Mathf.absin(time, 2f, 1f);
                g = flash * 0.4f;
                b = flash * 0.2f;
            }

            String text = String.valueOf((int) p.value);

            Draw.z(Layer.overlayUI);
            Draw.color(0f, 0f, 0f, 0.55f * alpha);
            float bgW = TEXT_SCALE * (text.length() * 6f + 6f);
            float bgH = TEXT_SCALE * 12f;
            Fill.rect(fx, fy, bgW, bgH);

            Fonts.def.setColor(r, g, b, alpha);
            Fonts.def.draw(text, fx, fy + bgH / 2f, Align.center);
        }

        Fonts.def.getData().setScale(1f);
        Fonts.def.setColor(Color.white);
        Draw.reset();
        Draw.z(prevZ);
    }
}
