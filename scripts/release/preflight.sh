#!/usr/bin/env bash
# Local dry run for the Android release pipeline. It mirrors the key CI steps
# without creating a GitHub Release or pushing gh-pages.
#
# Usage:
#   source /Users/zili/code/android/MusicFreeAndroid/.env.release.local
#   bash scripts/release/preflight.sh v0.1.0
set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 vX.Y.Z" >&2
    exit 1
fi

tag="$1"
expected="${tag#v}"
root=$(git rev-parse --show-toplevel)
reader="$root/apps/reader"
version_file="$reader/version.properties"

sha256_file() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    else
        shasum -a 256 "$1" | awk '{print $1}'
    fi
}

cd "$root"

echo "[dry] Validate version consistency"
actual=$(awk -F= '/^versionName/{print $2}' "$version_file" | tr -d '[:space:]')
[ "$expected" = "$actual" ] || {
    echo "::error::tag $tag vs versionName $actual mismatch" >&2
    exit 1
}
echo "  OK: $tag <-> versionName=$actual"

echo "[dry] Run Android baseline"
(cd "$reader" && ./gradlew :shared:allTests :androidApp:assembleDebug --no-daemon)

echo "[dry] Build signed Release AAB/APK"
release_built=0
if [ -n "${ANDROID_RELEASE_KEYSTORE_PATH:-}" ] && [ -f "${ANDROID_RELEASE_KEYSTORE_PATH}" ]; then
    (cd "$reader" && ./gradlew :androidApp:bundleRelease :androidApp:assembleRelease --no-daemon)
    release_built=1
else
    echo "  WARN: ANDROID_RELEASE_KEYSTORE_PATH not set; skipping signed Release build" >&2
    echo "  CI requires ANDROID_RELEASE_KEYSTORE_BASE64 and ANDROID_RELEASE_* password/alias secrets." >&2
fi

aab_src=""
apk_src=""
mapping_src=""
if [ "$release_built" -eq 1 ]; then
    aab_src=$(find "$reader/androidApp/build/outputs/bundle/release" -type f -name '*-release.aab' | head -n 1 || true)
    apk_src=$(find "$reader/androidApp/build/outputs/apk/release" -type f -name '*-release.apk' | head -n 1 || true)
    mapping_src="$reader/androidApp/build/outputs/mapping/release/mapping.txt"
    [ -f "$aab_src" ] || { echo "::error::Release AAB not found" >&2; exit 1; }
    [ -f "$apk_src" ] || { echo "::error::Release APK not found" >&2; exit 1; }
    [ -f "$mapping_src" ] || { echo "::error::R8 mapping.txt not found" >&2; exit 1; }
fi

echo "[dry] Compute artifact sha256 + size"
aab_name="LittleMandarinClassics-${tag}.aab"
apk_name="LittleMandarinClassics-${tag}.apk"
aab_sha=""; aab_size=""; apk_sha=""; apk_size=""
if [ -n "$aab_src" ]; then
    aab_sha=$(sha256_file "$aab_src")
    aab_size=$(wc -c < "$aab_src" | tr -d ' ')
    echo "  AAB: sha256=$aab_sha size=$aab_size"
fi
if [ -n "$apk_src" ]; then
    apk_sha=$(sha256_file "$apk_src")
    apk_size=$(wc -c < "$apk_src" | tr -d ' ')
    echo "  APK: sha256=$apk_sha size=$apk_size"
fi

echo "[dry] Pack mapping"
mapping_name=""
mapping_sha256=""
if [ -n "$mapping_src" ] && [ -f "$mapping_src" ]; then
    tmp_mapping="/tmp/lmc-mapping-${tag}"
    rm -rf "$tmp_mapping"
    mkdir -p "$tmp_mapping/mapping"
    cp "$mapping_src" "$tmp_mapping/mapping/"
    mapping_name="mapping-${tag}.zip"
    (cd "$tmp_mapping" && zip -9q "$mapping_name" mapping/mapping.txt)
    mapping_sha256=$(sha256_file "$tmp_mapping/$mapping_name")
    echo "  mapping zip: $tmp_mapping/$mapping_name sha256=$mapping_sha256"
else
    echo "  WARN: mapping.txt not present; skipping mapping pack" >&2
fi

echo "[dry] Generate release notes"
prev=$(git describe --tags --abbrev=0 2>/dev/null || git rev-list --max-parents=0 HEAD | tail -1)
notes="/tmp/lmc-release-notes-${tag}.md"
bash scripts/release/generate-notes.sh "$prev" HEAD > "$notes"
echo "  notes -> $notes"

echo "[dry] Prepend CHANGELOG.md"
bash scripts/release/prepend-changelog.sh "$notes" "$tag" --dry-run > "/tmp/lmc-changelog-${tag}.md"
diff CHANGELOG.md "/tmp/lmc-changelog-${tag}.md" || true

echo "[dry] Build version.json"
if [ -n "$aab_sha" ] && [ -n "$apk_sha" ]; then
    out="/tmp/lmc-version-${tag}.json"
    bash scripts/release/build-version-json.sh \
        --version "$expected" \
        --version-code "$(awk -F= '/^versionCode/{print $2}' "$version_file" | tr -d '[:space:]')" \
        --tag "$tag" \
        --artifact "android-aab=${aab_name},${aab_sha},${aab_size}" \
        --artifact "android-apk=${apk_name},${apk_sha},${apk_size}" \
        ${mapping_name:+--mapping-name "$mapping_name"} \
        ${mapping_sha256:+--mapping-sha256 "$mapping_sha256"} \
        --notes "$notes" > "$out"
    jq . "$out"
    echo "  version.json -> $out"
else
    echo "  SKIP: no release artifact hashes to fill; rerun with signing env configured." >&2
fi

echo "Preflight passed."
