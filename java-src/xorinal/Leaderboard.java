package xorinal;

import arc.Core;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Http;
import arc.util.Log;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Reads scores from a published Google Sheet CSV; writes new scores by POSTing
 * to a Google Form's formResponse endpoint. No GitHub, no token, no own server.
 * All endpoints live in LeaderboardConfig.
 */
public class Leaderboard {
    public static class Row {
        public final String date, map, mode, name;
        public final int score;
        public Row(String date, String map, String mode, int score, String name) {
            this.date = date; this.map = map; this.mode = mode; this.score = score; this.name = name;
        }
    }

    public static void fetchToday(Cons<Seq<Row>> callback) {
        if (!LeaderboardConfig.configured()) { callback.get(new Seq<>()); return; }

        String dateKey = DailyChallenge.todayKey();
        String mapName = DailyChallenge.today().mapName;
        boolean ascending = DailyChallenge.today().mode == DailyChallenge.Mode.FASTEST_CAPTURE;

        Http.get(LeaderboardConfig.SHEET_CSV_URL, response -> {
            Seq<Row> rows = parseCsv(response.getResultAsString());
            Seq<Row> filtered = rows.select(r -> r.date.equals(dateKey) && r.map.equals(mapName));
            filtered.sort((a, b) -> ascending ? Integer.compare(a.score, b.score) : Integer.compare(b.score, a.score));
            Core.app.post(() -> callback.get(filtered));
        }, err -> {
            Log.err("[Xorinal] leaderboard fetch failed: " + err);
            Core.app.post(() -> callback.get(new Seq<>()));
        });
    }

    /** Submit a score by POSTing to the form. onDone runs on the main thread with success flag. */
    public static void submit(String date, String map, String mode, int score, String name, String check, Cons<Boolean> onDone) {
        if (!LeaderboardConfig.configured()) {
            Log.warn("[Xorinal] leaderboard not configured — score not submitted.");
            Core.app.post(() -> onDone.get(false));
            return;
        }
        String body =
              enc(LeaderboardConfig.FIELD_DATE)  + "=" + enc(date)
            + "&" + enc(LeaderboardConfig.FIELD_MAP)   + "=" + enc(map)
            + "&" + enc(LeaderboardConfig.FIELD_MODE)  + "=" + enc(mode)
            + "&" + enc(LeaderboardConfig.FIELD_SCORE) + "=" + enc(String.valueOf(score))
            + "&" + enc(LeaderboardConfig.FIELD_NAME)  + "=" + enc(name)
            + "&" + enc(LeaderboardConfig.FIELD_CHECK) + "=" + enc(check);

        Http.post(LeaderboardConfig.FORM_POST_URL, body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .error(err -> {
                // Google Forms returns the confirmation HTML page on success; Http may flag the
                // redirect as an error. Treat any response as success unless we're certain it failed.
                Log.info("[Xorinal] form submit returned: " + err);
                Core.app.post(() -> onDone.get(true));
            })
            .submit(response -> Core.app.post(() -> onDone.get(true)));
    }

    private static Seq<Row> parseCsv(String body) {
        Seq<Row> out = new Seq<>();
        if (body == null) return out;
        String[] lines = body.split("\\r?\\n");
        if (lines.length < 2) return out;

        // Header row: Timestamp, date, map, mode, score, name, check (in form-creation order).
        String[] headers = splitCsv(lines[0]);
        int iDate = idx(headers, "date"), iMap = idx(headers, "map"), iMode = idx(headers, "mode"),
            iScore = idx(headers, "score"), iName = idx(headers, "name");
        if (iDate < 0 || iMap < 0 || iMode < 0 || iScore < 0 || iName < 0) {
            Log.err("[Xorinal] leaderboard CSV header missing expected columns: " + lines[0]);
            return out;
        }

        for (int i = 1; i < lines.length; i++) {
            String[] cols = splitCsv(lines[i]);
            if (cols.length <= Math.max(iName, iScore)) continue;
            try {
                int score = Integer.parseInt(cols[iScore].trim());
                out.add(new Row(cols[iDate].trim(), cols[iMap].trim(), cols[iMode].trim(), score, cols[iName].trim()));
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    private static int idx(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) if (headers[i].trim().equalsIgnoreCase(name)) return i;
        return -1;
    }

    /** Tiny CSV splitter — handles quoted fields with embedded commas, enough for Form responses. */
    private static String[] splitCsv(String line) {
        Seq<String> out = new Seq<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(c);
            } else {
                if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
                else if (c == '"') inQuotes = true;
                else cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(String.class);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
