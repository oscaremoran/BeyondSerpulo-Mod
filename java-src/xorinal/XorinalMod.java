package xorinal;

import arc.util.Log;
import mindustry.mod.Mod;

public class XorinalMod extends Mod {
    public XorinalMod() {
        Log.info("[Xorinal] Java mod constructor loaded");
    }

    @Override
    public void loadContent() {
        Log.info("[Xorinal] Java mod loadContent() — registering programmatic content");
        try { mindustry.world.meta.Attribute.add("crystal"); } catch (Exception e) { Log.err("[Xorinal] Attribute.add(crystal): " + e); }
        try { SporeClouds.registerContent(); } catch (Exception e) { Log.err("[Xorinal] SporeClouds.registerContent: " + e); }
        try { PilotEffects.register(); } catch (Exception e) { Log.err("[Xorinal] PilotEffects.register: " + e); }
    }

    @Override
    public void init() {
        Log.info("[Xorinal] Java mod init() — registering subsystems");
        try { Difficulties.init(); } catch (Exception e) { Log.err("[Xorinal] Difficulties.init: " + e); }
        try { UnitHealthBars.init(); } catch (Exception e) { Log.err("[Xorinal] UnitHealthBars.init: " + e); }
        try { DamageNumbers.init(); } catch (Exception e) { Log.err("[Xorinal] DamageNumbers.init: " + e); }
        try { EnemyPathOverlay.init(); } catch (Exception e) { Log.err("[Xorinal] EnemyPathOverlay.init: " + e); }
        try { PauseMenuExtras.init(); } catch (Exception e) { Log.err("[Xorinal] PauseMenuExtras.init: " + e); }
        try { CoreItemDelta.init(); } catch (Exception e) { Log.err("[Xorinal] CoreItemDelta.init: " + e); }
        try { DestroyLog.init(); } catch (Exception e) { Log.err("[Xorinal] DestroyLog.init: " + e); }
        try { GameSpeed.init(); } catch (Exception e) { Log.err("[Xorinal] GameSpeed.init: " + e); }
        try { SectorPause.init(); } catch (Exception e) { Log.err("[Xorinal] SectorPause.init: " + e); }
        try { Pilots.init(); } catch (Exception e) { Log.err("[Xorinal] Pilots.init: " + e); }
        try { CapsuleLauncher.init(); } catch (Exception e) { Log.err("[Xorinal] CapsuleLauncher.init: " + e); }
        try { PilotOverlay.init(); } catch (Exception e) { Log.err("[Xorinal] PilotOverlay.init: " + e); }
        try { PilotDownedHUD.init(); } catch (Exception e) { Log.err("[Xorinal] PilotDownedHUD.init: " + e); }
        try { Commanders.init(); } catch (Exception e) { Log.err("[Xorinal] Commanders.init: " + e); }
        try { CommanderArrowHUD.init(); } catch (Exception e) { Log.err("[Xorinal] CommanderArrowHUD.init: " + e); }
        try { WaveIntel.init(); } catch (Exception e) { Log.err("[Xorinal] WaveIntel.init: " + e); }
        try { ResourceFinder.init(); } catch (Exception e) { Log.err("[Xorinal] ResourceFinder.init: " + e); }
        try { InfectorDrain.init(); } catch (Exception e) { Log.err("[Xorinal] InfectorDrain.init: " + e); }
        try { LichenSystem.init(); } catch (Exception e) { Log.err("[Xorinal] LichenSystem.init: " + e); }
        try { PlanetMeshes.init(); } catch (Exception e) { Log.err("[Xorinal] PlanetMeshes.init: " + e); }
        try { DailyChallenge.init(); } catch (Exception e) { Log.err("[Xorinal] DailyChallenge.init: " + e); }
        try { MenuUI.init(); } catch (Exception e) { Log.err("[Xorinal] MenuUI.init: " + e); }
        try { Codex.init(); } catch (Exception e) { Log.err("[Xorinal] Codex.init: " + e); }
        try { Wizard.init(); } catch (Exception e) { Log.err("[Xorinal] Wizard.init: " + e); }
        try { Bloomheart.init(); } catch (Exception e) { Log.err("[Xorinal] Bloomheart.init: " + e); }
        try { SporeClouds.init(); } catch (Exception e) { Log.err("[Xorinal] SporeClouds.init: " + e); }
        try { BloomheartHUD.init(); } catch (Exception e) { Log.err("[Xorinal] BloomheartHUD.init: " + e); }
        try { HiddenPlanets.init(); } catch (Exception e) { Log.err("[Xorinal] HiddenPlanets.init: " + e); }
        try { VantresUnlock.init(); } catch (Exception e) { Log.err("[Xorinal] VantresUnlock.init: " + e); }
        try { SerpuloOnVantres.init(); } catch (Exception e) { Log.err("[Xorinal] SerpuloOnVantres.init: " + e); }
        Log.info("[Xorinal] Java mod init() complete");
    }
}
