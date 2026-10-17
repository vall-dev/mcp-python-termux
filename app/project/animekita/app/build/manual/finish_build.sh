#!/bin/sh
set -e
P=~/apk/codeassist/projects/project-2
APP=$P/app
W=$APP/build/manual
echo "== 1. dex =="
cat "$W/classpath.txt" | tr ':' '\n' | grep -v "listenablefuture/1.0/listenablefuture-1.0.jar" > "$W/deps_nodupe.txt"
find "$W/classes" -name "*.class" > "$W/d8_inputs.txt"
cat "$W/deps_nodupe.txt" >> "$W/d8_inputs.txt"
rm -rf "$W/dex"; mkdir -p "$W/dex"
d8 --release --min-api 23 --lib "$HOME/apk/codeassist/android.jar" --output "$W/dex" @"$W/d8_inputs.txt" 2> "$W/d8.log"
echo "d8 OK: $(ls $W/dex | wc -l) file"
echo "== 2. package APK =="
APK_UNSIGNED="$W/apk/app-debug-unsigned.apk"
cp "$W/apk/resources.ap_" "$APK_UNSIGNED"
cd "$W"
# add dex files
for d in "$W"/dex/*.dex; do
  aapt add "$APK_UNSIGNED" "classes$(echo "$(basename "$d")" | sed 's/classes//;s/.dex//')" 2>/dev/null || true
done
python3 - "$APK_UNSIGNED" "$W/dex" <<'PYEOF'
import sys, zipfile, os
apk = sys.argv[1]; dexdir = sys.argv[2]
dexs = sorted(f for f in os.listdir(dexdir) if f.endswith('.dex'))
with zipfile.ZipFile(apk, 'a') as z:
    for f in dexs:
        arc = 'classes.dex' if f == 'classes.dex' else f
        z.write(os.path.join(dexdir, f), arc)
print('added dex:', dexs)
PYEOF
echo "== 3. zipalign =="
ALIGNED="$W/apk/app-debug-aligned.apk"
zipalign -f 4 "$APK_UNSIGNED" "$ALIGNED"
echo "== 4. sign =="
SIGNED="$W/apk/app-debug-signed.apk"
apksigner sign --ks "$HOME/apk/codeassist/keystores/release.jks" --ks-pass pass:release --ks-key-alias key0 --key-pass pass:release --out "$SIGNED" "$ALIGNED"
apksigner verify --verbose "$SIGNED" | head -5
echo "== DONE =="
ls -la "$W/apk/"
