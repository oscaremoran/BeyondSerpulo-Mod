package beyondserpulo;

import arc.struct.Seq;
import arc.util.Strings;

import java.util.Locale;

public class BossProgram {
    public enum Op {
        MoveTo("Move To", "x,y"),
        Wait("Wait", "seconds"),
        AttackNearest("Attack Nearest", ""),
        ClearTarget("Clear Target", ""),
        Charge("Charge", "x,y"),
        SpawnAdd("Spawn Adds", "unit,count"),
        FireAt("Fire At", "x,y,seconds"),
        WaitPhase("Wait Until HP <", "fraction"),
        Repeat("Repeat", "");

        public final String label, args;
        Op(String label, String args) { this.label = label; this.args = args; }
    }

    public static class Stmt {
        public Op op;
        public float a, b, c;
        public String s = "";

        public Stmt(Op op) { this.op = op; }

        public String describe() {
            switch (op) {
                case MoveTo: return "Move To (" + (int) a + ", " + (int) b + ")";
                case Wait: return "Wait " + Strings.fixed(a, 1) + "s";
                case AttackNearest: return "Attack Nearest Enemy";
                case ClearTarget: return "Clear Target";
                case Charge: return "Charge To (" + (int) a + ", " + (int) b + ")";
                case SpawnAdd: return "Spawn " + (int) b + " x " + (s.isEmpty() ? "?" : s);
                case FireAt: return "Fire At (" + (int) a + ", " + (int) b + ") for " + Strings.fixed(c, 1) + "s";
                case WaitPhase: return "Wait Until HP < " + Strings.fixed(a, 2);
                case Repeat: return "Repeat From Start";
            }
            return op.name();
        }
    }

    public final Seq<Stmt> stmts = new Seq<>();

    public static BossProgram fromString(String src) {
        BossProgram p = new BossProgram();
        if (src == null || src.isEmpty()) return p;
        for (String chunk : src.split("\\|")) {
            if (chunk.isEmpty()) continue;
            String[] parts = chunk.split(":", 2);
            Op op;
            try { op = Op.valueOf(parts[0]); } catch (Exception e) { continue; }
            Stmt st = new Stmt(op);
            if (parts.length > 1) {
                String[] args = parts[1].split(",");
                try {
                    if (args.length > 0 && !args[0].isEmpty()) {
                        try { st.a = Float.parseFloat(args[0]); } catch (Exception e) { st.s = args[0]; }
                    }
                    if (args.length > 1) st.b = Float.parseFloat(args[1]);
                    if (args.length > 2) st.c = Float.parseFloat(args[2]);
                    if (op == Op.SpawnAdd && args.length >= 2) { st.s = args[0]; st.a = 0; }
                } catch (Exception e) {}
            }
            p.stmts.add(st);
        }
        return p;
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stmts.size; i++) {
            Stmt st = stmts.get(i);
            if (i > 0) sb.append('|');
            sb.append(st.op.name());
            switch (st.op) {
                case MoveTo: case Charge:
                    sb.append(':').append(fmt(st.a)).append(',').append(fmt(st.b)); break;
                case Wait: case WaitPhase:
                    sb.append(':').append(fmt(st.a)); break;
                case SpawnAdd:
                    sb.append(':').append(st.s == null ? "" : st.s).append(',').append((int) st.b); break;
                case FireAt:
                    sb.append(':').append(fmt(st.a)).append(',').append(fmt(st.b)).append(',').append(fmt(st.c)); break;
                default: break;
            }
        }
        return sb.toString();
    }

    private static String fmt(float v) {
        if (v == (int) v) return Integer.toString((int) v);
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
