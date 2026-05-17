package beyondserpulo;

import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.ResearchEvent;
import mindustry.type.Planet;

public class VantresUnlock {
    public static void init() {
        Events.on(ClientLoadEvent.class, e -> check());
        Events.on(ResearchEvent.class, e -> {
            if (e.content != null && "core-foundation".equals(e.content.name)) check();
        });
    }

    private static void check() {
        try {
            Planet vantres = Vars.content.getByName(ContentType.planet, "xorinal-vantres");
            if (vantres == null) return;
            var found = Vars.content.getByName(ContentType.block, "core-foundation");
            if (found instanceof UnlockableContent && ((UnlockableContent) found).unlocked() && !vantres.unlocked()) {
                vantres.unlock();
                Log.info("[BeyondSerpulo] Vantres unlocked (core-foundation researched)");
            }
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] VantresUnlock.check: " + ex);
        }
    }
}
