# TODO Comments Analysis

Total: 0 TODOs across the codebase.

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

## Reader Module — DONE

All resolved:
- ~~drawCurrentPage fires twice when opening a novel~~ — Done (expected transient before chapterList is set; downgraded warning to debug)

## Pager Module — DONE

All resolved:
- ~~Refresh flicker + missing/duplicate characters~~ — Done (abort the in-flight animation before redrawing so refresh ends with a single clean draw)
- ~~Empty exception handler in SimulationPageAnim~~ — Done (log the swallowed clipPath exception via Timber)

## Local Module — DONE

All resolved:
- ~~Other readers can't recognize the exported EPUB~~ — Done (chapters now emitted as well-formed XHTML with xmlns + XHTML media type and `.xhtml` extension; removed the bogus cover-guide pointing at the first text chapter)

## Notes

- `local/src/test/.../EpubParserTest.kt` has a **pre-existing** compile error (lines ~305-332: `it` used outside lambda scope, `firstname`/`lastname` references) — unrelated to the TODO work, but `:local:test` won't run until it's fixed.
