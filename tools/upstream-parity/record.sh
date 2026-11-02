#!/usr/bin/env bash
# record.sh: compile the staged upstream classes with Recorder.java and record the parity values.
#
#   record.sh <staged dir> <minecraft cache dir> <out dir>
#
# Needs JDK 17 or newer on the PATH (or JAVA_HOME) and, on the first run, internet access to Mojang's piston servers.
# <staged dir> is what stage.py wrote. <minecraft cache dir> receives the Minecraft 1.20.1 server jar and Mojang's
# server mappings, checked against the version manifest's SHA-1; nothing from it is ever committed. See README.md.
set -euo pipefail

mkdir -p "$2" "$3"
STAGED="$(cd "$1" && pwd)"
MC="$(cd "$2" && pwd)"
OUT="$(cd "$3" && pwd)"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
JAVAC="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
JAR="${JAVA_HOME:+$JAVA_HOME/bin/}jar"

cd "$MC"
if [ ! -f server.jar ] || [ ! -f server.txt ]; then
    curl -fsS https://piston-meta.mojang.com/mc/game/version_manifest_v2.json -o manifest.json
    url="$(python3 -I -c 'import json,sys;m=json.load(open("manifest.json"));print([v["url"] for v in m["versions"] if v["id"]=="1.20.1"][0])')"
    curl -fsS "$url" -o 1.20.1.json
    python3 -I -c 'import json;d=json.load(open("1.20.1.json"))["downloads"];print(d["server"]["url"]);print(d["server_mappings"]["url"]);print(d["server"]["sha1"]+"  server.jar")' > urls.txt
    curl -fsS -o server.jar "$(sed -n 1p urls.txt)"
    curl -fsS -o server.txt "$(sed -n 2p urls.txt)"
    sed -n 3p urls.txt | sha1sum -c -
fi
if [ ! -d bundle ]; then
    mkdir bundle
    (cd bundle && "$JAR" xf ../server.jar META-INF/versions META-INF/libraries)
fi

cd "$STAGED"
rm -rf classes
mkdir classes
"$JAVAC" -d classes $(find src -name '*.java')
"$JAVA" -cp classes Recorder "$MC/bundle/META-INF/versions/1.20.1/server-1.20.1.jar" "$MC/server.txt" \
    "$MC/bundle/META-INF/libraries" "$OUT"
echo "recorded into $OUT"
