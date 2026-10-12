# OpenClaw Companion — Android Starter Project

A native Kotlin Android starter app for Android 8.0+ (API 26), styled in dark black/red. It provides a chat UI, Hindi voice input, local phone actions, web search, and optional direct Gemini API chat. It does not require a custom backend.

## Features
- Dark black/red mobile UI
- Local commands: open YouTube, open Android Settings, Google web search, show date/time, share a note
- Optional Gemini API integration
- Hindi speech-to-text through Android's installed speech recognizer
- API key stored in app-private SharedPreferences
- No remote custom server

## Important limitations
- This is a starter implementation, not a full clone of the desktop OpenClaw runtime.
- Android restricts background execution, device control, and access to other apps. Actions require user interaction and system permission where applicable.
- The app does not run a local large language model by default. Without an API key it uses basic local command responses, not full generative AI.
- A free API tier, model name, rate limits, and availability depend on Google's current terms and account/region.
- For production use, avoid embedding a long-lived API key in a distributable app. Anyone who can inspect the app or device may be able to extract it. Consider a user-provided key or a trusted proxy you control.
- This project has not been compiled or tested on a physical device in this environment.

## Build APK in Android Studio
1. Install Android Studio and Android SDK Platform 35.
2. Open this folder as a project.
3. Let Gradle sync and download the Android Gradle Plugin/Kotlin dependencies.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Command line (after Android SDK and Gradle are configured):
```bash
gradle assembleDebug
```

## Get a Gemini API key
Create/manage a key in Google AI Studio: https://aistudio.google.com/app/apikey
Paste it into the app via the ⚙ button. The app sends prompts directly to Google's Gemini API over HTTPS.

## GitHub Actions build
The workflow `.github/workflows/android.yml` builds a debug APK when pushed to GitHub and publishes it as a workflow artifact. Download the artifact from the Actions run.
