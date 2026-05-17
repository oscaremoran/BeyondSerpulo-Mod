package beyondserpulo;

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
                Log.err("[BeyondSerpulo] SerpuloOnVantres: vantres planet not found");
                return;
            }
            Planet serpulo = Planets.serpulo;
            if (serpulo == null || serpulo.techTree == null) {
                Log.err("[BeyondSerpulo] SerpuloOnVantres: serpulo or its techTree missing");
                return;
            }

            vantres.techTree = serpulo.techTree;
            serpulo.techTree.addDatabaseTab(vantres);
            serpulo.techTree.addPlanet(vantres);

            Log.info("[BeyondSerpulo] Serpulo tech tree merged onto Vantres");
        } catch (Exception ex) {
            Log.err("[BeyondSerpulo] SerpuloOnVantres.init: " + ex);
        }
    }
}
