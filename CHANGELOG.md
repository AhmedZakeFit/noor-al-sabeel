# Changelog

All notable changes to Noor Al-Sabeel are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Removed

- **The nearby-mosque search and map feature, in full.** Despite three repair rounds
  (v1.0.3 mirror failover, v1.0.4 Nominatim-first, v1.0.5 parallel independent sources,
  and the subsequent permission/layout/location fixes), the search kept failing on real
  phones — so rather than ship more patches, the feature is removed completely:
  - the Mosques tab, screen, view model and all its strings (Arabic and English);
  - the mosque-search stack (`MosqueSearchClient`, `OverpassClient`) and the
    osmdroid map dependency, its configuration and its NOTICE attribution;
  - all mosque-related unit tests and the mosque sections of the docs, privacy policy
    and landing page.
  Everything else is untouched: prayer times, qibla compass, adhan, notifications,
  Hijri calendar, settings and the in-app update check. Location permissions are
  **kept** — they are still used by the settings location picker ("use current
  location") for prayer times and qibla, as documented in PRIVACY.md. The optional
  online city search (Nominatim) and the update check keep the `INTERNET` permission
  meaningfully in use. No official release was cut for the interim fix commits; the
  removal supersedes them before the next release.

## [1.0.5] — 2026-10-23

### Fixed

- **Nearby-mosque search (the phone-timeout report).** v1.0.4 made Nominatim the first source,
  but on the reporter's phone the search still waited the full deadline and then failed. Live
  probes with the app's exact request explain why: Nominatim and Photon both answer in ~1 s
  while **all three Overpass endpoints answer nothing at all** (HTTP 000; two of them accept
  the TCP connection and then stall) — and v1.0.4 ran Nominatim as a *sequential* gate in front
  of that dead family, so whenever Nominatim itself was unreachable on a given mobile network
  the cascade burned its socket timeouts and hit the overall deadline: the reported
  "the search took too long". The search now runs **two genuinely independent sources in
  parallel** — Nominatim (OpenStreetMap Foundation, Fastly) and Photon (komoot, IPv4-only,
  which also sidesteps broken-IPv6 carriers) — and the first to answer with mosques wins while
  the loser's request is cancelled: the death of either provider now costs the user nothing.
  The Arabic UI searches Photon with the Arabic keyword "مسجد", which finds the Arabic-named
  mosques a Latin keyword misses (Photon's `lang` parameter is never sent — it rejects
  unsupported values with HTTP 400). Overpass mirrors remain as a last chance with tighter
  3 s/6 s budgets, the overall deadline dropped from 20 s to 15 s, and every failure path
  still ends in an explainable message with a retry button.
- **Landing-page download button.** The button used to open the GitHub Releases *page*,
  which is confusing for non-technical users. It is now a fixed direct link
  (`/releases/latest/download/noor-al-sabeel.apk`) that downloads the latest stable signed
  APK immediately — no intermediate pages — and it keeps pointing at the newest release
  automatically because every release now also uploads a stable-named copy of the same APK.
  If a release fails to publish, `/releases/latest` still resolves to the previous release,
  so the link stays valid. "All releases" and the source code remain clear secondary links,
  and the version/size/date line on the page is rendered only from metadata that the
  release workflow has actually verified by downloading the APK back through that same link
  (magic bytes, size, SHA-256 equal to the published checksums).

### Added

- **Automated post-release link check** (`verify-download` workflow): after every release
  publish (and weekly) the fixed download link is resolved, the APK is downloaded through it,
  and its SHA-256 is compared against the versioned asset and the published `.sha256`
  checksum before the landing page's release metadata is updated. A broken or half-published
  release turns the workflow red and never leaks into the page.

[1.0.5]: https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.5

## [1.0.4] — 2026-10-23

### Fixed

- **Nearby-mosque search (again).** v1.0.3's mirror failover was not enough: on several mobile
  networks *every* Overpass endpoint stalls or drops the connection, so the search still ground
  through ~30 s of per-socket timeouts and then failed. The search now asks **Nominatim first** —
  independent infrastructure, verified reachable from networks where all Overpass endpoints
  answer nothing — and only falls back to the Overpass mirrors. One overall 20-second deadline
  bounds the whole search no matter what the networks do, leaving the screen aborts the request
  immediately (the socket is disconnected, the spinner never strands), and a failure still shows
  an explainable message with a retry button. Names arrive localized and the Islamic-places
  filter keeps non-Muslim places of worship out.
- **Pre-prayer reminder countdown.** The reminder notification used to freeze on "10 minutes
  left" and stay that way until dismissed. It now carries a **live countdown** (an
  Android-system chronometer counting down to the prayer time itself — rendered by the OS, so
  zero extra alarms and zero battery cost), and when the prayer time arrives it is replaced by
  the normal prayer notification or the adhan. Reminders whose prayer time already passed are
  never posted, and stale notifications for prayers whose alerts were switched off are cleaned
  up when the schedule is rebuilt (including after reboot or clock/time-zone changes).

### Added

- **In-app update check (GitHub Releases).** On app open the app quietly compares the installed
  version with the latest stable GitHub release (manual re-check available in
  Settings → About). When an update exists it shows the version, a change summary and clear
  actions: download (optional, with progress), postpone (that exact version is never nagged
  about again), or close. The downloaded APK is verified before anything is offered for
  installation: SHA-256 against the published checksum asset, plus package name, versionCode
  and signing certificate compared with the installed app. Installation uses the standard
  Android prompt and always requires the user's approval — there is no silent path. Update
  failures (offline, timeout) never disturb prayer or adhan functions. New permission:
  `REQUEST_INSTALL_PACKAGES` (needed only for the user-approved install flow).
- **Project landing page** (Arabic + English, responsive, no trackers, no external requests):
  <https://ahmedzakefit.github.io/noor-al-sabeel/> — with a download button that always points
  at `/releases/latest`, the privacy policy, usage instructions and the source repository.

### Compatibility

- The application ID stays `io.github.ahmedzakefit.noor` and the signing certificate is
  unchanged, so this update installs directly over v1.0.0–v1.0.3 with settings and data
  preserved.

[1.0.4]: https://github.com/AhmedZakeFit/noor-al-sabeel/releases/tag/v1.0.4

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
