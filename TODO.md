# TODO Comments Analysis

Total: 19 TODOs across the codebase.

## Scraper Module — DONE

All resolved:
- ~~`getNextPage` (dead code)~~ — Removed
- ~~Cache main page URL~~ — Discarded (micro-optimization, not worth it)
- ~~`requireElements` empty query support~~ — Implemented

## App - UI/UX (7)

| File | TODO |
|------|------|
| `app/.../BookListActivity.kt:53` | Switch to swipe-to-delete (better UX) |
| `app/.../NovelDetailActivity.kt:95` | Improve detail header — show author name, site name, etc. |
| `app/.../NovelDetailActivity.kt:96` | Use ViewPager with two pages for description and chapter list |
| `app/.../SingleSearchActivity.kt:159` | Needs a loading dialog here |
| `app/.../BackupActivity.kt:207` | Snackbar sometimes doesn't show, only flashes when collapsing — possibly keyboard-follow issue |
| `app/.../NovelViewHolder.kt:119` | Star widget should support `onCheckChanged`; check if external `isChecked` triggers click |
| `app/.../NovelViewHolder.kt:172` | Don't show this for local novels |

## App - Data/Logic (5)

| File | TODO |
|------|------|
| `app/.../ApiManager.kt:19` | Dmzj is still up — add after testing when it reopens |
| `app/.../DataManager.kt:150` | File I/O happens here after NovelContext gets cookies |
| `app/.../DownloadManager.kt:40` | Consider switching to async sequential per-book download |
| `app/.../DefaultNovelItemActionListener.kt:39` | Action handling is messy/inconsistent — think through before refactoring (some ops need to update VH) |
| `app/.../NovelListAdapter.kt:72` | ViewHolder reuse breaks after view settings change (e.g., grid item reused as list item) |

## App - Minor (3)

| File | TODO |
|------|------|
| `app/.../NovelListAdapter.kt:117` | Check if it auto-scrolls to bottom — don't want that |
| `app/.../NovelTextActivity.kt:230` | Smart case? Contracts |
| `app/.../other.kt:22` | Default color should be darker |

## Reader Module (1)

| File | TODO |
|------|------|
| `reader/.../ReaderDrawer.kt:126` | This fires twice when opening a novel |

## Pager Module (2)

| File | TODO |
|------|------|
| `pager/.../PageAnimation.java:226` | Refresh sometimes causes flicker, plus missing/duplicate character bugs |
| `pager/.../SimulationPageAnim.java:381` | Empty exception handler (swallowed exception) |

## Local Module (1)

| File | TODO |
|------|------|
| `local/.../EpubExporter.kt:16` | Other readers can't recognize the exported EPUB |

## Priority Recommendations

1. **`SimulationPageAnim.java:381`** — swallowed exception, should at least log it
2. **`NovelViewHolder.kt:172`** — visible bug (UI element shown for local novels when it shouldn't be)
3. **`NovelListAdapter.kt:72`** — ViewHolder reuse bug after settings change
4. **`EpubExporter.kt:16`** — exported EPUBs not recognized by other readers
