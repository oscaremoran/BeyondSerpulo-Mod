package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.EventType.SectorCaptureEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.maps.Map;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Daily Challenge: 5 hand-crafted maps in maps/daily/, one rotates in each day.
 * Date-derived selection so all players globally see the same map on a given UTC date.
 * One attempt per day, no save/pause during a run. Full research allowed.
 */
public class DailyChallenge {
    public enum Mode {
        // Score = highest wave reached before core dies.
        SURVIVE_WAVES,
        // Score = seconds elapsed when sector is captured (lower is better).
        FASTEST_CAPTURE
    }

    public static class Entry {
        public final String mapName;   // matches the .msav filename (without extension) in maps/daily/
        public final String title;
        public final Mode mode;
        public Entry(String mapName, String title, Mode mode) {
            this.mapName = mapName; this.title = title; this.mode = mode;
        }
    }

    // Rotation pool. Filenames must match files the user drops into maps/ (Mindustry flattens the maps dir).
    public static final Entry[] entries = new Entry[]{
        new Entry("daily-1", "Daily 1",  Mode.SURVIVE_WAVES),
        new Entry("daily-2", "Daily 2",  Mode.FASTEST_CAPTURE),
        new Entry("daily-3", "Daily 3",  Mode.SURVIVE_WAVES),
        // TEMP: index 3 swapped to daily-1/SURVIVE_WAVES for testing on 2026-05-15. Revert to ("daily-4", "Daily 4", FASTEST_CAPTURE).
        new Entry("daily-1", "Daily 1",  Mode.SURVIVE_WAVES),
        new Entry("daily-5", "Daily 5",  Mode.SURVIVE_WAVES)
    };

    private static final String KEY_LAST_ATTEMPT = "bs-daily-last-attempt";
    private static final String KEY_PLAYER_NAME  = "bs-daily-player-name";

    // True while a daily-challenge run is in progress; consulted by SectorPause/save hooks.
    private static boolean active = false;
    private static long runStartMillis = 0L;

    public static boolean isActive() { return active; }

    public static String todayKey() {
        return LocalDate.now(ZoneOffset.UTC).toString(); // e.g. "2026-05-15"
    }

    public static int todayIndex() {
        long days = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        return (int) Math.floorMod(days, entries.length);
    }

    public static Entry today() { return entries[todayIndex()]; }

    public static boolean attemptedToday() {
        return todayKey().equals(Core.settings.getString(KEY_LAST_ATTEMPT, ""));
    }

    public static void markAttempted() {
        Core.settings.put(KEY_LAST_ATTEMPT, todayKey());
    }

    public static String playerName() {
        return Core.settings.getString(KEY_PLAYER_NAME, "");
    }

    public static void setPlayerName(String name) {
        Core.settings.put(KEY_PLAYER_NAME, name);
    }

    /** Resolves today's map. Tries byName first, falls back to matching the .msav filename. */
    public static Map todayMap() {
        String key = today().mapName;
        try {
            Map m = Vars.maps.byName(key);
            if (m != null) return m;
            for (Map cm : Vars.maps.all()) {
                if (cm.file != null && cm.file.nameWithoutExtension().equals(key)) return cm;
            }
            return null;
        } catch (Exception ex) { Log.err("[BeyondSerpulo] DailyChallenge.todayMap: " + ex); return null; }
    }

    public static void beginRun() {
        active = true;
        runStartMillis = System.currentTimeMillis();
        markAttempted();
    }

    public static long elapsedSeconds() {
        if (runStartMillis == 0L) return 0L;
        return (System.currentTimeMillis() - runStartMillis) / 1000L;
    }

    public static void init() {
        // End-of-run detection wires score computation. Submission UI lives in ScoreSubmit.
        Events.on(GameOverEvent.class, e -> {
            Log.info("[BeyondSerpulo] GameOverEvent fired; active=" + active + " wave=" + Vars.state.wave);
            if (!active) return;
            int score = (today().mode == Mode.SURVIVE_WAVES) ? Math.max(0, Vars.state.wave - 1) : -1;
            if (today().mode == Mode.SURVIVE_WAVES) Ranks.updateFromScore(today().mapName, score);
            ScoreSubmit.offerSubmit(today(), score);
            active = false;
        });
        Events.on(SectorCaptureEvent.class, e -> {
            if (!active) return;
            if (today().mode == Mode.FASTEST_CAPTURE) {
                int score = (int) elapsedSeconds();
                ScoreSubmit.offerSubmit(today(), score);
            }
            active = false;
        });
        Events.on(WorldLoadEvent.class, e -> {
            Log.info("[BeyondSerpulo] WorldLoadEvent; active=" + active + " state.map=" + (Vars.state.map==null?"null":Vars.state.map.name()) + " today=" + today().mapName);
            // TEMP: defensive clear disabled while debugging — was firing right after beginRun.
            // if (active && Vars.state.map != todayMap()) active = false;
        });
    }
}
