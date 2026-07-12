# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PaNovel is an Android novel reader app being revived. Goals: **run again, easy to maintain, live longer**.

It supports local TXT/EPUB files, backup/restore to local files (via SAF; books in the chosen 书架/书单/历史 collections are backed up together with their chapter lists and cached chapter content), reading progress sync, and a pluggable site scraper system with 24 active scrapers (the original 68 were removed because the sites died; all current scrapers are new implementations).

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Compile scraper module (scraper DSL)
./gradlew scraper:compileKotlin

# Compile bookfile module (TXT/EPUB)
./gradlew bookfile:compileKotlin

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
| scraper | Java | Novel website scrapers (JSoup parsing) |
| shared | Java | Shared utilities (`shared.jsoup` DOM helpers, `shared.json`, `shared.regex`, `shared.ssl`, `shared.util`) |
| IronDB | Java | File-based NoSQL key-value store (kotlinx-serialization) |
| bookfile | Java | Book file formats: TXT/EPUB parsing and export (epub4j-core) |
| pager | Android | Pagination library |
| reader | Android | Novel reader UI |

## Writing Scrapers

See [WRITING_SCRAPERS.md](WRITING_SCRAPERS.md) for the full guide — creating a scraper, chapter-list pagination, content parsing, code conventions, and integration-test requirements (including anti-patterns to avoid).

## Key Patterns

- Novel site scrapers extend `DslJsoupNovelContext` in `scraper/src/main/java/cc/aoeiuv020/panovel/api/site/`
- Add new scrapers by copying an existing scraper with similar site structure as a starting point
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
- File save/open: Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ActivityResultContracts`), no storage permission needed. Novel export uses the `CreateDocumentActivity` bridge to pop the system "save as" dialog from a bare `Context`. The app declares **no** storage permissions (no `READ_/WRITE_/MANAGE_EXTERNAL_STORAGE`, no `requestLegacyExternalStorage`) — all file access is SAF or app-private (`filesDir`/`cacheDir`). Reader background-image/font picks copy the file into app-private storage via `UriDelegate` (`util/delegate.kt`) on assignment, so they survive restart without persistable URI permissions

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
