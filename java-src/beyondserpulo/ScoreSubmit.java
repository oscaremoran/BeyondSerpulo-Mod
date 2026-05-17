package beyondserpulo;

import arc.Core;
import arc.scene.ui.Label;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

/**
 * End-of-run flow: prompt for a name on first submit, then auto-POST the score
 * to the Google Form. Checksum stops trivial form-spam; it's not crypto.
 */
public class ScoreSubmit {
    private static final String SECRET = "beyond-serpulo-v1";

    public static void offerSubmit(DailyChallenge.Entry entry, int score) {
        if (score < 0) return;
        if (DailyChallenge.playerName().isEmpty()) {
            promptName(name -> { if (!name.isEmpty()) submit(entry, score, name); });
        } else {
            submit(entry, score, DailyChallenge.playerName());
        }
    }

    private static void promptName(arc.func.Cons<String> onSet) {
        BaseDialog d = new BaseDialog("Leaderboard Name");
        d.cont.add("Choose a name to appear on the daily-challenge leaderboard.").pad(10f);
        d.cont.row();
        TextField field = new TextField();
        d.cont.add(field).width(300f).pad(10f);
        d.cont.row();
        d.buttons.button("Cancel", Icon.cancel, d::hide);
        d.buttons.button("Save", Icon.ok, () -> {
            String n = field.getText().trim().replaceAll("[,\\r\\n]", "");
            if (n.isEmpty()) return;
            DailyChallenge.setPlayerName(n);
            d.hide();
            onSet.get(n);
        });
        d.show();
    }

    private static void submit(DailyChallenge.Entry entry, int score, String name) {
        String date = DailyChallenge.todayKey();
        String mode = entry.mode.name();
        // Append the Mindustry/Steam profile name in parens so the leaderboard can show both.
        String steamName = Core.settings.getString("name", "").trim().replaceAll("[,()\\r\\n]", "");
        String submittedName = steamName.isEmpty() ? name : name + " (" + steamName + ")";
        String check = checksumFor(date, entry.mapName, mode, score, submittedName);
        name = submittedName;

        BaseDialog d = new BaseDialog("Score Submitted");
        Table c = d.cont;
        c.add("Today's score").pad(4f).row();
        c.add("[accent]" + score + "[]  (" + readableMode(entry.mode) + ")").pad(6f).row();
        c.add("as [accent]" + name + "[]").pad(6f).row();
        Label status = new Label("[lightgray]Sending...[]");
        c.add(status).pad(10f).row();
        d.buttons.button("Close", Icon.ok, d::hide);
        d.show();

        Leaderboard.submit(date, entry.mapName, mode, score, name, check, ok -> {
            status.setText(ok ? "[lime]Submitted to leaderboard.[]" : "[scarlet]Submit failed — try again later.[]");
        });
    }

    private static String readableMode(DailyChallenge.Mode m) {
        return m == DailyChallenge.Mode.SURVIVE_WAVES ? "waves survived" : "seconds to capture";
    }

    static String checksumFor(String date, String map, String mode, int score, String name) {
        int h = (date + "|" + map + "|" + mode + "|" + score + "|" + name + "|" + SECRET).hashCode();
        return Integer.toHexString(h);
    }

    public static void init() { /* nothing — DailyChallenge invokes offerSubmit directly */ }
}
