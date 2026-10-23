# Changelog

All notable changes to Noor Al-Sabeel are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.2] — 2026-10-23

### Added

- **Full adhan (audio prayer call).** A new “Alert style” setting lets every prayer-time alert
  recite the complete adhan instead of a short system sound. Fajr uses its own dawn adhan
  (with “as-salatu khayrun min an-nawm”); the other prayers share the standard adhan.
- The adhan is bundled inside the app and plays fully offline from install. It plays on the
  alarm volume via a `mediaPlayback` foreground service, so it is audible with the screen off
  and in most Do-Not-Disturb configurations, and it survives Doze through a playback wake lock.
- A “Test the adhan” button in Settings previews the real sound before relying on it.
- The adhan respects the existing per-prayer toggles, pre-prayer reminders stay short
  notifications, and it can be stopped at any time from the notification (also on the lock
  screen). If a vendor ROM blocks the foreground service, the app automatically falls back to
  the regular notification so a prayer time is never missed silently.

### Changed

- New permissions (non-data, no new privacy surface): `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `WAKE_LOCK` for adhan playback only.

### Compatibility

- The application ID stays `io.github.ahmedzakefit.noor` and the signing certificate is
  unchanged, so this update installs directly over v1.0.0/v1.0.1 with settings and data
  preserved. Existing users keep the regular notification style until they opt into the adhan.

[1.0.2]: https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.2

## [1.0.1] — 2026-10-23

### Changed

- App renamed to **Noor Al-Sabeel** (نور السبيل). New display name in English and Arabic
  across the app, notifications and documentation. No feature or behaviour changes.
- Repository renamed to `AhmedZakeFit/noor-al-sabeel`; old links redirect automatically.
- Release APK asset is now named `noor-al-sabeel-<version>.apk`.

### Compatibility

- The application ID stays `io.github.ahmedzakefit.noor` and the signing certificate is
  unchanged, so this update installs directly over v1.0.0 with settings and data preserved.

[1.0.1]: https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.1

## [1.0.0] — 2026-10-23

### Added

- Prayer times and sunrise with a countdown to the next prayer; midnight and last third of
  night as expandable extras.
- 12 calculation methods (Egyptian General Authority of Survey by default) plus custom
  twilight angles and ±30-minute per-prayer offsets for local mosque alignment.
- Qibla compass with magnetic declination correction, accuracy feedback and calibration
  guidance.
- Nearby-mosque search on an OpenStreetMap map with distance, direction and navigation
  handoff via the standard `geo:` intent.
- Customizable notifications: per-prayer toggles, pre-prayer reminder (off/5–30 min),
  sound picker, vibration, exact alarms with graceful fallback, test notification,
  boot/timezone/time-change rescheduling.
- Official Umm al-Qura Hijri calendar (1924–2076) with arithmetic fallback outside the
  table and a ±2-day user adjustment.
- Location selection: automatic (GPS/network via platform LocationManager), offline city
  search over a bundled ~4,300-city catalog (2,088 Arabic names), optional online search
  (Nominatim), and manual coordinates with time zone selection.
- Full Arabic (RTL) and English (LTR) UI including notifications; Arabic-Indic/Latin
  numeral choice.
- Light/dark theme, per-app language, Material 3.
- Verification suite: 35 unit tests including 288 prayer-time rows, 731 Hijri conversions
  and 14 qibla bearings checked against independent references (AlAdhan, hijridate).
- CI (GitHub Actions) running tests and builds on every push; signed release APK
  publishing on tags.

[1.0.0]: https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.0
