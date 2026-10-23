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
  the Nominatim search service (primary) with the Overpass API mirrors as fallback. ©
  OpenStreetMap contributors, ODbL 1.0. Attribution is shown on the map screen. Requests are
  manual, bounded (radius ≤ 10 km, ≤ 50 results, 20 s overall deadline) and respect the
  public instances' usage policies.
- **GeoNames** — the bundled offline city catalog (`app/src/main/assets/cities.csv`) is derived
  from GeoNames `cities5000` plus alternate Arabic names. Licensed CC-BY-4.0; attribution
  given here and in the app's About section. <https://www.geonames.org>
- **Nominatim** (OpenStreetMap) — optional online city search when the user's query is not
  covered by the offline catalog, and the primary source for the nearby-mosque search (a
  bounded `mosque` keyword query inside the search radius). Usage policy respected
  (identified app, manual low-volume queries).
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

## Trademarks

"Noor Al-Sabeel" is used as the name of this open-source project. Other applications named
"Noor/TheNoor/Al Noor" exist on app stores and are unrelated to this project.
