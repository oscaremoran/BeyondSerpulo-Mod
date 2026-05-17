package xorinal;

import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.gen.Icon;
import mindustry.maps.Map;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Main dialog for the Daily Challenge:
 *  - Shows today's map + scoring mode
 *  - Loads + displays today's top scores from the GitHub leaderboard
 *  - Play button launches the map; locked state if already attempted today
 */
public class DailyChallengeUI {
    public static void open() {
        BaseDialog d = new BaseDialog("Daily Challenge");
        d.addCloseButton();
        rebuild(d);
        d.show();
    }

    private static void rebuild(BaseDialog d) {
        d.cont.clear();
        Table c = d.cont;

        DailyChallenge.Entry today = DailyChallenge.today();
        Map map = DailyChallenge.todayMap();
        boolean mapReady = map != null;
        boolean alreadyAttempted = false; // TEMP: bypassed for testing. Restore: DailyChallenge.attemptedToday();

        c.add("[accent]" + today.title + "[]").pad(8f).row();
        c.add("Mode: " + readableMode(today.mode)).pad(4f).row();
        c.add("Date: " + DailyChallenge.todayKey() + " (UTC)").pad(4f).row();

        String currentName = DailyChallenge.playerName();
        String nameLabel = currentName.isEmpty() ? "Set name" : "Name: " + currentName;
        c.button(nameLabel, Icon.pencil, () -> nameDialog(d)).size(220f, 38f).pad(4f).row();

        if (!mapReady) {
            c.add("[orange]Coming soon[] — map file '" + today.mapName + ".msav' not found.").pad(12f).row();
        } else if (alreadyAttempted) {
            c.add("[orange]You've already attempted today's challenge.[]\nCome back tomorrow.").pad(12f).row();
        } else {
            c.button("Play", Icon.play, () -> {
                try {
                    DailyChallenge.beginRun();
                    Gamemode mode = today.mode == DailyChallenge.Mode.FASTEST_CAPTURE ? Gamemode.attack : Gamemode.survival;
                    Rules rules = map.applyRules(mode);
                    rules.canGameOver = true;
                    Vars.control.playMap(map, rules);
                    d.hide();
                } catch (Exception ex) {
                    Log.err("[Xorinal] daily play failed: " + ex);
                }
            }).size(160f, 50f).pad(10f).row();
        }

        c.add("[lightgray]Today's leaderboard[]").pad(10f).row();

        Table board = new Table();
        board.add("Loading...").pad(10f);
        ScrollPane sp = new ScrollPane(board);
        c.add(sp).width(640f).height(360f).pad(10f).row();

        c.add("[lightgray]Ranks — " + today.mapName + "[]").pad(10f).row();
        if (today.mode != DailyChallenge.Mode.SURVIVE_WAVES) {
            c.add("[gray]Ranks track survive-waves maps only.[]").pad(8f).row();
        } else {
            int best = Ranks.bestWave(today.mapName);
            Ranks.Rank cur = Ranks.currentRank(today.mapName);
            String curText = "Best wave: [accent]" + best + "[]   Current rank: " + (cur == null ? "[gray]None[]" : "[accent]" + cur.name + "[]");
            Label curLabel = new Label(curText);
            curLabel.setFontScale(1.1f);
            c.add(curLabel).pad(4f).row();

            Table ranks = new Table();
            for (Ranks.Rank r : Ranks.ladderFor(today.mapName)) {
                boolean earned = Ranks.isEarned(today.mapName, r);
                String mark = earned ? "[lime]✔[]" : "[gray]•[]";
                String nameCol = (earned ? "[accent]" : "[gray]") + r.name + "[]";
                String waveCol = (earned ? "" : "[gray]") + "wave " + r.waveThreshold + (earned ? "" : "[]");
                addCell(ranks, mark, 60f, arc.util.Align.center);
                addCell(ranks, nameCol, 240f, arc.util.Align.left);
                addCell(ranks, waveCol, 200f, arc.util.Align.left);
                ranks.row();
            }
            ScrollPane ranksSp = new ScrollPane(ranks);
            c.add(ranksSp).width(560f).height(280f).pad(10f).row();
        }

        Leaderboard.fetchToday(rows -> {
            board.clear();
            if (rows.isEmpty()) {
                Label empty = new Label("No scores yet today. Be the first!");
                empty.setFontScale(1.2f);
                board.add(empty).pad(12f);
                return;
            }
            addHeader(board, "[lightgray]#[]", 60f, arc.util.Align.center);
            addHeader(board, "[lightgray]Player[]", 360f, arc.util.Align.left);
            addHeader(board, "[lightgray]Score[]", 160f, arc.util.Align.right);
            board.row();
            int rank = 1;
            for (var r : rows) {
                if (rank > 10) break;
                addCell(board, "" + rank, 60f, arc.util.Align.center);
                addCell(board, r.name, 360f, arc.util.Align.left);
                addCell(board, formatScore(r.score, today.mode), 160f, arc.util.Align.right);
                board.row();
                rank++;
            }
        });
    }

    private static void nameDialog(BaseDialog parent) {
        BaseDialog d = new BaseDialog("Leaderboard Name");
        d.cont.add("Name shown on the leaderboard.").pad(8f).row();
        TextField field = new TextField(DailyChallenge.playerName());
        d.cont.add(field).width(300f).pad(8f).row();
        d.buttons.button("Cancel", Icon.cancel, d::hide);
        d.buttons.button("Save", Icon.ok, () -> {
            String n = field.getText().trim().replaceAll("[,\\r\\n]", "");
            if (n.isEmpty()) return;
            DailyChallenge.setPlayerName(n);
            d.hide();
            rebuild(parent);
        });
        d.show();
    }

    private static void addHeader(Table t, String text, float width, int align) {
        Label l = new Label(text);
        l.setFontScale(1.15f);
        l.setAlignment(align);
        t.add(l).width(width).pad(8f).align(align);
    }

    private static void addCell(Table t, String text, float width, int align) {
        Label l = new Label(text);
        l.setFontScale(1.3f);
        l.setAlignment(align);
        t.add(l).width(width).pad(10f).align(align);
    }

    private static String readableMode(DailyChallenge.Mode m) {
        return m == DailyChallenge.Mode.SURVIVE_WAVES ? "Survive — highest wave wins" : "Capture — fastest time wins";
    }

    private static String formatScore(int score, DailyChallenge.Mode m) {
        if (m == DailyChallenge.Mode.FASTEST_CAPTURE) {
            int min = score / 60, sec = score % 60;
            return min + "m" + (sec < 10 ? "0" : "") + sec + "s";
        }
        return "wave " + score;
    }
}
