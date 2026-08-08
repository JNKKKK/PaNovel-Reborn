# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PaNovel is an Android novel reader app being revived. Goals: **run again, easy to maintain, live longer**.

It supports local TXT/EPUB files, backup/restore to local files (via SAF; books in the chosen 书架/书单/历史 collections are backed up together with their chapter lists and cached chapter content), reading progress sync, a pluggable site scraper system with 24 active scrapers (the original 68 were removed because the sites died; all current scrapers are new implementations), an in-reader Chinese dictionary (long-press a character to look up its pinyin and definition from a bundled MDX dictionary — greedy multi-character/proverb matching), and a **book-source availability dashboard** (the 书源 screen shows each source's recent reachability as a row of colored bars from local daily probes).

Deferred work and future improvements (with rationale) are tracked in [ROADMAP.md](ROADMAP.md).

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

Requires JDK 17+. Uses Gradle 9.6.1, AGP 9.3.0, Kotlin 2.4.10, KSP 2.3.11, compileSdk/targetSdk 36. AGP 9's "built-in Kotlin" and "new DSL" are opted out in `gradle.properties` (`android.builtInKotlin=false`, `android.newDsl=false`) so the `kotlin-android` plugin and existing DSL keep working; migrate off these before AGP 10.

## Architecture

**MVP pattern** with `DataManager` singleton coordinating:
- `AppDatabaseManager` – Room database (2.8.4, uses KSP)
- `ApiManager` – Novel website context/scraping
- `CookieManager` – Cookie persistence
- `CacheManager` – Content caching (IronDB + kotlinx-serialization)
- `LocalManager` – Local file novel support
- `DownloadManager` – Download management
- `AvailabilityManager` – Book-source availability probing (see [Book-Source Availability Dashboard](#book-source-availability-dashboard-书源-screen))

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
| mdict | Java | MDX (MDict) dictionary reader for the in-reader lookup feature |
| pager | Android | Pagination library |
| reader | Android | Novel reader UI |

## Writing Scrapers

See [WRITING_SCRAPERS.md](WRITING_SCRAPERS.md) for the full guide — creating a scraper, chapter-list pagination, content parsing, code conventions, and integration-test requirements (including anti-patterns to avoid).

## Dictionary Lookup (long-press in reader)

Long-pressing a character in the reader shows a bottom sheet with its pinyin and definition. The layer is split **generic vs. dictionary-specific** so new built-in dictionaries (and a future settings toggle to switch between them) only need a new `Dictionary` implementation:

- **`mdict` module (generic MDX):**
  - `MdxDictionary` — pure MDX (MDict) file format only: parses the container (v1.2 layout; none/LZO1X/zlib block compression; GBK/UTF-8/UTF-16/Big5), builds an in-memory key→offset index, decompresses record blocks on demand (LRU). Exposes `keys` and `lookup(key)` returning raw record text — it interprets **no** content conventions.
  - `DictEntry` — plain, dictionary-agnostic data model (headword, pinyin?, definitionHtml).
  - `Dictionary` — the app-facing interface (`senses(word)` / `contains(word)`). Upper layers depend only on this.
  - `XinhuaDictionary` — implements `Dictionary` and owns **all** `超级新华字典.mdx` quirks (documented in its header): the `` `1`词`2`拼音<br>释义 `` record markup, numeric-suffix polyphones (`的1/的2/的3`, `义/义1/义2`), and comma/semicolon variant merge-keys (`蹬腿,蹬腿儿`; `堤岸，堤坝`) with their comma-joined pinyin. **Add a new dictionary by writing another `Dictionary` implementation here — do not push a dictionary's quirks into the generic classes.**
- **App side:** `DictionaryManager` (in `DataManager`) is dictionary-agnostic — depends on `Dictionary`, does the greedy multi-character walk, and manages the asset. Built-in dictionaries are declared in the `BuiltinDictionary` enum (asset name + version + opener lambda); `current` is fixed to the default for now, and a settings-driven selection would just flip it.
- **Asset packaging:** the `.mdx` is a `noCompress` asset under `app/src/main/assets/dict/`; on first use it's copied to `filesDir` (so `RandomAccessFile` works). The copy is guarded by a sibling `.version` marker file — **bump `BuiltinDictionary.version` whenever the `.mdx` asset content changes** to force a re-copy of stale user copies (`assets.openFd`-based size checks fail on compressed assets, hence the marker).
- **Reader plumbing:** `Pager` uses a `GestureDetector` long-press with strict gesture separation (long-press never toggles bars or turns the page). Hit-testing maps a touch to a character via `PageHit` (opaque page tag + page-local content coords), resolved per animation family (`ReaderDrawer.hitTest` replays the typesetting geometry) — works in both paged and scroll modes.
- **Tests:** `mdict/src/test` splits `MdxDictionaryTest` (format-level only) from `XinhuaDictionaryTest` (dictionary semantics via the public API); both skip via `assumeTrue` if the bundled `.mdx` asset is absent.

## Book-Source Availability Dashboard (书源 screen)

The 书源 (source) screen is an availability dashboard: each source shows its recent reachability as a row of colored bars (a **14-sample rolling window**, newest on the right). There is intentionally **no** per-source user settings screen — cookie/header/charset overrides were removed (users shouldn't touch scraper internals; a correct scraper bakes those in). The scraper library still has `NovelContext.replaceCookies`/`replaceHeaders`/`forceCharset`, just no UI.

- **Bar colors** (`ProbeStatus`, DayNight-aware in `values*/colors.xml` as `availabilityOk/Recovered/Fail/Unknown`): 🟩 `OK` = reachable with valid content on first probe; 🟨 `RECOVERED` = was red, then recovered on a same-day re-probe (frozen — never re-probed again that day); 🟥 `FAIL` = unreachable/error/**Cloudflare-blocked** (CF is unsupported in the scraper, so a CF challenge page counts as unavailable); ⬜ `UNKNOWN` = no sample that slot (only the left-padding before 14 samples accumulate).
- **Sampling model** — one sample per **calendar day the app is opened** (not per calendar day; skipped days leave no bar, no gap-filling). Epoch-day is computed arithmetically (minSdk 24, no `java.time` desugaring).
- **`AvailabilityManager`** (in `data/availability/`, owned by `DataManager`) is the whole engine:
  - **Probe hook:** `NovelContext.probeHomePage()` (implemented in `OkHttpNovelContext`) does one lightweight GET reusing the site's own OkHttp client (correct UA/SSL/cookies) and returns raw `HomePageProbe` signals; **classification stays in the app layer** (`AvailabilityManager.classify`) — the scraper interprets nothing.
  - **Retry in place:** each probe attempts up to `PROBE_ATTEMPTS` (retry only on non-OK) before recording red.
  - **Two triggers:** `probeOnStartAsync` (from `App.onCreate`, main process only) runs the full probe once per day, else re-probes only today's reds; `reprobeFailuresAsync` (from `SiteChooseActivity.onStart`) re-probes only today's reds on every screen open. A red→OK on re-probe upgrades to `RECOVERED` (yellow).
  - **Guards:** skips entirely when offline (`ConnectivityManager`, `ACCESS_NETWORK_STATE` already granted) so an offline launch doesn't paint everything red; a per-source `inProgress` set (`ConcurrentHashMap.newKeySet()`) dedupes overlapping triggers so **no two probes run for the same source at once**; bounded concurrency via a `Semaphore`.
  - **Live UI:** exposes a `StateFlow<Map<siteName, List<ProbeRecord>>>`; `SiteChooseActivity` collects it (`repeatOnLifecycle`) so bars refresh live as a probe round finishes. `AvailabilityBarsView` is a pure custom `View` that renders `AvailabilityManager.barsFor(records, slots)`.
- **Storage:** `AvailabilityStore` uses **IronDB** (not Room) — probe history is regenerable telemetry, so it's a single serialized blob under `filesDir/availability/` (site names can't be IronDB keys — the key serializer mangles `/`, `:`, `.` and `keysContainer()` can't iterate), capped at 30 records/site, not included in backup. Using Room would have meant a schema-version bump + migration for throwaway data; IronDB matches how `CacheManager`/`LocalManager` treat regenerable data.
- **Tests:** `AvailabilityManagerTest` (app `src/test`, pure — no Android) covers `classify` (strict success + CF→FAIL) and `barsFor`/`upsertToday` (padding, newest-kept, red→yellow same-day replacement without adding a slot).

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
