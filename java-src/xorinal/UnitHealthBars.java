package xorinal;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import mindustry.graphics.Layer;
import arc.math.geom.Rect;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.ui.Fonts;

public class UnitHealthBars {
    private static boolean enabled = true;
    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    private static final float BAR_W = 16f;
    private static final float BAR_H = 2.2f;
    private static final float TEXT_SCALE = 0.18f;

    public static void init() {
        Events.run(EventType.Trigger.drawOver, UnitHealthBars::drawAll);
    }

    private static void drawAll() {
        if (!enabled) return;
        if (Vars.state == null || !Vars.state.isGame()) return;
        if (Vars.player == null) return;

        Rect view = Core.camera.bounds(Tmp.r1).grow(32f);
        float prevZ = Draw.z();
        float zBar = Layer.overlayUI;

        // Pass 1: bars (batched at one z so they all render together above the world)
        Draw.z(zBar);
        Groups.unit.each(u -> {
            if (u == null || u.dead || !u.isValid()) return;
            if (!view.contains(u.x, u.y)) return;

            float frac = Math.max(0f, Math.min(1f, u.health / u.maxHealth));
            float bx = Math.round(u.x);
            float by = Math.round(u.y + u.hitSize / 2f + 4f);
            Color fill = u.team == Vars.player.team() ? Color.royal : Color.scarlet;

            Draw.color(Color.black);
            Fill.rect(bx, by, BAR_W + 1f, BAR_H + 1f);
            Draw.color(Color.darkGray);
            Fill.rect(bx, by, BAR_W, BAR_H);
            Draw.color(fill);
            float fw = BAR_W * frac;
            Fill.rect(bx - BAR_W / 2f + fw / 2f, by, fw, BAR_H);
        });
        Draw.color();

        // Pass 2: text (font scale set once; backing box for readability)
        Fonts.def.getData().setScale(TEXT_SCALE);
        Fonts.def.setColor(Color.white);
        Fonts.def.setUseIntegerPositions(false);
        float textH = Fonts.def.getCapHeight() + 1.5f;

        Groups.unit.each(u -> {
            if (u == null || u.dead || !u.isValid()) return;
            if (!view.contains(u.x, u.y)) return;

            float bx = Math.round(u.x);
            float by = Math.round(u.y + u.hitSize / 2f + 4f);
            String text = ((int) u.health) + "/" + ((int) u.maxHealth);
            float textY = by + BAR_H / 2f + textH + 1f;

            Draw.z(zBar);
            Draw.color(0f, 0f, 0f, 0.55f);
            float bgW = Fonts.def.getData().scaleX * (text.length() * 6f + 4f);
            Fill.rect(bx, textY - textH / 2f, bgW, textH);
            Draw.color();

            Draw.z(zBar + 0.01f);
            Fonts.def.draw(text, bx, textY, Align.center);
        });

        Fonts.def.getData().setScale(1f);
        Fonts.def.setColor(Color.white);
        Draw.reset();
        Draw.z(prevZ);
    }
}
