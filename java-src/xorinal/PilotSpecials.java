package xorinal;

import arc.struct.ObjectMap;
import arc.util.Log;
import mindustry.content.UnitTypes;
import mindustry.type.UnitType;

public class PilotSpecials {
    public enum Special {
        VAMPIRE("Vampire", "[#ff5b8c]Vampire[]"),
        REGEN("Regen", "[#7fff7f]Regen[]"),
        OVERDRIVE("Overdrive", "[#ffff55]Overdrive[]"),
        SPEED("Speed", "[#5bffd0]Speed[]"),
        BOSS("Boss Aura", "[#ff8855]Boss Aura[]");

        public final String label, colored;
        Special(String l, String c) { label = l; colored = c; }
    }

    private static final ObjectMap<UnitType, Special> map = new ObjectMap<>();

    public static void load() {
        try {
            put(UnitTypes.dagger,   Special.VAMPIRE);
            put(UnitTypes.mace,     Special.REGEN);
            put(UnitTypes.fortress, Special.REGEN);
            put(UnitTypes.scepter,  Special.OVERDRIVE);
            put(UnitTypes.reign,    Special.OVERDRIVE);
            put(UnitTypes.nova,     Special.REGEN);
            put(UnitTypes.pulsar,   Special.SPEED);
            put(UnitTypes.quasar,   Special.REGEN);
            put(UnitTypes.vela,     Special.REGEN);
            put(UnitTypes.corvus,   Special.OVERDRIVE);
            put(UnitTypes.atrax,    Special.VAMPIRE);
            put(UnitTypes.spiroct,  Special.REGEN);
            put(UnitTypes.arkyid,   Special.SPEED);
            put(UnitTypes.toxopid,  Special.BOSS);
            put(UnitTypes.flare,    Special.SPEED);
            put(UnitTypes.horizon,  Special.VAMPIRE);
            put(UnitTypes.zenith,   Special.SPEED);
            put(UnitTypes.antumbra, Special.OVERDRIVE);
            put(UnitTypes.eclipse,  Special.OVERDRIVE);
            put(UnitTypes.risso,    Special.REGEN);
            put(UnitTypes.minke,    Special.VAMPIRE);
            put(UnitTypes.bryde,    Special.REGEN);
            put(UnitTypes.sei,      Special.SPEED);
            put(UnitTypes.omura,    Special.OVERDRIVE);
        } catch (Exception ex) {
            Log.err("[Xorinal] PilotSpecials.load: " + ex);
        }
    }

    private static void put(UnitType t, Special s) { if (t != null) map.put(t, s); }

    public static Special of(UnitType t) { return t == null ? null : map.get(t); }
    public static boolean isCombat(UnitType t) { return of(t) != null; }
}
