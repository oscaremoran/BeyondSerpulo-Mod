#!/bin/bash
# Build the Xorinal Java mod classes (no Gradle).
# Outputs compiled .class files to build/classes/, ready to be packaged into the mod zip.
set -e

MINDUSTRY_JAR="/Users/seanmoran/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/desktop.jar"

if [ ! -f "$MINDUSTRY_JAR" ]; then
    echo "ERROR: Mindustry desktop.jar not found at: $MINDUSTRY_JAR"
    exit 1
fi

rm -rf build/classes
mkdir -p build/classes

find java-src -name "*.java" > build/sources.txt
javac --release 17 -cp "$MINDUSTRY_JAR" -d build/classes @build/sources.txt

echo "Compiled $(find build/classes -name '*.class' | wc -l | tr -d ' ') classes."

# Strip macOS metadata so it doesn't get bundled into the jar.
find . -name ".DS_Store" -not -path "./.git/*" -not -path "./build/*" -delete

# Update the project jar with freshly compiled classes and current asset files.
# `jar uf` only adds/replaces — it doesn't delete entries, so renamed/removed assets can linger.
jar uf Xorinal-Mod.jar -C build/classes . >/dev/null
for d in content bundles sprites maps schematics; do
    [ -d "$d" ] && jar uf Xorinal-Mod.jar "$d" >/dev/null
done
[ -f mod.hjson ] && jar uf Xorinal-Mod.jar mod.hjson >/dev/null
[ -f icon.png ] && jar uf Xorinal-Mod.jar icon.png >/dev/null

# Sync to the Steam Mindustry mods folder (the path the Steam-launched game actually loads).
STEAM_MODS="/Users/seanmoran/Library/Application Support/Steam/steamapps/common/Mindustry/Mindustry.app/Contents/Resources/saves/mods"
if [ -d "$STEAM_MODS" ]; then
    cp Xorinal-Mod.jar "$STEAM_MODS/Xorinal-Mod.zip"
    echo "Synced to Steam: $STEAM_MODS/Xorinal-Mod.zip"
else
    echo "WARNING: Steam mods folder not found at $STEAM_MODS — skipping sync."
fi

# Also sync to the user-data Mindustry mods folder if it exists (non-Steam launches).
USER_MODS="$HOME/Library/Application Support/Mindustry/mods"
if [ -d "$USER_MODS" ]; then
    cp Xorinal-Mod.jar "$USER_MODS/Xorinal-Mod.jar"
    echo "Synced to user mods: $USER_MODS/Xorinal-Mod.jar"
fi
