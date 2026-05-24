# GameVault

Android app for tracking your personal video-game collection — like Steam, but for cataloguing, reviewing and tracking progress rather than buying.

Course project for **ПМП — ФИКТ, summer semester 2026**.

## Features

- **Auth**: Email/Password, Google, Anonymous (Facebook scaffolded, not wired)
- **Library**: list, search, filter by status, master-detail layout on tablets
- **Add/Edit Game**: form with platform, year, genre, status, rating, hours, progress, notes — and a custom cover photo via **device camera**
- **Reviews**: public Firestore-backed feed; write reviews with rating + text
- **Statistics**: total games, completed, hours played; pie chart by status; bar chart by top genres
- **Settings**: language (English / Macedonian / System), theme (Light / Dark / System), notifications toggle
- **Push notifications** via Firebase Cloud Messaging
- **Analytics** events on key flows
- **Internationalization**: full Macedonian + English coverage via XML string resources
- **Responsive layouts**: phone portrait + landscape, tablet portrait + landscape

## Tech stack

- Kotlin, Android XML Views (no Compose), MVVM with ViewModel + StateFlow
- Single-Activity navigation via **Jetpack Navigation Component** (with safe-args)
- **Room** for local persistence (offline-first)
- **Firebase** BoM 33.7.0 — Auth, Firestore, Cloud Messaging, Analytics
- **Material 3** theme, custom dark "gamer vault" palette
- **Coil** for image loading
- **MPAndroidChart** for stats visualization
- Manual ServiceLocator (`AppContainer`) instead of Hilt

## Project structure

```
app/src/main/java/mk/fikt/gamevault/
├── GameVaultApplication.kt
├── di/AppContainer.kt            # ServiceLocator
├── data/
│   ├── auth/                     # AuthRepository, GoogleSignInHelper
│   ├── local/                    # Room: entities, DAOs, database
│   ├── remote/                   # Firestore schema constants
│   ├── messaging/                # FCM service + topic subs
│   ├── analytics/                # AnalyticsLogger
│   ├── repo/                     # GameRepository, ReviewRepository
│   └── model/                    # GameStatus, GamePlatform enums
├── ui/
│   ├── auth/                     # AuthActivity, Login + Register fragments
│   ├── main/                     # MainActivity with bottom nav
│   ├── library/                  # LibraryFragment + adapter + ViewModel
│   ├── addgame/                  # AddEditGameFragment + camera capture
│   ├── details/                  # GameDetailsFragment
│   ├── reviews/                  # Feed + WriteReviewBottomSheet
│   ├── profile/
│   ├── settings/
│   └── stats/
└── util/                         # Prefs, CoverFileProvider
```

## Build & run

Requirements:
- Android Studio Iguana+ (AGP 8.13.x)
- **JDK 17+** (set via `gradle.properties` → `org.gradle.java.home`)
- Android SDK 36

```bash
./gradlew :app:assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## Firebase setup (required to actually run the app)

The app builds and assembles without Firebase, but you cannot sign in / use any cloud feature until `google-services.json` is provided.

1. Create a Firebase project at https://console.firebase.google.com — name it **GameVault**.
2. Add an Android app with package name `mk.fikt.gamevault`.
3. Add the **SHA-1** of your debug keystore in **Project settings → Your apps → Add fingerprint**. To find it:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
4. Download `google-services.json` and place it at `app/google-services.json`.
5. Enable in the Firebase console:
   - **Authentication** → Sign-in method → enable **Anonymous**, **Email/Password**, **Google** (FB optional)
   - **Firestore Database** → create database (start in **test mode**)
   - **Cloud Messaging** → no setup needed; FCM is enabled by default
   - **Analytics** → enabled by default with project
6. In `app/build.gradle.kts`, uncomment the line:
   ```kotlin
   // alias(libs.plugins.google.services)
   ```
7. Rebuild and run.

## Repository hygiene

- `/screenshots/` — screenshots of the app on phone and tablet, portrait and landscape
- `/videos/` — short screencast videos demoing each feature
- `google-services.json` is **not** checked in (gitignored) — each developer adds their own

## License

Educational use only — ПМП ФИКТ summer 2026 project.
