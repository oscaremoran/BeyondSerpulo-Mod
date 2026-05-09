package xorinal;

import arc.Events;
import arc.graphics.Color;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.graphics.g3d.GenericMesh;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.NoiseMesh;
import mindustry.type.Planet;

public class PlanetMeshes {
    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static void setup() {
        try {
            Planet tetra = Vars.content.getByName(ContentType.planet, "xorinal-tetra");
            Planet xorinal = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");
            Planet vantres = Vars.content.getByName(ContentType.planet, "xorinal-vantres");

            if (vantres != null) {
                // Blackened husk: deep void blacks through char/ash up into glowing crimson magma fissures and bright white-hot lava peaks.
                // Higher mag and contrast give craggy, cracked terrain instead of smooth gradient.
                vantres.meshLoader = () -> makeNoise(vantres, 27, 9, 7, 0.62f, 1.9f, 0.95f,
                    new String[]{"000000","050202","0a0405","140808","2a0c0a","6a1810","d83a18","ffd070"});
                // Layered atmosphere: dark smoke at low altitude, bright magma streaks mid, drifting embers high.
                vantres.cloudMeshLoader = () -> clouds(vantres,
                    new CloudLayer(2,  0.03f, 0.205f, 5, "1a08087a", 2, 0.55f, 1.6f, 0.62f),
                    new CloudLayer(7,  0.06f, 0.215f, 6, "ff3a10b0", 3, 0.65f, 2.2f, 0.84f),
                    new CloudLayer(13, 0.025f, 0.230f, 5, "ff7a3070", 2, 0.55f, 2.6f, 0.74f),
                    new CloudLayer(19, 0.02f, 0.250f, 4, "ffb04050", 2, 0.5f,  3.2f, 0.82f),
                    new CloudLayer(29, 0.012f,0.270f, 4, "ffd0701f", 2, 0.5f,  3.8f, 0.80f));
            }
            if (tetra != null) {
                tetra.meshLoader = () -> makeNoise(tetra, 7, 8, 7, 0.55f, 1.5f, 0.55f,
                    new String[]{"0e2440","1a3d63","265e85","4ea8d4","8ec8e8","b8dcef","e6f4ff","ffffff"});
                tetra.cloudMeshLoader = () -> clouds(tetra,
                    new CloudLayer(2, 0.20f, 0.205f, 6, "ffffff90", 3, 0.55f, 1.0f, 0.60f),
                    new CloudLayer(5, 0.12f, 0.225f, 6, "cde9ff70", 3, 0.6f, 1.5f, 0.62f),
                    new CloudLayer(11, 0.06f, 0.245f, 5, "e6f4ff40", 2, 0.5f, 2.2f, 0.7f));
            }
            if (xorinal != null) {
                try { xorinal.generator = new XorinalPlanetGenerator(); }
                catch (Throwable t) { Log.err("[Xorinal] generator install failed: " + t); }
                // Jungle gradient: deep canopy → vivid green → highland violet-corruption peaks.
                xorinal.meshLoader = () -> makeNoise(xorinal, 13, 8, 7, 0.55f, 1.6f, 0.65f,
                    new String[]{"08180c","112a14","1d4220","326b2c","58a83c","8acf4a","a070c8","5e2a78"});
                xorinal.cloudMeshLoader = () -> clouds(xorinal,
                    // surface-hugging spore patches: low altitude, sparse threshold = blotchy purple regions on the sphere itself
                    new CloudLayer(21, 0.015f, 0.202f, 6, "8a3ac0a8", 3, 0.55f, 1.3f, 0.78f),
                    new CloudLayer(25, 0.01f,  0.203f, 5, "c060e090", 3, 0.55f, 1.6f, 0.82f),
                    // Bloomheart-style void splotch: very rare, very dark
                    new CloudLayer(33, 0.008f, 0.204f, 5, "100018d0", 2, 0.5f,  2.0f, 0.88f),
                    // jungle haze
                    new CloudLayer(4, 0.18f, 0.215f, 6, "9fd47880", 3, 0.55f, 1.1f, 0.58f),
                    new CloudLayer(9, 0.10f, 0.230f, 6, "c8e6a060", 3, 0.6f, 1.6f, 0.62f),
                    new CloudLayer(17, 0.05f, 0.250f, 5, "e0f0a830", 2, 0.5f, 2.3f, 0.7f));
            }
        } catch (Exception e) {
            Log.err("[Xorinal] planet mesh setup failed: " + e);
            try {
                Planet tetra = Vars.content.getByName(ContentType.planet, "xorinal-tetra");
                Planet xorinal = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");
                if (tetra != null) tetra.meshLoader = () -> new HexMesh(tetra, 6);
                if (xorinal != null) xorinal.meshLoader = () -> new HexMesh(xorinal, 6);
                Planet vantres = Vars.content.getByName(ContentType.planet, "xorinal-vantres");
                if (vantres != null) vantres.meshLoader = () -> new HexMesh(vantres, 6);
            } catch (Exception e2) {}
        }
    }

    private static GenericMesh makeNoise(Planet planet, int seed, int divisions, int octaves, float persistence, float scale, float mag, String[] hex) {
        Color[] c = new Color[8];
        for (int i = 0; i < 8; i++) {
            String h = i < hex.length ? hex[i] : hex[hex.length - 1];
            c[i] = Color.valueOf(h);
        }
        // The 8-color NoiseMesh constructor exists in Mindustry's runtime jar but isn't in the
        // classpath jar we compile against — call it via reflection so we get gradient terrain.
        try {
            for (var ctor : NoiseMesh.class.getConstructors()) {
                if (ctor.getParameterCount() == 16) {
                    return (GenericMesh) ctor.newInstance(
                        planet, seed, divisions, 0.6f, octaves, persistence, scale, mag,
                        c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]);
                }
            }
        } catch (Exception ex) {
            Log.err("[Xorinal] reflective NoiseMesh failed: " + ex);
        }
        return new NoiseMesh(planet, seed, divisions, c[3], 0.6f, octaves, persistence, scale, mag);
    }

    private static class CloudLayer {
        final int seed; final float speed, radius; final int divisions; final String color;
        final int octaves; final float persistence, scale, threshold;
        CloudLayer(int seed, float speed, float radius, int divisions, String color, int octaves, float persistence, float scale, float threshold) {
            this.seed = seed; this.speed = speed; this.radius = radius; this.divisions = divisions;
            this.color = color; this.octaves = octaves; this.persistence = persistence;
            this.scale = scale; this.threshold = threshold;
        }
    }

    private static MultiMesh clouds(Planet planet, CloudLayer... layers) {
        GenericMesh[] meshes = new GenericMesh[layers.length];
        for (int i = 0; i < layers.length; i++) {
            CloudLayer L = layers[i];
            meshes[i] = new HexSkyMesh(planet, L.seed, L.speed, L.radius, L.divisions,
                Color.valueOf(L.color), L.octaves, L.persistence, L.scale, L.threshold);
        }
        return new MultiMesh(meshes);
    }
}
