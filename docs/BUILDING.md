# Building Noor Al-Sabeel — تعليمات البناء

**English instructions** (العربية موجزة بالأسفل).

## Requirements

- JDK 17 or newer (Temurin recommended)
- Android SDK with `platforms;android-35` and `build-tools;35.0.0`
  (Android Studio installs both automatically, or use `sdkmanager`)
- Git

## Setup

```bash
git clone https://github.com/AhmedZakeFit/noor-al-sabeel.git
cd noor-al-sabeel
```

Create `local.properties` pointing at your SDK (Android Studio does this for you):

```properties
sdk.dir=/path/to/android-sdk
```

## Run the tests

```bash
./gradlew :core:test :app:testDebugUnitTest
```

Test reports land in `core/build/reports/tests/test/` and
`app/build/reports/tests/testDebugUnitTest/`.

## Build the APK

```bash
./gradlew :app:assembleDebug          # debug APK: app/build/outputs/apk/debug/
./gradlew :app:assembleRelease        # unsigned release unless signing env is set
```

The debug APK is directly installable. The release APK is **unsigned** unless you provide
signing via environment variables (below).

## Signing a release (no secrets in the repo)

The build reads four environment variables:

| Variable | Meaning |
|---|---|
| `NOOR_KEYSTORE_FILE` | Path to a PKCS#12/JKS keystore file on the build machine |
| `NOOR_KEYSTORE_PASSWORD` | Keystore password |
| `NOOR_KEY_ALIAS` | Key alias inside the keystore |
| `NOOR_KEY_PASSWORD` | Key password |

```bash
keytool -genkeypair -v -keystore noor-release.keystore -alias noor \
  -keyalg RSA -keysize 2048 -validity 10000
export NOOR_KEYSTORE_FILE=$PWD/noor-release.keystore
export NOOR_KEYSTORE_PASSWORD=...
export NOOR_KEY_ALIAS=noor
export NOOR_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

GitHub Actions uses the same variables from repository **secrets**
(`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD`) — the keystore itself never enters the repository. See
`.github/workflows/release.yml`.

**Keep your keystore safe.** Without it you cannot ship an update that installs over the
existing app. Back it up (but never inside the repo).

## Development notes

- `core` is a pure JVM Kotlin module — fast tests without an emulator.
- Manual DI (`AppContainer`), no annotation processors, no DI framework: keeps builds
  fast and F-Droid-friendly.
- Release v1.0 ships without R8 shrinking for easier debugging and reproducibility; see
  the roadmap for the planned shrink step.
- Lint is set to non-blocking for v1.0; please still keep new code warning-free.

---

## بالعربية (موجز)

المتطلبات: JDK 17+ و Android SDK 35. ثم:

```bash
./gradlew :core:test :app:testDebugUnitTest   # الاختبارات
./gradlew :app:assembleDebug                  # نسخة التطوير
./gradlew :app:assembleRelease                # نسخة الإصدار (غير موقعة محليًا)
```

التوقيع عبر أربعة متغيرات بيئة (`NOOR_KEYSTORE_FILE` و`NOOR_KEYSTORE_PASSWORD`
و`NOOR_KEY_ALIAS` و`NOOR_KEY_PASSWORD`) دون وضع أي مفتاح داخل المستودع، وينشئ GitHub
Actions الإصدارات الموقعة من أسرار المستودع. **احتفظ بنسخة احتياطية من مفتاحك** — بدونه
لا يمكنك إصدار تحديث يثبّت فوق التطبيق الحالي.
