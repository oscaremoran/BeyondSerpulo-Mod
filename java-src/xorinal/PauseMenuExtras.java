package xorinal;

import arc.Core;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;

public class PauseMenuExtras {
    private static Table row1, row2, row3, row4;

    public static void init() {
        Core.app.post(PauseMenuExtras::hook);
    }

    private static void hook() {
        try {
            if (Vars.ui == null || Vars.ui.paused == null) return;
            Vars.ui.paused.shown(PauseMenuExtras::injectButtons);
        } catch (Exception ex) {
            Log.err("[Xorinal] PauseMenuExtras.hook: " + ex);
        }
    }

    private static void injectButtons() {
        try {
            Table cont = Vars.ui.paused.cont;
            if (cont == null) return;

            if (row1 != null) row1.remove();
            if (row2 != null) row2.remove();
            if (row3 != null) row3.remove();
            if (row4 != null) row4.remove();

            cont.row();
            row1 = cont.table().colspan(4).get();
            row1.defaults().size(210f, 64f).pad(4f);

            row1.button("[#2eff78]Xorinal Codex[]", () -> {
                Vars.ui.paused.hide();
                Codex.show();
            });

            TextButton pathBtn = row1.button("Enemy Paths: OFF", EnemyPathOverlay::toggle).get();
            pathBtn.update(() -> pathBtn.setText(
                    EnemyPathOverlay.isEnabled() ? "[#ff5555]Enemy Paths: ON[]" : "Enemy Paths: OFF"));

            cont.row();
            row2 = cont.table().colspan(4).get();
            row2.defaults().size(210f, 64f).pad(4f);

            TextButton flowBtn = row2.button("Item Flow: ON", CoreItemDelta::toggle).get();
            flowBtn.update(() -> flowBtn.setText(
                    CoreItemDelta.isEnabled() ? "[#7fff7f]Item Flow: ON[]" : "Item Flow: OFF"));

            TextButton destroyBtn = row2.button("Destroy Log: ON", DestroyLog::toggle).get();
            destroyBtn.update(() -> destroyBtn.setText(
                    DestroyLog.isEnabled() ? "[#7fff7f]Destroy Log: ON[]" : "Destroy Log: OFF"));

            cont.row();
            row3 = cont.table().colspan(4).get();
            row3.defaults().size(210f, 64f).pad(4f);

            TextButton hpBtn = row3.button("HP Bars: ON", UnitHealthBars::toggle).get();
            hpBtn.update(() -> hpBtn.setText(
                    UnitHealthBars.isEnabled() ? "[#7fff7f]HP Bars: ON[]" : "HP Bars: OFF"));

            TextButton dmgBtn = row3.button("Damage Numbers: ON", DamageNumbers::toggle).get();
            dmgBtn.update(() -> dmgBtn.setText(
                    DamageNumbers.isEnabled() ? "[#7fff7f]Damage Numbers: ON[]" : "Damage Numbers: OFF"));

            cont.row();
            row4 = cont.table().colspan(4).get();
            row4.defaults().size(210f, 64f).pad(4f);

            TextButton speedBtn = row4.button("Speed Bar: ON", GameSpeed::toggle).get();
            speedBtn.update(() -> speedBtn.setText(
                    GameSpeed.isEnabled() ? "[#7fff7f]Speed Bar: ON[]" : "Speed Bar: OFF"));
        } catch (Exception ex) {
            Log.err("[Xorinal] PauseMenuExtras.injectButtons: " + ex);
        }
    }
}
