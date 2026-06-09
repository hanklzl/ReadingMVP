# Little Mandarin Classics iOS

SwiftUI skeleton for the iOS reader app. It imports the Kotlin Multiplatform
`shared` framework and maps the shared `app_name` resource key through the iOS
String Catalog.

The Xcode project references:

- `../shared/build/XCFrameworks/debug/shared.xcframework`
- Gradle task `:shared:assembleDebugXCFramework` in a pre-build run script

Do not build this target until a full Xcode installation is available.
