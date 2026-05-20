package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.math.geom.Vec2;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.ctype.ContentType;
import mindustry.entities.units.AIController;
import mindustry.entities.units.UnitController;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Tex;
import mindustry.gen.Unit;
import mindustry.gen.Teamc;
import mindustry.type.Category;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

public class BossProgrammer {
    public static final String BLOCK_NAME = "boss-programmer";
    public static Block block;

    public static BossBuild activeBoss() {
        for (Building b : Groups.build) {
            if (b instanceof BossBuild) {
                BossBuild bb = (BossBuild) b;
                if (bb.findBoss() != null) return bb;
            }
        }
        return null;
    }

    public static void init() {
        Events.on(WorldLoadEvent.class, e -> Core.app.post(BossProgrammer::autoSpawnAll));
    }

    private static void autoSpawnAll() {
        try {
            if (Vars.state == null || !Vars.state.isGame()) return;
            if (Vars.state.rules != null && Vars.state.rules.editor) return;
            if (Vars.net != null && Vars.net.client()) return;
            for (Building b : Groups.build) {
                if (b instanceof BossBuild) {
                    BossBuild bb = (BossBuild) b;
                    if (bb.findBoss() == null && bb.bossUnitName != null && !bb.bossUnitName.isEmpty()) {
                        bb.spawnBoss(bb.bossUnitName);
                    }
                }
            }
        } catch (Exception ex) { Log.err("[BeyondSerpulo] BossProgrammer.autoSpawnAll: " + ex); }
    }

    public static void register() {
        try {
            block = new BossBlock(BLOCK_NAME);
            Log.info("[BeyondSerpulo] registered block: " + BLOCK_NAME);
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] BossProgrammer.register: " + e);
        }
    }

    public static class BossBlock extends Block {
        public BossBlock(String name) {
            super(name);
            size = 3;
            solid = true;
            update = true;
            hasItems = false;
            configurable = true;
            saveConfig = true;
            destructible = true;
            rebuildable = true;
            alwaysUnlocked = true;
            buildVisibility = BuildVisibility.editorOnly;
            category = Category.effect;
            buildType = BossBuild::new;
            config(String.class, (BossBuild b, String s) -> b.setProgramFromString(s));
            ItemStack[] reqs = new ItemStack[]{
                tryStack("quantum-alloy", 25),
                tryStack("mythril", 100)
            };
            int present = 0;
            for (ItemStack s : reqs) if (s != null) present++;
            ItemStack[] filtered = new ItemStack[present];
            int idx = 0;
            for (ItemStack s : reqs) if (s != null) filtered[idx++] = s;
            if (filtered.length == 0) filtered = new ItemStack[]{ new ItemStack(Items.thorium, 100) };
            requirements(Category.effect, filtered);
        }

        private static ItemStack tryStack(String item, int amount) {
            try {
                Object it = Vars.content.getByName(ContentType.item, item);
                if (it instanceof mindustry.type.Item) return new ItemStack((mindustry.type.Item) it, amount);
            } catch (Exception e) {}
            return null;
        }
    }

    public static class BossBuild extends Building {
        public BossProgram program = new BossProgram();
        public String bossUnitName = "corvus";
        public int bossId = -1;

        int pc;
        float waitTimer;
        float fireTimer;
        boolean firing;
        Vec2 firePoint = new Vec2();
        boolean spawnedThisStep;
        Teamc lockedTarget;

        public void setProgramFromString(String s) {
            program = BossProgram.fromString(s);
            pc = 0; waitTimer = 0f; fireTimer = 0f; firing = false; spawnedThisStep = false;
        }

        public Unit findBoss() {
            if (bossId < 0) return null;
            Unit u = Groups.unit.getByID(bossId);
            if (u == null) return null;
            if (u.dead) { bossId = -1; return null; }
            if (!u.isValid()) return null;
            return u;
        }

        @Override
        public void updateTile() {
            if (Vars.net != null && Vars.net.client()) return;
            Unit boss = findBoss();
            if (boss == null) {
                pc = 0; waitTimer = 0f; firing = false; spawnedThisStep = false;
                return;
            }
            if (program.stmts.isEmpty()) { firing = false; return; }
            if (pc >= program.stmts.size) pc = 0;

            BossProgram.Stmt st = program.stmts.get(pc);
            float dt = Time.delta / 60f;
            switch (st.op) {
                case MoveTo: {
                    float dx = boss.x - st.a, dy = boss.y - st.b;
                    if (dx * dx + dy * dy <= 12f * 12f) advance();
                    break;
                }
                case Charge: {
                    float dx = boss.x - st.a, dy = boss.y - st.b;
                    if (dx * dx + dy * dy <= 18f * 18f) advance();
                    break;
                }
                case Wait: {
                    waitTimer += dt;
                    if (waitTimer >= st.a) { waitTimer = 0f; advance(); }
                    break;
                }
                case AttackNearest: {
                    lockedTarget = findEnemy(boss);
                    advance();
                    break;
                }
                case ClearTarget: {
                    lockedTarget = null;
                    advance();
                    break;
                }
                case SpawnAdd: {
                    if (!spawnedThisStep) {
                        spawnAdds(boss, st.s, (int) st.b);
                        spawnedThisStep = true;
                    }
                    advance();
                    break;
                }
                case FireAt: {
                    firing = true;
                    firePoint.set(st.a, st.b);
                    fireTimer += dt;
                    if (fireTimer >= st.c) { fireTimer = 0f; firing = false; advance(); }
                    break;
                }
                case WaitPhase: {
                    if (boss.maxHealth > 0f && boss.health / boss.maxHealth <= st.a) advance();
                    break;
                }
                case Repeat: {
                    pc = 0;
                    spawnedThisStep = false;
                    break;
                }
            }

            UnitController c = boss.controller();
            if (!(c instanceof BossController) || ((BossController) c).owner != this) {
                boss.controller(new BossController(this));
            }
        }

        void advance() {
            pc++;
            waitTimer = 0f;
            fireTimer = 0f;
            firing = false;
            spawnedThisStep = false;
            if (pc >= program.stmts.size) pc = 0;
        }

        Teamc findEnemy(Unit boss) {
            Teamc best = null;
            float bestD = Float.MAX_VALUE;
            for (Unit u : Groups.unit) {
                if (u == boss || u.dead || u.team == boss.team) continue;
                float d = u.dst2(boss);
                if (d < bestD) { bestD = d; best = u; }
            }
            if (best != null) return best;
            for (Building b : Groups.build) {
                if (b.team == boss.team || b.dead()) continue;
                float d = b.dst2(boss);
                if (d < bestD) { bestD = d; best = b; }
            }
            return best;
        }

        void spawnAdds(Unit boss, String typeName, int count) {
            try {
                UnitType t = (UnitType) Vars.content.getByName(ContentType.unit, typeName);
                if (t == null) return;
                for (int i = 0; i < count; i++) {
                    Unit u = t.create(boss.team);
                    float ang = (360f / count) * i;
                    u.set(boss.x + arc.math.Mathf.cosDeg(ang) * 32f, boss.y + arc.math.Mathf.sinDeg(ang) * 32f);
                    u.rotation = ang;
                    u.add();
                }
            } catch (Exception e) { Log.err("[BeyondSerpulo] spawnAdds: " + e); }
        }

        public BossProgram.Stmt currentStmt() {
            if (program.stmts.isEmpty() || pc < 0 || pc >= program.stmts.size) return null;
            return program.stmts.get(pc);
        }

        public boolean isFiring() { return firing; }
        public Vec2 firePoint() { return firePoint; }
        public Teamc lockedTarget() { return lockedTarget; }

        public boolean spawnBoss(String unitName) {
            try {
                UnitType t = (UnitType) Vars.content.getByName(ContentType.unit, unitName);
                if (t == null) {
                    Vars.ui.showInfoToast("[red]Unknown unit: " + unitName + "[]", 3f);
                    return false;
                }
                Unit u = t.create(team);
                u.set(x, y);
                u.health = u.maxHealth;
                u.add();
                bossId = u.id;
                bossUnitName = unitName;
                pc = 0; waitTimer = 0f; fireTimer = 0f; firing = false; spawnedThisStep = false;
                u.controller(new BossController(this));
                Vars.ui.showInfoToast("[#7fffd0]Boss deployed: []" + t.localizedName, 2f);
                return true;
            } catch (Exception e) {
                Log.err("[BeyondSerpulo] spawnBoss: " + e);
                return false;
            }
        }

        @Override
        public Object config() { return program.encode(); }

        @Override
        public void write(Writes w) {
            super.write(w);
            String src = program.encode();
            w.str(src == null ? "" : src);
            w.str(bossUnitName == null ? "" : bossUnitName);
            w.i(bossId);
        }

        @Override
        public void read(Reads r, byte revision) {
            super.read(r, revision);
            try {
                String src = r.str();
                program = BossProgram.fromString(src);
                bossUnitName = r.str();
                bossId = r.i();
            } catch (Exception e) { Log.err("[BeyondSerpulo] read: " + e); }
        }

        @Override
        public void buildConfiguration(Table table) {
            table.button("Edit Program", Styles.cleart, this::openEditor).size(170f, 44f);
        }

        void openEditor() {
            BaseDialog d = new BaseDialog("[#7fffd0]Boss Programmer[]");
            Table panel = new Table();
            panel.defaults().left().pad(4f);

            Table top = new Table();
            top.defaults().pad(2f);
            top.add("[lightgray]Boss:[]").padRight(6f);
            TextField unitField = top.field(bossUnitName, s -> bossUnitName = s.trim()).width(180f).get();
            top.button("Test Spawn", () -> spawnBoss(unitField.getText().trim())).size(120f, 40f).padLeft(8f);
            top.button("Despawn", () -> {
                Unit u = findBoss();
                if (u != null) u.kill();
                bossId = -1;
            }).size(96f, 40f).padLeft(4f);
            panel.add(top).left();
            panel.row();

            panel.add("[lightgray]Boss spawns automatically when the map starts. Hints: nemesis, corvus, omega, eclipse, toxopid[]").left().padBottom(4f);
            panel.row();

            Table list = new Table();
            list.top();
            Runnable[] rebuild = new Runnable[1];
            rebuild[0] = () -> rebuildList(list, rebuild[0]);
            rebuild[0].run();

            ScrollPane scroll = new ScrollPane(list);
            scroll.setFadeScrollBars(false);
            panel.add(scroll).width(580f).height(320f);
            panel.row();

            Table addBar = new Table();
            addBar.defaults().pad(2f).height(32f);
            addBar.add("[lightgray]Add:[]").padRight(4f).colspan(4).left();
            addBar.row();
            int colIdx = 0;
            int colsPerRow = 3;
            for (BossProgram.Op op : BossProgram.Op.values()) {
                addBar.button(op.label, Styles.cleart, () -> {
                    program.stmts.add(new BossProgram.Stmt(op));
                    pushConfig();
                    rebuild[0].run();
                }).width(180f).height(32f);
                colIdx++;
                if (colIdx % colsPerRow == 0) addBar.row();
            }
            panel.add(addBar).left();

            d.cont.add(panel).pad(8f);
            d.addCloseButton();
            d.show();
        }

        void rebuildList(Table list, Runnable rebuild) {
            list.clear();
            list.defaults().growX().pad(2f);
            for (int i = 0; i < program.stmts.size; i++) {
                int idx = i;
                BossProgram.Stmt st = program.stmts.get(i);
                Table row = new Table();
                try { row.background(Tex.pane); } catch (Exception ex) {}
                row.add("[accent]" + (i + 1) + ".[]").width(28f).padLeft(4f);
                row.add(st.describe()).left().growX().padLeft(6f);

                row.button("Edit", Styles.cleart, () -> editStmt(st, rebuild)).size(60f, 30f).padLeft(4f);
                row.button("Up", Styles.cleart, () -> {
                    if (idx > 0) {
                        program.stmts.swap(idx, idx - 1);
                        pushConfig();
                        rebuild.run();
                    }
                }).size(40f, 30f).padLeft(2f);
                row.button("Dn", Styles.cleart, () -> {
                    if (idx < program.stmts.size - 1) {
                        program.stmts.swap(idx, idx + 1);
                        pushConfig();
                        rebuild.run();
                    }
                }).size(40f, 30f).padLeft(2f);
                row.button("X", Styles.cleart, () -> {
                    program.stmts.remove(idx);
                    pushConfig();
                    rebuild.run();
                }).size(40f, 30f).padLeft(2f);

                list.add(row).growX().row();
            }
            if (program.stmts.isEmpty()) {
                list.add("[lightgray](empty program — add statements below)[]").pad(20f);
            }
        }

        void editStmt(BossProgram.Stmt st, Runnable rebuild) {
            BaseDialog d = new BaseDialog("Edit " + st.op.label);
            Table t = d.cont;
            t.defaults().pad(4f);
            switch (st.op) {
                case MoveTo: case Charge:
                    floatField(t, "X (tile)", st.a / 8f, v -> st.a = v * 8f);
                    floatField(t, "Y (tile)", st.b / 8f, v -> st.b = v * 8f);
                    break;
                case Wait:
                    floatField(t, "Seconds", st.a, v -> st.a = v);
                    break;
                case SpawnAdd:
                    stringField(t, "Unit name", st.s, v -> st.s = v);
                    floatField(t, "Count", st.b, v -> st.b = Math.max(1f, v));
                    break;
                case FireAt:
                    floatField(t, "X (tile)", st.a / 8f, v -> st.a = v * 8f);
                    floatField(t, "Y (tile)", st.b / 8f, v -> st.b = v * 8f);
                    floatField(t, "Duration (sec)", st.c, v -> st.c = v);
                    break;
                case WaitPhase:
                    floatField(t, "HP Fraction (0-1)", st.a, v -> st.a = arc.math.Mathf.clamp(v));
                    break;
                default:
                    t.add("[lightgray]No parameters.[]");
                    break;
            }
            d.buttons.button("OK", () -> { pushConfig(); rebuild.run(); d.hide(); }).size(120f, 44f);
            d.buttons.button("Cancel", d::hide).size(120f, 44f);
            d.show();
        }

        void floatField(Table t, String label, float initial, arc.func.Floatc apply) {
            t.add(label).right().padRight(6f);
            t.field(Strings.autoFixed(initial, 2), s -> {
                try { apply.get(Float.parseFloat(s)); } catch (Exception e) {}
            }).width(180f).left();
            t.row();
        }

        void stringField(Table t, String label, String initial, arc.func.Cons<String> apply) {
            t.add(label).right().padRight(6f);
            t.field(initial == null ? "" : initial, s -> apply.get(s.trim())).width(180f).left();
            t.row();
        }

        void pushConfig() {
            try { configure(program.encode()); } catch (Exception e) {}
        }
    }

    public static class BossController extends AIController {
        public final BossBuild owner;
        public BossController(BossBuild owner) { this.owner = owner; }

        @Override
        public void updateUnit() {
            if (owner == null || owner.dead() || unit == null) { super.updateUnit(); return; }
            BossProgram.Stmt st = owner.currentStmt();
            if (st == null) return;

            switch (st.op) {
                case MoveTo:
                    moveTo(new Vec2(st.a, st.b), 4f);
                    unit.lookAt(st.a, st.b);
                    break;
                case Charge:
                    moveTo(new Vec2(st.a, st.b), 0f, 0.05f, true, null);
                    unit.lookAt(st.a, st.b);
                    break;
                case AttackNearest: {
                    Teamc tgt = owner.lockedTarget();
                    if (tgt != null) {
                        moveTo(tgt, unit.type.range * 0.8f);
                        unit.lookAt(tgt.x(), tgt.y());
                        unit.aimX = tgt.x(); unit.aimY = tgt.y();
                        unit.isShooting = true;
                    } else unit.isShooting = false;
                    break;
                }
                case FireAt:
                    if (owner.isFiring()) {
                        Vec2 fp = owner.firePoint();
                        unit.aimX = fp.x; unit.aimY = fp.y;
                        unit.lookAt(fp.x, fp.y);
                        unit.isShooting = true;
                    } else unit.isShooting = false;
                    break;
                case Wait: case WaitPhase: case ClearTarget: case SpawnAdd: case Repeat:
                    unit.isShooting = false;
                    break;
            }
        }

        @Override
        public boolean shouldShoot() { return unit != null && unit.isShooting; }
    }
}
