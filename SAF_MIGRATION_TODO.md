# SAF Migration TODO

Goal: move **all** file access to the Storage Access Framework (SAF) so the app needs
**no** broad storage permissions, then drop `MANAGE_EXTERNAL_STORAGE` /
`READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` / `requestLegacyExternalStorage`.

SAF = `ACTION_OPEN_DOCUMENT` (read), `ACTION_CREATE_DOCUMENT` (save), `ACTION_OPEN_DOCUMENT_TREE`
(folder), accessed via `ActivityResultContracts` and `contentResolver.openInputStream/openOutputStream`.

## Already on SAF (done — reference implementations)

- [x] Novel **export** → `CreateDocumentActivity` bridge + `contentResolver.openOutputStream`
      (`local/CreateDocumentActivity.kt`, `local/NovelExporter.kt`). Use this as the pattern
      for popping a picker from a bare `Context` deep in the UI.
- [x] Local novel **open/import** → `ActivityResultContracts.OpenDocument` in `MainActivity.kt:58`,
      stream read in `DataManager.importLocalNovel` (`DataManager.kt:344`).
- [x] Reader **background image** / **font** picking → `OpenDocument` in `NovelTextActivity.kt:76,90`.
- [x] Backup **import "select file"** → `OpenDocument` in `BackupActivity.kt` (`openDocumentLauncher`).

## 1. Backup screen — the main remaining offender

Backup still builds `file://` paths and falls back to raw-filesystem access, which is the
reason the storage permissions still exist.

- [ ] **Export default paths** (`BackupPresenter.kt:60,62`): replace the `Uri.fromFile(...)`
      `PaNovel-Backup-N.zip` radio options with a `CreateDocument("application/zip")` flow
      (reuse `CreateDocumentActivity` or a registered launcher in `BackupActivity`).
- [ ] **"Other path"** (`BackupPresenter.kt:67`): drop `Environment.getExternalStorageDirectory()`;
      the editable raw-path field should be replaced by SAF pick (open for import, create for export).
- [ ] **Rework `ActivityExportBinding` UI**: the radio group currently is {old file path,
      new file path, other path, WebDAV}. Re-model as {WebDAV, "choose file/location via SAF"}.
- [ ] **Remove backup permission machinery** once paths are all SAF:
      `BackupActivity.requestPermissions()` (`BackupActivity.kt:166`), `requestPermissionsLauncher`,
      `permissionLauncher`, and the `isExternalStorageManager()` / `READ_EXTERNAL_STORAGE`
      checks in `BackupPresenter.kt:108-110,158-160`.

## 2. Persistable URI permissions (correctness gap)

Reader background image & font are stored as URIs in settings and read across app restarts.
SAF URIs lose access after restart unless persisted — this is exactly why
`NovelTextActivity.kt:83,98` catches `SecurityException` and re-requests `READ_EXTERNAL_STORAGE`.

- [ ] Call `contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`
      when the user picks background image / font (`NovelTextActivity.kt:76-103`), and pass
      `FLAG_GRANT_PERSISTABLE_URI_PERMISSION` in the open intent.
- [ ] Once persisted access works, delete the `SecurityException` → `requestPermissions`
      fallbacks for background image and font.

## 3. Verify local-novel reading needs no permission (likely already fine)

- [ ] Confirm: import copies the picked file into internal `files/local`
      (`LocalManager.saveNovel`, `LocalManager.kt:134`) and the reader reads that internal copy
      via `File(novel.detail)` (`LocalNovelProvider.kt:18`). If so, reading needs no storage
      permission and nothing changes here — just document it.

## 4. Drop storage permissions from the manifest (final step)

After 1–3, remove from `app/src/main/AndroidManifest.xml`:

- [ ] `MANAGE_EXTERNAL_STORAGE` (line 7)
- [ ] `READ_EXTERNAL_STORAGE` (line 8)
- [ ] `WRITE_EXTERNAL_STORAGE` (line 15)
- [ ] `android:requestLegacyExternalStorage="true"` (line 24)
- [ ] Verify no remaining `Environment.getExternalStorage*` / `File(<external path>)` usages:
      `grep -rn "getExternalStorageDirectory\|READ_EXTERNAL_STORAGE\|MANAGE_EXTERNAL_STORAGE" app/src`

## 5. Cleanup / follow-ups

- [ ] Remove the now-dead `exportLocation` setting (`LocationSettings` object was deleted with the
      路径设置 page; confirm nothing references export/backup file-path prefs anywhere).
- [ ] Decide whether to migrate WebDAV-unrelated temp files (`BackupManager.getTempFile`,
      cache dirs) — these are app-private, no permission needed, probably leave as-is.
- [ ] Update `CLAUDE.md` Key Patterns once permissions are gone (note "no storage permissions").

## Notes

- `FileProvider` for QR share (`BookListFragment.kt:159`) is fine — it's app-private + grant URI,
  not broad storage. Leave it.
- The `filepicker` module has already been removed; do **not** reintroduce a custom picker.
