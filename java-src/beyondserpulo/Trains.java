package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.IntSeq;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;

/**
 * Phase 1 MVP: straight rails + train depot + train station + 2-segment shuttle train.
 * Curves, HP/damage, save/load, configurable routing, and proper sprites come next.
 */
public class Trains {
    public static final String RAIL = "xorinal-rail";
    public static final String DEPOT = "xorinal-train-depot";
    public static final String STATION = "xorinal-train-station";
    public static final String ASSEMBLER = "xorinal-train-assembler";

    public static Block railBlock, depotBlock, stationBlock, assemblerBlock;
    public static final float ASSEMBLY_TIME = 8f * 60f; // 8 seconds at 60 tps
    public static final Seq<Train> trains = new Seq<>();

    public static final float TRAIN_SPEED = 1.2f; // world units per tick
    public static final int CAR_CAPACITY = 200;
    public static final float DOCK_WAIT_SECS = 3f;

    public static void registerContent() {
        try {
            railBlock = new RailBlock(RAIL);
            depotBlock = new DepotBlock(DEPOT);
            stationBlock = new StationBlock(STATION);
            assemblerBlock = new AssemblerBlock(ASSEMBLER);
            Log.info("[BeyondSerpulo] Trains: registered " + RAIL + ", " + DEPOT + ", " + STATION + ", " + ASSEMBLER);
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] Trains.registerContent: " + e);
        }
    }

    public static void init() {
        try {
            Events.run(EventType.Trigger.update, Trains::tickAll);
            Events.run(EventType.Trigger.draw, Trains::drawAll);
            Events.on(EventType.WorldLoadEvent.class, e -> trains.clear());
            Events.on(EventType.ResetEvent.class, e -> trains.clear());
            Log.info("[BeyondSerpulo] Trains: hooks installed");
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] Trains.init: " + e);
        }
    }

    // ----- Blocks -----

    public static class RailBlock extends Block {
        public RailBlock(String name) {
            super(name);
            localizedName = "Rail";
            description = "Electrified rail tile. Carries power along its length and lets trains travel between depots and stations. Underpowered segments slow any train crossing them to 25% speed. Lay continuous rails between a Depot and a Station to open a shuttle route.";
            size = 1;
            health = 80;
            solid = false;
            update = true;
            destructible = true;
            rebuildable = true;
            rotate = true;
            hasPower = true;
            consumesPower = true;
            conductivePower = true;
            buildVisibility = BuildVisibility.shown;
            category = Category.distribution;
            alwaysUnlocked = true;
            consumePower(0.1f);
            requirements(Category.distribution, new ItemStack[]{
                new ItemStack(Items.titanium, 4),
                new ItemStack(Items.silicon, 2),
                new ItemStack(Items.surgeAlloy, 1)
            });
        }
    }

    public static class DepotBlock extends Block {
        public DepotBlock(String name) {
            super(name);
            localizedName = "Train Depot";
            description = "Home base for a shuttle train. Holds up to 400 items in a buffer and serves as the train's loading endpoint — the train picks cargo up here and ferries it to a connected Train Station. Place adjacent to a rail tile, and adjacent to a Train Assembler if you don't yet have a train.";
            size = 4;
            health = 2400;
            solid = true;
            update = true;
            destructible = true;
            rebuildable = true;
            hasItems = true;
            itemCapacity = 400;
            hasPower = true;
            consumesPower = true;
            buildVisibility = BuildVisibility.shown;
            category = Category.distribution;
            alwaysUnlocked = true;
            consumePower(8f);
            buildType = DepotBuild::new;
            requirements(Category.distribution, new ItemStack[]{
                new ItemStack(Items.copper, 400),
                new ItemStack(Items.graphite, 250),
                new ItemStack(Items.silicon, 200),
                new ItemStack(Items.titanium, 150),
                new ItemStack(Items.thorium, 120),
                new ItemStack(Items.surgeAlloy, 80),
                new ItemStack(Items.phaseFabric, 60)
            });
        }

        @Override
        public boolean canBreak(Tile tile) { return true; }
    }

    public static class StationBlock extends Block {
        public StationBlock(String name) {
            super(name);
            localizedName = "Train Station";
            description = "Drop-off terminal for arriving trains. Buffers up to 300 items received from incoming cargo cars. Place adjacent to a rail tile connected to a Depot; the train auto-finds the nearest reachable station via the rail network.";
            size = 3;
            health = 1500;
            solid = true;
            update = true;
            destructible = true;
            rebuildable = true;
            hasItems = true;
            itemCapacity = 300;
            hasPower = false;
            buildVisibility = BuildVisibility.shown;
            category = Category.distribution;
            alwaysUnlocked = true;
            buildType = StationBuild::new;
            requirements(Category.distribution, new ItemStack[]{
                new ItemStack(Items.copper, 200),
                new ItemStack(Items.graphite, 120),
                new ItemStack(Items.silicon, 100),
                new ItemStack(Items.titanium, 80),
                new ItemStack(Items.surgeAlloy, 30)
            });
        }
    }

    // ----- Buildings -----

    // Resources consumed from the depot inventory to assemble one train (locomotive + 1 cargo car).
    public static final ItemStack[] TRAIN_BUILD_COST = new ItemStack[]{
        new ItemStack(Items.titanium, 60),
        new ItemStack(Items.silicon, 40),
        new ItemStack(Items.surgeAlloy, 20),
        new ItemStack(Items.phaseFabric, 10)
    };

    public static class DepotBuild extends Building {
        @Override
        public boolean acceptItem(Building source, Item item) {
            return items != null && items.get(item) < block.itemCapacity;
        }
        // No custom draw — uses the block's PNG. No assembly logic — handled by AssemblerBuild.
    }

    public static class AssemblerBlock extends Block {
        public AssemblerBlock(String name) {
            super(name);
            localizedName = "Train Assembler";
            description = "Factory that constructs trains for an adjacent Train Depot. Only accepts items required for assembly (titanium, silicon, surge-alloy, phase-fabric). Pays the full build cost up front, then spends 8 seconds assembling a locomotive + cargo car under its twin gantry cranes. When the train is complete it launches onto the depot's rail; if a train is already in service, the assembler holds at full progress and waits.";
            size = 4;
            health = 2200;
            solid = true;
            update = true;
            destructible = true;
            rebuildable = true;
            hasItems = true;
            itemCapacity = 200;
            hasPower = true;
            consumesPower = true;
            buildVisibility = BuildVisibility.shown;
            category = Category.distribution;
            alwaysUnlocked = true;
            consumePower(12f);
            buildType = AssemblerBuild::new;
            requirements(Category.distribution, new ItemStack[]{
                new ItemStack(Items.copper, 300),
                new ItemStack(Items.graphite, 200),
                new ItemStack(Items.silicon, 160),
                new ItemStack(Items.titanium, 120),
                new ItemStack(Items.thorium, 80),
                new ItemStack(Items.surgeAlloy, 50)
            });
        }

        @Override
        public void setStats() {
            super.setStats();
            try {
                stats.add(Stat.output, StatValues.items(ASSEMBLY_TIME, TRAIN_BUILD_COST));
                stats.add(Stat.productionTime, ASSEMBLY_TIME / 60f, StatUnit.seconds);
            } catch (Exception e) {
                arc.util.Log.err("[BeyondSerpulo] Assembler.setStats: " + e);
            }
        }
    }

    public static class AssemblerBuild extends Building {
        // Only primitives stored on the Building — Mindustry serializes these safely.
        public float progress = 0f;     // 0..1, how built the train is
        public boolean costPaid = false; // build cost already consumed for the current train
        public boolean lastMissing = false;

        @Override
        public boolean acceptItem(Building source, Item item) {
            if (items == null) return false;
            // only accept items that are part of the build cost
            for (ItemStack s : TRAIN_BUILD_COST) if (s.item == item) {
                return items.get(item) < block.itemCapacity;
            }
            return false;
        }

        @Override
        public void updateTile() {
            DepotBuild depot = findAdjacentDepot();
            if (depot == null) { progress = 0f; return; }
            // if the linked depot already owns a train, hold construction at full and wait
            if (depotOwnsTrain(depot)) return;
            if (power == null || power.status < 0.5f) return;

            // pay the build cost once per train
            if (!costPaid) {
                if (!hasBuildCost()) {
                    if (!lastMissing) Log.info("[BeyondSerpulo] Assembler waiting on materials");
                    lastMissing = true;
                    return;
                }
                lastMissing = false;
                consumeBuildCost();
                costPaid = true;
                progress = 0f;
            }

            progress += Time.delta / ASSEMBLY_TIME;
            if (progress >= 1f) {
                progress = 1f;
                if (launchTrainFor(depot)) {
                    progress = 0f;
                    costPaid = false;
                }
            }
        }

        private boolean hasBuildCost() {
            if (items == null) return false;
            for (ItemStack s : TRAIN_BUILD_COST) if (items.get(s.item) < s.amount) return false;
            return true;
        }

        private void consumeBuildCost() {
            for (ItemStack s : TRAIN_BUILD_COST) items.remove(s.item, s.amount);
        }

        private DepotBuild findAdjacentDepot() {
            // scan ring of tiles around the 4x4 building
            int x0 = tileX() - 2, y0 = tileY() - 2, x1 = tileX() + 2, y1 = tileY() + 2;
            for (int xi = x0 - 1; xi <= x1 + 1; xi++) {
                for (int yi = y0 - 1; yi <= y1 + 1; yi++) {
                    if (xi > x0 - 1 && xi <= x1 && yi > y0 - 1 && yi <= y1) continue;
                    Tile t = Vars.world.tile(xi, yi);
                    if (t != null && t.build instanceof DepotBuild d) return d;
                }
            }
            return null;
        }

        @Override
        public void draw() {
            super.draw(); // draws block sprite first
            // overlay: locomotive silhouette growing as progress increases
            if (progress > 0f) {
                Draw.z(35f);
                float w = Mathf.lerp(2f, 14f, progress);
                float h = Mathf.lerp(1f, 7f, progress);
                Draw.color(0.92f, 0.18f, 0.92f, 0.5f + 0.5f * progress);
                Fill.rect(x, y, w, h);
                if (progress > 0.7f) {
                    Draw.color(0.2f, 1f, 1f, 1f);
                    Fill.circle(x + w * 0.45f, y, 1.4f);
                }
                // progress bar above
                Draw.color(0.1f, 0.1f, 0.15f, 0.85f);
                Fill.rect(x, y - block.size * 4f - 3f, block.size * 8f * 0.7f, 2.4f);
                Draw.color(0.4f, 1f, 0.6f, 1f);
                float bw = block.size * 8f * 0.7f * progress;
                Fill.rect(x - block.size * 8f * 0.7f / 2f + bw / 2f, y - block.size * 4f - 3f, bw, 2.4f);
                Draw.reset();
            }
        }
    }

    private static boolean depotOwnsTrain(DepotBuild depot) {
        for (Train t : trains) if (t.depot == depot) return true;
        return false;
    }

    private static boolean launchTrainFor(DepotBuild depot) {
        if (depot == null) return false;
        Tile railStart = adjacentRail(depot);
        if (railStart == null) return false;
        StationBuild target = findReachableStation(railStart);
        if (target == null) return false;
        Tile railEnd = adjacentRail(target);
        if (railEnd == null) return false;
        Seq<Tile> path = bfsRail(railStart, railEnd);
        if (path == null || path.size < 2) return false;
        Train t = new Train();
        t.depot = depot;
        t.station = target;
        t.outboundPath = path;
        t.returnPath = path.copy();
        t.returnPath.reverse();
        t.currentPath = t.outboundPath;
        t.x = railStart.worldx();
        t.y = railStart.worldy();
        t.seg = 0; t.segT = 0f;
        t.state = TrainState.OUTBOUND;
        trains.add(t);
        Log.info("[BeyondSerpulo] Train launched from assembler -> depot, " + path.size + " rail tiles");
        return true;
    }

    public static class StationBuild extends Building {
        @Override
        public boolean acceptItem(Building source, Item item) {
            return items != null && items.get(item) < block.itemCapacity;
        }
    }

    // ----- Train entity -----

    public enum TrainState { OUTBOUND, DOCKING_STATION, RETURNING, DOCKING_DEPOT }

    public static class Train {
        public DepotBuild depot;
        public StationBuild station;
        public Seq<Tile> outboundPath, returnPath, currentPath;
        public int seg;       // index of current segment start tile
        public float segT;    // 0..1 progress along segment
        public float x, y;    // world position of locomotive
        public float angle;   // facing
        public TrainState state = TrainState.OUTBOUND;
        public float dockTimer = 0f;
        public int cargoAmount = 0;
        public Item cargoItem = null;
    }

    private static void tickAll() {
        if (Vars.state == null || !Vars.state.isGame() || Vars.state.isPaused()) return;
        for (int idx = trains.size - 1; idx >= 0; idx--) {
            Train t = trains.get(idx);
            // remove if depot or station was destroyed
            if (t.depot == null || !t.depot.isValid() || t.station == null || !t.station.isValid()) {
                trains.remove(idx);
                continue;
            }
            switch (t.state) {
                case OUTBOUND, RETURNING -> advance(t);
                case DOCKING_STATION -> {
                    t.dockTimer += Time.delta / 60f;
                    if (t.dockTimer >= DOCK_WAIT_SECS) {
                        dumpAtStation(t);
                        t.state = TrainState.RETURNING;
                        t.currentPath = t.returnPath;
                        t.seg = 0; t.segT = 0; t.dockTimer = 0;
                    }
                }
                case DOCKING_DEPOT -> {
                    t.dockTimer += Time.delta / 60f;
                    if (t.dockTimer >= DOCK_WAIT_SECS) {
                        loadFromDepot(t);
                        t.state = TrainState.OUTBOUND;
                        t.currentPath = t.outboundPath;
                        t.seg = 0; t.segT = 0; t.dockTimer = 0;
                    }
                }
            }
        }
    }

    private static void advance(Train t) {
        if (t.currentPath == null || t.currentPath.size < 2) return;
        if (t.seg >= t.currentPath.size - 1) {
            // reached end of path
            if (t.state == TrainState.OUTBOUND) t.state = TrainState.DOCKING_STATION;
            else t.state = TrainState.DOCKING_DEPOT;
            t.dockTimer = 0;
            return;
        }
        Tile a = t.currentPath.get(t.seg);
        Tile b = t.currentPath.get(t.seg + 1);
        float ax = a.worldx(), ay = a.worldy();
        float bx = b.worldx(), by = b.worldy();
        float segLen = Mathf.dst(ax, ay, bx, by);
        if (segLen < 0.001f) { t.seg++; return; }
        // slow if either rail tile is underpowered
        float speedMul = railPowerMul(a) * railPowerMul(b);
        t.segT += (TRAIN_SPEED * Time.delta * speedMul) / segLen;
        while (t.segT >= 1f && t.seg < t.currentPath.size - 1) {
            t.segT -= 1f;
            t.seg++;
            if (t.seg >= t.currentPath.size - 1) {
                t.segT = 0f;
                break;
            }
            a = t.currentPath.get(t.seg);
            b = t.currentPath.get(t.seg + 1);
            ax = a.worldx(); ay = a.worldy(); bx = b.worldx(); by = b.worldy();
        }
        t.x = Mathf.lerp(ax, bx, t.segT);
        t.y = Mathf.lerp(ay, by, t.segT);
        t.angle = Mathf.angle(bx - ax, by - ay);
    }

    private static float railPowerMul(Tile tile) {
        if (tile == null || tile.build == null) return 1f;
        if (!(tile.build.block instanceof RailBlock)) return 1f;
        if (tile.build.power == null) return 0.25f;
        return tile.build.power.status > 0.5f ? 1f : 0.25f;
    }

    private static void dumpAtStation(Train t) {
        if (t.cargoAmount > 0 && t.cargoItem != null && t.station != null && t.station.items != null) {
            int can = Math.min(t.cargoAmount, t.station.block.itemCapacity - t.station.items.get(t.cargoItem));
            if (can > 0) {
                t.station.items.add(t.cargoItem, can);
                t.cargoAmount -= can;
            }
        }
    }

    private static void loadFromDepot(Train t) {
        if (t.depot == null || t.depot.items == null) return;
        // load up to CAR_CAPACITY of whatever the depot has the most of
        Item best = null; int bestAmt = 0;
        for (Item it : Vars.content.items()) {
            int got = t.depot.items.get(it);
            if (got > bestAmt) { bestAmt = got; best = it; }
        }
        if (best == null || bestAmt <= 0) return;
        int take = Math.min(CAR_CAPACITY, bestAmt);
        t.depot.items.remove(best, take);
        t.cargoItem = best;
        t.cargoAmount = take;
    }

    // ----- Rendering -----

    private static void drawAll() {
        if (trains.isEmpty()) return;
        for (Train t : trains) drawTrain(t);
    }

    private static void drawTrain(Train t) {
        Draw.z(70f);
        // locomotive
        float ang = t.angle;
        Draw.color(0.92f, 0.18f, 0.92f, 1f); // magenta
        Fill.rect(t.x, t.y, 10f, 6f, ang);
        Draw.color(0.2f, 1f, 1f, 1f); // cyan headlight
        float hx = t.x + Mathf.cosDeg(ang) * 5f;
        float hy = t.y + Mathf.sinDeg(ang) * 5f;
        Fill.circle(hx, hy, 1.5f);
        // cargo car: 8 units behind the locomotive
        float cx = t.x - Mathf.cosDeg(ang) * 8f;
        float cy = t.y - Mathf.sinDeg(ang) * 8f;
        Draw.color(0.35f, 0.3f, 0.55f, 1f);
        Fill.rect(cx, cy, 8f, 5f, ang);
        if (t.cargoAmount > 0 && t.cargoItem != null) {
            Draw.color(t.cargoItem.color);
            Fill.rect(cx, cy, 4f, 3f, ang);
        }
        Draw.reset();
    }

    // ----- Rail graph -----

    private static Tile adjacentRail(Building b) {
        if (b == null || b.tile == null) return null;
        int half = b.block.size / 2;
        // scan a ring around the building
        int x0 = b.tileX() - half - (b.block.size % 2 == 0 ? 1 : 0);
        int y0 = b.tileY() - half - (b.block.size % 2 == 0 ? 1 : 0);
        int x1 = b.tileX() + half + 1;
        int y1 = b.tileY() + half + 1;
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int y = y0 - 1; y <= y1 + 1; y++) {
                // only ring tiles, not inside
                if (x > x0 - 1 && x <= x1 && y > y0 - 1 && y <= y1) continue;
                Tile t = Vars.world.tile(x, y);
                if (t != null && t.build != null && t.build.block instanceof RailBlock) return t;
            }
        }
        return null;
    }

    private static StationBuild findReachableStation(Tile start) {
        // BFS over rail tiles; whenever a rail tile sits next to a station, return that station
        if (start == null) return null;
        IntSeq visited = new IntSeq();
        Queue<Tile> q = new Queue<>();
        q.addLast(start);
        visited.add(start.pos());
        int safety = 4000;
        while (!q.isEmpty() && safety-- > 0) {
            Tile cur = q.removeFirst();
            StationBuild s = stationTouching(cur);
            if (s != null) return s;
            for (Tile n : neighbors(cur)) {
                if (n != null && n.build != null && n.build.block instanceof RailBlock
                        && !visited.contains(n.pos())) {
                    visited.add(n.pos());
                    q.addLast(n);
                }
            }
        }
        return null;
    }

    private static StationBuild stationTouching(Tile rail) {
        for (Tile n : neighbors(rail)) {
            if (n != null && n.build instanceof StationBuild s) return s;
        }
        return null;
    }

    private static Seq<Tile> bfsRail(Tile start, Tile end) {
        if (start == null || end == null) return null;
        IntSeq parent = new IntSeq();
        IntSeq nodes = new IntSeq();
        Queue<Tile> q = new Queue<>();
        q.addLast(start);
        nodes.add(start.pos()); parent.add(-1);
        int safety = 8000;
        boolean found = false;
        while (!q.isEmpty() && safety-- > 0) {
            Tile cur = q.removeFirst();
            if (cur == end) { found = true; break; }
            for (Tile n : neighbors(cur)) {
                if (n == null) continue;
                if (n != end && (n.build == null || !(n.build.block instanceof RailBlock))) continue;
                if (nodes.contains(n.pos())) continue;
                nodes.add(n.pos());
                parent.add(cur.pos());
                q.addLast(n);
            }
        }
        if (!found) return null;
        Seq<Tile> path = new Seq<>();
        int cur = end.pos();
        while (cur != -1) {
            int idx = nodes.indexOf(cur);
            Tile t = Vars.world.tile(cur);
            if (t != null) path.add(t);
            if (idx < 0) break;
            cur = parent.get(idx);
        }
        path.reverse();
        return path;
    }

    private static Tile[] neighbors(Tile t) {
        if (t == null) return new Tile[0];
        return new Tile[]{
            Vars.world.tile(t.x + 1, t.y),
            Vars.world.tile(t.x - 1, t.y),
            Vars.world.tile(t.x, t.y + 1),
            Vars.world.tile(t.x, t.y - 1)
        };
    }
}
