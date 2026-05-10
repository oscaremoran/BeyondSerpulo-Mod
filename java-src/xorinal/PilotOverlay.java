package xorinal;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.util.Align;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.input.Binding;
import mindustry.ui.Fonts;

public class PilotOverlay {
    private static final float TEXT_SCALE = 0.22f;
    private static final float MARKER_BOB = 4f;

    public static void init() {
        Events.run(EventType.Trigger.update, PilotOverlay::checkClick);
        Events.run(EventType.Trigger.drawOver, PilotOverlay::draw);
    }

    private static void checkClick() {
        if (Vars.state == null || !Vars.state.isGame()) return;
        if (Pilots.pendingNamePilot == null || Pilots.pendingNameUnitId < 0) return;
        Unit u = Groups.unit.getByID(Pilots.pendingNameUnitId);
        if (u == null || !u.isValid()) return;
        if (!Core.input.keyTap(KeyCode.mouseLeft)) return;
        try {
            float mx = Core.input.mouseWorldX();
            float my = Core.input.mouseWorldY();
            float markerX = u.x;
            float markerY = u.y + u.hitSize / 2f + 22f;
            float dx = mx - markerX, dy = my - markerY;
            if (dx * dx + dy * dy < 10f * 10f) {
                Pilots.openNamePrompt(Pilots.pendingNamePilot);
            }
        } catch (Exception ex) {}
    }

    private static void draw() {
        if (Vars.state == null || !Vars.state.isGame()) return;
        if (Pilots.byUnit.size == 0 && Pilots.pendingNameUnitId < 0 && Pilots.downed.size == 0) return;

        float prevZ = Draw.z();
        Draw.z(Layer.overlayUI);
        Fonts.def.getData().setScale(TEXT_SCALE);
        Fonts.def.setUseIntegerPositions(false);

        for (IntMap.Entry<PilotData> e : Pilots.byUnit.entries()) {
            Unit u = Groups.unit.getByID(e.key);
            if (u == null || !u.isValid()) continue;
            PilotData p = e.value;
            if (p.name == null) continue;

            float baseY = u.y + u.hitSize / 2f + 16f;
            String label = p.name + " [#aaa]" + Pilots.TIER_NAMES[p.tier()] + "[]";
            drawLabel(label, u.x, baseY + 4f);
            drawXpBar(p, u.x, baseY);
        }

        for (Pilots.DownedPilot d : Pilots.downed) {
            if (d == null || d.pilot == null) continue;
            float pulse = 0.55f + 0.45f * Mathf.absin(arc.util.Time.time, 7f, 1f);
            Draw.color(1f, 0.2f * pulse, 0.2f * pulse, 0.85f);
            Fill.circle(d.x, d.y, 6f + Mathf.absin(arc.util.Time.time, 8f, 2f));
            Draw.color();
            long rem = Math.max(0L, d.deadline - System.currentTimeMillis());
            String name = d.pilot.name == null ? "Pilot" : d.pilot.name;
            drawLabel("[red]" + name + " is down — " + String.format("%.1fs", rem / 1000f) + "[]",
                d.x, d.y + 14f);
        }

        if (Pilots.pendingNameUnitId >= 0) {
            Unit u = Groups.unit.getByID(Pilots.pendingNameUnitId);
            if (u != null && u.isValid()) {
                float bob = Mathf.absin(arc.util.Time.time, 12f, MARKER_BOB);
                drawLabel("[gold]? click to name[]", u.x, u.y + u.hitSize / 2f + 22f + bob);
            }
        }

        Fonts.def.getData().setScale(1f);
        Fonts.def.setColor(Color.white);
        Draw.reset();
        Draw.z(prevZ);
    }

    private static void drawLabel(String text, float x, float y) {
        float w = TEXT_SCALE * (text.replaceAll("\\[[^\\]]*\\]", "").length() * 6f + 6f);
        float h = TEXT_SCALE * 12f;
        Draw.color(0f, 0f, 0f, 0.55f);
        Fill.rect(x, y, w, h);
        Fonts.def.setColor(Color.white);
        Fonts.def.draw(text, x, y + h / 2f, Align.center);
    }

    private static void drawXpBar(PilotData p, float cx, float cy) {
        int tier = p.tier();
        if (tier >= 3) return;
        int curThreshold = p.threshold(tier);
        int nextThreshold = p.threshold(tier + 1);
        int span = Math.max(1, nextThreshold - curThreshold);
        float frac = Mathf.clamp((p.killPoints - curThreshold) / (float) span);

        float w = 14f, h = 1.2f;
        float x = cx - w / 2f, y = cy - 1f;
        Draw.color(0f, 0f, 0f, 0.7f);
        Fill.rect(cx, y, w + 1f, h + 1f);
        Draw.color(Color.gold);
        Fill.rect(x + (w * frac) / 2f, y, w * frac, h);
        Draw.color();
    }
}
