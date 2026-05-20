package beyondserpulo;

import arc.graphics.Color;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.content.Fx;
import mindustry.content.Planets;
import mindustry.content.StatusEffects;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.ContinuousLaserBulletType;
import mindustry.entities.bullet.MissileBulletType;
import mindustry.entities.pattern.ShootPattern;
import mindustry.gen.Sounds;
import mindustry.gen.UnitEntity;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.type.Weapon;

public class BossUnits {
    public static UnitType nemesis, celestial, abyssal, ragnarok;
    public static StatusEffect rage, empJammed, blessed, starBurn, abyssalGrasp, suffocating, doomsday, ragnarokFear;

    public static void register() {
        try { registerStatuses(); } catch (Exception e) { Log.err("[BeyondSerpulo] BossUnits statuses: " + e); }
        try { nemesis = buildNemesis(); } catch (Exception e) { Log.err("[BeyondSerpulo] BossUnits.nemesis: " + e); }
        try { celestial = buildCelestial(); } catch (Exception e) { Log.err("[BeyondSerpulo] BossUnits.celestial: " + e); }
        try { abyssal = buildAbyssal(); } catch (Exception e) { Log.err("[BeyondSerpulo] BossUnits.abyssal: " + e); }
        try { ragnarok = buildRagnarok(); } catch (Exception e) { Log.err("[BeyondSerpulo] BossUnits.ragnarok: " + e); }
        try {
            if (Planets.serpulo != null) {
                if (nemesis != null && !Planets.serpulo.unlockedOnLand.contains(nemesis)) Planets.serpulo.unlockedOnLand.add(nemesis);
                if (celestial != null && !Planets.serpulo.unlockedOnLand.contains(celestial)) Planets.serpulo.unlockedOnLand.add(celestial);
                if (abyssal != null && !Planets.serpulo.unlockedOnLand.contains(abyssal)) Planets.serpulo.unlockedOnLand.add(abyssal);
                if (ragnarok != null && !Planets.serpulo.unlockedOnLand.contains(ragnarok)) Planets.serpulo.unlockedOnLand.add(ragnarok);
            }
        } catch (Exception e) { Log.err("[BeyondSerpulo] BossUnits planet assign: " + e); }
    }

    private static void registerStatuses() {
        rage = new StatusEffect("nemesis-rage");
        rage.color = Color.valueOf("ff4040");
        rage.damageMultiplier = 1.4f;
        rage.speedMultiplier = 1.25f;
        rage.show = true;
        rage.permanent = false;

        empJammed = new StatusEffect("nemesis-emp");
        empJammed.color = Color.valueOf("80c0ff");
        empJammed.disarm = true;
        empJammed.speedMultiplier = 0.7f;
        empJammed.show = true;

        blessed = new StatusEffect("celestial-blessed");
        blessed.color = Color.valueOf("ffe080");
        blessed.reloadMultiplier = 1.6f;
        blessed.show = true;

        starBurn = new StatusEffect("starlight-burn");
        starBurn.color = Color.valueOf("a8dfff");
        starBurn.damage = 6f;
        starBurn.show = true;

        abyssalGrasp = new StatusEffect("abyssal-grasp");
        abyssalGrasp.color = Color.valueOf("3a90b0");
        abyssalGrasp.speedMultiplier = 0.35f;
        abyssalGrasp.damage = 8f;
        abyssalGrasp.show = true;

        suffocating = new StatusEffect("suffocating");
        suffocating.color = Color.valueOf("a02020");
        suffocating.speedMultiplier = 0.65f;
        suffocating.show = true;

        doomsday = new StatusEffect("ragnarok-doomsday");
        doomsday.color = Color.valueOf("ff3010");
        doomsday.damageMultiplier = 2.2f;
        doomsday.speedMultiplier = 1.15f;
        doomsday.reloadMultiplier = 1.8f;
        doomsday.show = true;

        ragnarokFear = new StatusEffect("ragnarok-fear");
        ragnarokFear.color = Color.valueOf("804030");
        ragnarokFear.reloadMultiplier = 0.55f;
        ragnarokFear.speedMultiplier = 0.85f;
        ragnarokFear.show = true;
    }

    private static UnitType buildNemesis() {
        UnitType u = new UnitType("nemesis");
        u.constructor = UnitEntity::create;
        u.localizedName = "Nemesis";
        u.description = "Heavy assault tank. Vengeance: rages at half health, releases drones on death.";
        u.health = 25000f;
        u.armor = 30f;
        u.speed = 1.4f;
        u.accel = 0.05f;
        u.drag = 0.18f;
        u.rotateSpeed = 2.2f;
        u.hitSize = 28f;
        u.range = 360f;
        u.maxRange = 360f;
        u.faceTarget = true;
        u.flying = false;
        u.targetAir = true;
        u.targetGround = true;
        u.engineSize = 0f;
        u.lowAltitude = true;
        u.itemCapacity = 0;
        u.hidden = false;
        u.useUnitCap = false;
        u.singleTarget = false;

        // Dual heavy plasma cannons — front-facing, mounted forward of turret center.
        Weapon cannonL = makePlasmaCannon();
        cannonL.x = -10f;
        cannonL.y = 14f;
        cannonL.mirror = false;

        Weapon cannonR = makePlasmaCannon();
        cannonR.x = 10f;
        cannonR.y = 14f;
        cannonR.mirror = false;

        // Shoulder missile pods — burst seeking EMP/splash
        Weapon missilesL = makeMissilePod();
        missilesL.x = -16f;
        missilesL.y = 8f;
        missilesL.mirror = false;

        Weapon missilesR = makeMissilePod();
        missilesR.x = 16f;
        missilesR.y = 8f;
        missilesR.mirror = false;

        u.weapons = Seq.with(cannonL, cannonR, missilesL, missilesR);
        return u;
    }

    private static Weapon makePlasmaCannon() {
        Weapon w = new Weapon();
        w.reload = 55f;
        w.recoil = 3.5f;
        w.shake = 2.2f;
        w.shootCone = 10f;
        w.rotate = false;
        w.mirror = false;
        w.top = true;
        w.shootSound = Sounds.shootBeamPlasma;
        w.inaccuracy = 1.5f;
        w.bullet = makePlasmaBullet();
        return w;
    }

    private static BulletType makePlasmaBullet() {
        BasicBulletType b = new BasicBulletType(7f, 320f);
        b.lifetime = 60f;
        b.width = 14f;
        b.height = 22f;
        b.frontColor = Color.valueOf("ff8060");
        b.backColor = Color.valueOf("ff2040");
        b.trailColor = Color.valueOf("ff2040");
        b.trailLength = 10;
        b.trailWidth = 3f;
        b.homingPower = 0f;
        b.homingRange = 0f;
        b.hitEffect = Fx.hitBulletColor;
        b.despawnEffect = Fx.hitBulletColor;
        b.pierceCap = 2;
        b.knockback = 2f;
        return b;
    }

    private static Weapon makeMissilePod() {
        Weapon w = new Weapon();
        w.reload = 30f;
        ShootPattern sp = new ShootPattern();
        sp.shots = 4;
        sp.shotDelay = 3f;
        w.shoot = sp;
        w.inaccuracy = 18f;
        w.shake = 0.6f;
        w.rotate = false;
        w.mirror = false;
        w.top = true;
        w.shootCone = 90f;
        w.shootSound = Sounds.shootSmite;
        w.bullet = makeEmpMissile();
        return w;
    }

    private static BulletType makeEmpMissile() {
        MissileBulletType m = new MissileBulletType(3.2f, 60f);
        m.lifetime = 90f;
        m.homingPower = 0.18f;
        m.homingRange = 220f;
        m.width = 8f;
        m.height = 10f;
        m.frontColor = Color.valueOf("a0d8ff");
        m.backColor = Color.valueOf("4080c8");
        m.trailColor = Color.valueOf("4080c8");
        m.trailLength = 6;
        m.splashDamage = 90f;
        m.splashDamageRadius = 36f;
        m.hitEffect = Fx.hitBulletColor;
        m.despawnEffect = Fx.hitBulletColor;
        m.status = empJammed != null ? empJammed : StatusEffects.electrified;
        m.statusDuration = 180f;
        return m;
    }

    // ---- Celestial ----
    private static UnitType buildCelestial() {
        UnitType u = new UnitType("celestial");
        u.constructor = UnitEntity::create;
        u.localizedName = "Celestial";
        u.description = "Aerial T5 support DPS. Press [accent]Shift[] to activate Celestial Veil — temporary invulnerability that reflects projectiles and empowers nearby allies.";
        u.health = 15000f;
        u.armor = 10f;
        u.speed = 1.6f;
        u.accel = 0.08f;
        u.drag = 0.05f;
        u.rotateSpeed = 3.5f;
        u.hitSize = 30f;
        u.range = 320f;
        u.maxRange = 320f;
        u.flying = true;
        u.lowAltitude = false;
        u.targetAir = true;
        u.targetGround = true;
        u.engineSize = 5f;
        u.engineOffset = 18f;
        u.itemCapacity = 0;
        u.hidden = false;
        u.useUnitCap = false;
        u.fogRadius = 60f;

        // Twin starlight beams — front, slightly out from center
        Weapon beamL = makeStarlightBeam();
        beamL.x = -10f;
        beamL.y = 18f;
        beamL.mirror = false;

        Weapon beamR = makeStarlightBeam();
        beamR.x = 10f;
        beamR.y = 18f;
        beamR.mirror = false;

        // Orbital strike — single chest-mounted launcher (long reload)
        Weapon orbital = makeOrbitalStrike();
        orbital.x = 0f;
        orbital.y = 0f;
        orbital.mirror = false;

        u.weapons = Seq.with(beamL, beamR, orbital);
        return u;
    }

    private static Weapon makeStarlightBeam() {
        Weapon w = new Weapon();
        w.reload = 8f;
        w.recoil = 0.4f;
        w.shake = 0.3f;
        w.rotate = true;
        w.rotateSpeed = 5f;
        w.mirror = false;
        w.top = true;
        w.shootCone = 8f;
        w.continuous = true;
        w.alwaysContinuous = false;
        w.shootSound = Sounds.beamPlasma;
        w.bullet = makeStarlightLaser();
        return w;
    }

    private static BulletType makeStarlightLaser() {
        ContinuousLaserBulletType b = new ContinuousLaserBulletType(13f);
        b.length = 280f;
        b.width = 7f;
        b.lifetime = 40f;
        b.colors = new Color[]{
            Color.valueOf("a8dfff44"),
            Color.valueOf("c0e8ff"),
            Color.valueOf("ffffff")
        };
        b.pierce = true;
        b.pierceCap = -1;
        b.pierceBuilding = true;
        b.status = starBurn != null ? starBurn : StatusEffects.burning;
        b.statusDuration = 120f;
        b.hitEffect = Fx.hitBulletColor;
        b.despawnEffect = Fx.none;
        return b;
    }

    private static Weapon makeOrbitalStrike() {
        Weapon w = new Weapon();
        w.reload = 720f;
        w.recoil = 0f;
        w.shake = 4f;
        w.rotate = false;
        w.mirror = false;
        w.top = true;
        w.shootCone = 180f;
        w.shootSound = Sounds.shootArtillery;
        w.bullet = makeMeteor();
        return w;
    }

    private static BulletType makeMeteor() {
        ArtilleryBulletType m = new ArtilleryBulletType(4f, 1200f);
        m.lifetime = 110f;
        m.width = 22f;
        m.height = 22f;
        m.splashDamage = 1400f;
        m.splashDamageRadius = 100f;
        m.knockback = 8f;
        m.collidesAir = false;
        m.frontColor = Color.valueOf("ffe080");
        m.backColor = Color.valueOf("ff8030");
        m.trailColor = Color.valueOf("ffaa40");
        m.trailLength = 14;
        m.trailWidth = 4f;
        m.hitEffect = Fx.massiveExplosion;
        m.despawnEffect = Fx.massiveExplosion;
        m.hitShake = 6f;
        return m;
    }

    // ---- Abyssal ----
    private static UnitType buildAbyssal() {
        UnitType u = new UnitType("abyssal");
        u.constructor = UnitEntity::create;
        u.localizedName = "Abyssal";
        u.description = "Naval T5 sea monster. Heals in deep water. On land, suffocates — keep it submerged or watch the timer.";
        u.health = 32000f;
        u.armor = 16f;
        u.speed = 2.8f;
        u.accel = 0.08f;
        u.drag = 0.12f;
        u.rotateSpeed = 1.8f;
        u.hitSize = 40f;
        u.range = 320f;
        u.maxRange = 320f;
        u.flying = false;
        u.naval = true;
        u.canDrown = false;
        u.targetAir = false;
        u.targetGround = true;
        u.faceTarget = true;
        u.engineSize = 0f;
        u.itemCapacity = 0;
        u.hidden = false;
        u.useUnitCap = false;
        u.fogRadius = 50f;

        Weapon cannonL = makeAbyssCannon();
        cannonL.x = -14f;
        cannonL.y = 28f;
        cannonL.mirror = false;

        Weapon cannonR = makeAbyssCannon();
        cannonR.x = 14f;
        cannonR.y = 28f;
        cannonR.mirror = false;

        Weapon lash = makeTentacleLash();
        lash.x = 0f;
        lash.y = 0f;
        lash.mirror = false;

        u.weapons = Seq.with(cannonL, cannonR, lash);
        return u;
    }

    private static Weapon makeAbyssCannon() {
        Weapon w = new Weapon();
        w.reload = 180f;
        w.recoil = 4f;
        w.shake = 3f;
        w.shootCone = 10f;
        w.rotate = false;
        w.mirror = false;
        w.top = true;
        w.shootSound = Sounds.shootArtillery;
        w.bullet = makeAbyssShell();
        return w;
    }

    private static BulletType makeAbyssShell() {
        ArtilleryBulletType b = new ArtilleryBulletType(3.5f, 420f);
        b.lifetime = 92f;
        b.width = 16f;
        b.height = 16f;
        b.splashDamage = 380f;
        b.splashDamageRadius = 56f;
        b.knockback = 4f;
        b.collidesAir = false;
        b.frontColor = Color.valueOf("80e0d0");
        b.backColor = Color.valueOf("1c4060");
        b.trailColor = Color.valueOf("1c4060");
        b.trailLength = 10;
        b.trailWidth = 3f;
        b.hitEffect = Fx.massiveExplosion;
        b.despawnEffect = Fx.massiveExplosion;
        b.hitShake = 3f;
        return b;
    }

    private static Weapon makeTentacleLash() {
        Weapon w = new Weapon();
        w.reload = 30f;
        w.recoil = 0f;
        w.shake = 0.4f;
        w.rotate = true;
        w.rotateSpeed = 4f;
        w.mirror = false;
        w.top = true;
        w.shootCone = 360f;
        w.continuous = true;
        w.shootSound = Sounds.shockwaveTower;
        w.bullet = makeTentacleBullet();
        return w;
    }

    private static BulletType makeTentacleBullet() {
        ContinuousLaserBulletType b = new ContinuousLaserBulletType(14f);
        b.length = 64f;
        b.width = 6f;
        b.lifetime = 60f;
        b.colors = new Color[]{
            Color.valueOf("2e6080"),
            Color.valueOf("60c0c0"),
            Color.valueOf("a0f0e0")
        };
        b.pierce = true;
        b.pierceCap = 3;
        b.pierceBuilding = false;
        b.status = abyssalGrasp != null ? abyssalGrasp : StatusEffects.slow;
        b.statusDuration = 180f;
        b.hitEffect = Fx.hitBulletColor;
        b.despawnEffect = Fx.none;
        return b;
    }

    // ---- Ragnarok ----
    private static UnitType buildRagnarok() {
        UnitType u = new UnitType("ragnarok");
        u.constructor = UnitEntity::create;
        u.localizedName = "Ragnarok";
        u.description = "Apocalypse-class superweapon. SHIFT once: Ragnarok Protocol (Doomsday Mode). SHIFT again: Zantetsuken — wipes every building except cores and kills every unit, including itself.";
        u.health = 38000f;
        u.armor = 45f;
        u.speed = 0.55f;
        u.accel = 0.03f;
        u.drag = 0.2f;
        u.rotateSpeed = 1.0f;
        u.hitSize = 56f;
        u.range = 720f;
        u.maxRange = 720f;
        u.faceTarget = true;
        u.flying = false;
        u.targetAir = true;
        u.targetGround = true;
        u.engineSize = 0f;
        u.lowAltitude = true;
        u.itemCapacity = 0;
        u.hidden = false;
        u.useUnitCap = false;

        Weapon artillery = makeWorldbreakerArtillery();
        artillery.x = 0f;
        artillery.y = 22f;
        artillery.mirror = false;

        Weapon stormL = makePlasmaStorm();
        stormL.x = -22f;
        stormL.y = 6f;
        stormL.mirror = false;

        Weapon stormR = makePlasmaStorm();
        stormR.x = 22f;
        stormR.y = 6f;
        stormR.mirror = false;

        u.weapons = Seq.with(artillery, stormL, stormR);
        return u;
    }

    private static Weapon makeWorldbreakerArtillery() {
        Weapon w = new Weapon();
        w.reload = 240f;
        w.recoil = 6f;
        w.shake = 5f;
        w.shootCone = 6f;
        w.rotate = false;
        w.mirror = false;
        w.top = true;
        w.shootSound = Sounds.shootArtillery;
        w.bullet = makeWorldbreakerShell();
        return w;
    }

    private static BulletType makeWorldbreakerShell() {
        ArtilleryBulletType b = new ArtilleryBulletType(4.2f, 1800f);
        b.lifetime = 170f;
        b.width = 28f;
        b.height = 28f;
        b.splashDamage = 2200f;
        b.splashDamageRadius = 110f;
        b.knockback = 12f;
        b.collidesAir = false;
        b.frontColor = Color.valueOf("ffd060");
        b.backColor = Color.valueOf("ff3010");
        b.trailColor = Color.valueOf("ff6020");
        b.trailLength = 18;
        b.trailWidth = 5f;
        b.hitEffect = Fx.massiveExplosion;
        b.despawnEffect = Fx.massiveExplosion;
        b.hitShake = 8f;
        return b;
    }

    private static Weapon makePlasmaStorm() {
        Weapon w = new Weapon();
        w.reload = 14f;
        w.recoil = 0.4f;
        w.shake = 0.5f;
        w.rotate = true;
        w.rotateSpeed = 6f;
        w.mirror = false;
        w.top = true;
        w.shootCone = 12f;
        w.continuous = true;
        w.alwaysContinuous = false;
        w.shootSound = Sounds.shootBeamPlasma;
        w.bullet = makePlasmaStormBeam();
        return w;
    }

    private static BulletType makePlasmaStormBeam() {
        ContinuousLaserBulletType b = new ContinuousLaserBulletType(22f);
        b.length = 200f;
        b.width = 9f;
        b.lifetime = 40f;
        b.colors = new Color[]{
            Color.valueOf("ff502044"),
            Color.valueOf("ff8030"),
            Color.valueOf("ffe080")
        };
        b.pierce = true;
        b.pierceCap = -1;
        b.pierceBuilding = true;
        b.status = StatusEffects.burning;
        b.statusDuration = 180f;
        b.hitEffect = Fx.hitBulletColor;
        b.despawnEffect = Fx.none;
        return b;
    }
}
