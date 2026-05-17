package beyondserpulo;

/**
 * All the Google Form / Sheet endpoints live here. Fill these in after creating
 * the form and linked sheet (see step-by-step in this file's commit message /
 * project notes). Until they're set, the leaderboard quietly no-ops.
 */
public final class LeaderboardConfig {
    // 1. Form POST endpoint. Take the form's share URL (.../viewform) and replace
    //    "viewform" with "formResponse".
    public static final String FORM_POST_URL =
        "https://docs.google.com/forms/d/e/1FAIpQLSecKgYv7DSXO8Cr6t9l3123FNOPtmpeFjZbXWyo1-rCzWM0QA/formResponse";

    // Extracted from the pre-filled link (A=date, B=map, C=mode, 1=score, D=name, E=check).
    public static final String FIELD_DATE  = "entry.1930623480";
    public static final String FIELD_MAP   = "entry.609411128";
    public static final String FIELD_MODE  = "entry.863451618";
    public static final String FIELD_SCORE = "entry.716218321";
    public static final String FIELD_NAME  = "entry.2076220176";
    public static final String FIELD_CHECK = "entry.1105763323";

    // 3. Published-CSV URL of the linked sheet.
    public static final String SHEET_CSV_URL =
        "https://docs.google.com/spreadsheets/d/e/2PACX-1vQ1j9afKe-HSdabJQMYGRVWBIYQItXEzacyxKofYeZIGyM3E-tE2DCNnL-6QW_xjyRKWeriURBGlbYz/pub?gid=800911707&single=true&output=csv";

    /** True once you've replaced all the placeholders above. */
    public static boolean configured() {
        return !FORM_POST_URL.contains("REPLACE_WITH")
            && !SHEET_CSV_URL.contains("REPLACE_WITH")
            && !FIELD_DATE.equals("entry.0000000001");
    }

    private LeaderboardConfig() {}
}
