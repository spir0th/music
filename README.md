# Music
![GitHub Release](https://img.shields.io/github/v/release/spir0th/music?sort=semver&label=version)
![GitHub commit activity](https://img.shields.io/github/commit-activity/t/spir0th/music)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/spir0th/music/build.yml)
![Maintenance](https://img.shields.io/maintenance/yes/2024)
![GitHub Issues](https://img.shields.io/github/issues/spir0th/music)

A basic Android music player with it's purpose to handle URL data sent by your apps!

## Overview
Music uses Android Intent Filters to play audio files (which can be sent by 3rd-party apps with the `Open With` option) and lets ExoPlayer and
Media3 API handle the audio playback, which integrates well with other media apps. Music also features a Material You theme (with a slight MD2-ish design)
and supports Dynamic Colors! (introduced in Android 13)

Customizing Music is also a thing, it's settings can be accessed by using the App info page on your phone's settings.

## Screenshots
| ![player](https://github.com/spir0th/music/assets/66259245/cd12e3fe-70fc-4f75-b686-07616addf64f=50x50) | ![settings](https://github.com/spir0th/music/assets/66259245/530a1f83-0831-49f5-86e4-8723cf13a54a=50x50) |
|--------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|

## Download
APKs are uploaded through [GitHub releases](https://github.com/spir0th/music/releases) whenever a version is released.

If you have problems with these APKs, use the feedback feature as it gets sent into my Supabase project
or just use the [repository issues](https://github.com/spir0th/music/issues) as the problem gets solved immediately by other contributors.

## Building
### Feedback API key
The feedback feature requires an API key of a Supabase project to be set in order to upload
feedbacks. To disable it, put this on your `local.properties` file:
```
supabase.ApiKey=null
```

Or, if you want to enable feedbacks, create a Supabase project then put it's API key like this:
```
supabase.ApiKey=<YOUR API KEY>
```

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
