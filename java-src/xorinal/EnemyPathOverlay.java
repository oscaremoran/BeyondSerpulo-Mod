package xorinal;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.graphics.Layer;
import mindustry.world.Tile;
import mindustry.world.blocks.units.Reconstructor;
import mindustry.world.blocks.units.UnitAssembler;
import mindustry.world.blocks.units.UnitBlock;
import mindustry.world.blocks.units.UnitFactory;

public class EnemyPathOverlay {
    private static boolean enabled;
    private static TextButton btn;

    public static void init() {
        Core.app.post(EnemyPathOverlay::buildBtn);
        Events.run(EventType.Trigger.drawOver, EnemyPathOverlay::draw);
    }

    private static void buildBtn() {
        try {
            if (Vars.ui == null || Vars.ui.hudGroup == null) return;
            Table hud = new Table();
            hud.setFillParent(true);
            hud.top().right();
            btn = hud.button("Paths: OFF", () -> {
                enabled = !enabled;
                if (btn != null) btn.setText(enabled ? "[#ff5555]Paths: ON[]" : "Paths: OFF");
            }).size(140f, 40f).padTop(8f).padRight(330f).get();
            Vars.ui.hudGroup.addChild(hud);
            Log.info("[Xorinal] enemy path overlay button added");
        } catch (Exception ex) {
            Log.err("[Xorinal] EnemyPathOverlay.buildBtn: " + ex);
        }
    }

    private static void draw() {
        if (!enabled) return;
        if (Vars.state == null || !Vars.state.isGame()) return;
        if (Vars.spawner == null || Vars.pathfinder == null || Vars.state.rules == null) return;

        var team = Vars.state.rules.waveTeam;
        if (team == null) return;

        Pathfinder.Flowfield field;
        try {
            field = Vars.pathfinder.getField(team, Pathfinder.costGround, Pathfinder.fieldCore);
        } catch (Exception ex) { return; }
        if (field == null) return;

        float prevZ = Draw.z();
        Draw.z(Layer.overlayUI);
        Draw.color(Color.scarlet);
        Lines.stroke(1.6f);

        for (Tile spawn : Vars.spawner.getSpawns()) {
            traceFrom(spawn, field);
        }

        Groups.build.each(b -> {
            if (b == null || b.tile == null || b.block == null) return;
            if (b.team != team) return;
            if (!isUnitProducer(b)) return;
            traceFromBuilding(b, field);
        });

        Draw.color();
        Draw.z(prevZ);
    }

    private static void traceFrom(Tile start, Pathfinder.Flowfield field) {
        Tile cur = start;
        for (int i = 0; i < 600; i++) {
            Tile next = Vars.pathfinder.getTargetTile(cur, field);
            if (next == null || next == cur) break;
            Lines.line(cur.worldx(), cur.worldy(), next.worldx(), next.worldy());
            cur = next;
        }
    }

    private static boolean isUnitProducer(Building b) {
        if (b.block instanceof UnitBlock) return true;
        if (b.block instanceof UnitFactory) return true;
        if (b.block instanceof Reconstructor) return true;
        if (b.block instanceof UnitAssembler) return true;
        String n = b.block.getClass().getSimpleName().toLowerCase();
        return n.contains("factory") || n.contains("reconstructor") || n.contains("assembler") || n.contains("refabricator");
    }

    private static void traceFromBuilding(Building b, Pathfinder.Flowfield field) {
        int size = b.block.size;
        int cx = b.tileX();
        int cy = b.tileY();
        int half = size / 2;
        int minX = cx - half - 1;
        int maxX = cx - half + size;
        int minY = cy - half - 1;
        int maxY = cy - half + size;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (x > minX && x < maxX && y > minY && y < maxY) continue;
                Tile t = Vars.world.tile(x, y);
                if (t == null) continue;
                Tile next = Vars.pathfinder.getTargetTile(t, field);
                if (next == null || next == t) continue;
                Lines.line(b.x, b.y, next.worldx(), next.worldy());
                traceFrom(next, field);
                return;
            }
        }
    }
}
