package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.TextureRegion;
import arc.math.geom.Vec2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Tex;

public class DestroyLog {
    private static final int MAX_ENTRIES = 3;
    private static final float LIFETIME = 60f * 60f;
    private static final Seq<Entry> entries = new Seq<>();
    private static boolean enabled = true;

    static class Entry {
        TextureRegion icon;
        boolean ally;
        float priority;
        float x, y;
        float time;
        Entry(TextureRegion icon, boolean ally, float priority, float x, float y) {
            this.icon = icon; this.ally = ally; this.priority = priority;
            this.x = x; this.y = y; this.time = Time.time;
        }
    }

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    public static void init() {
        Events.on(EventType.BlockDestroyEvent.class, e -> {
            try {
                if (e.tile == null || e.tile.build == null || e.tile.block() == null) return;
                if (Vars.player == null) return;
                var block = e.tile.block();
                boolean ally = e.tile.build.team == Vars.player.team();
                push(new Entry(block.uiIcon, ally, block.health,
                        e.tile.worldx(), e.tile.worldy()));
            } catch (Exception ex) {}
        });

        Events.on(EventType.UnitDestroyEvent.class, e -> {
            try {
                if (e.unit == null || e.unit.type == null) return;
                if (Vars.player == null) return;
                boolean ally = e.unit.team() == Vars.player.team();
                push(new Entry(e.unit.type.uiIcon, ally, e.unit.type.health, e.unit.x, e.unit.y));
            } catch (Exception ex) {}
        });

        Core.app.post(DestroyLog::buildPanel);
    }

    private static void push(Entry e) {
        entries.add(e);
        if (entries.size > MAX_ENTRIES) {
            entries.sort((a, b) -> Float.compare(b.priority, a.priority));
            entries.truncate(MAX_ENTRIES);
        }
    }

    private static void buildPanel() {
        try {
            if (Vars.ui == null || Vars.ui.hudGroup == null) return;
            Vars.ui.hudGroup.fill(t -> {
                t.right();
                t.table(Tex.pane, panel -> {
                    panel.left();
                    panel.update(() -> rebuild(panel));
                }).pad(8f).right().minWidth(56f);
            });
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] DestroyLog.buildPanel: " + ex);
        }
    }

    private static void rebuild(Table panel) {
        if (!enabled || Vars.state == null || !Vars.state.isGame()) {
            panel.clear();
            return;
        }

        for (int i = entries.size - 1; i >= 0; i--) {
            if (Time.time - entries.get(i).time > LIFETIME) entries.remove(i);
        }

        panel.clear();
        if (entries.isEmpty()) return;

        Seq<Entry> sorted = entries.copy();
        sorted.sort((a, b) -> Float.compare(b.priority, a.priority));

        panel.add("[gold]Destroyed[]").pad(2f);
        panel.row();
        for (Entry e : sorted) {
            if (e.icon == null) continue;
            String tint = e.ally ? "[#ff7777]" : "[#77ff77]";
            panel.button(t -> {
                t.image(e.icon).size(28f);
            }, () -> {
                try {
                    if (Vars.control != null && Vars.control.input != null) {
                        Vars.control.input.panCamera(new Vec2(e.x, e.y));
                    } else {
                        Core.camera.position.set(e.x, e.y);
                    }
                } catch (Exception ex) {}
            }).size(40f).pad(2f);
            panel.row();
        }
    }
}
