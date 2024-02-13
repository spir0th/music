# Music
![GitHub Release](https://img.shields.io/github/v/release/spir0th/music?sort=semver&label=version)
![GitHub commit activity](https://img.shields.io/github/commit-activity/t/spir0th/music)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/spir0th/music/build.yml)
![Maintenance](https://img.shields.io/maintenance/yes/2024)
![GitHub Issues](https://img.shields.io/github/issues/spir0th/music)

A basic Android music player with it's purpose to handle URL data sent by your apps!

## Overview
Music uses Android Intent Filters to play audio files (which are sent by 3rd-party apps with the `Open With` option) and lets ExoPlayer and
Media3 API handle the audio playback, which integrates well with other media apps. Music also features a Material You theme (with a slight MD2-ish design)
and supports Dynamic Colors! (introduced in Android 13)

Customizing Music is also a thing, it's settings can be accessed by using the App info page on your phone's settings.

## Screenshots
| ![player](https://github.com/spir0th/music/assets/66259245/fdbf0b20-e5fa-4e9c-9a07-42120a575485) | ![settings](https://github.com/spir0th/music/assets/66259245/4d0a828e-d720-4f63-859f-b7165c6b82de) |
|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|

## Download
APKs are uploaded through [GitHub releases](https://github.com/spir0th/music/releases) whenever a version is released.

## Building
### Android Studio
Open the project in Android Studio, and generate a signed APK by using the application menu:
```
Build > Generate Signed Bundle / APK
```
or debug APKs:
```
Build > Build Bundle(s) / APK(s)
```

### Command-line tools
If you prefer to use command-line, you can generate a signed APK using:
```bash
./gradlew assembleRelease
zipalign -v -p 4 app-unsigned.apk app-unsigned-aligned.apk
apksigner sign --ks my-release-key.jks --out app-release.apk my-app-unsigned-aligned.apk
apksigner verify app-release.apk # Optional: Check if APK is signed properly
```
or debug APK:
```bash
./gradlew assembleDebug
```

## License
This project is licensed under the [GNU Public License v3.0](LICENSE)
