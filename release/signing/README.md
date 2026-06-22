# Signing Setup

Status: Android release signing is wired into `apps/reader/androidApp/build.gradle.kts` and `.github/workflows/android-release.yml`.

The Android release path intentionally uses the same signing variable names as MusicFreeAndroid, so the same keystore and passwords can be injected without a second Gradle signing scheme. Do not commit the keystore, decoded key files, passwords, or local env files.

## Local Signing

From this repository root:

```sh
source /Users/zili/code/android/MusicFreeAndroid/.env.release.local
bash scripts/release/preflight.sh v0.1.0
```

The sourced file must export:

```sh
ANDROID_RELEASE_KEYSTORE_PATH=/absolute/path/to/release.jks
ANDROID_RELEASE_STORE_PASSWORD=...
ANDROID_RELEASE_KEY_ALIAS=...
ANDROID_RELEASE_KEY_PASSWORD=...
```

The release Gradle tasks fail fast if any value is missing:

```sh
cd apps/reader
./gradlew :androidApp:bundleRelease :androidApp:assembleRelease --no-daemon
```

Expected Android outputs:

- `apps/reader/androidApp/build/outputs/bundle/release/*-release.aab`
- `apps/reader/androidApp/build/outputs/apk/release/*-release.apk`
- `apps/reader/androidApp/build/outputs/mapping/release/mapping.txt`

## CI Secrets

Configure these in the GitHub Environment named `release`. To use the same signing identity as MusicFreeAndroid, copy the same secret values into this repository's `release` environment:

| Secret | Purpose |
|---|---|
| `ANDROID_RELEASE_KEYSTORE_BASE64` | Base64-encoded release keystore bytes |
| `ANDROID_RELEASE_STORE_PASSWORD` | Keystore password |
| `ANDROID_RELEASE_KEY_ALIAS` | Key alias |
| `ANDROID_RELEASE_KEY_PASSWORD` | Key password |
| `ANTHROPIC_API_KEY` | Optional release-note summary; failures do not block release |

CI decodes `ANDROID_RELEASE_KEYSTORE_BASE64` into `$RUNNER_TEMP/release.jks`, sets `ANDROID_RELEASE_KEYSTORE_PATH` to that temporary file, and runs the signed release build. No decoded keystore is committed or uploaded except through the signed build artifacts.

## Play App Signing

For Google Play Internal Testing, upload the signed AAB produced by the release workflow. If this keystore is used as the Play upload key, enroll in Play App Signing and let Google hold the app signing key. Record the Play app signing and upload-key SHA-1/SHA-256 fingerprints in the Android release checklist before external testing.

## iOS Signing

Status: pending full Xcode. The iOS app remains a skeleton per `AGENTS.md`, so no iOS archive/signing automation is active yet.

Target bundle ID:

```text
com.littlemandarin.classics
```

Future iOS release work must use App Store distribution certificates, provisioning profiles, and App Store Connect API credentials through local keychain/CI secrets only. Do not commit `.p12`, `.mobileprovision`, App Store Connect private keys, or derived archive credentials.

## Never Commit

Never commit any of the following:

- `*.keystore`
- `*.jks`
- `*.p12`
- `*.mobileprovision`
- `.env.release.local`
- `.env.*.local`
- `local-signing.properties`
- decoded CI secret files
- App Store Connect API private keys
- Play service account JSON

Run `git status --short` before handoff and confirm only source, docs, scripts, and workflow files are staged or changed.
