# Roadmap

Deferred work and future improvements for PaNovel. These are intentionally
not-done-yet decisions with their rationale, so the reasoning isn't lost.

## Material Components theme migration (deferred)

**Status:** deferred — not worth it for maintenance alone.

The app uses `Theme.AppCompat.Light.DarkActionBar` (see `app/src/main/res/values/styles.xml`).
Migrating to `Theme.MaterialComponents` (and eventually Material 3) would unlock
the Material widget set and a modern semantic theming system.

**Trigger to revisit:** when we actually want Material widgets (`MaterialCardView`,
`MaterialButton`, `MaterialAlertDialogBuilder`, `TextInputLayout`, `Chip`, …), a
visual refresh, or dark-mode/dynamic-color support — *not* as part of routine
dependency maintenance.

**Benefits**
- Access to Material Components widgets (currently unusable — `MaterialCardView`
  crashes under an AppCompat theme with "requires Theme.MaterialComponents").
- Modern semantic color attributes (`colorSurface`, `colorOnSurface`, …) and a
  cleaner theming model; foundation for DayNight/dark mode.
- Foundation for Material 3 / dynamic color if a visual refresh is ever wanted.
- Aligns with where the AndroidX ecosystem is heading.

**Costs / risks**
- `Theme.MaterialComponents` restyles every widget (buttons, the many
  `AlertDialog.Builder` dialogs, text fields, switches, action bar). Not a
  one-line parent swap — Google recommends going via a `*.Bridge` theme first.
- App-wide visual-regression surface: every screen needs re-verification.
- The dark-grey `colorPrimary` and the reader's custom color-scheme logic would
  need re-mapping onto the new attribute model.

**Recommended approach when undertaken**
1. Switch to a `Theme.MaterialComponents.*.Bridge` parent first; verify no
   regressions.
2. Then move to the full Material (or Material 3) theme.
3. Verify every screen on-device (theme changes pass compile but can crash or
   regress at runtime — e.g. the `MaterialCardView` crash that surfaced this).
4. Only then migrate `androidx.cardview` → `MaterialCardView` and adopt other
   Material widgets.

**Related:** this was surfaced while cleaning up abandoned dependencies.
`androidx.cardview` (frozen at 1.0.0) is kept as-is for now — abandoned but
stable, like `jchardet` and `kxml2`.
