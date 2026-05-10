package xorinal;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectIntMap;
import arc.util.Align;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.SpawnGroup;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.world.Block;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.defense.turrets.Turret;

public class WaveIntel {
    private static boolean enabled = true;

    private static final String[] SECTION_NAMES = {"Don't Care", "Easy", "OK", "Tough", "You're Dead"};
    private static final Color[] SECTION_COLORS = {
        Color.valueOf("4f8fff"),
        Color.valueOf("7fff7f"),
        Color.valueOf("ffe066"),
        Color.valueOf("ffaa55"),
        Color.valueOf("ff5555")
    };

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    public static void init() {
        Core.app.post(WaveIntel::buildPanel);
    }

    private static void buildPanel() {
        try {
            if (Vars.ui == null || Vars.ui.hudGroup == null) return;
            Table root = new Table();
            root.setFillParent(true);
            root.bottom().left();

            Table panel = new Table();
            try { panel.setBackground(Styles.black6); } catch (Exception ex) {}
            panel.margin(8f);
            panel.defaults().left();

            Label header = new Label("[#ffd166]Next Wave[]");
            try { header.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            panel.add(header).left().padBottom(4f);
            panel.row();

            Table list = new Table();
            list.left().top();
            panel.add(list).left().padBottom(6f).width(220f);
            panel.row();

            Element dial = new Element() {
                @Override
                public void draw() {
                    drawDial(x + getWidth() / 2f, y + getHeight() / 2f - 4f);
                }
            };
            panel.add(dial).size(180f, 100f).padTop(4f);
            panel.row();

            Label dialLabel = new Label("");
            try { dialLabel.setStyle(Styles.outlineLabel); } catch (Exception ex) {}
            dialLabel.setAlignment(Align.center);
            dialLabel.setFontScale(1.1f);
            panel.add(dialLabel).growX().padTop(2f);

            list.update(() -> rebuildList(list));
            dialLabel.update(() -> {
                int idx = currentSection();
                if (idx < 0) { dialLabel.setText(""); return; }
                dialLabel.setColor(SECTION_COLORS[idx]);
                dialLabel.setText(SECTION_NAMES[idx]);
            });

            root.add(panel).padBottom(8f).padLeft(8f);
            root.visible(() -> enabled && Vars.state != null && Vars.state.isGame() && hasUpcomingWave());

            Vars.ui.hudGroup.addChild(root);
            Log.info("[Xorinal] WaveIntel installed");
        } catch (Exception ex) {
            Log.err("[Xorinal] WaveIntel.buildPanel: " + ex);
        }
    }

    private static int waveIndex() {
        int w = Vars.state.wave - 1;
        return Math.max(0, w);
    }

    private static boolean hasUpcomingWave() {
        try {
            if (Vars.state == null || Vars.state.rules == null || Vars.state.rules.spawns == null) return false;
            int wave = waveIndex();
            for (SpawnGroup g : Vars.state.rules.spawns) {
                if (g != null && g.type != null && g.getSpawned(wave) > 0) return true;
            }
            return false;
        } catch (Exception ex) { return false; }
    }

    private static void rebuildList(Table list) {
        list.clear();
        if (Vars.state == null || Vars.state.rules == null || Vars.state.rules.spawns == null) return;
        int wave = waveIndex();
        ObjectIntMap<UnitType> totals = new ObjectIntMap<>();
        for (SpawnGroup g : Vars.state.rules.spawns) {
            if (g == null || g.type == null) continue;
            int n = g.getSpawned(wave);
            if (n > 0) totals.increment(g.type, 0, n);
        }
        int col = 0;
        for (var e : totals.entries()) {
            list.image(e.key.uiIcon).size(22f).padRight(2f);
            list.add("[white]x" + e.value + "[]").left().padRight(8f);
            col++;
            if (col % 3 == 0) list.row();
        }
    }

    private static float enemyStrength() {
        if (Vars.state == null || Vars.state.rules == null || Vars.state.rules.spawns == null) return 0f;
        int wave = waveIndex();
        float total = 0f;
        for (SpawnGroup g : Vars.state.rules.spawns) {
            if (g == null || g.type == null) continue;
            int n = g.getSpawned(wave);
            if (n <= 0) continue;
            total += n * g.type.health;
        }
        return total;
    }

    private static float defenseStrength() {
        if (Vars.player == null) return 1f;
        var team = Vars.player.team();
        float total = 0f;
        for (Building b : Groups.build) {
            if (b == null || b.team != team) continue;
            Block block = b.block;
            if (block == null) continue;
            float weight = 1f;
            if (block instanceof Turret) weight = 5f;
            else if (block instanceof Wall) weight = 2f;
            total += block.health * weight;
        }
        return Math.max(1f, total);
    }

    private static int currentSection() {
        float ratio = enemyStrength() / defenseStrength();
        if (ratio < 0.10f) return 0;
        if (ratio < 0.40f) return 1;
        if (ratio < 1.00f) return 2;
        if (ratio < 2.50f) return 3;
        return 4;
    }

    private static void drawDial(float cx, float cy) {
        float radius = 42f;
        Lines.stroke(8f);
        for (int i = 0; i < 5; i++) {
            float a0 = 180f - i * 36f;
            float a1 = 180f - (i + 1) * 36f;
            Draw.color(SECTION_COLORS[i]);
            Lines.arc(cx, cy, radius, (a0 - a1) / 360f, a1);
        }
        Draw.color();

        int idx = Math.max(0, Math.min(4, currentSection()));
        float needleAngle = 180f - (idx + 0.5f) * 36f;
        float nx = cx + Mathf.cosDeg(needleAngle) * (radius + 2f);
        float ny = cy + Mathf.sinDeg(needleAngle) * (radius + 2f);
        Lines.stroke(2.5f);
        Draw.color(Color.white);
        Lines.line(cx, cy, nx, ny);
        Fill.circle(cx, cy, 3.5f);
        Draw.reset();
    }
}
