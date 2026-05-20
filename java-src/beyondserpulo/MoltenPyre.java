package beyondserpulo;

import arc.struct.FloatSeq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.ctype.ContentType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.Category;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.world.Block;
import mindustry.world.meta.BuildVisibility;

public class MoltenPyre {
    public static final String BLOCK_NAME = "xorinal-molten-pyre";
    public static Block block;

    public static final float DURATION_SECS_PER_HP = 1f / 100f;

    public static void register() {
        try {
            block = new MoltenPyreBlock(BLOCK_NAME);
            Log.info("[BeyondSerpulo] registered block: " + BLOCK_NAME);
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] MoltenPyre.register: " + e);
        }
    }

    private static ItemStack tryStack(String item, int amount) {
        try {
            Object it = Vars.content.getByName(ContentType.item, item);
            if (it == null) it = Vars.content.getByName(ContentType.item, "xorinal-" + item);
            if (it instanceof Item) return new ItemStack((Item) it, amount);
            Log.info("[BeyondSerpulo] MoltenPyre: missing item " + item);
        } catch (Exception e) {}
        return null;
    }

    public static class MoltenPyreBlock extends Block {
        public MoltenPyreBlock(String name) {
            super(name);
            localizedName = "Molten Pyre";
            size = 2;
            health = 800;
            armor = 6;
            solid = false;
            update = true;
            destructible = true;
            rebuildable = true;
            hasPower = true;
            outputsPower = true;
            consumesPower = false;
            buildVisibility = BuildVisibility.shown;
            category = Category.power;
            alwaysUnlocked = true;
            buildType = MoltenPyreBuild::new;

            requirements(Category.power, new ItemStack[]{ new ItemStack(mindustry.content.Items.copper, 1) });
        }
    }

    public static void init() {
        try {
            if (block == null) return;
            ItemStack[] reqs = new ItemStack[]{
                tryStack("copper", 120),
                tryStack("graphite", 80),
                tryStack("silicon", 60),
                tryStack("surge-alloy", 25),
                tryStack("phase-fabric", 15),
                tryStack("mythril", 30)
            };
            int n = 0;
            for (ItemStack s : reqs) if (s != null) n++;
            ItemStack[] filtered = new ItemStack[n];
            int i = 0;
            for (ItemStack s : reqs) if (s != null) filtered[i++] = s;
            block.requirements = filtered;
            Log.info("[BeyondSerpulo] MoltenPyre init: " + filtered.length + " req stacks");
            for (ItemStack s : filtered) Log.info("[BeyondSerpulo]   req " + s.item.name + " x" + s.amount);
        } catch (Exception e) {
            Log.err("[BeyondSerpulo] MoltenPyre.init: " + e);
        }
    }

    public static class MoltenPyreBuild extends Building {
        public final FloatSeq rates = new FloatSeq();
        public final FloatSeq remains = new FloatSeq();
        public float currentRate = 0f;
        public float peakRate = 1f;

        @Override
        public void updateTile() {
            float halfWorld = (block.size * 8f) / 2f;
            float minX = x - halfWorld, maxX = x + halfWorld;
            float minY = y - halfWorld, maxY = y + halfWorld;

            for (Unit u : Groups.unit) {
                if (u == null || u.dead || !u.isValid()) continue;
                if (u.x < minX || u.x > maxX || u.y < minY || u.y > maxY) continue;
                float hp = Math.max(0f, u.maxHealth);
                if (hp <= 0f) continue;
                rates.add(hp);
                remains.add(hp * DURATION_SECS_PER_HP);
                try { Fx.burning.at(u.x, u.y); } catch (Exception e) {}
                try { Fx.fireSmoke.at(u.x, u.y); } catch (Exception e) {}
                u.kill();
            }

            float dt = Time.delta / 60f;
            float sum = 0f;
            for (int i = remains.size - 1; i >= 0; i--) {
                float left = remains.get(i) - dt;
                if (left <= 0f) {
                    remains.removeIndex(i);
                    rates.removeIndex(i);
                } else {
                    remains.set(i, left);
                    sum += rates.get(i);
                }
            }
            currentRate = sum;
            if (currentRate > peakRate) peakRate = currentRate;
        }

        @Override
        public float getPowerProduction() {
            return currentRate / 60f;
        }

        @Override
        public float ambientVolume() {
            return peakRate > 0f ? Math.min(1f, currentRate / peakRate) * 0.4f : 0f;
        }
    }
}
