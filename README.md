# AniTrack - Anime Tracker App

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-green?logo=android" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/Kotlin-1.9.22-purple?logo=kotlin" alt="Kotlin 1.9.22">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material3-blue?logo=jetpackcompose" alt="Jetpack Compose M3">
  <img src="https://img.shields.io/badge/AniList-API-orange" alt="AniList API">
</p>

AniTrack is a beautiful, modern Android application for tracking and discovering anime using the [AniList](https://anilist.co) API. Built with Kotlin, Jetpack Compose, and Material Design 3.

## ✨ Features

### 🏠 Home Screen
- **Trending Anime Carousel**: Discover what's popular right now with horizontal scrolling cards
- **Popular This Season**: See the most watched anime of the current season
- **Welcome Banner**: Personalized greeting with gradient background

### 🔍 Search Screen
- **Real-time Search**: Search anime by title, genre, or keyword
- **Debounced Input**: Optimized API calls with 300ms debounce
- **Rich Results**: Shows score, episodes, status, and genres for each result

### 📖 Detail Screen
- **Full Information**: Complete anime details including synopsis, genres, status
- **Hero Image**: Beautiful cover image with gradient overlay
- **Favorite Toggle**: Add/remove from favorites with animated button
- **Stats Display**: Score, episodes, status in visually appealing badges

### ⭐ Favorites Screen
- **Local Persistence**: Favorites saved using DataStore
- **Quick Actions**: Remove favorites directly from list
- **Empty State**: Friendly message when no favorites exist

### 👤 Profile Screen
- **Settings**: Dark mode, notifications toggles
- **Remote Control**: Enable/disable remote control overlay (for testing)
- **About Section**: App version, rate app, share, privacy policy

## 🎮 Remote Control System

AniTrack includes a complete remote control system for automated testing:

### Components:
| Component | Description |
|-----------|-------------|
| `WebSocketClient` | Connects to WebSocket server with auto-reconnect |
| `RemoteControlOverlay` | Floating UI that can be dragged around screen |
| `ScreenshotCapture` | Captures screenshots via MediaProjection API |
| `CommandExecutor` | Executes tap, swipe, navigation commands |
| `LogcatCollector` | Collects and streams device logs |

### Supported Commands:
- **TAP** - Simulate touch at coordinates
- **SWIPE** - Simulate swipe gesture
- **BACK/HOME/RECENT** - Navigation actions
- **TEXT** - Input text or paste from clipboard
- **SCREENSHOT** - Capture screen as base64 image
- **LOGCAT** - Start/stop/get device logs
- **GET_UI_HIERARCHY** - Get current view hierarchy XML

## 🛠️ Tech Stack

| Technology | Version |
|------------|---------|
| Kotlin | 1.9.22 |
| Jetpack Compose | 2024.01.00 |
| Material Design 3 | 1.11.0 |
| Hilt/Dagger | Latest |
| Retrofit + OkHttp | 2.9.2 / 4.12.0 |
| Moshi | 1.15.1 |
| Coil | 2.5.0 |
| Room | 2.6.1 |
| DataStore | 1.0.0 |
| Coroutines | 1.7.3 |

## 📱 Requirements

- **minSdk**: 26 (Android 8.0 Oreo)
- **targetSdk**: 34 (Android 14)
- **compileSdk**: 34

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17+
- Android SDK 34+

### Build Instructions

1. Clone the repository:
```bash
git clone https://github.com/testplay-byte/AGENT_ACCESS.git
cd android-project
```

2. Sync Gradle:
```bash
./gradlew build
```

3. Build Debug APK:
```bash
./gradlew assembleDebug
```

4. Install on device/emulator:
```bash
./gradlew installDebug
```

## 📁 Project Structure

```
android-project/
├── app/
│   └── src/main/
│       ├── java/com/anitrack/app/
│       │   ├── MainActivity.kt              # Main activity entry point
│       │   ├── AniTrackApplication.kt       # Application class
│       │   ├── ui/
│       │   │   ├── theme/                  # Theme, colors, typography
│       │   │   ├── home/                   # Home screen & ViewModel
│       │   │   ├── search/                 # Search screen & ViewModel
│       │   │   ├── detail/                 # Detail screen & ViewModel
│       │   │   ├── favorites/              # Favorites screen & ViewModel
│       │   │   ├── profile/                # Profile screen & ViewModel
│       │   │   ├── components/             # Reusable components
│       │   │   └── navigation/             # Navigation setup
│       │   ├── data/
│       │   │   ├── api/                   # AniList GraphQL API
│       │   │   └── repository/            # Data repository
│       │   └── remotecontrol/             # Remote control system
│       └── res/                           # Resources
├── .github/workflows/                     # CI/CD pipeline
└── gradle/                                # Build configuration
```

## 🎨 UI Design

The app features a modern, anime-themed design with:
- **Purple/Pink Gradient Theme**: Perfect for anime aesthetics
- **Card-based Layouts**: Clean, material design cards
- **Smooth Animations**: Spring animations and transitions
- **Shimmer Loading Effects**: Beautiful placeholder loading states
- **Responsive Layout**: Adapts to different screen sizes

## 📡 AniList Integration

The app uses the [AniList GraphQL API](https://graphql.anilist.info) to fetch anime data:

### Example Queries:
```graphql
# Trending Anime
query {
  Page(page: 1, perPage: 20) {
    media(type: ANIME, sort: TRENDING_DESC) {
      id
      title { romaji english }
      coverImage { extraLarge }
      episodes
      averageScore
    }
  }
}
```

## 🔄 CI/CD Pipeline

GitHub Actions automatically builds debug APKs on push to main branch:
- Builds debug APK
- Uploads artifact (retained for 30 days)
- Creates pre-release with downloadable APK

## 📄 License

This project is created for educational purposes.

## 🙏 Acknowledgments

- [AniList](https://anilist.co) for providing the amazing anime database API
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern Android UI toolkit
- [Material Design 3](https://m3.material.io/) for beautiful design guidelines

---

Made with ❤️ by AniTrack Team
