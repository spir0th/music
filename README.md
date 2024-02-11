# Music
Android music player for handling intent filters.

## Screenshots
To be added soon

## Downloads
Built APKs are uploaded in [GitHub Releases](https://github.com/spir0th/music/releases/latest).

## Building
### Feedback API key (optional)
The feedback feature needs a Supabase project created and it's API key set.

This section might be optional, but if you don't tend to use your own API key,
the feedback feature may not work properly and crash the application as intended.

Or, if you really care about feedbacks and prevent it from crashing, define it in `local.properties`:
```
supabase.apiKey=<API KEY HERE>
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