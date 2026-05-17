package beyondserpulo;

import arc.graphics.Color;
import arc.util.Log;
import mindustry.type.StatusEffect;

public class PilotEffects {
    public static StatusEffect recruit, veteran, elite;

    public static void register() {
        try {
            recruit = mk("xorinal-pilot-recruit", 1.10f, 1.10f, Color.valueOf("a0d8ff"));
            veteran = mk("xorinal-pilot-veteran", 1.20f, 1.20f, Color.valueOf("ffd166"));
            elite   = mk("xorinal-pilot-elite",   1.35f, 1.35f, Color.valueOf("ff5b8c"));
            Log.info("[BeyondSerpulo] PilotEffects registered: " + recruit.name + ", " + veteran.name + ", " + elite.name);
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] PilotEffects.register: " + ex);
        }
    }

    public static StatusEffect forTier(int tier) {
        if (tier >= 3) return elite;
        if (tier == 2) return veteran;
        if (tier == 1) return recruit;
        return null;
    }

    private static StatusEffect mk(String name, float hp, float dmg, Color color) {
        StatusEffect e = new StatusEffect(name);
        e.healthMultiplier = hp;
        e.damageMultiplier = dmg;
        e.color = color;
        e.show = true;
        e.permanent = false;
        return e;
    }
}
