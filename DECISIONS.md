# Decisions and trade-offs

These are the choices made where the PRD left room, plus the known limits of v1.

## Platform and build

- **Toolchain:** AGP 8.13.2, Kotlin 2.2.21, Compose BOM 2025.10.01, Gradle 8.14.3, JDK 17 in CI. All versions are pinned in `gradle/libs.versions.toml` and were resolved from Google Maven / Maven Central.
- **SDK:** min 26, compile 36, target 36. CI installs Android SDK 36 explicitly so the runner image cannot surprise the build.
- **No Android Studio dependency:** the Gradle wrapper is committed and the GitHub Actions workflow runs the tests and builds the debug APK.
- **Committed debug keystore:** `keystore/peel-debug.jks` signs every debug APK with the same key. This lets the owner install build N+1 over build N without uninstalling and losing notes. It is a debug key only, not a Play signing key.
- **No `INTERNET` permission**, and the manifest removes it defensively with `tools:node="remove"`. No analytics, accounts, crash reporting or network libraries.
- **Room** has no destructive fallback migration. Version 1 has one schema; future changes must ship a real migration so notes are never wiped.

## Wallpaper capture and applying

- **No `MANAGE_EXTERNAL_STORAGE`.** The PRD allowed it only if Strategy A truly needs it. Rather than ask for a heavy, Play-restricted permission, Peel tries Strategy A and falls back to the system photo picker on any refusal. This is friendlier and keeps the app installable.
- **Strategy A is best-effort.** On Android 13+ some devices return nothing or a default image for wallpaper reads. The picker path is fully implemented and is the guaranteed path.
- **Live wallpapers** are detected with `wallpaperInfo` and sent straight to the picker with a plain-English explanation.
- **The picker sets both bases** from the one chosen photo. The PRD only requires a working picker fallback; asking the user for two nearly identical photos would add friction. Users can re-capture later.
- **Base files are lossless PNGs** in `filesDir/base/`. Reads are sampled to at most 12 MP to avoid memory crashes on 4K/8K photos; the saved copy is still lossless, just capped on very large images.
- **Screen size:** apply uses `WindowManager.maximumWindowMetrics` (or the real display size before API 30). The preview fills the Compose window. On phones with unusual cutouts the final wallpaper can shift by a few pixels; launchers crop differently anyway.
- **Wallpaper-change protection:** if the current wallpaper ID differs from the one Peel saved, applying is blocked and the Notes banner asks the user to re-capture. This prevents Peel from silently overwriting a wallpaper the user just chose. “Restore original wallpaper” is also a no-op when the wallpaper was changed elsewhere, because the user's new wallpaper is already the original.
- **Restoring:** when the last note for a target is deleted, the untouched base is set back and the target is marked unmodified. If no base exists, the system default is restored with `WallpaperManager.clear`.

## Notes and rendering

- **One renderer:** `NoteRenderer` is the only code that paints a note. Compose previews call it through `drawIntoCanvas`/`nativeCanvas`, and the wallpaper generator calls the same method onto a bitmap. Shadows are drawn as layered translucent shapes instead of `BlurMaskFilter`/`setShadowLayer`, so they render identically on hardware preview canvases and software wallpaper bitmaps.
- **Height cap:** a note is 300 units wide with a minimum height of 300 and a maximum of 480 (1.6× width). Long content is ellipsized, and the editor shows “Text is long. Shorten it or make the note bigger.” This is the PRD's requested cap.
- **Width cap:** the chosen width fraction is additionally capped so a note can never be taller than the screen in landscape/tablet layouts.
- **Drag bounds:** the note center is clamped to 8–92% horizontally and 5–95% vertically, so it cannot be dragged completely off-screen.
- **List editing:** reordering uses an item menu (Move up / Move down) instead of a drag handle. It is accessible, works with one hand, and avoids a fragile drag implementation in v1. Checked indexes are re-mapped when blank items are removed so the wallpaper always strikes the right lines.
- **List/save normalization:** blank list items are dropped on save. Ticked items are stored as comma-separated indexes per the PRD data model.
- **Timestamp:** formatted with the device locale and 12/24-hour setting, stored as epoch milliseconds and never changed on edit. `updatedAt` is not drawn on the note.
- **Fonts:** Nippo for the UI, Excon for notes. Android's system fallback handles Devanagari/Bengali glyphs that Excon does not contain.

## UI

- **iOS-like restyle:** custom segmented controls, grouped inset cards, sheet actions, large titles, accent color and generous corners. Material 3 is used only as the engine.
- **Preview:** full-window preview with the real saved wallpaper, live gestures (drag, pinch, twist), sliders, snap lines and haptic ticks. Existing notes are drawn underneath. The editing note gets a yellow outline that is not part of the wallpaper.
- **Mock overlay:** a generic clock/notifications/shortcuts (lock) or status bar/grid/dock (home). It is illustrative; real layouts vary by phone and launcher.
- **Gesture area:** the whole preview accepts drag/pinch/twist rather than requiring a precise hit on the note. This is more forgiving on a phone.
- **Undo/discard:** backing out of the editor discards the draft. Backing out of the preview keeps it so the user can return and adjust. Saving commits it.
- **Recycle Bin:** never auto-deletes. Restore and Delete forever both reappear at their old position/color because all geometry is stored.
- **No backup/export in v1.** `allowBackup=false` and data-extraction rules exclude everything, per the privacy requirement. Uninstalling therefore removes the notes.

## Known limits

- The note is part of the wallpaper image: it cannot be tapped or ticked on the lock screen, and the phone's clock/notifications sit above it.
- Some OEM lock-screen magazine features replace the wallpaper. There is help text and change detection, but no app can override every manufacturer skin.
- Home-screen launchers may crop or parallax the wallpaper, so the note can shift slightly behind icons.
- The picker fallback is the reliable route on Android 13+ because wallpaper reads may be restricted without All files access.
- Debug APKs are for direct installation, not Play Store distribution.
