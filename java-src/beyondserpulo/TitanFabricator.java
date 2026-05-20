package beyondserpulo;

import arc.struct.Seq;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.blocks.units.UnitFactory;
import mindustry.world.meta.BuildVisibility;

public class TitanFabricator {
    public static final String BLOCK_NAME = "xorinal-titan-fabricator";
    public static Block block;

    public static void register() {
        try {
            UnitFactory f = new UnitFactory(BLOCK_NAME);
            f.localizedName = "Titan Fabricator";
            f.size = 10;
            f.health = 12000;
            f.armor = 12;
            f.itemCapacity = 200;
            f.buildVisibility = BuildVisibility.shown;
            f.category = Category.units;
            f.alwaysUnlocked = true;
            try { f.consumePower(50f); } catch (Exception e) { Log.err("[BeyondSerpulo] Titan consumePower: " + e); }
            f.requirements(Category.units, new ItemStack[]{ new ItemStack(mindustry.content.Items.copper, 1) });
            block = f;
            Log.info("[BeyondSerpulo] registered block: " + BLOCK_NAME + " (plans pending init)");
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] TitanFabricator.register: " + e);
        }
    }

    public static void init() {
        try {
            if (!(block instanceof UnitFactory)) return;
            UnitFactory f = (UnitFactory) block;

            Seq<UnitFactory.UnitPlan> plans = new Seq<>();
            addPlan(plans, "nemesis", 60f * 180f,
                stack("copper", 1200), stack("titanium", 900), stack("thorium", 700),
                stack("plastanium", 400), stack("surge-alloy", 400), stack("phase-fabric", 300),
                stack("steel", 500), stack("mythril", 250));
            addPlan(plans, "celestial", 60f * 240f,
                stack("titanium", 1100), stack("thorium", 900), stack("plastanium", 500),
                stack("surge-alloy", 500), stack("phase-fabric", 400),
                stack("steel", 700), stack("mythril", 350));
            addPlan(plans, "abyssal", 60f * 240f,
                stack("titanium", 1000), stack("thorium", 800), stack("plastanium", 500),
                stack("surge-alloy", 600), stack("phase-fabric", 400),
                stack("steel", 700), stack("mythril", 350));
            addPlan(plans, "ragnarok", 60f * 360f,
                stack("titanium", 1500), stack("thorium", 1200), stack("plastanium", 700),
                stack("surge-alloy", 800), stack("phase-fabric", 600),
                stack("steel", 1000), stack("mythril", 500));
            f.plans = plans;

            ItemStack[] reqs = filteredStacks(
                stack("copper", 1500),
                stack("titanium", 1200),
                stack("thorium", 900),
                stack("plastanium", 700),
                stack("surge-alloy", 700),
                stack("phase-fabric", 500),
                stack("steel", 900),
                stack("mythril", 800),
                stack("quantum-alloy", 400)
            );
            f.requirements = reqs;
            Log.info("[BeyondSerpulo] Titan init: " + plans.size + " plans, " + reqs.length + " req stacks");
            for (ItemStack s : reqs) Log.info("[BeyondSerpulo]   req " + s.item.name + " x" + s.amount);
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] TitanFabricator.init: " + e);
        }
    }

    private static void addPlan(Seq<UnitFactory.UnitPlan> plans, String unitName, float time, ItemStack... stacks) {
        try {
            UnitType u = (UnitType) Vars.content.getByName(ContentType.unit, unitName);
            if (u == null) u = (UnitType) Vars.content.getByName(ContentType.unit, "xorinal-" + unitName);
            if (u == null) { Log.info("[BeyondSerpulo] Titan: missing unit " + unitName); return; }
            plans.add(new UnitFactory.UnitPlan(u, time, filteredStacks(stacks)));
        } catch (Exception e) { Log.err("[BeyondSerpulo] Titan addPlan: " + e); }
    }

    private static ItemStack stack(String item, int amount) {
        try {
            Object it = Vars.content.getByName(ContentType.item, item);
            if (it == null) it = Vars.content.getByName(ContentType.item, "xorinal-" + item);
            if (it instanceof Item) return new ItemStack((Item) it, amount);
            Log.info("[BeyondSerpulo] Titan: missing item " + item);
        } catch (Exception e) {}
        return null;
    }

    private static ItemStack[] filteredStacks(ItemStack... stacks) {
        int n = 0;
        for (ItemStack s : stacks) if (s != null) n++;
        ItemStack[] out = new ItemStack[n];
        int i = 0;
        for (ItemStack s : stacks) if (s != null) out[i++] = s;
        if (out.length == 0) return new ItemStack[]{ stack("copper", 1) };
        return out;
    }
}
