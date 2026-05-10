package xorinal;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;

public class CommanderArrowHUD {
    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static void setup() {
        try {
            Element el = new Element() {
                @Override
                public void draw() {
                    if (Commanders.commanderId < 0) return;
                    if (Vars.state == null || !Vars.state.isGame()) return;
                    Unit u = Groups.unit.getByID(Commanders.commanderId);
                    if (u == null || !u.isValid()) return;

                    Vec2 screen = Core.camera.project(new Vec2(u.x, u.y));
                    float w = Core.graphics.getWidth();
                    float h = Core.graphics.getHeight();
                    float margin = 70f;

                    boolean onScreen = screen.x >= margin && screen.x <= w - margin
                        && screen.y >= margin && screen.y <= h - margin;
                    if (onScreen) return;

                    float cx = w / 2f, cy = h / 2f;
                    float dx = screen.x - cx, dy = screen.y - cy;
                    if (dx == 0 && dy == 0) return;

                    float maxX = w / 2f - margin;
                    float maxY = h / 2f - margin;
                    float scale = Math.min(maxX / Math.max(1f, Math.abs(dx)), maxY / Math.max(1f, Math.abs(dy)));
                    float ax = cx + dx * scale;
                    float ay = cy + dy * scale;
                    float angle = Mathf.angle(dx, dy);

                    float pulse = 0.55f + 0.45f * Mathf.absin(Time.time, 7f, 1f);
                    Draw.color(1f, 0.2f * pulse, 0.2f * pulse, 1f);
                    Fill.poly(ax, ay, 3, 22f, angle);
                    Draw.color(0f, 0f, 0f, 0.9f);
                    Lines.stroke(2f);
                    Lines.poly(ax, ay, 3, 22f, angle);
                    Draw.color();
                }
            };
            el.setFillParent(true);
            el.touchable(() -> arc.scene.event.Touchable.disabled);
            Vars.ui.hudGroup.addChild(el);
            Log.info("[Xorinal] CommanderArrowHUD installed");
        } catch (Exception ex) {
            Log.err("[Xorinal] CommanderArrowHUD: " + ex);
        }
    }
}
