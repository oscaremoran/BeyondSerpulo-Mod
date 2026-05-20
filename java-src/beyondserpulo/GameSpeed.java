package beyondserpulo;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.scene.ui.TextField;
import arc.util.Strings;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;

public class GameSpeed {
    private static final float[] LEVELS = {0.1f, 0.3f, 0.5f, 0.75f, 1f, 1.5f, 2f, 2.5f, 5f, 10f};
    private static final KeyCode[] KEYS = {
            KeyCode.num1, KeyCode.num2, KeyCode.num3, KeyCode.num4, KeyCode.num5,
            KeyCode.num6, KeyCode.num7, KeyCode.num8, KeyCode.num9, KeyCode.num0
    };

    private static int idx = 4;
    private static boolean enabled = true;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() {
        enabled = !enabled;
        if (!enabled) idx = 4;
    }

    public static void init() {
        // Replace Mindustry's delta provider globally — this affects every reader of
        // Time.delta, including unit physics and async processes, which the old
        // beforeGameUpdate/afterGameUpdate wrap didn't reach.
        Time.setDeltaProvider(() -> {
            float raw = Math.min(Core.graphics.getDeltaTime() * 60f, 3f);
            if (!enabled || Vars.net != null && Vars.net.client()) return raw;
            float mul = LEVELS[idx];
            // Clamp the post-multiplied delta to avoid runaway per-frame jumps.
            return Math.min(raw * mul, 30f);
        });

        Events.run(EventType.Trigger.update, GameSpeed::pollKeys);
    }

    private static void pollKeys() {
        if (!enabled) return;
        if (Vars.state == null || !Vars.state.isGame()) return;
        if (Vars.net != null && Vars.net.client()) return;
        if (Core.scene != null && Core.scene.getKeyboardFocus() instanceof TextField) return;
        if (Vars.ui != null && Vars.ui.chatfrag != null && Vars.ui.chatfrag.shown()) return;

        for (int i = 0; i < KEYS.length; i++) {
            if (Core.input.keyTap(KEYS[i])) {
                idx = i;
                if (Vars.ui != null) {
                    Vars.ui.showInfoToast("[gold]Speed:[] " + Strings.fixed(LEVELS[idx], 2) + "x", 1.2f);
                }
                return;
            }
        }
    }
}
