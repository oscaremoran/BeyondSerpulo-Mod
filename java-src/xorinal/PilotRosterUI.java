package xorinal;

import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.type.UnitType;
import mindustry.ui.dialogs.BaseDialog;

public class PilotRosterUI {
    public static void show() {
        BaseDialog dialog = new BaseDialog("Pilot Roster");
        dialog.cont.pane(t -> rebuild(t)).grow();
        dialog.addCloseButton();
        dialog.show();
    }

    private static void rebuild(Table t) {
        t.clear();
        t.defaults().pad(4f);

        t.add("[gold]Active Roster[]").colspan(2).left();
        t.row();
        if (Pilots.roster.isEmpty()) {
            t.add("[lightgray]No pilots in roster yet — promote a unit to Recruit (10 kill pts) to enroll one.[]").colspan(2).left();
            t.row();
        } else {
            for (PilotData p : Pilots.roster.copy()) addRosterRow(t, p);
        }

        t.add().height(12f); t.row();
        Table fameHeader = new Table();
        fameHeader.add("[gold]Hall of Fame[] [lightgray](top 3 fallen)[]").left().growX();
        if (!Pilots.hallOfFame.isEmpty()) {
            fameHeader.button("[#ff7777]Clear[]", () -> {
                Vars.ui.showConfirm("Clear the Hall of Fame? This cannot be undone.", () -> {
                    Pilots.clearHallOfFame();
                    rebuild(t);
                });
            }).size(110f, 36f).right();
        }
        t.add(fameHeader).colspan(2).growX();
        t.row();
        if (Pilots.hallOfFame.isEmpty()) {
            t.add("[lightgray]No fallen pilots yet.[]").colspan(2).left();
            t.row();
        } else {
            for (PilotData p : Pilots.hallOfFame) addFameRow(t, p);
        }
    }

    private static void addRosterRow(Table t, PilotData p) {
        UnitType type = Pilots.lookupType(p.unitTypeName);
        Table card = new Table(Tex.pane);
        card.left().defaults().pad(2f);
        if (type != null) card.image(type.uiIcon).size(32f);
        card.add("[white]" + p.name + "[]\n[lightgray]" + (type != null ? type.localizedName : p.unitTypeName)
                + " — " + Pilots.TIER_NAMES[p.tier()] + "[]\n[gold]" + p.killPoints + " kill pts[]").left().padLeft(8f);
        PilotSpecials.Special s = type != null ? PilotSpecials.of(type) : null;
        if (s != null) {
            String special = p.tier() >= 3 ? s.colored : "[gray]" + s.label + " (locked)[]";
            card.add("\n" + special).left().padLeft(8f);
        }
        t.add(card).growX().left();

        Table actions = new Table();
        int cost = Pilots.deployCost(p);
        actions.button("Deploy ([gold]" + cost + "[] " + Pilots.deployCostItem().localizedName + ")", () -> {
            if (Pilots.tryDeploy(p)) {
                if (Vars.ui != null && Vars.ui.paused != null) Vars.ui.paused.hide();
            }
        }).size(220f, 44f);
        t.add(actions).right();
        t.row();
    }

    private static void addFameRow(Table t, PilotData p) {
        UnitType type = Pilots.lookupType(p.unitTypeName);
        Table card = new Table(Tex.pane);
        card.left().defaults().pad(2f);
        if (type != null) card.image(type.uiIcon).size(28f);
        card.add("[white]" + p.name + "[]   [lightgray]" + (type != null ? type.localizedName : p.unitTypeName)
                + " — " + Pilots.TIER_NAMES[p.tier()] + "[]   [gold]" + p.killPoints + " kp[]").left().padLeft(8f);
        t.add(card).colspan(2).growX().left();
        t.row();
    }
}
