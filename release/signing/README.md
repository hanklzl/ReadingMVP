# Signing Setup

This file is the signing handoff for Little Mandarin Classics. It is documentation only: do not generate real keys, paste real secrets, or edit Gradle/Xcode settings from this file without a release-manager task.

Repo-specific build root:

```sh
cd /Users/zili/code/android/ReadingMVP/apps/reader
```

Android app module:

```sh
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:bundleRelease
```

`bundleRelease` requires a release signing configuration to be added later.

## Android Upload Keystore

Generate one Android upload keystore outside the repo. Recommended local-only path:

```sh
mkdir -p "$HOME/.littlemandarin/signing/android"
chmod 700 "$HOME/.littlemandarin" "$HOME/.littlemandarin/signing" "$HOME/.littlemandarin/signing/android"

keytool -genkeypair \
  -v \
  -storetype JKS \
  -keystore "$HOME/.littlemandarin/signing/android/littlemandarin-upload.jks" \
  -alias littlemandarin_upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Little Mandarin Classics, OU=Release, O=<LEGAL_ENTITY>, L=<CITY>, ST=<STATE>, C=<COUNTRY_CODE>"

chmod 600 "$HOME/.littlemandarin/signing/android/littlemandarin-upload.jks"
```

Policy:

- Generate the key once, by the release owner, on a trusted machine.
- Keep the keystore outside `/Users/zili/code/android/ReadingMVP`.
- Store the keystore backup in an encrypted password manager or secret vault with restricted access.
- Store passwords separately from the keystore when possible.
- Do not send the keystore or passwords through chat, email, issue trackers, or docs.
- CI should receive the keystore only as an encrypted secret, commonly base64-encoded and decoded into a temporary file at build time.

## Play App Signing

Recommended MVP flow for Google Play:

1. Generate a local upload key as above.
2. Add a Gradle release signing config that signs `:androidApp` release bundles with the upload key.
3. Build the Android App Bundle from `apps/reader`:

   ```sh
   ./gradlew :androidApp:bundleRelease
   ```

4. Upload `androidApp-release.aab` to Play Console.
5. Enroll in Play App Signing and let Google Play generate and protect the app signing key.
6. Keep the local key as the upload key only. Google Play uses the app signing key to sign APKs delivered to users.
7. Record Play Console SHA-1/SHA-256 fingerprints in release-manager checklists. If API providers are added later, register the Google-held app signing key fingerprint, not only the local upload key fingerprint.

Use a separate upload key and app signing key. If the upload key is lost, Play can reset upload-key access; if an app signing key is self-managed and lost, app updates can be permanently blocked. For this repo's first Play-only MVP, prefer Google-generated Play App Signing unless release-manager identifies a multi-store requirement before first release.

## Gradle Signing Template

Do not paste this blindly. This is the intended Kotlin DSL shape for a future edit to `apps/reader/androidApp/build.gradle.kts`.

It reads signing values from environment variables first, then from a local-only properties file. A practical local file name is `apps/reader/local-signing.properties`; it must never be committed.

Sample `apps/reader/local-signing.properties`:

```properties
ANDROID_UPLOAD_STORE_FILE=/Users/zili/.littlemandarin/signing/android/littlemandarin-upload.jks
ANDROID_UPLOAD_STORE_PASSWORD=<store-password>
ANDROID_UPLOAD_KEY_ALIAS=littlemandarin_upload
ANDROID_UPLOAD_KEY_PASSWORD=<key-password>
```

Sample Kotlin DSL documentation snippet:

```kotlin
import java.util.Properties

val localSigningProperties = rootProject.file("local-signing.properties")
val signingProperties = Properties().apply {
    if (localSigningProperties.isFile) {
        localSigningProperties.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? =
    providers.environmentVariable(name).orNull
        ?: signingProperties.getProperty(name)

val androidUploadStoreFile = signingValue("ANDROID_UPLOAD_STORE_FILE")
val androidUploadStorePassword = signingValue("ANDROID_UPLOAD_STORE_PASSWORD")
val androidUploadKeyAlias = signingValue("ANDROID_UPLOAD_KEY_ALIAS")
val androidUploadKeyPassword = signingValue("ANDROID_UPLOAD_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    androidUploadStoreFile,
    androidUploadStorePassword,
    androidUploadKeyAlias,
    androidUploadKeyPassword,
).all { !it.isNullOrBlank() }

android {
    val releaseSigningConfig = if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = file(androidUploadStoreFile!!)
            storePassword = androidUploadStorePassword
            keyAlias = androidUploadKeyAlias
            keyPassword = androidUploadKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            releaseSigningConfig?.let {
                signingConfig = it
            }
        }
    }
}
```

CI should avoid writing `local-signing.properties`. It should provide environment variables and write the decoded keystore to a temporary path referenced by `ANDROID_UPLOAD_STORE_FILE`.

## CI Secret Names

Proposed Android CI secrets:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_STORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON`

`PLAY_SERVICE_ACCOUNT_JSON` is only needed when release-manager adds automated Play upload. It should use the minimum Play Console permissions required for internal testing upload.

Proposed iOS CI secrets, pending full Xcode:

- `IOS_DISTRIBUTION_CERTIFICATE_P12_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `IOS_APP_STORE_PROVISION_PROFILE_BASE64`
- `APPSTORE_CONNECT_API_KEY_ID`
- `APPSTORE_CONNECT_API_ISSUER_ID`
- `APPSTORE_CONNECT_API_KEY_P8_BASE64`

## iOS Signing and TestFlight

Status: pending full Xcode. The iOS app currently remains a skeleton per `AGENTS.md` and the platform design spec, so this is the target flow, not a completed setup.

Bundle ID:

```text
com.littlemandarin.classics
```

Manual signing flow:

1. Enroll or confirm Apple Developer Program access.
2. In Certificates, Identifiers & Profiles, create or confirm the explicit App ID for `com.littlemandarin.classics`.
3. On a trusted Mac, create a Certificate Signing Request with Keychain Access.
4. Create an Apple Distribution certificate from that CSR.
5. Export the certificate/private key as a `.p12` only for secure backup or CI import.
6. Create an App Store provisioning profile for `com.littlemandarin.classics` and the distribution certificate.
7. In Xcode, configure the iOS target signing with the Apple Team, bundle ID, distribution certificate, and App Store provisioning profile.
8. Build the KMP shared framework/XCFramework as required by the iOS project.
9. Archive the iOS app in Xcode and upload to App Store Connect.
10. In App Store Connect, add TestFlight beta details, upload the build, invite internal testers, then request beta review before external testing.

Automatic signing can be used locally once full Xcode is available, but release-manager should still record the Apple Team ID, bundle ID, certificate type, provisioning profile name, and TestFlight group plan.

## Never Commit

Never commit any of the following:

- `*.keystore`
- `*.jks`
- `*.p12`
- `*.mobileprovision`
- password files
- `local-signing.properties`
- any other local signing properties file
- decoded CI secret files
- App Store Connect API private keys
- Play service account JSON

The repo `.gitignore` already blocks the main key/certificate patterns, but do not rely on `.gitignore` as the only safeguard. Run `git status --short` before handoff.

## Release-Manager Handoff

When release-manager prepares release artifacts, it should add checklist items for:

- `release/RELEASE.md`: versionCode/versionName, Android AAB command, iOS build number policy, and artifact paths.
- `release/checklist-android.md`: Play App Signing enrollment status, upload key certificate fingerprint, Play app signing fingerprint, CI secret presence, and Google Play Internal Testing upload.
- `release/checklist-ios.md`: full Xcode readiness, Apple Team ID, bundle ID, distribution certificate, App Store provisioning profile, archive/export steps, App Store Connect API access, and TestFlight internal/external group setup.
- `privacy-compliance`: child privacy, Data Safety, and App Store privacy labels must be complete before any public or external-test release.

Official references:

- Android app signing: https://developer.android.com/studio/publish/app-signing
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Apple TestFlight: https://developer.apple.com/testflight/
- Apple certificate CSR: https://developer.apple.com/help/account/certificates/create-a-certificate-signing-request
- Apple App Store provisioning profile: https://developer.apple.com/help/account/provisioning-profiles/create-an-app-store-provisioning-profile
