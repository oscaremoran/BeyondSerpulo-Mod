package xorinal;

import arc.util.Log;
import mindustry.game.Difficulty;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class Difficulties {
    private static Unsafe unsafe;

    public static void init() {
        try {
            Field uf = Unsafe.class.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            unsafe = (Unsafe) uf.get(null);
        } catch (Exception e) {
            Log.err("[Xorinal] Difficulties: failed to acquire Unsafe: " + e);
            return;
        }

        try {
            Difficulty[] vanilla = Difficulty.values();
            if (vanilla[vanilla.length - 1].name().equals("annihilation")) {
                Log.info("[Xorinal] Difficulties already injected, skipping");
                return;
            }

            Difficulty annihilation = make("annihilation", 5f, 3f, 0.4f);
            Difficulty[] extras = { annihilation };
            Difficulty[] merged = new Difficulty[vanilla.length + extras.length];
            System.arraycopy(vanilla, 0, merged, 0, vanilla.length);

            Field ordF = Enum.class.getDeclaredField("ordinal");
            long ordOff = unsafe.objectFieldOffset(ordF);
            for (int i = 0; i < extras.length; i++) {
                merged[vanilla.length + i] = extras[i];
                unsafe.putInt(extras[i], ordOff, vanilla.length + i);
            }

            Field valuesF = Difficulty.class.getDeclaredField("$VALUES");
            unsafe.putObject(unsafe.staticFieldBase(valuesF), unsafe.staticFieldOffset(valuesF), merged);

            Field allF = Difficulty.class.getDeclaredField("all");
            unsafe.putObject(unsafe.staticFieldBase(allF), unsafe.staticFieldOffset(allF), merged);

            Log.info("[Xorinal] Injected " + extras.length + " difficulties; total now = " + Difficulty.values().length);
        } catch (Exception e) {
            Log.err("[Xorinal] Difficulties.init failed: " + e);
        }
    }

    private static Difficulty make(String name, float health, float spawn, float waveTime) throws Exception {
        Difficulty d = (Difficulty) unsafe.allocateInstance(Difficulty.class);
        Field nameF = Enum.class.getDeclaredField("name");
        unsafe.putObject(d, unsafe.objectFieldOffset(nameF), name);
        Field h = Difficulty.class.getDeclaredField("enemyHealthMultiplier");
        Field s = Difficulty.class.getDeclaredField("enemySpawnMultiplier");
        Field w = Difficulty.class.getDeclaredField("waveTimeMultiplier");
        unsafe.putFloat(d, unsafe.objectFieldOffset(h), health);
        unsafe.putFloat(d, unsafe.objectFieldOffset(s), spawn);
        unsafe.putFloat(d, unsafe.objectFieldOffset(w), waveTime);
        return d;
    }
}
