This is a Kotlin Multiplatform project targeting Android, Server.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

* `/server` is for the Ktor server application.

* `/shared` is for the code that will be shared between all targets in the project.
  The most important subfolder is `commonMain`. If preferred, you can add code to the platform-specific folders here
  too.

## Google Sign-In Setup

**IMPORTANT**: The current `google-services.json` file contains placeholder values. To enable Google Sign-In:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the Google Sign-In API
4. Set up OAuth consent screen
5. Create OAuth client ID credentials:
   - **Android client**: For the Android app (package: `org.jetbrains.demo`)
   - **Web client**: For server-side token verification
6. Download the real `google-services.json` file and replace the placeholder file in `/composeApp/`
7. Update the `WEB_CLIENT_ID` constants in `MainActivity.kt` and `GoogleAuthManager.kt` with your actual web client ID

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…