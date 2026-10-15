# Chess Review (com.sangeetastudios.chessreview)

Android app that reviews chess games from a PGN — shared from Chess.com (Share → PGN),
pasted, or opened as a `.pgn` file — using an on-device Stockfish engine. Each move is
classified (Best / Excellent / Good / Inaccuracy / Mistake / Blunder / Forced) and shown on
the board with badges, best-move arrows, an eval bar, eval graph and per-side accuracy.

## Build for personal use

Everything needed is in this repo except files that are regenerated locally (`.gitignore`):
the Stockfish binaries, `local.properties` and build outputs. A fresh clone rebuilds like this.

**Prerequisites**

- JDK 17 or newer (Android Studio's bundled JBR works).
- Android SDK with platform 36 and build-tools; plus the **NDK** (Android Studio → SDK Manager →
  SDK Tools → NDK, or `sdkmanager "ndk;28.2.13676358"`).
- `git`, `make` and an internet connection (the first engine build downloads Stockfish's
  neural-network files, ~80 MB). macOS or Linux.

**Steps**

```sh
git clone https://github.com/sangeeta-studios/chess-review.git
cd chess-review

# Tell Gradle where the SDK is (or export ANDROID_HOME instead):
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # Linux: $HOME/Android/Sdk

# macOS with Android Studio; on Linux point JAVA_HOME at any JDK 17+.
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./scripts/build-stockfish.sh     # builds the engine into app/src/main/jniLibs (a few minutes)
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
```

Install it on a phone with USB debugging on (`./gradlew installDebug`, or
`adb install -r app/build/outputs/apk/debug/app-debug.apk`), or copy the APK to the phone and
open it. The build stops with an explanation if the engine hasn't been built yet.

**Debug signing:** debug APKs are signed with `~/.android/debug.keystore`, which lives on your
machine, not in this repo. A different machine (or a lost key) produces a different signature,
so Android won't install over an existing copy: uninstall it first, which deletes the games
saved in the app. Back up `~/.android/debug.keystore` if that matters to you.

**Tests**

```sh
./gradlew testDebugUnitTest           # parser / classifier tests (no engine needed)
./gradlew connectedDebugAndroidTest   # engine smoke test on a device/emulator
```

Simulate a share from Chess.com:

```sh
adb shell am start -a android.intent.action.SEND -t text/plain \
  --es android.intent.extra.TEXT "$(cat game.pgn)" \
  -n com.sangeetastudios.chessreview/.MainActivity
```

## Release (Google Play)

1. Create an upload key and `keystore.properties` in the project root (gitignored):
   ```
   storeFile=upload.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
2. Bump `versionCode`/`versionName` in `app/build.gradle.kts`.
3. `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

Play compliance notes: targetSdk 36, 64-bit only ABIs, native code 16 KB page aligned,
no permissions, edge-to-edge, adaptive + themed icon, R8 minified. Privacy policy text is in
`docs/privacy-policy.md` (host it at a public URL for the listing). Data safety form: no data
collected or shared.

Before the first release, set `chessreview.sourceUrl` in `gradle.properties` to the public
GitHub repo; release builds fail without it so every shipped version links to its source.
Tag each Play release in git (e.g. `v1.0.0`).

## License

Copyright (C) 2026 Sangeeta Studios. Chess Review is free software, licensed under the
[GNU General Public License v3.0 or later](LICENSE). You may sell copies, and anyone who
receives the app may share and modify it under the same license.

Bundled third-party components (Stockfish — GPLv3, chesslib — Apache 2.0, cburnett pieces —
GPLv2+) are listed with their notices in [NOTICE.md](NOTICE.md); their full license texts ship
inside the app under *About → Source code & licenses*.

## Trademarks

"Chess Review", the Sangeeta Studios name and the app icon are not covered by the GPL. Forks
are welcome but must use a different name and icon and must not present themselves as the
official app. Chess Review is not affiliated with Chess.com, Lichess or any other chess platform.
