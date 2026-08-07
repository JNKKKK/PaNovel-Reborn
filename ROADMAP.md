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

**Future (not done):** dynamic color (Material You / `DynamicColors`), a
user-facing Light/Dark/System toggle (currently follow-system only), and
optionally migrating the many `AlertDialog.Builder` sites to
`MaterialAlertDialogBuilder` (they already pick up Material dialog styling from
the theme, so this is cosmetic).
