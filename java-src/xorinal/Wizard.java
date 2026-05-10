package xorinal;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.scene.actions.Actions;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.gen.Tex;
import mindustry.maps.Map;
import mindustry.ui.dialogs.BaseDialog;

public class Wizard {
    private static final String SHOWN_KEY = "xorinal-wizard-shown";

    private static class Page {
        final String title, body;
        Page(String title, String body) { this.title = title; this.body = body; }
    }

    // Intro pages shown in the menu before the player launches the sandbox walkthrough.
    private static final Seq<Page> intro = Seq.with(
        new Page("Welcome to Beyond Serpulo",
            "Beyond Serpulo is a story-driven expansion that adds new planets and a host of quality-of-life systems to Mindustry.\n\n" +
            "This guide will (1) walk you through the new features here, then (2) drop you into a sandbox Maze map for a hands-on tour.\n\n" +
            "You can replay it any time from Settings -> Game -> 'Show Beyond Serpulo Wizard'."),
        new Page("New Planets: Vantres, Tetra & Xorinal",
            "Three new worlds are reachable from the planet map.\n\n" +
            "[#ff7a3d]Vantres[]  - the volcanic planet: ash plains, magma flows, and Serpulo factions clashing in the heat.\n" +
            "[#7fdfff]Tetra[]    - the frozen frontier: ice fields and brittle alloys at the edge of charted space.\n" +
            "[#2eff78]Xorinal[]  - the living jungle planet: the Sporocyst takes root and the Bloomheart sleeps beneath.\n\n" +
            "Each has its own campaign, sectors, enemies, and tech."),
        new Page("The Xorinal Codex",
            "The Codex is your in-game lore + progression journal. Entries unlock as you discover the planet, defeat new enemies, capture key sectors, and build new structures.\n\n" +
            "Open it from the [#2eff78]Pause Menu -> Xorinal Codex[] button. New unread entries are marked with a red dot."),
        new Page("Pause Menu: QoL Toggles",
            "Pressing pause opens a panel of toggles for new visual aids:\n\n" +
            "- [#ff5555]Enemy Paths[] - draw projected enemy spawn lanes.\n" +
            "- [#7fff7f]Item Flow[]   - core item delta panel (per-second gain/loss).\n" +
            "- [#7fff7f]Destroy Log[] - log of recently destroyed buildings.\n" +
            "- [#7fff7f]HP Bars[] / [#7fff7f]Damage Numbers[] - combat readability.\n" +
            "- [#7fff7f]Speed Bar[] / [#7fff7f]Resource Finder[] - extra HUD widgets.\n" +
            "- [#ffd166]Sector Pause[] - pause time progression in the current sector."),
        new Page("Pilots & Roster",
            "Pilots are persistent characters that level up across sectors and gain specials.\n\n" +
            "Open the [#ffe066]Pilot Roster[] from the pause menu to view your roster, rename pilots, and check their specials. Pilot effects appear in-world above your unit."),
        new Page("Hands-On Tour",
            "Next, the wizard will launch the built-in [#ffd166]Maze[] map in sandbox mode and walk you through each feature in-game with on-screen prompts.\n\n" +
            "Press [#7fff7f]Start Tour[] below to begin, or [#aaaaaa]Skip[] to finish the wizard here.")
    );

    // In-game walkthrough steps. anchor is one of: TL, TR, BL, BR, C - where to place the floating panel.
    private static class Step {
        final String anchor, title, body;
        Step(String anchor, String title, String body) { this.anchor = anchor; this.title = title; this.body = body; }
    }

    private static final Seq<Step> steps = Seq.with(
        new Step("C",  "Welcome to the Maze",
            "You're in a sandbox copy of the [#ffd166]Maze[] map. Infinite resources are on. Nothing here is permanent - feel free to experiment.\n\n" +
            "Click [#7fff7f]Next[] to continue."),
        new Step("TL", "Open the Pause Menu",
            "Press [#ffd166]Esc[] (or click the menu button in the top-left of the HUD).\n\n" +
            "All Beyond Serpulo toggles live there. Click Next once you've seen it."),
        new Step("C",  "Pause Menu: Codex",
            "In the pause menu, the [#2eff78]Xorinal Codex[] button opens your lore + progression journal.\n\n" +
            "Try opening it now, then close it and click Next."),
        new Step("C",  "Pause Menu: HUD Toggles",
            "These toggles control HUD widgets:\n" +
            "- [#7fff7f]Item Flow[] - core deltas\n" +
            "- [#7fff7f]Destroy Log[] - recent kills\n" +
            "- [#7fff7f]HP Bars[] / [#7fff7f]Damage Numbers[]\n" +
            "- [#7fff7f]Speed Bar[] - fast-forward / slow-mo\n" +
            "- [#7fff7f]Resource Finder[] - locate ores\n" +
            "Toggle a few, then click Next."),
        new Step("C",  "Pause Menu: Sector Pause",
            "[#ffd166]Sector Pause[] freezes the sector clock - useful when planning under attack.\n\n" +
            "Try toggling it on/off, then click Next."),
        new Step("C",  "Pilot Roster",
            "[#ffe066]Pilot Roster[] (pause menu) shows your persistent pilots, their levels, and specials.\n\n" +
            "Open it once, then click Next."),
        new Step("BR", "Speed Bar (HUD)",
            "When the [#7fff7f]Speed Bar[] toggle is on, a slider appears in the HUD letting you fast-forward / slow-mo the simulation in singleplayer.\n\n" +
            "Click Next when ready."),
        new Step("C",  "You're Ready",
            "That's the tour. The Maze map is yours to play in.\n\n" +
            "When you're done here, return to the planet map and explore [#ff7a3d]Vantres[], [#7fdfff]Tetra[], and [#2eff78]Xorinal[].\n\n" +
            "Click [#7fff7f]Finish[] to close the wizard.")
    );

    private static BaseDialog dialog;
    private static int introIdx;
    private static Label titleLabel;
    private static Label bodyLabel;

    private static boolean tourPending = false;
    private static int stepIdx = 0;
    private static Table tourOverlay;

    public static void init() {
        Events.on(ClientLoadEvent.class, e -> {
            try {
                addSettingsButton();
                if (!Core.settings.getBool(SHOWN_KEY, false)) {
                    Core.app.post(Wizard::show);
                }
            } catch (Exception ex) { Log.err("[Xorinal] Wizard.init: " + ex); }
        });
        Events.on(WorldLoadEvent.class, e -> {
            try {
                if (tourPending) {
                    tourPending = false;
                    stepIdx = 0;
                    Core.app.post(Wizard::showTourStep);
                }
            } catch (Exception ex) { Log.err("[Xorinal] Wizard tour world load: " + ex); }
        });
    }

    public static void show() {
        try {
            if (dialog == null) build();
            introIdx = 0;
            refresh();
            dialog.show();
        } catch (Exception ex) { Log.err("[Xorinal] Wizard.show: " + ex); }
    }

    private static void build() {
        dialog = new BaseDialog("[#2eff78]Beyond Serpulo - First-Time Guide[]");

        Table content = new Table();
        content.defaults().pad(8f);

        titleLabel = new Label("");
        try { titleLabel.setFontScale(1.2f); } catch (Exception ex) {}
        content.add(titleLabel).left();
        content.row();

        bodyLabel = new Label("");
        bodyLabel.setWrap(true);
        Table card = new Table();
        try { card.background(Tex.button); } catch (Exception ex) {}
        card.add(bodyLabel).width(620f).pad(12f).left();
        ScrollPane sp = new ScrollPane(card);
        content.add(sp).minWidth(680f).minHeight(320f).grow();

        dialog.cont.add(content).grow();

        dialog.buttons.button("Skip", () -> {
            Core.settings.put(SHOWN_KEY, true);
            dialog.hide();
        }).size(120f, 50f);

        dialog.buttons.button("Back", () -> {
            if (introIdx > 0) { introIdx--; refresh(); }
        }).size(120f, 50f);

        TextButton next = dialog.buttons.button("Next", () -> {
            if (introIdx < intro.size - 1) {
                introIdx++;
                refresh();
            } else {
                Core.settings.put(SHOWN_KEY, true);
                dialog.hide();
                launchMazeAndStartTour();
            }
        }).size(120f, 50f).get();
        next.update(() -> next.setText(introIdx >= intro.size - 1 ? "Start Tour" : "Next"));
    }

    private static void refresh() {
        try {
            Page p = intro.get(introIdx);
            titleLabel.setText("[#2eff78]" + p.title + "[]   [lightgray](" + (introIdx + 1) + "/" + intro.size + ")[]");
            bodyLabel.setText(p.body);
        } catch (Exception ex) { Log.err("[Xorinal] Wizard.refresh: " + ex); }
    }

    private static void launchMazeAndStartTour() {
        try {
            Map maze = null;
            for (Map m : Vars.maps.all()) {
                String n = m.name();
                if (n != null && n.equalsIgnoreCase("Maze")) { maze = m; break; }
            }
            if (maze == null) {
                try { Vars.ui.showInfoFade("[#ff7a3d]Could not find built-in Maze map. Wizard finished.[]"); } catch (Exception ex) {}
                return;
            }
            Rules rules = maze.applyRules(Gamemode.sandbox);
            tourPending = true;
            final Map fmaze = maze;
            Core.app.post(() -> {
                try {
                    Vars.control.playMap(fmaze, rules);
                } catch (Exception ex) {
                    Log.err("[Xorinal] Wizard playMap: " + ex);
                    tourPending = false;
                }
            });
        } catch (Exception ex) { Log.err("[Xorinal] launchMazeAndStartTour: " + ex); }
    }

    private static void showTourStep() {
        try {
            if (tourOverlay != null) { tourOverlay.remove(); tourOverlay = null; }
            if (stepIdx < 0 || stepIdx >= steps.size) return;
            Step s = steps.get(stepIdx);

            Table wrap = new Table();
            wrap.setFillParent(true);
            switch (s.anchor) {
                case "TL": wrap.top().left(); break;
                case "TR": wrap.top().right(); break;
                case "BL": wrap.bottom().left(); break;
                case "BR": wrap.bottom().right(); break;
                default:   wrap.center(); break;
            }

            Table card = new Table();
            try { card.background(Tex.button); } catch (Exception ex) {}
            card.defaults().pad(6f);

            Label tl = new Label("[#2eff78]" + s.title + "[]   [lightgray](" + (stepIdx + 1) + "/" + steps.size + ")[]");
            try { tl.setFontScale(1.05f); } catch (Exception ex) {}
            card.add(tl).left();
            card.row();

            // arrow pointing toward the anchor edge (gives the "where to look" hint)
            String arrow = arrowFor(s.anchor);
            if (arrow != null) {
                Label al = new Label(arrow);
                try { al.setFontScale(1.6f); al.setColor(Color.valueOf("ffd166")); } catch (Exception ex) {}
                al.actions(Actions.forever(Actions.sequence(Actions.alpha(0.35f, 0.6f), Actions.alpha(1f, 0.6f))));
                card.add(al).left();
                card.row();
            }

            Label bl = new Label(s.body);
            bl.setWrap(true);
            card.add(bl).width(360f).left();
            card.row();

            Table btns = new Table();
            btns.defaults().size(110f, 44f).pad(4f);
            btns.button("Skip Tour", () -> {
                if (tourOverlay != null) { tourOverlay.remove(); tourOverlay = null; }
                stepIdx = steps.size;
            });
            btns.button("Back", () -> {
                if (stepIdx > 0) { stepIdx--; showTourStep(); }
            });
            TextButton nb = btns.button("Next", () -> {
                if (stepIdx < steps.size - 1) {
                    stepIdx++;
                    showTourStep();
                } else {
                    if (tourOverlay != null) { tourOverlay.remove(); tourOverlay = null; }
                }
            }).get();
            nb.update(() -> nb.setText(stepIdx >= steps.size - 1 ? "Finish" : "Next"));
            card.add(btns).left();

            wrap.add(card).pad(20f).width(420f);
            Vars.ui.hudGroup.addChild(wrap);
            tourOverlay = wrap;
        } catch (Exception ex) { Log.err("[Xorinal] showTourStep: " + ex); }
    }

    private static String arrowFor(String anchor) {
        switch (anchor) {
            case "TL": return "<-- look top-left";
            case "TR": return "look top-right -->";
            case "BL": return "<-- look bottom-left";
            case "BR": return "look bottom-right -->";
            default: return null;
        }
    }

    private static void addSettingsButton() {
        try {
            var game = Vars.ui.settings.game;
            if (game == null) return;
            game.row();
            game.button("Show Beyond Serpulo Wizard", Wizard::show).size(260f, 50f).padTop(10f);
        } catch (Exception ex) { Log.err("[Xorinal] Wizard settings button: " + ex); }
    }
}
