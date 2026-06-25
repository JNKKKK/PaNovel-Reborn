# TODO Comments Analysis

Total: 4 TODOs across the codebase.

## Scraper Module — DONE

All resolved:
- ~~`getNextPage` (dead code)~~ — Removed
- ~~Cache main page URL~~ — Discarded (micro-optimization, not worth it)
- ~~`requireElements` empty query support~~ — Implemented

## App - UI/UX — DONE

All resolved:
- ~~Switch to swipe-to-delete~~ — Discarded
- ~~Improve detail header — show author name, site name, etc.~~ — Done
- ~~Use ViewPager with two pages for description and chapter list~~ — Done
- ~~Needs a loading dialog~~ — Done
- ~~Snackbar sometimes doesn't show~~ — Discarded
- ~~Star widget `onCheckChanged`~~ — Discarded (click action settings removed; hardcoded behavior)
- ~~Don't show update time for local novels~~ — Done

## App - Data/Logic — DONE

All resolved:
- ~~Dmzj scraper — add when it reopens~~ — Discarded (external gate; site status tracked in SITES.md)
- ~~Cookie file I/O on main thread~~ — Discarded (deliberate, documented; cookie set is tiny)
- ~~Switch to sequential per-book download~~ — Done (`downloadAll` now serializes via a suspend core instead of firing every book concurrently)
- ~~Action handling messy/inconsistent~~ — Done (unified bookshelf handling in listener; VH no longer round-trips through `onStarChanged`)
- ~~ViewHolder reuse breaks after view settings change~~ — Done (layout drives `getItemViewType`, so RecyclerView rebuilds VHs on settings change)

## App - Minor — DONE

All resolved:
- ~~Check if addAll auto-scrolls to bottom~~ — Done (verified: end-insert doesn't scroll; FuzzySearchActivity also saves/restores scroll state)
- ~~Smart case? Contracts~~ — Done (extracted single `notNullOrReport()` call so a null exception isn't reported twice)
- ~~Default cached-chapter color should be darker~~ — Done (pure green 0xff00ff00 → dark green 0xff008000)

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
2. **`EpubExporter.kt:16`** — exported EPUBs not recognized by other readers
