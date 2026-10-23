# Changelog

All notable changes to Noor Al-Sabeel are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.3] — 2026-10-23

### Fixed

- **Stopping the test adhan.** The settings screen now swaps “Test the adhan” for a live
  “Stop the adhan” button while the adhan is reciting, which stops the sound and the foreground
  service completely. This matters on Android 13+ when notification permission has not been
  granted: the service notification (and its stop action) is invisible there, which previously
  left no way at all to stop a test adhan.
- **Nearby-mosque search failures.** The search now retries the official Overpass mirror
  endpoints when the primary public instance answers rate-limited (HTTP 429) or overloaded
  (5xx) status, or a connection fails. Instead of a dead-end message, the failure screen
  explains what happened (no connection / service busy / timeout / other) and offers an
  explicit retry button.
- **Distance options.** The radius chips above the mosque map showed duplicated, garbled text
  (the number was stripped by a formatting hack, so every chip read just “km”/“كم”). The
  options are now a clean 1–10 km ladder, each chip showing its own localized value and unit,
  with the selected value marked.
- **First-run flow.** After picking the very first location in Settings, the app now jumps
  straight to the prayer-times screen — no need to close and reopen the app.
- **Permission sequencing.** “Use current location (GPS)” no longer races the system permission
  dialog: it explains why the permission is needed, then opens the official system dialog, and
  starts locating immediately when the user grants. Denial shows a clear message and the city
  search / manual coordinates paths remain available; a later grant (from app settings) is
  picked up because the permission is re-checked at the moment of use, never assumed.
- **Notification permission.** On Android 13+ the POST_NOTIFICATIONS dialog is now asked at
  most once per install (previously it could re-appear on every tab switch after a denial);
  granting it later from system settings simply enables notifications.

### Compatibility

- The application ID stays `io.github.ahmedzakefit.noor` and the signing certificate is
  unchanged, so this update installs directly over v1.0.0–v1.0.2 with settings and data
  preserved. No new permissions.

[1.0.3]: https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.3

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
