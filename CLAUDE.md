# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PaNovel is an Android novel reader app being revived. Goals: **run again, easy to maintain, live longer**.

It supports local TXT/EPUB files, WebDAV backup, reading progress sync, and a pluggable site scraper system with 24 active scrapers (the original 68 were removed because the sites died; all current scrapers are new implementations).

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Compile scraper module (scraper DSL)
./gradlew scraper:compileKotlin

# Compile local module (TXT/EPUB)
./gradlew local:compileKotlin

# Run scraper unit tests (no network required)
./gradlew scraper:test

# Run site integration tests (requires internet, hits live sites)
./gradlew scraper:siteTest

# Run all scraper tests including site integration
./gradlew scraper:test -Dtest.integration=true
```

Requires JDK 21. Uses Gradle 8.7, AGP 8.3.2, Kotlin 1.9.22.

## Architecture

**MVP pattern** with `DataManager` singleton coordinating:
- `AppDatabaseManager` – Room database (2.6.1, uses KSP)
- `ApiManager` – Novel website context/scraping
- `CookieManager` – Cookie persistence
- `CacheManager` – Content caching (IronDB + kotlinx-serialization)
- `LocalManager` – Local file novel support
- `DownloadManager` – Download management

**Dependency management:**
- `AppContainer` (in `App.kt`) holds app-scoped dependencies
- `PrefContext` provides application context to the settings system
- No global `App.context` — context flows through `PrefContext.appContext` or is passed explicitly
- `DataManager` stores its own `appContext` from initialization

Base classes: `MvpView` interface + `Presenter<T : MvpView>` abstract class. Presenters use `CoroutineScope(Dispatchers.Main + SupervisorJob())` for async work.

## Module Structure

| Module | Type | Purpose |
|--------|------|---------|
| app | Android | Main application (activities, presenters, fragments) |
| scraper | Java | Novel website scrapers (JSoup + Rhino JS parsing) |
| core | Java | Shared utilities (JSON, regex, SSL, jsoup helpers) |
| IronDB | Java | File-based NoSQL key-value store (kotlinx-serialization) |
| rhino | Java | Rhino JavaScript engine wrapper |
| local | Java | Local file support (TXT, EPUB via epub4j-core) |
| pager | Android | Pagination library |
| reader | Android | Novel reader UI |
| filepicker | Android | File picker UI |

## Key Patterns

- Novel site scrapers extend `DslJsoupNovelContext` in `scraper/src/main/java/cc/aoeiuv020/panovel/api/site/`
- Add new scrapers by copying `Deqixs.kt` as a starting point
- The `rhino` module wraps Mozilla Rhino for scrapers that need to evaluate JS (e.g., sites with JS-based content decryption); use regex/`pick` first, Rhino when that's insufficient
- Dependency versions are centralized in `version.properties`
- App package structure is feature-based: `cc.aoeiuv020.panovel.{bookshelf,download,search,settings,...}`
- Room database schemas are exported to `app/schemas/` for migration validation
- ViewBinding is used for view access (no kotlin-android-extensions)
- Logging: Timber in Android modules, SLF4J in pure-Java modules
- Async: Kotlin Coroutines (scope in Presenter base class, lifecycleScope in Activities)
- Dialogs: AlertDialog.Builder (no DSL wrappers)
- Navigation: standard Intent with putExtra
- Settings: `Pref` interface + SharedPreferences delegates (`delegate.kt`), context from `PrefContext`
- Serialization: kotlinx-serialization throughout (no GSON)
- Activity results: `ActivityResultContracts` (no deprecated `startActivityForResult`)
- Preferences UI: AndroidX `PreferenceFragmentCompat` (no deprecated `PreferenceFragment`)

## Tech Stack

- Kotlin 1.9.22, Java 17 target, Gradle 8.7, AGP 8.3.2
- Target/Compile SDK 34, Min SDK 16, Multidex enabled
- AndroidX, Material Design, Glide 4.16.0, OkHttp 4.12.0
- Room 2.6.1 (KSP), JSoup 1.17.2, Rhino 1.7.14
- Timber 5.0.1 (logging), Kotlin Coroutines 1.7.3 (async)
- kotlinx-serialization-json 1.6.2, epub4j-core 4.2.1 (EPUB support)
- AndroidX Preference 1.2.1
- SLF4J for non-Android modules

## Migration Status (from original codebase)

- Gradle 4.10→8.7, AGP 3.3→8.3.2, Kotlin 1.3→1.9.22
- Removed 68 dead site scrapers (all websites offline); rebuilt with 24 new scrapers for live sites (see `SITES.md`)
- Removed Anko entirely — replaced with Timber + Coroutines + AlertDialog.Builder
- Removed Bugly crash reporting — Reporter now logs via Timber only
- Removed old version migration system (no upgrades from legacy versions)
- Replaced all custom cc.aoeiuv020.* utility wrappers with standard library calls
- Replaced epublib stubs with real epub4j-core from Maven Central
- OkHttp 3→4 API migration
- ViewBinding migration from kotlin-android-extensions
- Replaced GSON with kotlinx-serialization across all modules
- Replaced `startActivityForResult` with `ActivityResultContracts`
- Replaced `ProgressDialog` with `ProgressDialogCompat` (AlertDialog-based)
- Replaced deprecated `PreferenceFragment`/`PreferenceActivity` with AndroidX `PreferenceFragmentCompat`
- Replaced raw Thread/Handler patterns with Kotlin Coroutines
- Replaced `ExecutorService` with `Dispatchers.IO` in reader module
- Added DiffUtil to RecyclerView adapters
- Removed global `App.context` — replaced with `PrefContext` + `AppContainer`
- Fixed `PendingIntent` flags for Android 12+ compatibility
- IronDB rewritten to use `KSerializer<T>` instead of `java.lang.reflect.Type`
- Removed ad system entirely (GDT SDK, splash ads, list ads, AdSettings)
- Consolidated backup to single `BackupImpl` (removed versioned V1-V3 importers)
- Simplified book list sharing to single `SharedBookList` (removed V1/V2 legacy formats)
- Removed `server` module and `batchRefresher` module entirely (server infra dead, push notifications via JPush removed)
- Removed `DnsUtils` (was only used for server config via DNS TXT records)
- `./gradlew assembleDebug` passes on JDK 21

## Release Workflow

To release a new version:

```bash
# 1. Bump version (increments version_code, sets version_name, commits)
./bump-version.sh 1.2.3

# 2. Push to remote
git push

# 3. Create GitHub release (creates tag, uploads release notes in one step)
gh release create v1.2.3 --title "v1.2.3" --notes "what changed"
```

The app checks for updates by hitting `https://api.github.com/repos/JNKKKK/PaNovel-Reborn/releases/latest`. It reads `tag_name` for the version and `body` for the changelog shown to users. No `ChangeLog.txt` maintenance needed for updates.

Repository: https://github.com/JNKKKK/PaNovel-Reborn

## CI/CD

- GitHub Actions: builds on push/PR (`.github/workflows/main.yml`)
- Scheduled test run Fridays (`.github/workflows/test.yml`) against `dev` branch
- Release workflow creates GitHub releases with APK artifacts and sends Telegram notifications
- Requires JDK 21 (`actions/setup-java@v4` with `temurin` distribution)
