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
  the Overpass API. © OpenStreetMap contributors, ODbL 1.0. Attribution is shown on the map
  screen. Requests are manual, bounded (radius ≤ 10 km, ≤ 50 results, 20 s timeout) and
  respect the public instances' usage policies.
- **GeoNames** — the bundled offline city catalog (`app/src/main/assets/cities.csv`) is derived
  from GeoNames `cities5000` plus alternate Arabic names. Licensed CC-BY-4.0; attribution
  given here and in the app's About section. <https://www.geonames.org>
- **Nominatim** (OpenStreetMap) — optional online city search when the user's query is not
  covered by the offline catalog. Usage policy respected (identified app, debounced queries).
- **Umm al-Qura calendar table** — the month-length table in
  `core/.../hijri/UmmalquraCalendar.kt` encodes the official Umm al-Qura calendar as
  published by KACST and is adapted from the MIT-licensed
  [hijri-converter / hijridate](https://github.com/dralshehri/hijridate) project.
- **AlAdhan API** — reference prayer-time and qibla values used **only to build the test
  fixtures** (`core/src/test/resources/fixtures/`) and never contacted by the app itself.

## Assets

- Launcher icons, splash icon, notification icon and the compass artwork were drawn for this
  project and are covered by the repository's Apache-2.0 license.
- No fonts or audio files are bundled; Noor Al-Sabeel uses system fonts and system notification sounds.

## Trademarks

"Noor Al-Sabeel" is used as the name of this open-source project. Other applications named
"Noor/TheNoor/Al Noor" exist on app stores and are unrelated to this project.
