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

### Creating a new scraper

1. **Identify the site structure** — use `curl -s --compressed <url>` to inspect raw HTML. Check:
   - Search: form action, method, field names, result container/selectors
   - Detail page: selectors for name, author, image, update time, introduction
   - Chapter list: selector for chapter links, **pagination mechanism**
   - Content page: how text is structured (`<p>` tags, `<br/>` separated, JS-decoded, etc.)

2. **Create the scraper** in `scraper/src/main/java/cc/aoeiuv020/panovel/api/site/`. Use an existing scraper as reference — pick one with similar site structure.

3. **Create the integration test** in `scraper/src/test/java/cc/aoeiuv020/panovel/api/site/`.

### Chapter list pagination

Many sites paginate chapters (100 per page). **Always verify how pagination works** — don't assume any specific HTML pattern. Common patterns:

- `<li>` with text like "1/4" (current/total) — parse total from regex `(\d+)/(\d+)`
- `<select>` with `<option>` elements — count options for page count
- "Next" link with page number in href — extract max page from last link

**Never** use selectors that assume a specific element exists (like `aria-label=Next`) without first confirming via `curl` on the actual site. If the selector matches nothing, pagination silently fails and only page 1 is returned.

### Content parsing

Sites use different content formats — **always check the raw HTML** of a chapter page:

- `<p>` tags inside a container → use `items("#container > p")`
- `<br/>` separated text → split on `<br/>`, strip HTML tags, filter empty
- Multiple formats on same site → check for `<p>` first, fall back to `<br/>` splitting
- JS-decoded content (Base64, encryption) → decode with regex/`pick`; no active scraper needs a JS engine (the Rhino module was removed)
- Multi-page chapters (e.g., "第(1/3)页") → detect page indicator, fetch subsequent pages

### Scraper code conventions

- Each scraper gets its own private regex constants (prefix with site name, e.g., `biquge520PaginationRegex`)
- Use `parse(connect(url))` for fetching and parsing pages
- Use `getNovelChapterUrl(extra)` / `getNovelContentUrl(extra)` for URL construction
- Chapter `extra` field stores the ID needed to construct content URLs
- Return `emptyList()` from content block if no content found (don't throw)

### Integration test requirements

Every scraper test must include `@get:Rule val retryRule = RetryRule()` and `@Category(SiteIntegrationTest::class)`. Required test methods:

**testSearch** — verify search returns results:
```kotlin
val list = context.searchNovelName("书名")
assertTrue("search should return results", list.isNotEmpty())
// Verify first result has name, author, extra
```

**testDetail** — verify detail page parsing with a known book:
```kotlin
val detail = context.getNovelDetail("bookId")
assertEquals("expected name", detail.novel.name)
assertEquals("expected author", detail.novel.author)
```

**testChapters** — **must verify pagination actually works**:
```kotlin
val chapters = context.getNovelChaptersAsc("bookId")
assertTrue("should have chapters", chapters.isNotEmpty())
// Use a book with >200 chapters; assert > 200 to guarantee multi-page fetch
assertTrue("should have many chapters (paginated across multiple pages)", chapters.size > 200)
```
Pick a test novel that is **completed and well-known** (e.g., 斗破苍穹) so chapter count is stable and always exceeds a single page.

**testContent** — verify content extraction returns real text:
```kotlin
val content = context.getNovelContent("bookId/chapterId")
assertTrue("should have content lines", content.isNotEmpty())
assertTrue("should have many content lines", content.size > 10)
assertTrue("first line should have text", content.first().isNotBlank())
```
If the site uses multiple content formats, add a separate test for each format.

### Test anti-patterns to avoid

- `chapters.size > 100` when site shows 100 per page — this passes without pagination working
- Only checking `content.isNotEmpty()` — passes if even 1 garbage line is returned
- Testing with a novel that has few chapters — won't catch pagination bugs
- Assuming all novels on a site use the same HTML format — test multiple if formats differ

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
