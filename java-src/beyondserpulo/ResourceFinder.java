package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType;
import mindustry.gen.Tex;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;

public class ResourceFinder {
    private static final int MIN_PATCH = 16;
    private static boolean enabled = true;
    private static UnlockableContent selected;
    private static float targetX, targetY;
    private static boolean hasTarget;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() {
        enabled = !enabled;
        if (!enabled) { selected = null; hasTarget = false; }
    }

    public static void init() {
        Core.app.post(ResourceFinder::buildPanel);
        Events.run(EventType.Trigger.drawOver, ResourceFinder::draw);
    }

    private static Seq<UnlockableContent> collectNaturalResources() {
        Seq<UnlockableContent> out = new Seq<>();
        IntSet seenItems = new IntSet();
        IntSet seenLiquids = new IntSet();
        for (Block b : Vars.content.blocks()) {
            if (!(b instanceof Floor)) continue;
            Floor f = (Floor) b;
            if (f.itemDrop != null && seenItems.add(f.itemDrop.id)) {
                out.add(f.itemDrop);
            }
            if (f.liquidDrop != null && seenLiquids.add(f.liquidDrop.id)) {
                out.add(f.liquidDrop);
            }
        }
        return out;
    }

    private static void buildPanel() {
        try {
            if (Vars.ui == null || Vars.ui.hudGroup == null) return;
            Vars.ui.hudGroup.fill(t -> {
                t.left();
                t.table(Tex.pane, panel -> {
                    panel.update(() -> rebuild(panel));
                }).pad(8f).left();
            });
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] ResourceFinder.buildPanel: " + ex);
        }
    }

    private static Seq<UnlockableContent> cached;
    private static int cachedWorld = -1;

    private static void rebuild(Table panel) {
        if (!enabled || Vars.state == null || !Vars.state.isGame()) {
            panel.clear();
            return;
        }
        int wid = Vars.world == null ? 0 : Vars.world.width();
        if (cached == null || cachedWorld != wid) {
            cached = collectNaturalResources();
            cachedWorld = wid;
            panel.clear();
            panel.add("[gold]Find[]").pad(2f);
            panel.row();
            for (UnlockableContent uc : cached) {
                var btn = panel.button(t -> t.image(uc.uiIcon).size(28f), () -> onClick(uc))
                        .size(40f).pad(2f).get();
                btn.update(() -> btn.setColor(uc == selected ? Color.lime : Color.white));
                panel.row();
            }
        }
    }

    private static void onClick(UnlockableContent uc) {
        if (selected == uc) { selected = null; hasTarget = false; return; }
        selected = uc;
        hasTarget = false;
        Tile patch = findNearestPatch(uc);
        if (patch != null) {
            targetX = patch.worldx();
            targetY = patch.worldy();
            hasTarget = true;
        } else {
            if (Vars.ui != null) Vars.ui.showInfoToast("[red]No patch ≥4x4 found for[] " + uc.localizedName, 2f);
        }
    }

    private static Tile findNearestPatch(UnlockableContent uc) {
        if (Vars.player == null) return null;
        CoreBuild core = Vars.player.core();
        if (core == null) return null;
        if (Vars.world == null || Vars.world.tiles == null) return null;

        int w = Vars.world.width();
        int h = Vars.world.height();
        boolean[] visited = new boolean[w * h];
        Tile bestTile = null;
        float bestDist = Float.MAX_VALUE;

        int[] queue = new int[w * h];
        for (int sy = 0; sy < h; sy++) {
            for (int sx = 0; sx < w; sx++) {
                int idx = sy * w + sx;
                if (visited[idx]) continue;
                Tile start = Vars.world.tile(sx, sy);
                if (start == null) continue;
                if (!matches(start, uc)) {
                    visited[idx] = true;
                    continue;
                }
                int qHead = 0, qTail = 0;
                queue[qTail++] = idx;
                visited[idx] = true;
                int sumX = 0, sumY = 0, count = 0;
                while (qHead < qTail) {
                    int cur = queue[qHead++];
                    int cx = cur % w, cy = cur / w;
                    sumX += cx; sumY += cy; count++;
                    int[][] d = {{1,0},{-1,0},{0,1},{0,-1}};
                    for (int[] dd : d) {
                        int nx = cx + dd[0], ny = cy + dd[1];
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int ni = ny * w + nx;
                        if (visited[ni]) continue;
                        Tile nt = Vars.world.tile(nx, ny);
                        if (nt == null || !matches(nt, uc)) {
                            visited[ni] = true;
                            continue;
                        }
                        visited[ni] = true;
                        queue[qTail++] = ni;
                    }
                }
                if (count >= MIN_PATCH) {
                    int cx = sumX / count, cy = sumY / count;
                    Tile center = Vars.world.tile(cx, cy);
                    if (center == null) continue;
                    float dx = center.worldx() - core.x;
                    float dy = center.worldy() - core.y;
                    float dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestTile = center;
                    }
                }
            }
        }
        return bestTile;
    }

    private static boolean matches(Tile t, UnlockableContent uc) {
        if (uc instanceof Item) {
            return t.drop() == uc;
        } else if (uc instanceof Liquid) {
            Floor f = t.floor();
            return f != null && f.liquidDrop == uc;
        }
        return false;
    }

    private static void draw() {
        if (!enabled || !hasTarget) return;
        if (Vars.player == null) return;
        CoreBuild core = Vars.player.core();
        if (core == null) return;

        float prevZ = Draw.z();
        Draw.z(Layer.overlayUI);
        Draw.color(Color.lime);
        Lines.stroke(2.5f);
        Lines.line(core.x, core.y, targetX, targetY);
        Lines.stroke(1f);
        Draw.color(Color.lime, 0.5f);
        Lines.circle(targetX, targetY, 14f);
        Draw.color();
        Draw.z(prevZ);
    }
}
