package xorinal;

import arc.math.geom.Vec3;
import arc.util.noise.Ridged;
import arc.util.noise.Simplex;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.ctype.ContentType;
import mindustry.maps.planet.SerpuloPlanetGenerator;
import mindustry.type.Planet;
import mindustry.type.Sector;
import mindustry.world.Block;
import mindustry.world.TileGen;

public class XorinalPlanetGenerator extends SerpuloPlanetGenerator {
    public static final int BLOOMHEART_SECTOR = 22;
    private static final int[] PURPLE_SECTORS = {3, 7, 12, 18};

    @Override
    public void genTile(Vec3 position, TileGen tile) {
        tile.floor = pickFloor(position);
        tile.block = tile.floor.asFloor().wall;
        if (Ridged.noise3d(2, position.x, position.y, position.z, 22) > 0.31) {
            tile.block = Blocks.air;
        }
    }

    private Block pickFloor(Vec3 pos) {
        Planet planet = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");
        Sector closest = null;
        if (planet != null) {
            float best = Float.MAX_VALUE;
            for (Sector s : planet.sectors) {
                float d = s.tile.v.dst2(pos);
                if (d < best) { best = d; closest = s; }
            }
        }
        int sid = closest == null ? -1 : closest.id;

        if (sid == BLOOMHEART_SECTOR) {
            double splotch = Simplex.noise3d(7, 4, 0.55, 1.7, pos.x, pos.y, pos.z);
            if (splotch > 0.60) return Blocks.sporeMoss;
            if (splotch > 0.48) return Blocks.charr;
            return Blocks.darksand;
        }

        for (int s : PURPLE_SECTORS) {
            if (sid == s) {
                double n = Simplex.noise3d(13, 4, 0.5, 1.5, pos.x, pos.y, pos.z);
                if (n > 0.55) return Blocks.shale;
                if (n > 0.30) return Blocks.sporeMoss;
                return Blocks.moss;
            }
        }

        double jungle = Simplex.noise3d(11, 5, 0.5, 1.6, pos.x, pos.y, pos.z);
        double blotch = Ridged.noise3d(3, pos.x * 1.4f, pos.y * 1.4f, pos.z * 1.4f, 19);
        if (blotch > 0.55) return Blocks.sporeMoss;
        if (jungle > 0.62) return Blocks.shale;
        if (jungle > 0.45) return Blocks.moss;
        if (jungle > 0.25) return Blocks.grass;
        return Blocks.stone;
    }
}
