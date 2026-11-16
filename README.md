# NovaTube

> **Note:** Hi! This app is still under development (work in progress). Some features might still change or have small bugs while I keep working on it.

**NovaTube** (`com.appnova.novatube`) is a simple Android app I made for watching videos without ads and without needing root, Magisk, or LSPosed.

It opens YouTube (`m.youtube.com`) and Anichin (`anichin.moe`) inside a custom Android `WebView` (`NovaYouTubeWebView`), and adds extra features on top like ad blocking, SponsorBlock, background play, and a dark OLED theme.

---

## What Can This App Do?

### 1. Switch Channels (YouTube & Anichin)
- You can switch between **YouTube** (`https://m.youtube.com`) and **Anichin** (`https://anichin.moe/`) right inside the app.
- There is a quick channel button (`YT` / `Anichin`) on the floating Nova bar, and also a **Channel** picker inside **NovaTube Settings**.

### 2. AdBlock (Like Brave Browser) (`BraveShieldsEngine`)
- **Blocks ad links**: Stops known ad and tracker websites (`doubleclick.net`, `googlesyndication.com`, `popads.net`, `adsterra.com`, etc.) before they load.
- **Removes YouTube ad data**: Cleans up `adPlacements`, `playerAds`, `adSlots`, and `adBreakHeartbeatParams` from `JSON.parse`, `Response.prototype.json`, and `window.ytInitialPlayerResponse`.
- **Hides ad boxes & popups**: Hides ad cards on the page, blocks annoying `window.open` popups on Anichin, and auto-skips video ads if any show up.

### 3. SponsorBlock
- Connects to the SponsorBlock API (`https://sponsor.ajay.app/api/skipSegments`).
- Can **Auto Skip** sponsor parts in videos, show a **Manual Skip** button, or just track them.

### 4. Background Play & Picture-in-Picture (PiP)
- **Background Play**: Keeps the audio playing even when you turn off the screen or switch to another app (using `NovaPlaybackService`).
- **PiP Mode**: Shrinks the video into a small floating window when you leave the app.
- **Quick Player Controls**: Change video speed (`0.75x` to `3.0x`) and turn **Loop** on or off from the floating bar.

### 5. Clean UI & Google Login
- **OLED Black Mode**: Makes the background pure black (`#000000`) to look nice and save battery on OLED screens.
- **Hide Shorts**: Option to hide the Shorts shelf if you don't want to see it.
- **Google Sign-In**: You can log in to your Google account directly inside the app.

---

## What I Used to Build It

- **Language**: Kotlin (`2.0.21`)
- **UI**: Jetpack Compose (Material 3)
- **Media Service**: AndroidX Media3 (`1.5.0`)
- **JSON**: `kotlinx.serialization` (`1.7.3`)
- **Android SDK**: `minSdk = 26` (Android 8.0+), `targetSdk = 35`, `compileSdk = 36`

---

## Folder Structure

```text
app/src/main/java/com/appnova/novatube/
├── adblock/
│   └── BraveShieldsEngine.kt       # Blocks ad URLs, cleans ad JSON, and hides ad boxes
├── config/
│   ├── NovaConfig.kt               # App settings & channel list (YouTube / Anichin)
│   └── NovaConfigRepository.kt     # Saves settings on the phone
├── player/
│   ├── NovaPlaybackService.kt      # Keeps audio playing in the background
│   └── NovaPlayerHolder.kt         # Shared player helper
├── sponsorblock/
│   ├── SponsorBlockClient.kt       # Talks to SponsorBlock API
│   └── SponsorSegment.kt           # Sponsor segment data
└── ui/
    ├── MainActivity.kt             # Main app entry & PiP handler
    ├── NovaViewModel.kt            # Holds app state and settings
    ├── components/                 # Glass UI cards and toggles
    └── screens/
        ├── YouTubeCloneScreen.kt   # WebView screen, channel switcher & floating menu
        ├── SettingsTab.kt          # Settings popup page
        ├── PlaybackTab.kt          # Speed, loop, PiP & background play options
        ├── SponsorBlockTab.kt      # SponsorBlock options
        └── UiCustomizationTab.kt   # AdBlock, OLED & UI cleanup options
```

---

## How to Build the APK Yourself

You need **Android Studio** (or **JDK 17**) and **Android SDK 36**.

1. **Download this project**
   ```bash
   git clone https://github.com/<your-username>/<your-repo-name>.git
   cd AppNova
   ```

2. **Run tests**
   - Windows:
     ```powershell
     .\gradlew.bat testDebugUnitTest
     ```
   - macOS / Linux:
     ```bash
     ./gradlew testDebugUnitTest
     ```

3. **Build the Debug APK**
   - Windows:
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - macOS / Linux:
     ```bash
     ./gradlew assembleDebug
     ```

   After it finishes, your APK file will be here:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## Things I Want to Improve Later

- Make the ad-block filters even better as websites change.
- Add more unit tests.
- Add a release signing setup for GitHub Releases.

---

## Disclaimer

This project is made for learning and personal use only. It is **not** an official app from Google, YouTube, or Anichin.
