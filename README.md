# Music
Android music player for handling intent filters.

## Screenshots
To be added soon

## Downloads
Built APKs are uploaded in [GitHub Releases](https://github.com/spir0th/music/releases/latest).

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