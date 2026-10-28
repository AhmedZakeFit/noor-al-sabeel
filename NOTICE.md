# NOTICE — Third-party components and data

This file lists the third-party libraries, data and assets used by Noor Al-Sabeel, together with
their licenses. Noor Al-Sabeel's own code is licensed under Apache-2.0 (see LICENSE).

## Libraries

| Component | License | Use |
|---|---|---|
| Kotlin stdlib & coroutines (JetBrains) | Apache-2.0 | Language runtime, async |
| AndroidX: core-ktx, appcompat, splashscreen, Activity, Lifecycle, ViewModel, Navigation-Compose, DataStore (Google) | Apache-2.0 | Android framework integration, per-app locales, UI navigation, settings storage |
| Jetpack Compose & Material 3 (Google) | Apache-2.0 | Entire user interface |
| osmdroid | Apache-2.0 | OpenStreetMap map view for the mosque search |
| desugar_jdk_libs (Google) | Apache-2.0 | `java.time` support on Android < API 26 |
| JUnit (JUnit team) | EPL-1.0 | Unit tests only — not shipped in the APK |
| org.json (JSON.org) | License-free / public-domain-style notice | Test-classpath JSON parsing (the Android platform's bundled copy is used at runtime) |

All runtime dependencies are Apache-2.0 licensed, which is why Noor Al-Sabeel ships under Apache-2.0.

## Data & services

- **OpenStreetMap** — map tiles rendered in the mosques screen, and place data returned by
  the Nominatim search service and the Photon geocoder (the two independent parallel
  sources of the nearby-mosque search), with the Overpass API mirrors as a last-chance
  fallback. © OpenStreetMap contributors, ODbL 1.0. Attribution is shown on the map
  screen. Requests are manual, bounded (radius ≤ 10 km, ≤ 50 results, 15 s overall
  deadline) and respect the public instances' usage policies.
- **GeoNames** — the bundled offline city catalog (`app/src/main/assets/cities.csv`) is derived
  from GeoNames `cities5000` plus alternate Arabic names. Licensed CC-BY-4.0; attribution
  given here and in the app's About section. <https://www.geonames.org>
- **Nominatim** (OpenStreetMap Foundation) — optional online city search when the user's query
  is not covered by the offline catalog, and one of the two parallel primary sources for the
  nearby-mosque search (a bounded `mosque` keyword query inside the search radius, which as
  a category search also finds Arabic-named mosques). Usage policy respected (identified
  app, manual low-volume queries).
- **Photon** (komoot GmbH) — the second parallel primary source for the nearby-mosque
  search: a free-text search bounded to the radius, with the keyword following the UI
  language ("مسجد" for Arabic — verified live to return the Arabic-named mosques a Latin
  keyword misses). Different company, datacenter and stack from Nominatim/Overpass, so a
  network failure of one family does not affect the other. <https://photon.komoot.io>
- **GitHub Releases** — the in-app update check reads this repository's latest published
  release via GitHub's public API and, when the user opts in, downloads the release APK
  from the project's own GitHub Releases page. No account, no identifiers sent.
- **Umm al-Qura calendar table** — the month-length table in
  `core/.../hijri/UmmalquraCalendar.kt` encodes the official Umm al-Qura calendar as
  published by KACST and is adapted from the MIT-licensed
  [hijri-converter / hijridate](https://github.com/dralshehri/hijridate) project.
- **AlAdhan API** — reference prayer-time and qibla values used **only to build the test
  fixtures** (`core/src/test/resources/fixtures/`) and never contacted by the app itself.

## Assets

- Launcher icons, splash icon, notification icon and the compass artwork were drawn for this
  project and are covered by the repository's Apache-2.0 license.
- No fonts are bundled; Noor Al-Sabeel uses system fonts. Regular (non-adhan) notifications
  use system notification sounds chosen by the user.

### Adhan audio

The full-adhan alert style bundles two recordings from the same reciter (Fouad Charif,
“Adhan Fouad Adan”), published on archive.org with the **Creative Commons Public Domain
Mark 1.0** applied by the author when uploading — i.e. dedicated by the reciter himself to
the public domain, which permits unrestricted redistribution, including inside this app
(<https://creativecommons.org/publicdomain/mark/1.0/>).

| Bundled file | Source recording | Author | License |
|---|---|---|---|
| `app/src/main/res/raw/adhan.mp3` | [AdanAlfajrFouadAdan — `Mp3.mp3` (standard adhan)](https://archive.org/details/AdanAlfajrFouadAdan) | Fouad Charif (“Adhan Fouad Adan”) | Public Domain Mark 1.0 |
| `app/src/main/res/raw/adhan_fajr.mp3` | [AdanAlfajrFouadAdan — `adan alfajr fouad adan.mp3` (dawn adhan)](https://archive.org/details/AdanAlfajrFouadAdan) | Fouad Charif (“Adhan Fouad Adan”) | Public Domain Mark 1.0 |

Both recordings were content-verified before bundling: the standard adhan carries the full
structure (takbīr, both shahādas, both ḥayya ʿalā phrases, the closing takbīr and tawḥīd)
without the dawn phrase, while the Fajr recording adds “aṣ-ṣalātu khayrun min an-nawm” —
transcription-checked with an automated speech-recognition pass over head, middle and tail
segments of each file.

Changes applied to the bundled copies: leading silence trimmed from the standard adhan,
and both recordings loudness-normalized (EBU R128, linear two-pass, −16 LUFS) and transcoded
to 128 kbps MP3 at 44.1 kHz. The recitations themselves are otherwise unmodified.

#### Adhan voices audit (added v1.0.17)

The four selectable reciter voices (`Makkah`, `Madinah`, `Cairo`, `Aqsa`) resolve to
additional bundled recordings in `res/raw/`. A byte-level and speech-recognition audit on
2026-10-28 established the following facts, which the release carries **as documented
known defects** (also pinned by `AdhanAssetsTest`'s asset inventory so no future audio
change can happen silently):

- **Cairo voice = the default recording.** `adhan_cairo.mp3` is byte-identical to
  `adhan.mp3` and `adhan_cairo_fajr.mp3` is byte-identical to `adhan_fajr.mp3`. The
  opening of that recording recites **two** takbirs — ASR transcript of the first 16 s:
  “الله أكبر الله أكبر … أشهد أن لا إله إلا الله” — not the **four** takbirs of the
  Egyptian (radio/Egyptian-style) adhan tradition. This contradicts the v1.0.12 release
  notes, which claimed a “Cairo adhan corrected to 4 takbeers”: that commit only copied
  the default Fajr file over `adhan_cairo_fajr.mp3` (hash comparison proves it), never
  touched the regular file, and never produced a 4-takbir recording.
- **Source and license of the Cairo pair:** identical to the default pair above, i.e.
  the Fouad Charif archive.org recording under Public Domain Mark 1.0 (the copies were
  introduced during the v1.0.11 size-optimization re-compression).
- **Madinah and Aqsa Fajr variants equal their regular variants** byte-for-byte, so the
  Fajr phrase “aṣ-ṣalātu khayrun min an-nawm” is not actually specific to Fajr for those
  two voices.
- **The Makkah, Madinah and Aqsa recordings have no documented source or license**
  (introduced in v1.0.9–v1.0.10 without provenance). Their redistribution licensing is
  therefore *unverified* and must be resolved before any future release treats them as
  cleared assets. The Makkah pair opens with four takbirs (ASR-verified), matching the
  label.

**Replacement criteria for the Cairo voice (open):** any replacement recording must
(a) open with four takbirs in the Egyptian style, (b) carry a redistribution license that
can be verified (e.g. CC0/CC-PDM from the author, or an expired-rights historical
recording), and (c) be content-verified by an ASR transcript of its opening published in
this file. Free-license candidates evaluated for v1.0.17 were rejected on exactly these
grounds: Wikimedia Commons CC0 candidates could not be retrieved for verification during
the audit window, and other freely-licensed candidates were either not Egyptian-style or
their openings could not be machine-verified at all.

## Trademarks

"Noor Al-Sabeel" is used as the name of this open-source project. Other applications named
"Noor/TheNoor/Al Noor" exist on app stores and are unrelated to this project.
