# NovaTube

> **Status: Active Development (Work in Progress)**
> NovaTube is currently under active development. Features, internal APIs, and UI components are continuously evolving, and occasional bugs or breaking changes may occur between builds.

**NovaTube** (`com.appnova.novatube`) is a standalone, non-root Android YouTube client designed to deliver a fast, ad-free, and customizable viewing experience without requiring Magisk, LSPosed, or system-level modifications.

Built with **Kotlin** and **Jetpack Compose**, NovaTube wraps YouTube's mobile web experience (`m.youtube.com`) inside a custom-hooked `WebView` (`NovaYouTubeWebView`) paired with a foreground Media3 service, a **Brave Shields-inspired 3-layer ad-blocking engine**, **SponsorBlock** integration, and a floating **Liquid Glass** control HUD.

---

## Key Features

### 1. Brave Shields-Inspired 3-Layer AdBlock (`BraveShieldsEngine`)
- **Network-Level Request Filtering**: Intercepts and drops requests to known ad/tracking domains (`doubleclick.net`, `googlesyndication.com`, `googleadservices.com`, `imasdk.googleapis.com`, etc.) and YouTube ad endpoints (`/pagead/`, `/api/stats/ads`, `/get_midroll_info`, `/youtubei/v1/player/ad_break`) before they are downloaded.
- **JSON-Prune Scriptlet**: Hooks `JSON.parse`, `Response.prototype.json`, and `window.ytInitialPlayerResponse` to strip `adPlacements`, `playerAds`, `adSlots`, and `adBreakHeartbeatParams` before the player can schedule pre-roll or mid-roll ads.
- **Cosmetic DOM Filtering & Fallback Skipper**: Hides promoted feed cards, companion ads, and banner overlays via injected CSS, with an automated fallback skipper for any residual player ads.

### 2. SponsorBlock Integration
- Queries the official SponsorBlock API (`https://sponsor.ajay.app/api/skipSegments`) for the active video.
- Supports configurable categories and three skip modes:
  - **Auto Skip**: Automatically seeks past sponsored segments with optional HUD toast notifications.
  - **Manual Skip Pill**: Displays a floating skip button when inside a segment.
  - **Markers Only**: Tracks segments without interrupting playback.

### 3. Enhanced Playback & Background Support
- **Background Playback**: Overrides `onWindowVisibilityChanged` and `visibilitychange` events and maintains a Media3 foreground service (`NovaPlaybackService`) so audio continues playing when the screen is off or when switching apps.
- **Picture-in-Picture (PiP)**: Supports automatic PiP on leaving the app (`onUserLeaveHint`) as well as a quick PiP trigger in the floating control pill.
- **Quick Controls**: Cycle playback speeds (`0.75x` – `3.0x`) and toggle video looping directly from the auto-hiding floating pill.

### 4. UI Customization & Native Google Sign-In
- **OLED Pure Black Mode**: Forces `#000000` surfaces across YouTube feeds and app shells.
- **Liquid Glass HUD**: Translucent auto-hiding floating control pill and toast notifications.
- **Feed Cleanup**: Option to hide Shorts shelves and promotional clutter.
- **Account Support**: Allows full Google OAuth sign-in directly inside the app without hijacking auth sub-frames or triggering unintended feed redirects.

---

## Tech Stack

- **Language**: Kotlin `2.0.21` (Java 17 target)
- **UI**: Jetpack Compose (Material 3, Compose BOM `2024.10.01`)
- **Media & Background**: AndroidX Media3 (`1.5.0`)
- **Serialization**: `kotlinx.serialization` (`1.7.3`)
- **SDK Targets**: `minSdk = 26` (Android 8.0), `targetSdk = 35`, `compileSdk = 36`
- **Build System**: Android Gradle Plugin `8.7.3`

---

## Project Structure

```text
app/src/main/java/com/appnova/novatube/
├── adblock/
│   └── BraveShieldsEngine.kt       # 3-layer network, JSON-prune & cosmetic ad blocker
├── config/
│   ├── NovaConfig.kt               # Immutable app, playback, SponsorBlock & UI config models
│   └── NovaConfigRepository.kt     # Persistent configuration storage
├── player/
│   ├── NovaPlaybackService.kt      # Media3 foreground service for background audio
│   └── NovaPlayerHolder.kt         # Shared player state holder
├── sponsorblock/
│   ├── SponsorBlockClient.kt       # SponsorBlock API client
│   └── SponsorSegment.kt           # Segment & category models
└── ui/
    ├── MainActivity.kt             # Entry point & PiP lifecycle handling
    ├── NovaViewModel.kt            # State management & SponsorBlock coordination
    ├── components/                 # Reusable Glass UI components
    └── screens/
        ├── YouTubeCloneScreen.kt   # Core hooked WebView, fullscreen & floating HUD
        ├── SettingsTab.kt          # Settings sheet container
        ├── PlaybackTab.kt          # Speed, loop, PiP & background play settings
        ├── SponsorBlockTab.kt      # SponsorBlock categories & skip mode settings
        └── UiCustomizationTab.kt   # AdBlock, OLED, Liquid Glass & cleanup toggles
```

---

## Building from Source

### Prerequisites
- **Android Studio** (Ladybug or newer recommended) or **JDK 17**
- **Android SDK** with Platform `36` installed

### Build Commands

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   cd AppNova
   ```

2. **Run unit tests**
   - Windows:
     ```powershell
     .\gradlew.bat testDebugUnitTest
     ```
   - macOS / Linux:
     ```bash
     ./gradlew testDebugUnitTest
     ```

3. **Build Debug APK**
   - Windows:
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - macOS / Linux:
     ```bash
     ./gradlew assembleDebug
     ```

   The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Roadmap & Development Notes

As this project is still in **active development**, planned improvements include:
- Additional cosmetic filter updates as YouTube's mobile web DOM evolves.
- Expanded unit and instrumentation test coverage.
- Release signing configuration and automated CI builds.
- Further refinements to gesture controls and custom tab management.

---

## Disclaimer

NovaTube is an independent open-source project created for educational and personal use. It is **not affiliated with, endorsed by, or associated with Google LLC or YouTube**. All trademarks, service marks, and brand names belong to their respective owners.
