package xorinal;

import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.ui.Styles;

public class PilotDownedHUD {
    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static void setup() {
        try {
            Table root = new Table();
            root.setFillParent(true);
            root.top();
            root.marginTop(110f);

            Table card = new Table();
            try { card.setBackground(Styles.black6); } catch (Exception ex) {}
            card.margin(12f);

            Label label = new Label("");
            try { label.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            label.setFontScale(1.5f);
            label.setAlignment(Align.center);
            card.add(label);

            label.update(() -> {
                int n = Pilots.downedCount();
                if (n <= 0) return;
                long ms = Pilots.earliestDownedMillisRemaining();
                float seconds = Math.max(0f, ms / 1000f);
                float pulse = 0.55f + 0.45f * Mathf.absin(Time.time, 7f, 1f);
                label.setColor(1f, 0.2f * pulse, 0.2f * pulse, 1f);
                String prefix = n > 1 ? n + " PILOTS DOWN — " : "PILOT DOWN — ";
                label.setText(prefix + String.format("%.1fs", seconds));
            });

            root.add(card);
            root.visible(() -> Pilots.downedCount() > 0);

            Vars.ui.hudGroup.addChild(root);
            Log.info("[Xorinal] PilotDownedHUD installed");
        } catch (Exception ex) {
            Log.err("[Xorinal] PilotDownedHUD: " + ex);
        }
    }
}
