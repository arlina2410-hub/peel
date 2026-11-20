# Peel — Notes that stick.

Peel puts a realistic sticky note on top of your own phone wallpaper, so your list or reminder is there every time you wake your phone.

Everything stays on your phone. There is no account, no cloud, no ads and no internet permission.

---

## Get the app on your phone (no coding needed)

You only need a GitHub account and your phone.

### 1. Put this folder on GitHub

If a repository already exists, skip to step 2.

1. Create a new repository on GitHub (private is fine).
2. Upload this whole folder to it. Keep the hidden `.github` folder — that is where the build lives.
   The easiest way is GitHub Desktop: **File → Add local repository**, choose this folder, then **Publish repository**.

### 2. Run the build

1. Open the repository on GitHub.
2. Click **Actions**.
3. Click **Build APK** in the left list, then **Run workflow → Run workflow**.
   (A build also starts automatically every time the code changes.)
4. Wait for the green check mark. The first build takes a few minutes.

### 3. Download the APK

1. Open the finished run.
2. Scroll to **Artifacts** at the bottom.
3. Download **Peel-debug-apk**. It is a `.zip` file.
4. Unzip it. You will find `app-debug.apk` inside.

### 4. Install it

1. Copy the APK to your phone (USB cable, Google Drive, email — any way works).
2. Tap the APK on the phone.
3. If Android asks, allow **Install unknown apps** for the app you opened it with (Files, Chrome, Drive…). This is normal for apps that do not come from the Play Store.
4. Tap **Install**, then **Open**.

### Updating later

Download the new APK and install it over the old one. Your notes are kept.
This works because the repository includes the same `keystore/peel-debug.jks` signing key for every build. Do not delete or replace that file unless you are happy to uninstall the app (and lose its notes) first.

### First run

1. Read the three welcome screens.
2. Tap **Use my current wallpaper**.
   If your phone says no (Android 13 and newer, or a live wallpaper), tap **Choose a photo instead** and pick the picture you use as your wallpaper.
3. Create your first note.

Your wallpaper itself is not changed in any way other than the notes drawn on top. The original is saved privately inside the app.

---

## If something looks wrong

**The note is not on my lock screen**
- Tap the refresh button on the Notes screen, then lock your phone.
- Some phones rotate lock-screen wallpapers automatically (“Wallpaper Carousel”, “Glance”, “Lock screen magazine”). Turn that off in your phone's wallpaper settings.
- Live/animated wallpapers cannot be used. Choose a still photo in Peel.
- If you changed your wallpaper, open Peel and accept the “Your wallpaper changed” banner.
- Open **Settings → Why doesn't my note show?** for the full list. Some phone makers replace lock-screen wallpapers and cannot be fully controlled by any app.

**I want my normal wallpaper back**
Open **Settings → Restore original wallpaper**.

**The APK will not install**
- Make sure the old Peel app was not installed from a different signing key. Because this repo always uses the same debug key, updates should install over each other.
- Android 8.0 or newer is required.

**The build failed**
Open the failed run in **Actions**, click the red step, and read the last lines of the log. If you send that text back, it can be fixed.

---

## Optional: signed release build

A debug APK is enough for testing. If you later want a release-signed APK, add these four repository secrets under **Settings → Secrets and variables → Actions**:

- `KEYSTORE_BASE64` — your keystore file, base64-encoded
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

When all four exist, the build also produces **Peel-release-apk**. When they do not, the release step is skipped and the build still succeeds.

---

## For developers

- Kotlin + Jetpack Compose, single activity, Navigation Compose.
- Room for notes, DataStore for settings, app-private files for the original wallpaper.
- One shared `NoteRenderer` draws both the preview and the final wallpaper bitmap.
- Min SDK 26, compile/target SDK 36, JDK 17.
- Build: `./gradlew assembleDebug`
- Tests: `./gradlew testDebugUnitTest`
- Versions are pinned in `gradle/libs.versions.toml`.
- Choices and trade-offs are listed in `DECISIONS.md`; font licensing is in `LICENSES.md`.

The app deliberately has **no `INTERNET` permission**.
