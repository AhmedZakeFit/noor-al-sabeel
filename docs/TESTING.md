# Testing — الاختبارات

**English** | [العربية بالأسفل](#العربية)

## What runs automatically (every push / PR — GitHub Actions)

| Check | Where |
|---|---|
| Core engine unit tests | `:core:test` |
| App unit tests | `:app:testDebugUnitTest` |
| Debug build | `:app:assembleDebug` |
| Signed release build + APK (on tags) | `:app:assembleRelease` in `release.yml` |

### Automated coverage

- **`PrayerCalculatorFixtureTest`** — 288 reference rows (9 cities × 6 methods × 5 dates,
  incl. DST transition days, Hanafi asr, and Ramadan for Umm al-Qura) fetched from the
  AlAdhan API and stored as CSV fixtures. Tolerances: 2 min standard, 4 min with
  high-latitude clamping, 10 min for asr at |lat| ≥ 55° (apparent-vs-geometric altitude
  convention, see docs/PRAYER_TIMES.md), 35 min for Umm al-Qura isha (convention
  difference, also documented).
- **`UmmalquraCalendarTest`** — 731 reference conversions vs hijridate, round-trips,
  known events, month-length sanity, out-of-range approximation flags.
- **`QiblaFixtureTest`** — 14 bearings vs AlAdhan (±0.5°), plus known-value sanity checks
  (Cairo ≈ 136°, Jakarta ≈ 295°, Cairo–Kaaba ≈ 1300 km).
- **`CityCatalogTest`** — parses the real bundled catalog (≥4,000 cities), Arabic/Latin
  search incl. alef-variant normalization, nearest-city, CSV quoting, display fallbacks.
- **Engine unit tests** — time ordering, per-prayer offsets, Umm al-Qura 90/120-min rule,
  high-latitude clamping invariants, next-prayer logic, EoT bounds, declination at
  solstices.
- **App unit tests** — digit localization (both directions), clock formatting (12/24h),
  countdown formatting, Hijri display formatting, Overpass JSON parsing (nodes, ways,
  unnamed, malformed, non-Islamic filtering), Nominatim parsing, settings→parameters
  mapping incl. the Ramadan rule and custom angles, adhan routing (mode/reminder/prayer
  matrix, per-prayer gating), and the bundled adhan assets (both MP3s present, valid,
  non-empty, distinct, size-bounded).

## Manual test matrix (for releases / on your phone)

Things that genuinely require real hardware — run through this checklist before relying
on the app:

1. **Language switch:** Arabic ↔ English in settings; UI mirrors (RTL/LTR), notifications
   arrive in the selected language; restart the app and the phone — the choice persists
   (per-app locale storage).
2. **Location permission denied:** app stays usable; city search and manual coordinates
   still work.
3. **Auto location:** allow permission, tap "use current location" — coordinates + nearest
   city label + device time zone are picked up.
4. **Airplane mode:** prayer times, qibla bearing and Hijri date still work; mosque search
   fails gracefully with the "needs internet" note; map tiles do not load.
5. **Reboot:** notifications for the next prayers still fire without opening the app
   (boot receiver reschedules).
6. **Time zone change / travel:** change the system time zone, or pick a city in another
   zone — times recompute correctly.
7. **Notifications:** send the test notification; check sound, vibration, per-prayer
   toggles and the pre-prayer reminder; verify arrival at the exact minute when exact
   alarms are allowed.
8. **Adhan (audio alert style):** in Settings → Notifications choose “Full adhan” and tap
   “Test the adhan” — the standard adhan should play on the alarm volume with an ongoing
   notification offering a Stop action. Then verify on a real prayer time: the correct
   recording plays (dawn adhan with “as-salatu khayrun min an-nawm” for Fajr), playback
   continues with the screen locked/turned off and in Doze, the Stop action works from
   the lock screen, the notification leaves an auto-cancel record when the adhan finishes,
   disabled prayers stay silent, reminders remain short notifications, and after a reboot
   the next adhan still fires. With the alarm volume set to zero the adhan is silent but the
   heads-up notification still appears.
9. **Battery optimization:** on vendor ROMs (Xiaomi/Samsung/Huawei...), check whether
   notifications are delayed; the settings screen offers the exemption dialog.
10. **Qibla:** compare the bearing number against a known value for your city; check the
   compass responds, calibration hint appears after figure-8 motion, declination is
   displayed.
11. **Mosque search:** search in a city centre; verify distances, one-tap navigation opens
    a navigation app; check the OSM attribution line.
12. **Hijri date:** compare with your local calendar; adjust ±1–2 days if needed.
13. **Upgrade path:** install a new version over the old one — settings survive.

## Rebuilding the fixtures (maintainers only)

The fixture CSVs under `core/src/test/resources/fixtures/` were generated on 2026-10-23
with the scripts in the (development-time) tooling; regenerating requires network access
to api.aladhan.com and download.geonames.org. Reference tolerance rationale is documented
in docs/PRAYER_TIMES.md.

---

# العربية

## ما يعمل آليًا (عند كل دفعة — GitHub Actions)

اختبارات الوحدتين `core` و`app` وبناء النسختين debug وrelease الموقعة عند الوسوم.

### التغطية الآلية

- **288 صف مواقيت** من مراجع AlAdhan (9 مدن × 6 طرق × 5 تواريخ مع أيام تغيير التوقيت
  الصيفي والعصر الحنفي ورمضان) بتسامح دقيقتين (4 مع قواعق العروض العليا، و10 للعصر
  فوق 55°، و35 لإيشا أم القرى — كلها موثقة الأسباب).
- **731 تحويلًا هجريًا** مع الفحص الرجعي والتواريخ المعروفة.
- **14 اتجاه قبلة** (±0.5°).
- **فهرس المدن الحقيقي** مع البحث العربي/اللاتيني وتطبيع الهمزات وأقرب مدينة.
- **اختبارات المحرك** (الترتيب، الفروق، قاعدة رمضان، العروض العليا، الصلاة التالية).
- **اختبارات التطبيق** (الأرقام، الساعة 12/24، العد التنازلي، الهجري، تحليل Overpass
  وNominatim، خرائط الإعدادات).

## مصفوفة الاختبار اليدوي (على هاتفك)

1. **تبديل اللغة** عربي/إنجليزي: انعكاس الواجهة، وصول الإشعارات باللغة المختارة،
   والثبات بعد إعادة التشغيل.
2. **رفض إذن الموقع:** يبقى التطبيق قابلًا للاستخدام مع البحث والإحداثيات اليدوية.
3. **الموقع التلقائي** بعد منح الإذن.
4. **وضع الطيران:** المواقيت والقبلة والهجري تعمل، وفشل المساجد بأسلوب رشيق.
5. **إعادة تشغيل الهاتف:** تستمر الإشعارات دون فتح التطبيق.
6. **تغيير المنطقة الزمنية أو السفر.**
7. **الإشعارات:** التجربة، الصوت، الاهتزاز، التفعيل لكل صلاة، التذكير المسبق.
8. **الأذان (أسلوب التنبيه الصوتي):** اختر «أذان كامل» في الإعدادات ثم اضغط «تجربة
   الأذان» — يجب أن يُرفَع الأذان العادي على مستوى صوت المنبّه مع إشعار مستمر فيه زر
   إيقاف. ثم تحقّق عند صلاة حقيقية: يُرفَع التسجيل الصحيح (أذان الفجر بعبارة «الصلاةُ
   خيرٌ من النوم» للفجر)، ويستمر التشغيل مع قفل الشاشة/إطفائها وفي وضع Doze، ويعمل زر
   الإيقاف من شاشة القفل، ويترك الإشعار سجلًا يُلغى باللمس بعد انتهاء الأذان، وتبقى
   الصلوات المعطّلة صامتة، ويبقى التذكير المسبق إشعارًا قصيرًا، ويظل الأذان يعمل بعد
   إعادة التشغيل. مع تصویر مستوى المنبّه إلى الصفر يسكت الصوت لكن يبقى الإشعار ظاهرًا.
9. **تحسين البطارية** على رومات الشركات.
10. **القبلة:** قارن رقم الاتجاه بقيمة معروفة لمدينتك، واستجابة البوصلة والمعايرة.
11. **المساجد:** المسافة والملاحة والإسناد.
12. **التاريخ الهجري** مع التعديل اليدوي.
13. **الترقية فوق نسخة قائمة** مع بقاء الإعدادات.
