# Noor Al-Sabeel — نور السبيل

**Free, open-source prayer times, qibla compass and nearby mosques for Android.**
No ads. No subscriptions. No accounts. No tracking.

**العربية:** [اقرأ الوثائق بالعربية](README-AR.md)

<p>
  <img src="art/icon-512.png" width="96" alt="Noor Al-Sabeel logo" />
</p>

[![CI](https://github.com/AhmedZakeFit/noor-al-sabeel/actions/workflows/ci.yml/badge.svg)](https://github.com/AhmedZakeFit/noor-al-sabeel/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/AhmedZakeFit/noor-al-sabeel)](https://github.com/AhmedZakeFit/noor-al-sabeel/releases)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

## Features

- **Prayer times & sunrise** for the five daily prayers with a countdown to the next prayer,
  plus midnight and the last third of the night.
- **Multiple calculation methods** — Muslim World League, Egyptian General Authority of Survey
  (default), ISNA, Umm al-Qura (with the Ramadan isha rule), Karachi, Gulf, Kuwait, Qatar,
  Singapore, Turkey, France, or fully custom angles.
- **Mosque tuning** — shift any individual time by ±30 minutes to match your local mosque.
- **Qibla compass** with magnetic declination correction and calibration guidance.
- **Nearby mosques** on an OpenStreetMap map, with distance, direction and one-tap navigation
  handoff to any installed navigation app.
- **Customizable notifications** — per-prayer toggles, a pre-prayer reminder, sound and
  vibration choices, exact alarms, and a test button.
- **Hijri calendar** (official Umm al-Qura table for 1924–2076) with a manual day adjustment
  for local moon sighting.
- **Arabic (RTL) and English (LTR)** from the first release, including notifications and
  settings. Arabic-Indic or Latin numerals, your choice.
- **Offline-first** — all prayer-time, qibla and calendar math runs locally. A bundled catalog
  of ~4,300 cities (2,088 with Arabic names) works with no internet at all.
- **Light & dark themes** following the system or forced manually.

## Install

Download the latest signed APK from
[GitHub Releases](https://github.com/AhmedZakeFit/noor-al-sabeel/releases) and open it on your phone
(allow "install from unknown sources" when asked). No Google Play account is needed.

> Submitting Noor Al-Sabeel to F-Droid is planned (see the [roadmap](docs/ROADMAP.md)). Acceptance is
> not guaranteed and is up to the F-Droid maintainers.

## Privacy

Noor Al-Sabeel collects nothing. There are no ads, no analytics, no accounts and no trackers. All
settings and locations stay on your device. The app only touches the network when you open the
map, search for mosques, or use the optional online city search. See
[PRIVACY.md](PRIVACY.md) for the full policy.

## Accuracy & sources

Prayer times are computed on-device with standard solar-position algorithms
(Meeus, *Astronomical Algorithms*) and verified in CI against independent references. No
prayer-time computation can be perfectly exact: twilight angles, rounding and local
conventions each contribute small differences, so always double-check against your mosque.
See [docs/PRAYER_TIMES.md](docs/PRAYER_TIMES.md) for every method, source and known
limitation. The qibla compass depends on your device's magnetic sensors — the app shows its
accuracy and guides you through calibration, but consumer phone compasses have real physical
limits.

## Build it yourself

```bash
git clone https://github.com/AhmedZakeFit/noor-al-sabeel.git
cd noor-al-sabeel
./gradlew :core:test :app:testDebugUnitTest   # run the test suite
./gradlew :app:assembleDebug                  # build the APK
```

You need JDK 17+ and Android SDK 35. Full instructions, including release signing with
environment variables (no secrets in the repo), are in [docs/BUILDING.md](docs/BUILDING.md).

## Project layout

```
core/    Pure-Kotlin engine: solar math, prayer times, qibla, Hijri calendar, city catalog
app/     Android UI (Jetpack Compose), notifications, alarms, map and mosque search
docs/    Documentation: building, testing, calculation sources, roadmap
art/     Source icon artwork
```

## Contributing

Issues, translations and pull requests are welcome — see
[CONTRIBUTING.md](CONTRIBUTING.md) (بالعربية أيضاً). Security reports follow
[SECURITY.md](SECURITY.md).

## License

Code is licensed under [Apache-2.0](LICENSE). Third-party data and libraries and their
licenses are listed in [NOTICE.md](NOTICE.md), including OpenStreetMap/ODbL map data,
GeoNames cities (CC-BY-4.0) and the Umm al-Qura calendar table adapted from the MIT-licensed
hijri-converter project.
