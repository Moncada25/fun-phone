# Fun Phone - Developer Cheatsheet

Quick reference for common development tasks.

## 🏗️ Building

```bash
./gradlew assembleDebug
./gradlew assembleRelease

./gradlew bundleDebug
./gradlew bundleRelease

./gradlew clean build
```

## 🧪 Testing

```bash
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest

./gradlew testDebugUnitTest --tests "com.bookverse.contacts.ContactRepositoryTest"

./gradlew jacocoTestReport
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## 🔍 Code Quality

```bash
./gradlew lintDebug
./gradlew lintRelease

./gradlew ktlintCheck
./gradlew ktlintFormat

open app/build/reports/lint-results-debug.html
```

## 📦 Artifacts

```bash
./gradlew getApk
./gradlew preparePlayStoreArtifacts

find app/build/outputs/apk -name "*.apk"
find app/build/outputs/bundle -name "*.aab"
```

## 🗄️ Database

```bash
adb shell run-as com.bookverse.contacts
cd databases
sqlite3 contacts.db

.tables
.schema Contact
SELECT * FROM contacts LIMIT 10;
.quit
```

### Export Schema
```bash
./gradlew assembleDebug
ls core/database/schemas/com.bookverse.contacts.data.db.AppDatabase/
```

## 🔥 Firebase

```bash
firebase login
firebase projects:list
firebase use contacts-app

firebase emulators:start
firebase emulators:export ./firebase-backup
firebase emulators:import ./firebase-backup
```

## 📱 Device/Emulator

```bash
adb devices
adb shell
adb logcat -s FunPhone:V

adb install app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk

adb uninstall com.bookverse.contacts
adb shell pm clear com.bookverse.contacts
```

### Get App Info
```bash
adb shell dumpsys package com.bookverse.contacts | grep versionName
adb shell dumpsys package com.bookverse.contacts | grep versionCode
adb shell pm path com.bookverse.contacts
```

### Permissions
```bash
adb shell dumpsys package com.bookverse.contacts | grep permission
adb shell pm grant com.bookverse.contacts android.permission.READ_CONTACTS
adb shell pm revoke com.bookverse.contacts android.permission.READ_CONTACTS
```

## 🎨 Resources

```bash
grep -r "TODO" app/src/main/
grep -r "FIXME" app/src/main/

find app/src/main/res -name "*.xml" | xargs grep "hardcoded"
```

## 📊 Dependencies

```bash
./gradlew app:dependencies
./gradlew app:dependencies --configuration releaseRuntimeClasspath

./gradlew dependencyUpdates
./gradlew buildEnvironment
```

## 🔐 Signing

```bash
keytool -list -v -keystore my-release-key.jks

keytool -genkey -v \
  -keystore release.keystore \
  -alias funphone-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

base64 -i my-release-key.jks | pbcopy
```

## 🚀 GitHub CLI

```bash
gh auth login
gh repo view

gh workflow list
gh workflow run release.yml
gh workflow view android.yml

gh run list
gh run view 1234567890
gh run download 1234567890

gh release list
gh release create v3.12.9 app/build/outputs/apk/release/app-release.apk
```

### Setup Release Secrets
```bash
./.github/setup-release-secrets.sh

gh secret list
gh secret set RELEASE_STORE_PASSWORD
gh secret delete RELEASE_STORE_PASSWORD
```

## 🏷️ Git Tags

```bash
git tag
git tag -a v3.12.9 -m "Release v3.12.9"
git push origin v3.12.9
git push origin --tags

git tag -d v3.12.9
git push origin --delete v3.12.9
```

## 📈 Performance

```bash
adb shell am start -n com.bookverse.contacts/.MainActivity --start-profiler profile.trace
adb pull /data/local/tmp/profile.trace
android-studio profile.trace

adb shell dumpsys meminfo com.bookverse.contacts
adb shell dumpsys cpuinfo | grep bookverse
```

### Measure Startup Time
```bash
adb shell am start -W com.bookverse.contacts/.MainActivity
```

## 🧹 Clean

```bash
./gradlew clean
./gradlew cleanBuildCache

rm -rf .gradle build app/build
rm -rf ~/.gradle/caches/

find . -type d -name "build" -exec rm -rf {} +
```

## 📝 Logs

```bash
adb logcat *:S FunPhone:V

adb logcat -c
adb logcat -d > logcat.txt
adb logcat | grep -i "crash\|error\|exception"

adb logcat -b crash
```

### Crashlytics
```bash
firebase crashlytics:symbols:upload \
  --app=1:xxx:android:xxx \
  app/build/outputs/native-debug-symbols/release/native-debug-symbols.zip
```

## 🌍 Localization

```bash
find app/src/main/res -name "strings.xml"
diff app/src/main/res/values/strings.xml app/src/main/res/values-es/strings.xml

grep -r "hardcoded text" app/src/main/java
```

## 🎯 Gradle Properties

```bash
./gradlew assembleRelease \
  -PRELEASE_STORE_FILE=release.keystore \
  -PRELEASE_STORE_PASSWORD=pass123 \
  -PRELEASE_KEY_ALIAS=key-alias \
  -PRELEASE_KEY_PASSWORD=pass123

RELEASE_STORE_FILE=release.keystore ./gradlew assembleRelease
```

## 📲 Install & Run

```bash
./gradlew installDebug
adb shell am start -n com.bookverse.contacts/.MainActivity

./gradlew installDebug && adb shell am start -n com.bookverse.contacts/.MainActivity
```

## 🔄 Sync & Index

```bash
./gradlew --refresh-dependencies
./gradlew --rerun-tasks

invalidate IDE cache and restart Android Studio
```

## 📊 Build Scan

```bash
./gradlew build --scan

open https://scans.gradle.com/
```

## 🎪 Emulator

```bash
emulator -list-avds
emulator -avd Pixel_7_API_36

emulator -avd Pixel_7_API_36 -no-snapshot-load
emulator -avd Pixel_7_API_36 -wipe-data
```

## 🔍 APK Analysis

```bash
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --mode=universal

unzip app.apks

aapt dump badging app-release.apk
aapt list -v app-release.apk
```

### APK Size
```bash
ls -lh app/build/outputs/apk/release/app-release.apk
du -sh app/build/outputs/apk/release/app-release.apk
```

## 📋 Version Info

```bash
./gradlew -version
java -version
kotlin -version

cat app/build.gradle.kts | grep "VERSION"
```

## 🎨 Compose

```bash
find app/src -name "*.kt" | xargs grep "@Composable" | wc -l
find app/src -name "*.kt" | xargs grep "@Preview"

./gradlew :app:assembleDebug -Pandroid.composeCompilerTrace=true
cat app/build/compose_compiler/debug/composables.txt
```

## 💾 Backup & Restore

```bash
adb backup -f backup.ab -noapk com.bookverse.contacts
adb restore backup.ab

adb pull /sdcard/FunPhone/backups/ ./backups/
adb push ./backups/ /sdcard/FunPhone/backups/
```

---

## 🔗 Useful Links

- [Gradle Tasks Documentation](https://docs.gradle.org/current/userguide/command_line_interface.html)
- [ADB Commands](https://developer.android.com/studio/command-line/adb)
- [GitHub CLI Manual](https://cli.github.com/manual/)
- [Bundletool](https://developer.android.com/studio/command-line/bundletool)

---

**Tip:** Add aliases to your `.zshrc`:

```bash
alias apk='./gradlew assembleDebug'
alias test='./gradlew testDebugUnitTest'
alias lint='./gradlew lintDebug'
alias install='./gradlew installDebug && adb shell am start -n com.bookverse.contacts/.MainActivity'
```

