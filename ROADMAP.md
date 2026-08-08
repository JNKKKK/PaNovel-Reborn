# Roadmap

Deferred work and future improvements for PaNovel. These are intentionally
not-done-yet decisions with their rationale, so the reasoning isn't lost.

## Material 3 theme migration + DayNight (completed)

**Status:** done. The app now uses `Theme.Material3.DayNight` (see
`app/src/main/res/values/styles.xml`) with follow-system dark mode.

Migrated in three phases, each verified on-device before the next:
1. **Bridge** — parent → `Theme.MaterialComponents.*.Bridge`; migrated the sole
   `CardView` → `MaterialCardView` and dropped the `androidx.cardview` dep. The
   bridge added the Material theme attributes widgets require (`MaterialCardView`
   crashes under a plain AppCompat theme) with no visual restyle.
2. **Full Material Components** — parent → `Theme.MaterialComponents.Light.DarkActionBar`;
   mapped the palette onto the semantic color model.
3. **Material 3 + DayNight** — parent → `Theme.Material3.DayNight`; added
   follow-system dark mode (`AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM` in
   `App.kt`) and `values-night/` overrides.

**Key decisions (read before touching theming):**
- **`colorPrimary` = the pink accent, not the dark grey.** M3 merges the
  primary/accent roles, so buttons, dialog text-buttons, and control tints
  (switch/checkbox/seekbar/radio) all follow `colorPrimary`. Mapping it to the
  accent keeps controls legible in both light and dark (dark grey would vanish on
  a dark surface at night).
- **The dark top/nav bars are driven by the `@color/colorPrimary` *resource*,
  not `?attr/colorPrimary`.** Bar backgrounds (`activity_main`, `activity_novel_detail`)
  and the programmatic system-bar scrims (`insets.kt`, `NovelTextBaseFullScreenActivity`)
  reference the resource so they stay dark grey in both modes regardless of the
  now-pink `colorPrimary` attribute.
- **The reader canvas does not participate in DayNight.** Reading colors come
  from `ReaderConfig` (user-chosen), and the reader module has no theme/color
  refs; system dark mode must never invert the reading area. The reader's
  always-dark bottom control chrome intentionally has no night variant.
- **FABs** pin `backgroundTint` to the accent via `@style/AppTheme.Fab` (M3
  defaults FAB background to the off-brand `colorPrimaryContainer` lavender).
- **Dark-mode content text** works because nearly all text routes through
  `@color/textBlack` / `@color/textDefault`, overridden in `values-night/colors.xml`.

**Dialog/surface polish (also done):** dialogs, popups, the snackbar, and
preference screens were pushed onto a neutral grey-scale surface
(`colorDialogSurface`, a touch lighter than the content surface in dark) with
M3's pink elevation overlay disabled. Key gotcha for future work: an AppCompat
dialog's window background is a shape drawable whose color re-resolves from a
theme attribute, so theme-level `colorSurface` overrides and `setTint()` don't
stick — `View.kt#applyNeutralSurface()` replaces the drawable on show(), and all
dialogs route through it (`showWithNeutralSurface()` / on-show listeners). Text
tints (`android:textColorPrimary/Secondary`, `colorOnSurfaceVariant`) are pinned
because M3's on-surface literals carry a lavender cast.

**Future (not done):** dynamic color (Material You / `DynamicColors`) and a
user-facing Light/Dark/System toggle (currently follow-system only).

## Jetpack Compose UI migration (assessment — not started)

**Status:** not planned yet — captured so the trade-off is on record. This is a
reflection prompted by how much on-device back-and-forth the DayNight styling
took; the question was whether a modern Compose rewrite would have avoided it.

**Why the theming took many rounds (root causes):**
1. *Inherent to visual migration, not code quality.* Theme changes compile but
   only reveal problems at runtime — often only in dark mode, on a specific
   device, while scrolling. There's no static check for "this dialog renders
   pink at night." The change → look → fix loop is structural to UI work; it was
   slow here mainly because verification was remote (no local emulator in the
   loop).
2. *The app fights M3's defaults by design.* Dark-grey bars + a single pink
   accent + neutral surfaces is a custom look layered on M3, which wants
   accent-colored bars, primary-tinted elevated surfaces, and a merged
   primary/accent role. Much effort went into opting out (`elevationOverlayEnabled=false`,
   resource-vs-attr bar colors, pinned on-surface text). A from-scratch app that
   *embraced* M3 would hit almost none of this — but the same custom look would
   cost the same in any codebase, new or old.
3. *Genuinely dated View-system patterns — the real "old code" tax.* The
   multi-round dialog-surface chase (InsetDrawable → GradientDrawable → theme-attr
   re-resolution; `textColorAlertDialogListItem` vs `colorOnSurfaceVariant`; the
   deprecated preference `setTargetFragment`) and the edge-to-edge +
   nested-CoordinatorLayout + CollapsingToolbar overlap fight are artifacts of
   XML Views + theme-attribute indirection + AlertDialog/PreferenceFragmentCompat.

**What Compose would change:** one `MaterialTheme { ColorScheme }` that every
component reads directly — change a surface color once and dialogs, menus,
snackbar, sheets all update. No `?attr` resolution, no `values-night/` parallel
tree (`isSystemInDarkTheme()` picks the scheme), no runtime-drawable-vs-theme
mismatch. Most of this session's multi-round chases would have been single theme
values. Category (3) largely disappears; categories (1) and (2) do **not** — the
per-screen on-device verification loop and any fight against M3 defaults remain.

**Costs / caveats (why it's not obviously worth it):**
- It's a real rewrite of every non-reader screen (main, detail, search, backup,
  settings, download, book list), plus MVP→state and ViewBinding→Compose.
- **The reader is the hard, app-specific part** and does *not* map cleanly:
  custom canvas pagination/animations (`pager` module, `NovelTextActivity`).
  Realistically it stays a View hosted in Compose (`AndroidView`), which is its
  own interop seam.
- Trades this set of papercuts for Compose's own (state hoisting, recomposition
  perf, View interop for the reader, less mature preference tooling).

**Scope of work (non-reader screens).** Measured surface: 10 activities, 8
fragments, ~12 MVP presenter/view pairs, 30 layout XML files, 13 adapters. The
work per screen is *not* just layout translation (that's roughly a third of it):

1. **Layout XML → composables.** The mechanical part.
2. **MVP `Presenter<View>` → state holder** (`ViewModel` + `StateFlow`/state).
   The bulk of the work: the `MvpView` callbacks (`showX` / `showError` /
   `showMessage`) become observed state, and the presenter's coroutine logic
   moves into the ViewModel. ~12 presenters.
3. **RecyclerView `Adapter` → `LazyColumn`.** The shared novel-item adapter
   (`list/NovelViewHolder` + `NovelListAdapter`, reused by bookshelf / history /
   book list / search) becomes one `NovelItem` composable — high leverage, do it
   first.
4. **Cross-cutting:** a `MaterialTheme`/`ColorScheme` built from the current
   palette; navigation (Intent-based today — either keep Intents between
   Activities or adopt Compose navigation); dialogs (the `applyNeutralSurface`
   machinery goes away — dialogs read the theme); a dark-mode pass on every
   screen.

Screens by difficulty:
- *Simple* (toolbar + list/form): bookshelf, history, download, site choose,
  site settings.
- *Moderate* (real state/interaction): main (tabs + pager + FAB + edge-to-edge),
  detail (collapsing header), book list, backup (many dialogs + SAF flows),
  settings.
- *Trickiest:* the search cluster (fuzzy / single / site, WebView-based) and the
  shared `list` package.

Known long-poles: **settings** — Compose has no first-party preference library,
so preference screens are hand-built or use a third-party lib; and the **search
WebView** flows need `AndroidView` interop.

**Recommendation:** only worth it if the UI will keep evolving (aligns with the
project's "easy to maintain, live longer" goal). If the app is now in
"works/leave it alone" mode, the current theming is complete and a rewrite won't
pay back. If undertaken, migrate screen-by-screen (shared `NovelItem` and the
theme first, then simple → moderate → search) and leave the reader as a hosted
View initially.
