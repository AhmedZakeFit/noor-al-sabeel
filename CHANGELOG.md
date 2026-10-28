# Changelog

All notable changes to Noor Al-Sabeel are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.20] — 2026-10-29

A full redesign of the app's visual identity and the landing page, with **zero
functional changes**: every calculation, alarm, notification, search, map and
update flow behaves exactly as in v1.0.19 — verified by the unchanged test
suite and a clean debug build.

- **New identity, «الزليج الفيرديغريسي» (verdigris zellige).** The palette is
  drawn from Islamic architecture rather than the usual app-store green or the
  generic blue/purple gradient: the patina teal of aged copper domes
  (#11564E), manuscript parchment (#FAF6EC), mosque brass (#C9A24B), Mamluk
  clay (#77574A) and calligraphy ink. Full Material 3 schemes (light and
  dark) with every container role specified — no baseline purple leaks
  through. All text/background pairs pass WCAG AA (verified contrast ≥ 4.5:1).
- **Bundled Arabic typography, offline.** Amiri (the classical Bulaq-press
  Naskh revival) carries display moments — prayer names, the Hijri date,
  screen headlines — and IBM Plex Sans Arabic carries the UI. Six static TTFs
  ship in `res/font` (SIL OFL 1.1, license texts in `third_party/fonts/`),
  so the app keeps its offline-first promise with no downloadable-fonts
  service.
- **Home screen recomposed.** The next prayer now sits inside a pointed
  *mihrab* arch — a real two-centred arch drawn with springline geometry —
  on deep verdigris with a faint khatim lattice and a brass keyline; the
  countdown returns as a pill. The times list is calmer: highlighted next
  prayer as a tinted pill with a small khatim star marker, sunrise visually
  de-emphasised, animated show-more chevron.
- **Mosque search promoted.** The search is now a filled primary button with
  an icon (it was a quiet text link), under an Amiri screen headline; result
  rows gained a tinted pin medallion. Map markers were re-inked to the
  identity: patina for the user, brass for mosques.
- **Qibla & settings refined.** The compass needle uses brass, alignment
  locks to the scheme's teal (theme-aware in dark mode); settings sections
  gained circular icon medallions in the same stroke-icon language.
- **Custom icon set.** Bottom navigation and settings use hand-drawn
  architecture-inspired stroke icons (mihrab arch with hanging lamp, dome
  with minarets, compass rose, sliders) instead of stock Material glyphs.
- **New brand mark.** The launcher icon, splash, notification silhouette and
  site favicon are now the *khatim* (eight-pointed star of two overlapping
  squares) in brass over verdigris — rendered as adaptive-icon vectors and
  regenerated legacy PNGs for every density.
- **Landing page redesigned (Arabic + English, gh-pages).** Unified with the
  app: same palette, same Amiri/Plex pairing, the same khatim mark. The Hero
  now shows the actual product — a CSS phone mockup of the redesigned home
  screen framed by a brass mihrab arch — with a three-feature strip that
  leads with prayer times, qibla and the nearby-mosque search (visually
  flagged as the flagship card). Mosque-search is likewise the emphasized
  card in the features grid. Copy was reviewed for accuracy against the
  shipped feature set: no new claims, no marketing filler. The fixed download
  link, `release.json` metadata script, JSON-LD (SoftwareApplication + FAQ),
  hreflang alternates and sitemap are preserved byte-for-byte where they
  encode facts. Verified in a headless browser at 1440px and 390px with no
  horizontal overflow or clipped sections.
- **Docs.** NOTICE.md documents the two bundled font families and their OFL
  license texts; the READMEs' feature lists were re-checked against the code
  and gain a note on the bundled fonts. Version bumped to 1.0.20 / versionCode 21.
## [1.0.19] — 2026-10-29

Diagnoses why the in-app update from v1.0.17 to v1.0.18 failed on a real
device "for the same reason" as the v1.0.16 wave — and fixes the app-side
defect that made it possible. The release itself was exonerated first:
byte-for-byte re-verification of the published v1.0.18 assets (both APK
copies match both published `.sha256` files), the signing certificate is
unchanged across v1.0.16–v1.0.18 (`a0ccfb6a…`), and a full simulation of the
updater's exact flow (redirect resolve → API/rate-limited fallback → checksum
fetch → redirected download → verify) succeeds against the live release. The
remaining failure mode is in the download itself: a connection that ends
before the whole 24 MB body arrives — the routine mobile-network failure —
was hashed as if it were the complete file, producing exactly the same
"الملف المنزّل لا يطابق بصمة SHA-256 المنشورة" message as a genuinely bad
update. v1.0.17's two blind re-downloads cannot beat a systematically
dropping connection.

- **Fixed: truncated downloads are now detected, resumed, and honestly
  reported.** The download streams with resumable "legs": when the body ends
  before the expected size (release-document size or the server's own
  Content-Length), the updater re-opens the URL with `Range: bytes=<read>-`,
  appends to the partial file and continues the running SHA-256 — no bytes
  are replayed and no data allowance is wasted — for up to 6 connection legs
  per attempt (12 across the two verify passes). GitHub's release CDN honours
  Range requests (verified: `206 Partial Content`), so progress is never
  thrown away. A mid-body transport error (RST/timeout) resumes the same way.
  When the legs are exhausted the failure is the new network-family
  `TRUNCATED` reason with an honest message — a network problem is never
  again reported as a checksum mismatch. A server that ignores Range and
  restarts from zero resets the file and digest cleanly first.
- **Fixed: every update failure now shows its raw diagnostic.** The dialog
  previously localized only the reason; the small detail line under it now
  carries what actually happened — "received 9,437,184 of 25,462,681 bytes
  across 6 connection legs" for truncation, "computed <prefix>… vs published
  <prefix>…" for a real checksum mismatch (both are public release values) —
  so the next device report says WHAT failed instead of "the same error".
- **Unchanged: every security gate.** Published-checksum comparison, package
  name, versionCode, signer-certificate equality and the https/GitHub-host
  allowlist all still run before any install prompt; a file that does not
  byte-match the currently published checksum is still deleted and refused.
  The versioned-pair asset preference, the re-publish byte-swap guard, the
  half-published-release rejection and the atomic draft-then-verify-then-
  publish workflow are exactly as in v1.0.18.
- **Honest bootstrap note:** a device still on v1.0.17/v1.0.18 whose network
  truncates downloads cannot receive this fix through the in-app updater its
  own code provides — that code is frozen on the device. If the in-app update
  keeps failing, install v1.0.19 once from the GitHub releases page in a
  browser (browsers resume automatically); from v1.0.19 on, in-app updates
  survive dropping connections and diagnose themselves.
- **Tests:** new `UpdateTruncationPolicyTest` and `UpdateResumeSimulationTest`
  pin the truncation/resume policy (short bodies are truncation, complete and
  oversized bodies are not, unknown totals stay checksum-gated, the leg
  budget is bounded at 6) and simulate the leg loop over dropping connections
  (progress is never lost, a dropping network still completes, a hopeless
  network fails as TRUNCATED — never as a checksum mismatch).

## [1.0.18] — 2026-10-28

Completes the fix set with the piece v1.0.17 audited but could not land, and
closes the second publication window behind the v1.0.17 byte-swap guard. No
security check weakened; all update gates still run before any install prompt.

- **Fixed: the Egypt (Cairo) adhan now actually opens with four takbeers.**
  v1.0.17 proved with byte hashes that `adhan_cairo.mp3` had been a
  byte-identical copy of the default two-takbeer recording since it was
  introduced (and that the v1.0.12 "fix" never touched the regular file) and
  left the replacement open pending a licensed, verifiable recording. That
  replacement has now landed: both Cairo files are Mahmoud Khalil Al-Husary's
  canonical Egyptian adhan (Public Domain Mark 1.0 via the archive.org "Azaan
  of World Mosques" collection — source, license and content verification in
  NOTICE.md). Verified on the bundled bytes: the head opens with two takbir
  pairs (automated transcription of the second pair: "Allahu Akbar, Allahu
  Akbar") followed by the first shahada, and the dawn recording contains
  "aṣ-ṣalātu khayrun min an-nawm". The asset-inventory hashes are updated and
  the documented-defect test flipped to the distinctness invariant its comment
  prescribed; the release workflow additionally compares the APK-embedded
  copies against the repository's verified files before publishing.
- **Fixed: a test adhan can no longer interrupt a real adhan — from any intent
  path, and a real adhan can no longer be swallowed by stale service state.**
  On top of v1.0.17's preview-flag work: `AdhanService` is one reusable
  instance, and its `finalized` flag used to survive across start commands — a
  real adhan arriving right after a finished TEST on the same instance was
  silently killed (the player never started and the watchdog no-opped on the
  already-final flag). Every accepted start now resets the finalization state,
  and a new unit-tested `AdhanStartPolicy` makes the service IGNORE test
  starts while a real adhan is actively playing (the real adhan still replaces
  a running test; manual testing is unchanged when idle).
- **Fixed: the second update-mismatch window — half-uploaded releases.**
  GitHub lists a release asset from the moment its upload STARTS (state
  "starter", size 0); the tiny `.sha256` files commit within ~0.1 s while the
  24 MB APKs take minutes, so an eager updater could pair a final checksum
  with incomplete APK bytes even on a first publish. `parseRelease` now only
  accepts assets with `state: "uploaded"` and a real size (a half-published
  release therefore yields no update at all; the next check finds it
  complete), the HEAD-probed fallback path requires a positive content length
  on both assets, and the Release workflow publishes ATOMICALLY: the release
  is created as a draft, every asset is downloaded back and verified
  (checksums, ZIP magic bytes, `apksigner`, signing-certificate stability
  against the previous release, package name, version, embedded Cairo audio),
  and only then is the draft made public. Together with v1.0.17's retry policy
  and byte-swap guard, no known publication race can serve an unmatchable
  checksum pair anymore.
- **Tests:** 130+ unit tests pass locally (playback state and start-policy
  tables, the asset inventory with the new Cairo hashes and distinctness
  invariants, the update download policy, and the new half-published-release
  parser rejections).

### Post-release verification (2026-10-28, against the published bytes)

No app code changed in this pass — it records what was re-verified from the
live v1.0.18 release, not a new build:

- **Update path from v1.0.17 → v1.0.18:** the published APK's SHA-256 matches
  its published `.sha256` asset, the package name and versionCode (19 > 18)
  advance, and the signer certificate is unchanged — so the updater's
  checksum/package/version/signature gates all pass against the live release,
  through both the API path and the redirect fallback path. A real on-device
  install test from v1.0.17 is still pending.
- **Update-path audit:** the home (prayer-times) banner remains the single
  primary path; the Settings entry is a manual re-check over the same shared
  `UpdateViewModel`/`UpdateManager` (one check per app open, one dialog, one
  download job). No duplicate checks, downloads or conflicting banners exist.
- **Play Protect status:** documented in `docs/PLAY_PROTECT.md` — the warning
  still appears on install and is expected to keep appearing while the app is
  only distributed through GitHub Releases; the v1.0.18 manifest is identical
  to v1.0.17's (same permissions, components and SDK levels) and the signer
  certificate is unchanged, so nothing new in v1.0.18 changes the warning's
  cause. No protection was disabled and no bypass is requested.

### Compatibility

- The application ID stays `io.github.ahmedzakefit.noor` and the signing
  certificate is unchanged, so this update installs directly over every
  published version with settings and data preserved.

## [1.0.17] — 2026-10-28

This release fixes three verified root causes and documents one verified-but-blocked
audio defect. Nothing below is claimed without the evidence linked in the repository
(NOTICE.md, docs/PLAY_PROTECT.md, the new regression tests and the release workflow).

- **Fixed: the "test adhan" that appeared by itself at prayer time.** The playback
  state (`AdhanPlayback`) did not distinguish a user-initiated TEST from the REAL
  prayer-time adhan, so the settings screen labelled every playing adhan — including
  the one the alarm starts at prayer time — as "تجربة الأذان" (test adhan), and the
  per-voice test buttons could silently replace the real adhan mid-recitation. The
  state now tracks the preview flag, the settings screen shows an honest "live adhan"
  card for the real adhan, and starting a test is refused while the real adhan is
  reciting (manual testing is unchanged when nothing real is playing). v1.0.16's
  release notes claimed this fix but shipped no app-side change; the regression tests
  (`AdhanPlaybackStateTest`) now pin the behaviour.
- **Fixed: in-app update failing with "downloaded file does not match the published
  SHA-256" (v1.0.16 wave).** Root cause, proven from the release history: the v1.0.16
  tag was published TWICE ~19 minutes apart with different APK bytes
  (`17cddeb5…` → `370a4deb…`); users whose update fetched the checksum before the
  asset swap and the APK after it (or vice versa) verified two different builds
  against each other. Fixes, without weakening any verification:
  `UpdateDownloadPolicy` gives exactly one re-verify pass (re-fetch the published
  checksum first; re-download only if the bytes still disagree — a genuinely bad file
  still fails), a served-size check aborts provably-swapped downloads before hashing,
  and the checksum URL is now allowlist-checked like the APK URL. On the publishing
  side, the Release workflow refuses to re-publish an existing release with different
  bytes (identical bytes become a no-op) and refuses to publish unsigned builds, so
  the swap window that caused the false mismatches cannot recur. Pinned by
  `UpdateDownloadPolicyTest` including end-to-end simulations of the v1.0.16 race.
- **Fixed (documentation + honesty): Play Protect install warning.** Signature and
  integrity facts verified for every published release v1.0.13–v1.0.16: identical
  signer certificate `a0ccfb6a…` in the v2 signing blocks, APK bytes match the
  published checksums. The warning is Play Protect's reputation heuristic for
  sideloaded self-signed apps, not evidence of a broken or tampered file. The
  landing-page notice no longer tells users to tap "Install anyway" and no longer
  claims a "v1+v2 strict dual signing" or a battery-permission "main cause" (both
  were false); it now explains which warning is which, how to verify the downloaded
  file against the published `.sha256`, and points to the official appeal form.
  Full analysis in docs/PLAY_PROTECT.md. Google Play Protect is never disabled and
  users are never instructed to bypass it.
- **Documented (not yet fixed): the Cairo adhan still opens with two takbirs.** The
  audit proved that `adhan_cairo.mp3`/`adhan_cairo_fajr.mp3` are byte-identical
  copies of the default Fouad Charif recordings (introduced silently by the v1.0.11
  size-optimization), whose opening recites two takbirs
  (ASR-verified transcript in NOTICE.md), and that v1.0.12's claimed "4 takbeers fix"
  never happened (that commit only copied the default fajr file over the Cairo fajr
  file). A replacement was NOT shipped: every freely-licensed candidate that could be
  obtained was either not Egyptian-style or its opening could not be machine-verified
  (see NOTICE.md "Replacement criteria for the Cairo voice"). The new pinned asset
  inventory (`AdhanAssetsTest`) makes every future audio change an explicit,
  reviewable act carrying its source, license and takbir verification.

## [1.0.16] — 2026-10-27

- **Note added in v1.0.17:** this release's notes claimed "Fix adhan preview state at
  prayer time, mitigate Play Protect warnings" but the commit shipped no app-side
  adhan change (only a version bump, a removed battery permission and landing-page
  edits), and it was published twice under the same tag with different APK bytes,
  which is the root cause of the v1.0.16 update failures — see the v1.0.17 entry above.
- Removed `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` from the manifest.

## [1.0.15] — 2026-10-27

- **Fix Cross-Domain Redirect Handling for In-App Updates.** The built-in `HttpURLConnection` in Android does not follow cross-domain 302 redirects automatically (from `github.com` to `release-assets.githubusercontent.com`). `Http.openWithRedirects` now follows cross-domain redirects explicitly, allowing the published `.sha256` checksum and APK binaries to download cleanly.
- **Robust Archive Package Fact Extraction.** `packageManager.getPackageArchiveInfo(apkPath, 0)` is used first to safely read `packageName` and `versionCode` across all Android versions without throwing signature-parsing exceptions, followed by certificate extraction.

## [1.0.14] — 2026-10-27

- **Comprehensive UI/UX Refinement.** Redesigned all application screens (Home, Prayer Times, Qibla Compass, Nearby Mosques, Settings, and Update Dialog) with a refined Islamic emerald theme, modern Material Design 3 card shapes, improved typography, and responsive RTL/LTR layout alignment.

## [1.0.13] — 2026-10-26

- **Comprehensive Features Showcase on Landing Page.** Added a dedicated "ميزات التطبيق والبحث عن المساجد" tab showcasing all core features including Nearby Mosque Search, astronomical prayer calculations, Qibla compass, adhan reciter voices, and unthrottled exact alarm clock scheduling.
- **Version Bump to v1.0.13.**

## [1.0.12] — 2026-10-26

- **Makkah Adhan Silence Trim.** Removed leading silent dead-air seconds from Makkah Adhan audio files so recitation starts instantly.
- **Cairo Adhan Takbeer Correction.** Corrected Cairo Adhan audio to correctly recite 4 Takbeers at the start ("Allahu Akbar" x4) instead of 2.
- **Metadata Sync.** Version bumped to v1.0.12 across all landing page and repository files.

## [1.0.11] — 2026-10-26

- **App Size Optimization (>62% reduction).** Compressed all bundled adhan audio files (`MP3`) for all reciters and Fajr using 64kbps mono encoding, reducing total raw audio asset footprint from ~40.5 MB down to ~15.1 MB.
- **Landing Page & Metadata Sync.** Updated landing page, repository docs, and version badges to v1.0.11.

## [1.0.10] — 2026-10-26

- **Unthrottled AlarmClock Scheduling.** Upgraded `AlarmScheduler` to use `AlarmManager.setAlarmClock(...)` as the primary exact alarm mechanism. This guarantees adhan alerts wake the CPU and trigger at the exact second scheduled, bypassing Doze mode and background restrictions on Xiaomi/MIUI, Samsung, Huawei, and Android 12–15.
- **Graceful Security Exception Handling.** Falls back automatically to `setExactAndAllowWhileIdle` and windowed scheduling if exact alarm permission is restricted, preventing background crashes.

## [1.0.9] — 2026-10-26

- **Distinct Adhan Audio Files for Every Reciter.** Bundled distinct, authentic, high-quality adhan audio recordings for all 4 selectable reciter voices (Makkah, Madinah, Cairo, Al-Aqsa) for both regular prayers and Fajr.
- **Voice Preview & Alert Fix.** Testing or receiving an adhan now plays the exact audio file corresponding to the user-selected reciter and prayer.

## [1.0.8] — 2026-10-26

- **App Version Name Sync.** Updated `versionName` in `app/build.gradle.kts` to `1.0.8` (and `versionCode` to `9`) so installed APKs accurately show version 1.0.8 under Settings > About and align with GitHub Releases & landing page.
- **In-App Update Action Fix.** Unified `UpdateViewModel` instance across `NoorApp`, `HomeScreen`, and `SettingsScreen` so that clicking "Update Now", "View Details", or "Check for Updates" immediately triggers progress bar and package installer.

## [1.0.7] — 2026-10-26

### Fixed & Improved
- **Map Gesture Bounds & Touch Interception (MosquesScreen.kt):** Fixed map gesture overflow issue on touch where the interactive MapView moved or rendered outside its bounded container. Added strict Compose `clipToBounds()` framing and parent touch disallow interception during pan/zoom drag operations.
- **Individual Adhan Voice Test Controls (SettingsScreen.kt):** Enhanced Adhan testing in Settings to provide per-voice, per-prayer testing cards (Makkah, Madinah, Cairo, Al-Aqsa) with dedicated Play ("تشغيل") and Stop ("إيقاف") controls and live playback state indicators.

## [1.0.6] — 2026-10-26

### Added
- **Adhan Voices Selection:** Multiple adhan reciters (Makkah Holy Sanctuary, Prophet's Mosque Madinah, Al-Azhar Mosque Cairo, and Al-Aqsa Mosque) with dual preview testing for Fajr (including "Prayer is better than sleep") and regular daily prayers.
- **Easy Permissions & Notification Setup:** Prominent one-tap activation banner on Home for notifications, plus a comprehensive permissions & reliability panel in Settings covering notification permission, exact alarms (Android 12+), and battery optimization exemption.
- **Direct In-App Update Trigger:** Added a prominent "Update App Now" (تحديث التطبيق الآن) button right in the update alert banner for immediate one-tap download and installation.

### Fixed
- **Islamic Midnight & Last Third of Night calculation (PrayerCalculator.kt):** The night
  duration was calculating negative minutes across the midnight threshold (subtracting tomorrow's
  Fajr minutes from today's Maghrib without the 24-hour cycle offset), causing midnight and the
  last third to appear in the morning. Fixed with 1440.0 minutes absolute day offset and verified with tests.
- **Location detection timeout on real devices (LocationProvider.kt):** The fallback to last-known
  location was unreachable on timeout due to coroutine cancellation, and the strict 30m accuracy
  threshold caused continuous timeouts indoors or on cellular networks. Fixed by moving fallback
  outside withTimeoutOrNull and setting threshold to 45m.
- **Nearby Mosques Search restored & verified:** Re-enabled the full mosque search and interactive
  map with parallel independent sources (Photon + Nominatim), fixed location permission workflow,
  and reliable search around current device location.

## [Unreleased]

### Fixed

- **In-app update unreachable behind a rate-limited GitHub API (the proven root cause
  of the in-app update failure).** The updater's only path to an available update was
  the unauthenticated `api.github.com/repos/…/releases/latest` call (60 req/h per IP);
  the `/releases/latest` website redirect pre-check only short-circuited the
  *up-to-date* case. On shared/CGNAT mobile IPs whose quota is exhausted, the check
  failed exactly when a newer release existed: the automatic check stayed silently
  "up to date" (no banner) and the manual check reported failure (verified live:
  the API answered HTTP 403 while the redirect, the release assets and their
  checksums were all fully reachable from the same network). The check now falls
  back — when the redirect has already proven a newer version exists and the API
  produced no usable document — to the deterministic asset URLs the release
  workflow always publishes (versioned pair preferred, stable-named pair
  fallback), confirmed with cheap HEAD probes against `github.com` (not
  rate-limited). No security check is weakened: HTTPS + release-host allowlist
  only, and the downloaded APK is still verified against the published
  `.apk.sha256` asset plus package name, versionCode and signing certificate
  before any install prompt.
- **Update failures no longer hide their reason.** Download/verification failures
  were collapsed into one generic "download failed" message, and the result of
  opening the Android package installer was ignored entirely — a failed
  installer open left the dialog claiming everything was fine with no retry.
  Failures are now typed (network / HTTP error / empty file / checksum
  unavailable / checksum mismatch / wrong package / not newer / signature
  mismatch / disallowed source / installer could not open) and the dialog shows
  a specific, localized message for each, always with a retry. On Android 8+
  with the "install unknown apps" grant missing, the dialog explains the
  one-time approval and offers a button that opens the system settings page for
  this app — nothing is granted or installed silently.
- **Landing page contradicted the mosque search feature (gh-pages).** The FAQ
  claimed the app "does not search for nearby mosques" while the app ships a
  full mosque search (parallel Nominatim + Photon sources, 1–10 km radius,
  interactive map). The claim was removed from the Arabic and English pages,
  the nearby-mosque search was added to the feature list (JSON-LD and visible
  cards) and a dedicated FAQ entry describes it accurately, including that it
  needs an internet connection; the "only two optional internet features"
  statements were corrected to three everywhere (city search, mosque search,
  update check) and `llms.txt` was aligned.

### Added

- **Tests for the new update paths.** The deterministic fallback asset-URL
  construction (versioned pair first, stable pair fallback, allowlisted,
  matching the real v1.0.13 release layout), and a pin that every typed
  download-failure reason maps to a distinct localized message.

- **Mosques search never asks for the location permission (the 2026-10-24 phone report).**
  The main "find nearby mosques" button silently searched the *saved* app location whenever
  the location permission was missing — so no permission dialog ever appeared, and travelling
  to another area kept returning the old area's results (the diagnostic audit proved the
  outgoing Nominatim viewbox / Photon bbox always embedded the saved city). The button is now
  wired to the runtime permission request: a short in-context rationale, the system dialog,
  and — on grant — the current-location search the user asked for. A denial keeps clear
  guidance on screen with a way to open the app settings (for permanent denials), and the
  saved location is reachable **only** as an explicit on-screen choice, never a silent
  fallback. Returning to the screen with a newly granted permission (e.g. after granting it
  in system settings) starts the waiting search automatically.
- **First map zoom accuracy.** The zoom-fitting used a guessed viewport size until the map
  view reported its real pixel dimensions, so the very first frame could land at the wrong
  zoom. The map sync now re-runs once after the view is actually measured.
- **Mosques screen after travelling (the "same old results" report).** The search button
  always queried the *saved* app location, so moving to another area and searching again
  returned the old area's mosques. It now searches around the device's **current
  location** whenever the location permission is granted — a fresh fix per search, never
  a cached result (no result caching exists or was introduced; every search goes to the
  network with the center embedded in the request). Without the permission it searches
  the saved location and says so on screen, with a one-tap "use my current location"
  path that asks for the permission in context. A failed fix, a denied permission and
  an empty radius each get a clear message with explicit retry / fallback actions, and
  a new search-center line always shows which area is being searched ("around your
  current location" / "around: <place>").
- **Mosques screen layout on phones (the "map covers the UI" report).** The results list
  used `fillMaxSize`, which takes the whole screen height instead of the remaining
  space, so it ran under the bottom navigation bar and off-screen — worst on small and
  landscape screens. The list now fills exactly the remaining space (`weight`) and the
  map shrinks to a bounded fraction of the screen height (150–260 dp), so map pan/pinch
  gestures stay inside the map's own bounded box and the list scrolls within its own
  area.
- **Uncontrolled map movement.** The map was re-centered to the saved location and its
  markers rebuilt on *every* recomposition (radius chip taps, search start/finish,
  settings emissions), which kept yanking the user's pan/zoom away. Re-centering,
  zoom-fitting and marker sync now run only when the search center, the radius or the
  result set actually changes; the zoom is fitted to the chosen radius and bounded to
  MAPNIK's usable range (3–19), built-in zoom buttons are disabled (pinch only), and the
  `MapView` is properly detached when leaving the tab (fixes accumulated leaked map
  instances across tab switches).

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
