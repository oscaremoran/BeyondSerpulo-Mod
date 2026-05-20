package beyondserpulo;

import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Unit;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

public class BossHUD {
    private static final Color FILL = Color.valueOf("ff5566");
    private static final Color OUTLINE = Color.valueOf("4a0c12");

    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static BossProgrammer.BossBuild currentBuild() {
        try { return BossProgrammer.activeBoss(); } catch (Exception ex) { return null; }
    }

    private static Unit currentBoss() {
        BossProgrammer.BossBuild b = currentBuild();
        return b == null ? null : b.findBoss();
    }

    private static void setup() {
        try {
            Table root = new Table();
            root.setFillParent(true);
            root.top().left();
            root.marginTop(96f).marginLeft(12f);

            Table card = new Table();
            try { card.setBackground(Styles.black6); } catch (Exception ex) {}
            card.margin(8f);

            Label title = card.add("[#ff8a99]BOSS[]").left().get();
            try { title.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            title.setFontScale(1.0f);
            card.row();

            Label pct = card.add("").left().get();
            try { pct.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            pct.update(() -> {
                Unit u = currentBoss();
                if (u == null || u.maxHealth <= 0f) { pct.setText(""); return; }
                int p = (int) Math.ceil((u.health / u.maxHealth) * 100f);
                if (p < 0) p = 0; if (p > 100) p = 100;
                pct.setText("[white]" + p + "%[]");
            });
            card.row();

            Bar bar = new Bar("", FILL, () -> {
                Unit u = currentBoss();
                if (u == null || u.maxHealth <= 0f) return 0f;
                return Math.max(0f, Math.min(1f, u.health / u.maxHealth));
            });
            try { bar.outline(OUTLINE, 2.2f); } catch (Exception ex) {}
            card.add(bar).size(220f, 18f).padTop(4f);

            root.add(card);
            root.visible(() -> currentBoss() != null);

            Vars.ui.hudGroup.addChild(root);
            Log.info("[BeyondSerpulo] Boss HUD installed");
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] BossHUD: " + ex);
        }
    }
}
