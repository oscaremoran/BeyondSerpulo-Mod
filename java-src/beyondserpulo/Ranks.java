package beyondserpulo;

import arc.Core;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Per-map rank ladders. Each daily map has its own wave thresholds because
 * some maps are harder than others. Default ladder is used for any map
 * without a custom entry.
 */
public class Ranks {
    public static class Rank {
        public final String name;
        public final int waveThreshold;
        public Rank(String name, int waveThreshold) {
            this.name = name; this.waveThreshold = waveThreshold;
        }
    }

    public static final Rank[] DEFAULT_LADDER = {
        new Rank("Recruit",   5),
        new Rank("Cadet",     10),
        new Rank("Pilot",     20),
        new Rank("Veteran",   35),
        new Rank("Commander", 50),
        new Rank("Vanguard",  75),
        new Rank("Legend",    100)
    };

    /** Per-map overrides. Add an entry here for any map you want custom thresholds on. */
    public static final HashMap<String, Rank[]> ladders = new LinkedHashMap<>();
    static {
        ladders.put("daily-1", new Rank[]{
            new Rank("Recruit",   5),
            new Rank("Cadet",     10),
            new Rank("Pilot",     20),
            new Rank("Veteran",   35),
            new Rank("Commander", 50),
            new Rank("Vanguard",  75),
            new Rank("Legend",    100)
        });
        ladders.put("daily-3", new Rank[]{
            new Rank("Recruit",   3),
            new Rank("Cadet",     7),
            new Rank("Pilot",     14),
            new Rank("Veteran",   25),
            new Rank("Commander", 40),
            new Rank("Vanguard",  60),
            new Rank("Legend",    85)
        });
        ladders.put("daily-5", new Rank[]{
            new Rank("Recruit",   2),
            new Rank("Cadet",     5),
            new Rank("Pilot",     10),
            new Rank("Veteran",   18),
            new Rank("Commander", 30),
            new Rank("Vanguard",  45),
            new Rank("Legend",    65)
        });
    }

    public static Rank[] ladderFor(String mapName) {
        return ladders.getOrDefault(mapName, DEFAULT_LADDER);
    }

    private static final String KEY_PREFIX = "bs-daily-best-wave-";

    public static int bestWave(String mapName) { return Core.settings.getInt(KEY_PREFIX + mapName, 0); }

    public static boolean updateFromScore(String mapName, int wave) {
        if (wave > bestWave(mapName)) { Core.settings.put(KEY_PREFIX + mapName, wave); return true; }
        return false;
    }

    public static Rank currentRank(String mapName) {
        int w = bestWave(mapName);
        Rank cur = null;
        for (Rank r : ladderFor(mapName)) if (w >= r.waveThreshold) cur = r;
        return cur;
    }

    public static boolean isEarned(String mapName, Rank r) { return bestWave(mapName) >= r.waveThreshold; }
}
