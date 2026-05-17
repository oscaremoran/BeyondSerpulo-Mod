package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.type.Item;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class CoreItemDelta {
    private static final float SAMPLE_TICKS = 60f * 2f;
    private static final ObjectMap<Item, Integer> lastSample = new ObjectMap<>();
    private static final ObjectMap<Item, Float> ratePerSec = new ObjectMap<>();
    private static float accum;
    private static boolean enabled = true;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    public static void init() {
        Core.app.post(CoreItemDelta::buildPanel);
        Events.run(EventType.Trigger.update, CoreItemDelta::tick);
    }

    private static void buildPanel() {
        try {
            if (Vars.ui == null || Vars.ui.hudGroup == null) return;
            Vars.ui.hudGroup.fill(t -> {
                t.top().right();
                t.table(panel -> {
                    panel.right().top();
                    panel.update(() -> rebuild(panel));
                }).padTop(60f).padRight(8f);
            });
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] CoreItemDelta.buildPanel: " + ex);
        }
    }

    private static void rebuild(Table panel) {
        if (!enabled || Vars.state == null || !Vars.state.isGame() || Vars.player == null) {
            panel.clear();
            return;
        }
        panel.clear();
        var team = Vars.player.team();
        var data = Vars.state.teams.get(team);
        if (data == null || data.cores == null || data.cores.isEmpty()) return;

        for (Item item : Vars.content.items()) {
            Float rate = ratePerSec.get(item);
            if (rate == null || Math.abs(rate) < 0.05f) continue;
            String color = rate >= 0f ? "[#7fff7f]" : "[#ff7f7f]";
            String arrow = rate >= 0f ? "▲" : "▼";
            String text = color + arrow + " " + Strings.fixed(Math.abs(rate), 1) + "/s[]";
            panel.image(item.uiIcon).size(20f).padRight(4f);
            panel.add(text).left().padRight(8f);
            panel.row();
        }
    }

    private static void tick() {
        if (Vars.state == null || !Vars.state.isGame() || Vars.player == null) return;

        accum += Time.delta;
        if (accum < SAMPLE_TICKS) return;
        float seconds = accum / 60f;
        accum = 0f;

        var team = Vars.player.team();
        var data = Vars.state.teams.get(team);
        if (data == null || data.cores == null) return;

        ObjectMap<Item, Integer> current = new ObjectMap<>();
        for (CoreBuild c : data.cores) {
            if (c == null || c.items == null) continue;
            for (Item it : Vars.content.items()) {
                int amt = c.items.get(it);
                current.put(it, current.get(it, 0) + amt);
            }
        }

        for (Item it : Vars.content.items()) {
            int prev = lastSample.get(it, current.get(it, 0));
            int cur = current.get(it, 0);
            float rate = (cur - prev) / seconds;
            ratePerSec.put(it, rate);
            lastSample.put(it, cur);
        }
    }
}
