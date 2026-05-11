# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PaNovel is an Android novel reader app that scrapes 35+ Chinese novel websites. It also supports local TXT/EPUB files, WebDAV backup, and reading progress sync. Written in Kotlin with some Java modules.

## Build Commands

```bash
# Build release APK
./gradlew assembleRelease

# Build debug APK
./gradlew assembleDebug

# Run API module tests (all site scrapers)
./gradlew api:test --tests "**.site.*"

# Run a single site test
./gradlew api:test --tests "**.site.BiqugeTest"

# Compile tests without running
./gradlew api:compileTestJava
```

Requires JDK 21. Uses Gradle 8.7, AGP 8.3.2, Kotlin 1.9.22.

## Architecture

**MVP pattern** with a centralized `DataManager` singleton coordinating:
- `AppDatabaseManager` – Room database (2.6.1, uses KSP)
- `ApiManager` – Novel website context/scraping
- `CookieManager` – Cookie persistence
- `CacheManager` – Content caching
- `ServerManager` – Push notifications and sync
- `LocalManager` – Local file novel support
- `DownloadManager` – Download management

Base classes: `IView` interface + `Presenter<T : IView>` abstract class.

## Module Structure

| Module | Type | Purpose |
|--------|------|---------|
| app | Android | Main application (activities, presenters, fragments) |
| api | Java | Novel website scrapers (JSoup + Rhino JS parsing) |
| baseJar | Java | Shared utilities (GSON, OkHttp, regex, string, logging wrappers) |
| IronDB | Java | File-based NoSQL key-value store |
| js | Java | Rhino JavaScript engine wrapper |
| local | Java | Local file support (TXT, EPUB) |
| pager | Android | Pagination library |
| reader | Android | Novel reader UI |
| filepicker | Android | File picker UI |
| refresher | Java | Standalone CLI for batch refreshing |
| server | Java | Server communication |

## Key Patterns

- Novel site scrapers extend `DslJsoupNovelContext`, `JsNovelContext`, or `OkHttpNovelContext` in `api/src/main/java/cc/aoeiuv020/panovel/api/site/`
- Dependency versions are centralized in `version.properties`
- App package structure is feature-based: `cc.aoeiuv020.panovel.{bookshelf,download,search,settings,...}`
- Room database schemas are exported to `app/schemas/` for migration validation
- ViewBinding is used for view access (no kotlin-android-extensions)
- Custom utility libraries are inlined in `baseJar/src/main/java/cc/aoeiuv020/` (anull, gson, okhttp, regex, jsonpath, string, log, encrypt, atry)

## Tech Stack

- Kotlin 1.9.22, Java 17 target, Gradle 8.7, AGP 8.3.2
- Target/Compile SDK 34, Min SDK 16, Multidex enabled
- AndroidX, Material Design, Glide 4.16.0, OkHttp 4.12.0
- Room 2.6.1 (KSP), JSoup 1.17.2, Rhino 1.7.14
- SLF4J logging, GSON serialization, Bugly crash reporting
- Anko 0.10.8 (logging, dialogs, async — requires JCenter or cached JAR)

## Known Issues

The following dependencies are only available from JCenter (now dead) and need cached JARs or replacement:
- `org.jetbrains.anko:anko-commons:0.10.8` — used in 97 files for doAsync, alert dialogs, AnkoLogger
- `com.amitshekhar.android:debug-db:1.0.6` — debug-only database browser
- `com.miguelcatalan:materialsearchview:1.4.0` — search UI widget

## CI/CD

- GitHub Actions: builds on push/PR (`.github/workflows/main.yml`)
- Scheduled test run Fridays (`.github/workflows/test.yml`) against `dev` branch
- Release workflow creates GitHub releases with APK artifacts and sends Telegram notifications
- Requires JDK 21 (`actions/setup-java@v4` with `temurin` distribution)
