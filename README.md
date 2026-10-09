# Chess Review (com.sangeetastudios.chessreview)

Android app that reviews chess games from a PGN — shared from Chess.com (Share → PGN),
pasted, or opened as a `.pgn` file — using an on-device Stockfish engine. Each move is
classified (Best / Excellent / Good / Inaccuracy / Mistake / Blunder / Forced) and shown on
the board with badges, best-move arrows, an eval bar, eval graph and per-side accuracy.

## Build

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./scripts/build-stockfish.sh          # once: builds Stockfish (arm64-v8a, x86_64) into app/src/main/jniLibs
./gradlew testDebugUnitTest           # parser / classifier tests
./gradlew installDebug
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
