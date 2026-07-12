# Writing Scrapers

Guidance for adding and testing PaNovel site scrapers. See [CLAUDE.md](CLAUDE.md) for the overall project architecture.

## Creating a new scraper

1. **Identify the site structure** — use `curl -s --compressed <url>` to inspect raw HTML. Check:
   - Search: form action, method, field names, result container/selectors
   - Detail page: selectors for name, author, image, update time, introduction
   - Chapter list: selector for chapter links, **pagination mechanism**
   - Content page: how text is structured (`<p>` tags, `<br/>` separated, JS-decoded, etc.)

2. **Create the scraper** in `scraper/src/main/java/cc/aoeiuv020/panovel/api/site/`. Use an existing scraper as reference — pick one with similar site structure.

3. **Create the integration test** in `scraper/src/test/java/cc/aoeiuv020/panovel/api/site/`.

## Chapter list pagination

Many sites paginate chapters (100 per page). **Always verify how pagination works** — don't assume any specific HTML pattern. Common patterns:

- `<li>` with text like "1/4" (current/total) — parse total from regex `(\d+)/(\d+)`
- `<select>` with `<option>` elements — count options for page count
- "Next" link with page number in href — extract max page from last link

**Never** use selectors that assume a specific element exists (like `aria-label=Next`) without first confirming via `curl` on the actual site. If the selector matches nothing, pagination silently fails and only page 1 is returned.

## Content parsing

Sites use different content formats — **always check the raw HTML** of a chapter page:

- `<p>` tags inside a container → use `items("#container > p")`
- `<br/>` separated text → split on `<br/>`, strip HTML tags, filter empty
- Multiple formats on same site → check for `<p>` first, fall back to `<br/>` splitting
- JS-decoded content (Base64, encryption) → decode with regex/`pick`; no active scraper needs a JS engine (the Rhino module was removed)
- Multi-page chapters (e.g., "第(1/3)页") → detect page indicator, fetch subsequent pages

## Scraper code conventions

- Each scraper gets its own private regex constants (prefix with site name, e.g., `biquge520PaginationRegex`)
- Use `parse(connect(url))` for fetching and parsing pages
- Use `getNovelChapterUrl(extra)` / `getNovelContentUrl(extra)` for URL construction
- Chapter `extra` field stores the ID needed to construct content URLs
- Return `emptyList()` from content block if no content found (don't throw)

## Integration test requirements

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

## Test anti-patterns to avoid

- `chapters.size > 100` when site shows 100 per page — this passes without pagination working
- Only checking `content.isNotEmpty()` — passes if even 1 garbage line is returned
- Testing with a novel that has few chapters — won't catch pagination bugs
- Assuming all novels on a site use the same HTML format — test multiple if formats differ
