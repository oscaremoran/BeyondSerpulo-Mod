package beyondserpulo;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Universe;

import java.lang.reflect.Field;

public class SectorPause {
    private static boolean enabled = false;
    private static Field turnCounterField;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    public static void init() {
        try {
            turnCounterField = Universe.class.getDeclaredField("turnCounter");
            turnCounterField.setAccessible(true);
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] SectorPause: failed to access Universe.turnCounter: " + ex);
            return;
        }

        Events.run(EventType.Trigger.update, SectorPause::tick);
    }

    private static void tick() {
        if (!enabled) return;
        if (Vars.universe == null) return;
        if (Vars.net != null && Vars.net.client()) return;
        if (Vars.state == null || !Vars.state.hasSector()) return;

        try {
            turnCounterField.setFloat(Vars.universe, 0f);
        } catch (Exception ignored) {}
    }
}
