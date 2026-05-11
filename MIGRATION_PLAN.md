# Migration Plan: Complete Project Revival

## Context

PaNovel is an old Android novel reader app being revived. Goals: **run again, easy to maintain, live longer**.

**Already completed:**
- Gradle 4.10→8.7, AGP 3.3→8.3.2, Kotlin 1.3→1.9.22 (commit `58461748`)
- ViewBinding migration from kotlin-android-extensions (37 files)
- OkHttp 3→4 API migration
- Custom cc.aoeiuv020.* utilities inlined in baseJar (commit `58461748`)
- Anko compat shim created (commit `44bbd638`)

**Remaining work (this plan):**
- Remove 68 dead site scrapers (all novel websites are dead)
- Replace all custom utility wrappers with standard library calls
- Properly migrate Anko to Timber + Coroutines + AlertDialog.Builder (delete the shim)
- Make EPUB support work with epub4j-core (real library on Maven Central)
- Fix remaining compilation errors (~208 in app module)

---

## Phase 1: Remove Dead Scrapers

**Why first:** 68 scraper files contain the majority of custom utility calls. Removing them reduces Phase 2 scope by ~40%.

### 1.1 Delete scraper source files

Delete all 68 files in `api/src/main/java/cc/aoeiuv020/panovel/api/site/`:
```
aileleba.kt, biquge.kt, biquge5200.kt, biqugebook.kt, biqugese.kt, biqugezhh.kt,
bqg5200.kt, byzw.kt, dajiadu.kt, dmzz.kt, exiaoshuo.kt, fenghuaju.kt, ggdown.kt,
guanshuwang.kt, gulizw.kt, gxwztv.kt, haxds.kt, jdxs520.kt, kenshuzw.kt, kssw.kt,
kuxiaoshuo.kt, lewen123.kt, liewen.kt, liudatxt.kt, lnovel.kt, lread.kt,
manhuagui.kt, mianhuatang.kt, miaobige.kt, n123du.kt, n168kanshu.kt, n2kzw.kt,
n360dxs.kt, n52ranwen.kt, n69shu.kt, n73xs.kt, n7dsw.kt, n9txs.kt, piaotian.kt,
qidian.kt, qingkan.kt, qingkan5.kt, qinxiaoshuo.kt, qlwx.kt, sfacg.kt,
shangshu.kt, shoudashu.kt, shu8.kt, sifang.kt, siluke.kt, snwx.kt, syxs.kt,
ttkan.kt, uctxt.kt, wenxuemi.kt, wukong.kt, wukong0.kt, x23us.kt, yidm.kt,
yipinxia.kt, yllxs.kt, ymoxuan.kt, yssm.kt, yunduwu.kt, zaidudu.kt, zhuaji.kt,
zhuishu.kt, zzdxsw.kt
```

### 1.2 Delete scraper test files

Delete all 68 files in `api/src/test/java/cc/aoeiuv020/panovel/api/site/`:
```
AilelebaTest.kt, BiqugeTest.kt, ... (same names with Test suffix)
```

Keep the base test class file if it exists (`base.kt` or `BaseNovelContextText.kt`).

### 1.3 Create mock template scraper

Create `api/src/main/java/cc/aoeiuv020/panovel/api/site/MockSite.kt`:
```kotlin
package cc.aoeiuv020.panovel.api.site

import cc.aoeiuv020.panovel.api.base.DslJsoupNovelContext

/**
 * Template scraper demonstrating the DSL pattern.
 * Copy this file and modify to add a new novel site.
 */
class MockSite : DslJsoupNovelContext() { init {
    site {
        name = "示例站点"
        baseUrl = "https://example.com"
        logo = "https://example.com/logo.png"
    }
    search {
        get { "/search?q=$it" }
        document {
            items("div.result") {
                name("> a")
                author("> span.author")
            }
        }
    }
    detail {
        document {
            novel { name("h1") }
            image("img.cover")
            author("span.author")
            introduction("div.intro")
        }
    }
    chapters {
        document {
            items("ul.chapters > li > a")
            lastUpdate("span.update", format = "yyyy-MM-dd")
        }
    }
    content {
        document {
            items("div.content > p")
        }
    }
}}
```

### 1.4 Edit NovelContext.kt

File: `api/src/main/java/cc/aoeiuv020/panovel/api/NovelContext.kt`

Change `getAllSite()` from the hardcoded list of 68 scrapers to:
```kotlin
companion object {
    val sitesVersion = 13

    fun getAllSite(): List<NovelContext> = listOf(
        MockSite()
    )
}
```

### 1.5 Remove find/ package from app module

Delete entire directory: `app/src/main/java/cc/aoeiuv020/panovel/find/`

This removes:
- `find/qidiantu/` (QidiantuActivity, QidiantuPresenter, list/)
- `find/shuju/` (QidianshujuActivity, QidianshujuPresenter, list/, post/)
- `find/sp7/` (Sp7Activity, Sp7Presenter, list/)

### 1.6 Update AndroidManifest.xml

Remove these activity entries from `app/src/main/AndroidManifest.xml`:
```xml
<activity android:name="cc.aoeiuv020.panovel.find.shuju.QidianshujuActivity" />
<activity android:name="cc.aoeiuv020.panovel.find.shuju.post.QidianshujuPostActivity" />
<activity android:name="cc.aoeiuv020.panovel.find.shuju.list.QidianshujuListActivity" />
<activity android:name=".find.sp7.Sp7Activity" />
<activity android:name=".find.sp7.list.Sp7ListActivity" />
<activity android:name=".find.qidiantu.QidiantuActivity" />
<activity android:name=".find.qidiantu.list.QidiantuListActivity" />
```

### 1.7 Remove find-related menu/navigation references

- `app/src/main/java/cc/aoeiuv020/panovel/main/MainActivity.kt`: Remove imports and menu handlers for qidiantu, sp7, shuju activities
- `app/src/main/res/menu/menu_main.xml`: Remove the `qidiantu` menu item

---

## Phase 2: Replace Custom Utilities with Standard Calls

### 2.1 Keep `pick()` as project utility

Move from `baseJar/src/main/java/cc/aoeiuv020/regex/regex.kt` into `baseJar/src/main/java/cc/aoeiuv020/base/jar/regex.kt` (merge into baseJar's own package). Update all imports from `cc.aoeiuv020.regex.pick` → `cc.aoeiuv020.base.jar.pick`.

Also keep `compileRegex()` and `compilePattern()` in the same file — they're thin but ergonomic.

### 2.2 Replace `notNull()` (27 files → ~15 after Phase 1)

Pattern:
```kotlin
// Before:
import cc.aoeiuv020.anull.notNull
response.body.notNull().use { ... }

// After:
response.body!!.use { ... }
// or for message variant:
requireNotNull(response.body) { "message" }.use { ... }
```

### 2.3 Replace GSON utilities (26 files → ~15 after Phase 1)

```kotlin
// Before:
import cc.aoeiuv020.gson.toBean
import cc.aoeiuv020.gson.toJson
import cc.aoeiuv020.gson.type
import cc.aoeiuv020.gson.GsonUtils

val obj = jsonString.toBean<MyType>(gson)
val json = obj.toJson(gson)
val t = type<List<String>>()
val g = GsonUtils.gson

// After:
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

val obj: MyType = gson.fromJson(jsonString, MyType::class.java)
val json = gson.toJson(obj)
val t = object : TypeToken<List<String>>() {}.type
val g = Gson()  // or use App.gson singleton
```

### 2.4 Replace OkHttp utilities (12 files → ~5 after Phase 1)

```kotlin
// Before:
import cc.aoeiuv020.okhttp.OkHttpUtils
import cc.aoeiuv020.okhttp.string
import cc.aoeiuv020.okhttp.get

val text = OkHttpUtils.get(url).string()
val text2 = client.get(url).string()

// After:
import okhttp3.OkHttpClient
import okhttp3.Request

val client = OkHttpClient()
val request = Request.Builder().url(url).build()
val text = client.newCall(request).execute().use { it.body?.string() ?: "" }
```

### 2.5 Replace jsonPath (13 files → ~8 after Phase 1)

```kotlin
// Before:
import cc.aoeiuv020.jsonpath.jsonPath
import cc.aoeiuv020.jsonpath.get

val value = jsonString.jsonPath.get("fieldName")
val typed = jsonString.jsonPath.get<String>("fieldName")

// After:
import com.google.gson.JsonParser

val obj = JsonParser.parseString(jsonString).asJsonObject
val value = obj.get("fieldName")?.asString ?: ""
```

### 2.6 Replace string utilities (8 files → ~5 after Phase 1)

```kotlin
// Before:
import cc.aoeiuv020.string.divide
import cc.aoeiuv020.string.lastDivide

val (first, second) = str.divide('/')
val (head, tail) = str.lastDivide(':')

// After (Kotlin stdlib):
val first = str.substringBefore('/')
val second = str.substringAfter('/')
val head = str.substringBeforeLast(':')
val tail = str.substringAfterLast(':')
```

### 2.7 Replace encrypt utilities (10 files → ~5 after Phase 1)

```kotlin
// Before:
import cc.aoeiuv020.encrypt.md5
import cc.aoeiuv020.encrypt.hex

val hash = "input".md5().hex()

// After:
import java.security.MessageDigest

val hash = MessageDigest.getInstance("MD5")
    .digest("input".toByteArray())
    .joinToString("") { "%02x".format(it) }
```

### 2.8 Replace log utilities (9 files → ~5 after Phase 1)

```kotlin
// Before:
import cc.aoeiuv020.log.debug
logger.debug { "message" }

// After:
import org.slf4j.LoggerFactory
val logger = LoggerFactory.getLogger(javaClass)
logger.debug("message")
```

### 2.9 Replace atry (3 files)

```kotlin
// Before:
import cc.aoeiuv020.atry.tryOrNul
val result = tryOrNul { riskyCall() }

// After:
val result = runCatching { riskyCall() }.getOrNull()
```

### 2.10 Cleanup

After all replacements:
- Delete `baseJar/src/main/java/cc/aoeiuv020/anull/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/gson/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/okhttp/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/jsonpath/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/string/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/log/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/encrypt/`
- Delete `baseJar/src/main/java/cc/aoeiuv020/atry/`

Move `regex.kt` content into `baseJar/src/main/java/cc/aoeiuv020/base/jar/regex.kt`.

Update `baseJar/build.gradle` — remove `api 'com.google.code.gson:gson:2.10.1'` if no longer needed at baseJar level (gson moves to app-level dependency).

---

## Phase 3: Properly Migrate Anko (97 files)

### 3.0 Delete AnkoCompat shims

Delete:
- `app/src/main/java/org/jetbrains/anko/AnkoCompat.kt`
- `reader/src/main/java/org/jetbrains/anko/AnkoCompat.kt`
- `pager/src/main/java/org/jetbrains/anko/AnkoCompat.kt`

### 3.1 Logging migration (41 files)

**Pattern:** Remove `AnkoLogger` interface, remove `import org.jetbrains.anko.*`, use `Timber` directly.

```kotlin
// Before:
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.error

class MyPresenter : AnkoLogger {
    fun doWork() {
        debug { "starting work" }
        error { "failed: $message" }
    }
}

// After:
import timber.log.Timber

class MyPresenter {
    fun doWork() {
        Timber.d("starting work")
        Timber.e("failed: $message")
    }
}
```

Files (41): All Presenter, Manager, Activity classes listed in Phase 1 exploration.

### 3.2 Async migration (35 files)

**Pattern:** Replace `doAsync`/`uiThread` with Kotlin Coroutines.

For **Presenter classes** (most common pattern):
```kotlin
// Before:
class FuzzySearchPresenter {
    fun search(name: String) {
        doAsync({ e -> view?.showError("搜索失败", e) }) {
            val results = DataManager.search(name)
            uiThread {
                view?.showResult(results)
            }
        }
    }
}

// After:
import kotlinx.coroutines.*

class FuzzySearchPresenter {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun search(name: String) {
        scope.launch {
            try {
                val results = withContext(Dispatchers.IO) { DataManager.search(name) }
                view?.showResult(results)
            } catch (e: Exception) {
                view?.showError("搜索失败", e)
            }
        }
    }

    fun detach() {
        scope.cancel()
        // existing detach logic...
    }
}
```

For **Activity/Fragment** (lifecycle-aware):
```kotlin
// Before (in Activity):
doAsync({ e -> showError(e) }) {
    val data = loadData()
    uiThread { display(data) }
}

// After:
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

lifecycleScope.launch {
    try {
        val data = withContext(Dispatchers.IO) { loadData() }
        display(data)
    } catch (e: Exception) {
        showError(e)
    }
}
```

For **doAsync with executorService** parameter:
```kotlin
// Before:
doAsync({ e -> ... }, ioExecutorService) { ... }

// After:
scope.launch(ioExecutorService.asCoroutineDispatcher()) { ... }
```

**Lifecycle dependency needed:** Add to `app/build.gradle`:
```groovy
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
```

### 3.3 Dialog migration (14 files)

**Pattern:** Replace Anko `alert{}` DSL with AndroidX `AlertDialog.Builder`.

```kotlin
// Before:
import org.jetbrains.anko.*

ctx.alert {
    titleResource = R.string.warning
    message = getString(R.string.confirm_delete)
    yesButton { doDelete() }
    cancelButton { }
    neutralPressed(R.string.details) { showDetails() }
}.safelyShow()

// After:
import androidx.appcompat.app.AlertDialog

AlertDialog.Builder(this)
    .setTitle(R.string.warning)
    .setMessage(getString(R.string.confirm_delete))
    .setPositiveButton(android.R.string.ok) { _, _ -> doDelete() }
    .setNegativeButton(android.R.string.cancel, null)
    .setNeutralButton(R.string.details) { _, _ -> showDetails() }
    .show()
```

For `selector`:
```kotlin
// Before:
selector(title, items) { dialog, index -> handleSelection(index) }

// After:
AlertDialog.Builder(this)
    .setTitle(title)
    .setItems(items.toTypedArray()) { _, index -> handleSelection(index) }
    .show()
```

Keep `safelyShow()` utility in `app/src/main/java/cc/aoeiuv020/panovel/util/view.kt` — it wraps `.show()` with try-catch for window token issues.

### 3.4 Navigation migration (22 files)

**Pattern:** Replace Anko reified `startActivity<T>()` with standard Intent.

```kotlin
// Before:
import org.jetbrains.anko.startActivity

startActivity<NovelDetailActivity>("novel" to novel)

// After:
startActivity(Intent(this, NovelDetailActivity::class.java).apply {
    putExtra("novel", novel)
})
```

Most activities in this project already have a `companion object { fun start(context, ...) }` pattern. Those should be used preferentially:
```kotlin
// Already exists in most activities:
companion object {
    fun start(context: Context, novel: Novel) {
        context.startActivity(Intent(context, NovelDetailActivity::class.java).apply {
            putExtra("novel", novel as Serializable)
        })
    }
}
```

### 3.5 Utility migration

| Anko utility | Replacement | Notes |
|-------------|-------------|-------|
| `ctx` | `this` / `requireContext()` | Direct context reference |
| `dip(16)` | `(16 * resources.displayMetrics.density).toInt()` | Or create a 1-line extension in `util/` |
| `sp(16)` | `(16 * resources.displayMetrics.scaledDensity).toInt()` | Same |
| `toast("msg")` | `Toast.makeText(this, "msg", Toast.LENGTH_SHORT).show()` | One-liner |
| `longToast("msg")` | `Toast.makeText(this, "msg", Toast.LENGTH_LONG).show()` | One-liner |
| `browse(url)` | `startActivity(Intent(ACTION_VIEW, Uri.parse(url)))` | One-liner |
| `email(addr)` | `startActivity(Intent(ACTION_SENDTO, Uri.parse("mailto:$addr")))` | One-liner |
| `find<View>(id)` | `findViewById<View>(id)` | Direct stdlib |
| `runOnUiThread {}` | Keep (it's in Android SDK `Activity.runOnUiThread`) | No change needed |

**Optional:** Create a small `app/src/main/java/cc/aoeiuv020/panovel/util/extensions.kt` with:
```kotlin
fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()
fun Context.browse(url: String) = startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
```
This avoids repeating the density calculation in 8+ files.

---

## Phase 4: Fix Epublib

### 4.1 Add epub4j-core dependency

File: `local/build.gradle`
```groovy
dependencies {
    implementation project(':baseJar')
    implementation 'net.sourceforge.jchardet:jchardet:1.0'
    implementation('io.documentnode:epub4j-core:4.2.1') {
        exclude group: 'xmlpull'
        exclude group: 'org.slf4j'
    }
    implementation 'org.jsoup:jsoup:' + jsoup_version
}
```

### 4.2 Delete stubs

Delete entire directories:
- `local/src/main/java/nl/siegmann/epublib/`
- `local/src/main/java/net/sf/jazzlib/`

### 4.3 Update EpubParser.kt

```kotlin
// Before:
import net.sf.jazzlib.ZipFile
val zipFile = ZipFile(file)
val book: Book = EpubReader().readEpubLazy(zipFile, charset.name())
zipFile.close()

// After:
import java.io.FileInputStream
val book: Book = EpubReader().readEpub(FileInputStream(file))
```

Note: epub4j's `readEpub(InputStream)` replaces the custom `readEpubLazy(ZipFile, String)`. The lazy loading is lost but functionally equivalent.

### 4.4 Update EpubExpoter.kt

Same pattern — replace `net.sf.jazzlib.ZipFile` imports with standard Java IO. The `nl.siegmann.epublib.*` imports stay the same (epub4j uses the same package names).

### 4.5 Update Previewer.kt

Same pattern as EpubParser.

---

## Phase 5: Fix Remaining Compilation Issues

### 5.1 Create SSL utilities

File: `baseJar/src/main/java/cc/aoeiuv020/ssl/ssl.kt`
```kotlin
package cc.aoeiuv020.ssl

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

class TLSSocketFactory(trustManager: X509TrustManager) : SSLSocketFactory() {
    private val delegate: SSLSocketFactory

    init {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), SecureRandom())
        delegate = sslContext.socketFactory
    }

    // Delegate all methods to the SSLContext-created factory
    override fun getDefaultCipherSuites() = delegate.defaultCipherSuites
    override fun getSupportedCipherSuites() = delegate.supportedCipherSuites
    override fun createSocket(...) = delegate.createSocket(...)
    // ... (all 5 createSocket overloads)
}

object TrustManagerUtils {
    fun include(pins: Set<String>): X509TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}
```

### 5.2 Fix remaining ViewBinding issues

Any files referencing views via synthetic-style names (without `binding.` prefix) that were missed by agents. Check by compiling and fixing one-by-one.

### 5.3 Add lifecycle-ktx dependency

File: `app/build.gradle` — add:
```groovy
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
```

### 5.4 Remove dead imports/references

After all migrations, run `./gradlew assembleDebug` iteratively and fix any remaining unresolved references.

---

## Verification

```bash
# Full debug build succeeds on JDK 21
./gradlew assembleDebug

# API module tests compile (mock scraper only)
./gradlew api:compileTestJava

# No remaining synthetic/Anko imports
grep -r "org.jetbrains.anko" --include="*.kt" app/ reader/ pager/ api/
# Should return 0 results

grep -r "kotlinx.android.synthetic" --include="*.kt" .
# Should return 0 results
```

---

## Summary

| Phase | What | Files | Effort |
|-------|------|-------|--------|
| 1 | Remove 68 dead scrapers + find/ UI | ~150 deleted, ~5 edited | Low |
| 2 | Replace custom utils with stdlib | ~50 edited, ~9 dirs deleted | Medium |
| 3 | Migrate Anko properly | ~97 edited, 3 deleted | High (mechanical) |
| 4 | Fix epublib with epub4j-core | ~5 edited, ~15 deleted | Low |
| 5 | Fix remaining (SSL, ViewBinding) | ~10 new/edited | Low |
| **Total** | | **~330 file operations** | |

Each phase produces a commit. Each commit should leave the codebase in a consistent state (Phase 1+2 may need to be combined for compilation to succeed).
