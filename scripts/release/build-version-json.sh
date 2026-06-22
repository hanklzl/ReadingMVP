#!/usr/bin/env bash
# Build release/version.json for GitHub Pages publishing.
#
# Usage:
#   build-version-json.sh \
#     --version 0.1.0 \
#     --version-code 100 \
#     --tag v0.1.0 \
#     --artifact "android-aab=LittleMandarinClassics-v0.1.0.aab,abc...,12345" \
#     --artifact "android-apk=LittleMandarinClassics-v0.1.0.apk,def...,12346" \
#     --mapping-name "mapping-v0.1.0.zip" \
#     --mapping-sha256 "9c4e..." \
#     --notes /tmp/release_notes.md
set -euo pipefail

repo="${GITHUB_REPOSITORY:-hanklzl/ReadingMVP}"
version=""
version_code=""
tag=""
mapping_name=""
mapping_sha256=""
notes_file=""
declare -a artifact_args=()

while [ $# -gt 0 ]; do
    case "$1" in
        --version)         version="$2"; shift 2 ;;
        --version-code)    version_code="$2"; shift 2 ;;
        --tag)             tag="$2"; shift 2 ;;
        --artifact)        artifact_args+=("$2"); shift 2 ;;
        --mapping-name)    mapping_name="$2"; shift 2 ;;
        --mapping-sha256)  mapping_sha256="$2"; shift 2 ;;
        --notes)           notes_file="$2"; shift 2 ;;
        *) echo "Unknown option: $1" >&2; exit 1 ;;
    esac
done

[ -n "$version" ] || { echo "--version required" >&2; exit 1; }
[ -n "$version_code" ] || { echo "--version-code required" >&2; exit 1; }
[ -n "$tag" ] || { echo "--tag required" >&2; exit 1; }
[ "${#artifact_args[@]}" -gt 0 ] || { echo "at least one --artifact required" >&2; exit 1; }

released_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
release_notes_url="https://github.com/${repo}/releases/tag/${tag}"

declare -a change_log
if [ -n "$notes_file" ] && [ -f "$notes_file" ]; then
    while IFS= read -r line; do
        case "$line" in
            "- "*)
                clean="${line:2}"
                change_log+=("$clean")
                if [ "${#change_log[@]}" -ge 8 ]; then break; fi
                ;;
        esac
    done < "$notes_file"
fi

artifacts_json="{}"
for artifact_arg in "${artifact_args[@]}"; do
    key="${artifact_arg%%=*}"
    rest="${artifact_arg#*=}"
    IFS=',' read -r artifact_name sha256 size <<< "$rest"
    gh_url="https://github.com/${repo}/releases/download/${tag}/${artifact_name}"
    download_list=$(jq -n --arg gh "$gh_url" '[$gh]')
    artifact_json=$(jq -n \
        --arg name "$artifact_name" \
        --argjson download "$download_list" \
        --argjson size "$size" \
        --arg sha "$sha256" \
        '{name: $name, download: $download, size: $size, sha256: $sha}')
    artifacts_json=$(jq --arg key "$key" --argjson artifact "$artifact_json" '. + {($key): $artifact}' <<< "$artifacts_json")
done

mapping_json="null"
if [ -n "$mapping_name" ] && [ -n "$mapping_sha256" ]; then
    mapping_url="https://github.com/${repo}/releases/download/${tag}/${mapping_name}"
    mapping_json=$(jq -n \
        --arg name "$mapping_name" \
        --arg url "$mapping_url" \
        --arg sha "$mapping_sha256" \
        '{name: $name, url: $url, sha256: $sha}')
fi

if [ "${#change_log[@]}" -eq 0 ]; then
    change_log_json='[]'
else
    change_log_json=$(printf '%s\n' "${change_log[@]}" | jq -R . | jq -s 'map(select(length > 0))')
fi

jq -n \
    --argjson schemaVersion 1 \
    --arg appId "com.littlemandarin.classics" \
    --arg platform "android" \
    --arg version "$version" \
    --argjson versionCode "$version_code" \
    --arg releasedAt "$released_at" \
    --arg releaseNotesUrl "$release_notes_url" \
    --argjson changeLog "$change_log_json" \
    --argjson artifacts "$artifacts_json" \
    --argjson mapping "$mapping_json" \
    '{
        schemaVersion: $schemaVersion,
        appId: $appId,
        platform: $platform,
        version: $version,
        versionCode: $versionCode,
        releasedAt: $releasedAt,
        releaseNotesUrl: $releaseNotesUrl,
        changeLog: $changeLog,
        artifacts: $artifacts
    } + (if $mapping == null then {} else {mapping: $mapping} end)'
