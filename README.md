# Base — Calm Personal Finance

<div align="center">

[![CI](https://github.com/david-arango/base/actions/workflows/ci.yml/badge.svg)](https://github.com/david-arango/base/actions/workflows/ci.yml)
[![Release Pipeline](https://github.com/david-arango/base/actions/workflows/release.yml/badge.svg)](https://github.com/david-arango/base/actions/workflows/release.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)
![Android SDK](https://img.shields.io/badge/API-24%2B%20%7C%20Target%2036-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Storage](https://img.shields.io/badge/Database-100%25%20Local%20Room-2E7D32)
![Privacy](https://img.shields.io/badge/Network%20Calls-Zero%20(Offline)-success)

**A calm, distraction-free, 100% local personal finance app for Android featuring perpetual rolling balances, automated income engine, flow analytics, and complete data privacy.**

[Website & Live Preview](https://david-arango.github.io/base/) • [Privacy Policy](https://david-arango.github.io/base/privacy.html) • [Play Store Listing Assets](play_store_assets/)

</div>

---

## Overview & Philosophy

Most personal finance apps are built for banks and advertisers — burdened by mandatory cloud accounts, fragile bank-sync logins, noisy push notifications, and arbitrary month-end balance resets.

**Base** takes a tranquil, privacy-first approach:
- **100% Offline**: Your financial data lives entirely inside a local SQLite database on your device via Android Room. Zero third-party analytics, zero server logins, and zero cloud tracking.
- **Perpetual Rolling Balance**: Real financial life does not reset when the calendar flips to the 1st. Base maintains a continuous cash runway so you always know your true liquidity across time.
- **Automated Income Engine**: Configure your expected paycheck amount and cadence (Weekly, Bi-Weekly, Monthly). Base calculates your cash runway and automatically books incoming paychecks without needing bank credentials.
- **Visual Flow Analytics**: Perceptually tuned 24-color flow visualization and ranked category breakdown with hairline segment separators.
- **Full Data Ownership**: Export your complete transaction history to standard CSV at any time, or import existing statements and historical ledger backdated data.

---

## Core Features

| Feature | Description |
|---|---|
| **Perpetual Ledger** | Chronological transaction feed with instant search, category filtering, running balance calculation, and rapid transaction entry/editing. |
| **Income Engine** | Background paycheck scheduler supporting Weekly, Bi-Weekly, and Monthly cadences. Computes remaining runway days until next payday. |
| **Flow Analytics** | Visual spending breakdown for current paycheck or trailing 30 days with distinct category color engine and ranked expenditure cards. |
| **Custom Categories** | Full category customization with 50+ curated Material symbols, custom palette selection, and safe re-indexing of existing records. |
| **CSV Ledger Import** | Parse standard bank CSV exports with automated column matching, date format detection, duplicate prevention, and historical backdating. |
| **Balance Offsets** | Discreet balance adjustment transactions that reconcile your true account balance without artificially skewing expense analytics. |

---

## Architecture & Tech Stack

Base is built following modern Android Architecture Components and Clean Architecture principles:

```
┌─────────────────────────────────────────────────────────────┐
│                       UI Layer                              │
│  MainActivity ──► BaseApp ──► Compose Screens & Modals      │
│  (LedgerScreen, AnalyticsScreen, SettingsScreen)            │
│  Theme: BaseTheme (Calm Light Palette)                      │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow (UI State)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                          │
│  FinanceViewModel                                           │
│  • Manages reactive state via StateFlow                     │
│  • Handles UI events, mutations, and analytics aggregation  │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutines & Flows
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Repository Layer                         │
│  FinanceRepository                                          │
│  • Single source of truth for financial business logic      │
│  • Paycheck scheduling, balance math, CSV parsing & export │
└──────────────────────────────┬──────────────────────────────┘
                               │ DAO Operations
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                              │
│  AppDatabase (Room Database with automated migrations)      │
│  FinanceDao                                                 │
│  Entities: Transaction, IncomeEngine, Category, Offset, Pref│
└─────────────────────────────────────────────────────────────┘
```

- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose with Material 3 design system
- **Architecture**: MVVM with unidirectional data flow (UDF)
- **Local Database**: AndroidX Room 2.7.0 with Kotlin Symbol Processing (KSP)
- **Asynchronous**: Kotlin Coroutines & reactive `StateFlow`
- **Screenshot & UI Testing**: Roborazzi 1.59.0 & Robolectric 4.16.1

---

## Repository Structure

```
.
├── .github/
│   └── workflows/
│       ├── ci.yml                 # Automated unit tests on pull requests & dev push
│       └── release.yml            # Auto-incrementing build, signing, and GitHub release
├── app/
│   ├── build.gradle.kts           # App-level build configuration and dependencies
│   ├── proguard-rules.pro         # ProGuard/R8 optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/arango/base/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/     # Room Database, DAO, Entities, Migrations
│       │   │   │   ├── model/     # Domain models & Category visual configurations
│       │   │   │   └── repository/# FinanceRepository (business logic & CSV engine)
│       │   │   └── ui/
│       │   │       ├── FinanceViewModel.kt
│       │   │       ├── components/ # Modular Compose sheets and dialogs
│       │   │       ├── screens/    # Ledger, Analytics, and Settings screens
│       │   │       └── theme/      # BaseTheme, calm palette, typography
│       │   └── res/                # App launcher icons, logo, strings, themes
│       └── test/                   # Comprehensive unit, dao, repository & screenshot tests
├── docs/                           # GitHub Pages static landing website and privacy policy
├── gradle/
│   ├── libs.versions.toml         # Clean Gradle Version Catalog
│   └── wrapper/                   # Gradle wrapper binaries
├── play_store_assets/             # App icon, feature graphics, and Google Play metadata
├── build.gradle.kts               # Root build configuration
├── settings.gradle.kts            # Project repositories and module settings
├── version.properties             # Single source of truth for semantic build versioning
└── AGENTS.md                      # Workspace development & branching guidelines
```

---

## Development & Build Instructions

### Prerequisites
- Java Development Kit (JDK) 21
- Android SDK Platform 36 (compileSdk 36, minSdk 24)

### Building the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

### Running Tests

```bash
# Execute all unit, ViewModel, DAO, and Roborazzi Compose tests
./gradlew testDebugUnitTest

# Run Roborazzi screenshot verification tests
./gradlew recordRoborazziDebug
```

### Building Signed Release

```bash
# Build release APK and Android App Bundle (.aab)
./gradlew assembleRelease bundleRelease
```

---

## Continuous Integration & Release Pipeline

The repository uses automated GitHub Actions workflows:

1. **`CI` (`.github/workflows/ci.yml`)**:
   - Triggers on every push and pull request targeting the `dev` branch.
   - Sets up JDK 21 and runs `./gradlew testDebugUnitTest`.

2. **`Release Pipeline` (`.github/workflows/release.yml`)**:
   - Triggers on merge to `main`.
   - Reads `version.properties`, increments the build counter, and creates an annotated git tag (e.g. `v1.0.93`).
   - Builds signed release APKs and App Bundle (`.aab`) using repository secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`).
   - Automatically generates a GitHub Release with downloadable APKs, App Bundle, and ProGuard R8 mapping files.

---

## Contributing & Branching Rules

As defined in [`AGENTS.md`](AGENTS.md):
- **Default branch for development**: `dev`
- **Production release branch**: `main`
- All feature additions, bug fixes, and cleanup branch from and target `dev`.
- Direct pushes or merges into `main` are reserved exclusively for approved production releases.

---

## License

Base is developed as an open, private personal finance utility. All rights reserved.
