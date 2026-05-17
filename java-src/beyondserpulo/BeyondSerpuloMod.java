package beyondserpulo;

import arc.util.Log;
import mindustry.mod.Mod;

public class BeyondSerpuloMod extends Mod {
    public BeyondSerpuloMod() {
        Log.info("[BeyondSerpulo] Java mod constructor loaded");
    }

    @Override
    public void loadContent() {
        Log.info("[BeyondSerpulo] Java mod loadContent() — registering programmatic content");
        try { mindustry.world.meta.Attribute.add("crystal"); } catch (Exception e) { Log.err("[BeyondSerpulo] Attribute.add(crystal): " + e); }
        try { SporeClouds.registerContent(); } catch (Exception e) { Log.err("[BeyondSerpulo] SporeClouds.registerContent: " + e); }
        try { PilotEffects.register(); } catch (Exception e) { Log.err("[BeyondSerpulo] PilotEffects.register: " + e); }
    }

    @Override
    public void init() {
        Log.info("[BeyondSerpulo] Java mod init() — registering subsystems");
        try { mindustry.editor.MapResizeDialog.maxSize = 1500; } catch (Exception e) { Log.err("[BeyondSerpulo] MapResizeDialog.maxSize override: " + e); }
        try { Difficulties.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] Difficulties.init: " + e); }
        try { UnitHealthBars.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] UnitHealthBars.init: " + e); }
        try { DamageNumbers.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] DamageNumbers.init: " + e); }
        try { EnemyPathOverlay.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] EnemyPathOverlay.init: " + e); }
        try { PauseMenuExtras.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] PauseMenuExtras.init: " + e); }
        try { CoreItemDelta.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] CoreItemDelta.init: " + e); }
        try { DestroyLog.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] DestroyLog.init: " + e); }
        try { GameSpeed.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] GameSpeed.init: " + e); }
        try { SectorPause.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] SectorPause.init: " + e); }
        try { Pilots.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] Pilots.init: " + e); }
        try { CapsuleLauncher.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] CapsuleLauncher.init: " + e); }
        try { PilotOverlay.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] PilotOverlay.init: " + e); }
        try { PilotDownedHUD.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] PilotDownedHUD.init: " + e); }
        try { Commanders.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] Commanders.init: " + e); }
        try { CommanderArrowHUD.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] CommanderArrowHUD.init: " + e); }
        try { WaveIntel.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] WaveIntel.init: " + e); }
        try { ResourceFinder.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] ResourceFinder.init: " + e); }
        try { InfectorDrain.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] InfectorDrain.init: " + e); }
        try { LichenSystem.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] LichenSystem.init: " + e); }
        try { PlanetMeshes.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] PlanetMeshes.init: " + e); }
        try { DailyChallenge.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] DailyChallenge.init: " + e); }
        try { MenuUI.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] MenuUI.init: " + e); }
        try { Codex.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] Codex.init: " + e); }
        try { Wizard.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] Wizard.init: " + e); }
        try { Bloomheart.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] Bloomheart.init: " + e); }
        try { SporeClouds.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] SporeClouds.init: " + e); }
        try { BloomheartHUD.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] BloomheartHUD.init: " + e); }
        try { HiddenPlanets.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] HiddenPlanets.init: " + e); }
        try { VantresUnlock.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] VantresUnlock.init: " + e); }
        try { SerpuloOnVantres.init(); } catch (Exception e) { Log.err("[BeyondSerpulo] SerpuloOnVantres.init: " + e); }
        Log.info("[BeyondSerpulo] Java mod init() complete");
    }
}
