package xorinal;

import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.ctype.ContentType;
import mindustry.type.Planet;

public class SerpuloOnVantres {
    public static void init() {
        try {
            Planet vantres = Vars.content.getByName(ContentType.planet, "xorinal-vantres");
            if (vantres == null) {
                Log.err("[Xorinal] SerpuloOnVantres: vantres planet not found");
                return;
            }
            Planet serpulo = Planets.serpulo;
            if (serpulo == null || serpulo.techTree == null) {
                Log.err("[Xorinal] SerpuloOnVantres: serpulo or its techTree missing");
                return;
            }

            vantres.techTree = serpulo.techTree;
            serpulo.techTree.addDatabaseTab(vantres);
            serpulo.techTree.addPlanet(vantres);

            Log.info("[Xorinal] Serpulo tech tree merged onto Vantres");
        } catch (Exception ex) {
            Log.err("[Xorinal] SerpuloOnVantres.init: " + ex);
        }
    }
}
