package xorinal;

import arc.Events;
import arc.scene.ui.layout.Table;
import arc.struct.IntMap;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;

public class CapsuleLauncher {
    private static final String BLOCK_NAME = "xorinal-capsule-launcher";
    private static Table hudWidget;
    private static int onPadUnitId = -1;

    public static void init() {
        Events.on(EventType.WorldLoadEvent.class, e -> setupWidget());
        Events.run(EventType.Trigger.update, CapsuleLauncher::tick);
    }

    private static void setupWidget() {
        try {
            if (hudWidget != null) { hudWidget.remove(); hudWidget = null; }
            Table wrap = new Table();
            wrap.setFillParent(true);
            wrap.bottom();
            Table card = new Table();
            try { card.background(Tex.button); } catch (Exception ex) {}
            card.add("[#7fffd0]Pilot on Capsule Launcher[]").padRight(10f);
            card.button("Launch...", CapsuleLauncher::openLaunchDialog).size(140f, 44f);
            wrap.add(card).pad(80f).bottom();
            wrap.visible(() -> onPadUnitId >= 0);
            Vars.ui.hudGroup.addChild(wrap);
            hudWidget = wrap;
        } catch (Exception ex) { Log.err("[Xorinal] CapsuleLauncher.setupWidget: " + ex); }
    }

    private static void tick() {
        try {
            if (Vars.state == null || !Vars.state.isGame()) { onPadUnitId = -1; return; }
            int found = -1;
            for (IntMap.Entry<PilotData> e : Pilots.byUnit.entries()) {
                Unit u = Groups.unit.getByID(e.key);
                if (u == null || !u.isValid()) continue;
                Tile t = Vars.world.tile(u.tileX(), u.tileY());
                if (t == null) continue;
                Building b = t.build;
                if (b == null || b.block == null) continue;
                if (BLOCK_NAME.equals(b.block.name)) { found = u.id; break; }
            }
            onPadUnitId = found;
        } catch (Exception ex) { onPadUnitId = -1; }
    }

    private static void openLaunchDialog() {
        if (onPadUnitId < 0) return;
        PilotData pilot = Pilots.byUnit.get(onPadUnitId);
        Unit unit = Groups.unit.getByID(onPadUnitId);
        if (pilot == null || unit == null) return;

        BaseDialog d = new BaseDialog("[#7fffd0]Capsule Launcher[]");
        Table list = new Table();
        list.defaults().pad(4f);

        String pname = pilot.name == null ? "Unnamed pilot" : pilot.name;
        list.add("[gold]Pilot: " + pname + "[]   [lightgray]" + Pilots.TIER_NAMES[pilot.tier()] + "[]").left();
        list.row();
        Item costItem = Items.graphite;
        int cost = launchCost(pilot);
        list.add("[lightgray]Cost: []" + "[gold]" + cost + " " + costItem.localizedName + "[]").left();
        list.row();
        list.add("[lightgray]Pick a destination — only sectors you control are listed.[]").left();
        list.row();

        boolean[] any = {false};
        for (Planet p : Vars.content.planets()) {
            Seq<Sector> owned = new Seq<>();
            try {
                for (Sector s : p.sectors) {
                    if (s.hasBase() && (Vars.state.rules.sector == null || s != Vars.state.rules.sector)) owned.add(s);
                }
            } catch (Exception ex) { continue; }
            if (owned.isEmpty()) continue;
            list.add("[#" + colorOf(p) + "]" + p.localizedName + "[]").left().padTop(8f);
            list.row();
            Table grid = new Table();
            grid.defaults().pad(2f);
            int col = 0;
            for (Sector s : owned) {
                String label = (s.preset != null ? s.preset.localizedName : ("Sector " + s.id));
                grid.button(label, () -> {
                    if (tryLaunch(pilot, unit, s, costItem, cost)) d.hide();
                }).size(220f, 40f);
                col++;
                if (col % 3 == 0) grid.row();
                any[0] = true;
            }
            list.add(grid).left();
            list.row();
        }
        if (!any[0]) {
            list.add("[red]You don't control any other sectors yet.[]").left();
            list.row();
        }
        d.cont.add(list).grow();
        d.addCloseButton();
        d.show();
    }

    private static String colorOf(Planet p) {
        if (p == null || p.name == null) return "ffffff";
        if (p.name.contains("xorinal")) return "2eff78";
        if (p.name.contains("vantres")) return "ff7a3d";
        if (p.name.contains("tetra")) return "7fdfff";
        return "ffffff";
    }

    private static int launchCost(PilotData p) {
        if (p == null) return 60;
        return 60 + 20 * p.tier();
    }

    private static boolean tryLaunch(PilotData pilot, Unit unit, Sector dest, Item costItem, int cost) {
        try {
            Building core = Vars.player == null ? null : Vars.player.team().core();
            if (core == null) {
                Vars.ui.showInfoToast("[red]No core to launch from[]", 2f);
                return false;
            }
            if (core.items == null || core.items.get(costItem) < cost) {
                Vars.ui.showInfoToast("[red]Need " + cost + " " + costItem.localizedName + "[]", 2f);
                return false;
            }
            core.items.remove(costItem, cost);

            String sectorKey = dest.planet.name + ":" + dest.id;
            if (!Pilots.parkPilotForLaunch(pilot, unit, sectorKey)) {
                Vars.ui.showInfoToast("[red]Launch failed[]", 2f);
                return false;
            }

            String destName = dest.preset != null ? dest.preset.localizedName : ("Sector " + dest.id);
            String pname = pilot.name == null ? "Pilot" : pilot.name;
            Vars.ui.showInfoToast("[#7fffd0]" + pname + "[] launched to [accent]"
                + destName + "[] on [accent]" + dest.planet.localizedName + "[].", 4f);
            return true;
        } catch (Exception ex) { Log.err("[Xorinal] tryLaunch: " + ex); return false; }
    }
}
