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
  unnamed, malformed, non-Islamic filtering), Overpass mirror failover + failure
  classification (429/5xx retryable, offline/timeout reasons), mosque radius chips
  (1–10 km ladder, number+unit kept together in Latin and Arabic digits), adhan
  routing (mode/reminder/prayer matrix, per-prayer gating), adhan playback flag
  (drives the settings-screen stop button), and the bundled adhan assets (both MP3s
  present, valid, non-empty, distinct, size-bounded).
- **Mosque-search reliability tests (`MosqueSearchTest`)** — Nominatim keyword-response
  parsing against a fixture copied from a live Cairo response (Islamic filtering keeps
  churches out, keeps landuse/religious mosques, collapses exact duplicates), the bounded
  viewbox (dot decimals, corner order, center inside), the overall 20 s deadline and the
  per-mirror socket budgets beneath it, failure-reason ranking (offline > timeout > busy),
  and the Overpass fallback parser.
- **Reminder countdown tests (`ReminderRulesTest`)** — the reminder spec targets the prayer
  time with a counting-down chronometer, stale reminders (target already passed) are never
  posted, notification IDs are distinct and stable, and prayers without scheduled alarms
  get their notifications cleaned up.
- **Update-check tests (`UpdateParserTest` / `UpdateVerifierTest`)** — tag/version parsing,
  version resolution from the `/releases/latest` redirect (the rate-limit-free pre-check
  path), newer-version detection (numeric, not lexicographic), release-JSON parsing (APK +
  checksum asset URLs, size, changelog summary stripping), sha256sum-format checksum
  parsing, the HTTPS host allowlist, and the full decision matrix: newer → available,
  same/older → up to date, connection failure → silent up-to-date in auto mode but a
  visible failure for manual checks, postponed version silenced in auto mode only. The
  verifier matrix blocks install on checksum unknown/mismatch, wrong package, non-newer
  versionCode, or a mismatched signing certificate.

## Manual test matrix (for releases / on your phone)

Things that genuinely require real hardware — run through this checklist before relying
on the app:

1. **Language switch:** Arabic ↔ English in settings; UI mirrors (RTL/LTR), notifications
   arrive in the selected language; restart the app and the phone — the choice persists
   (per-app locale storage).
2. **Location permission denied:** app stays usable; city search and manual coordinates
   still work.
3. **Auto location:** allow permission, tap "use current location" — coordinates + nearest
   city label + device time zone are picked up. On first run (no location saved yet) the
   explanation dialog appears before the system permission dialog, and granting it starts
   locating immediately without restarting the app.
4. **First-run flow:** fresh install → home screen "Set location" → pick any city → the app
   should land on the prayer-times screen right away (no app restart needed).
5. **Airplane mode:** prayer times, qibla bearing and Hijri date still work; mosque search
   shows the "no internet connection" error with a Retry button; map tiles do not load.
6. **Mosque search resilience:** on mobile data, search a city centre — results should
   arrive quickly via the Nominatim source even when every Overpass endpoint is
   unreachable (v1.0.3's failure mode); the total wait never exceeds ~20 s; leaving the
   screen mid-search stops the spinner immediately; when everything fails the error
   screen states the reason and offers Retry; the radius chips show every value 1–10 km
   with its unit (Arabic and English), and the selected value is marked.
7. **Reboot:** notifications for the next prayers still fire without opening the app
   (boot receiver reschedules).
8. **Time zone change / travel:** change the system time zone, or pick a city in another
   zone — times recompute correctly.
9. **Notifications:** send the test notification; check sound, vibration, per-prayer
   toggles and the pre-prayer reminder; verify arrival at the exact minute when exact
   alarms are allowed. The pre-prayer reminder must show a **live counting-down
   chronometer** that keeps ticking every second without the app waking up, and at the
   prayer time it is replaced by the prayer notification (or the adhan notification) — no
   stale “N minutes left” remains. On Android 13+, after denying POST_NOTIFICATIONS once,
   the system dialog must not be spammed again on later tab switches; granting it later
   from system settings makes notifications appear without reinstalling.
10. **Adhan (audio alert style):** in Settings → Notifications choose “Full adhan” and tap
   “Test the adhan” — the standard adhan should play on the alarm volume with an ongoing
   notification offering a Stop action, and the settings button itself flips to “Stop the
   adhan” while playing; tapping it stops sound and service completely (also verify with
   notification permission denied, where the notification is invisible and the in-screen
   stop button is the only way to stop). Then verify on a real prayer time: the correct
   recording plays (dawn adhan with “as-salatu khayrun min an-nawm” for Fajr), playback
   continues with the screen locked/turned off and in Doze, the Stop action works from
   the lock screen, the notification leaves an auto-cancel record when the adhan finishes,
   disabled prayers stay silent, reminders remain short notifications, and after a reboot
   the next adhan still fires. With the alarm volume set to zero the adhan is silent but the
   heads-up notification still appears.
11. **Battery optimization:** on vendor ROMs (Xiaomi/Samsung/Huawei...), check whether
   notifications are delayed; the settings screen offers the exemption dialog.
12. **Qibla:** compare the bearing number against a known value for your city; check the
   compass responds, calibration hint appears after figure-8 motion, declination is
   displayed.
13. **Mosque search:** search in a city centre; verify distances, one-tap navigation opens
    a navigation app; check the OSM attribution line.
14. **Hijri date:** compare with your local calendar; adjust ±1–2 days if needed.
15. **Upgrade path:** install a new version over the old one — settings survive.
16. **Update check:** with the app on the latest version, Settings → About → “Check for
    updates” reports up-to-date; with a newer release published, the quiet banner appears
    on the home screen after (re)opening the app, the dialog shows the version and change
    summary, “Later” silences that exact version, the download shows progress and then the
    install prompt (approve “install unknown apps” once), and the system installs the
    verified APK over the existing app with data intact. In airplane mode nothing pops up
    and prayer functions are unaffected.
17. **Landing page:** open <https://ahmedzakefit.github.io/noor-al-sabeel/> — Arabic by
    default, the language toggle flips direction (RTL/LTR) and persists, the download
    button leads to `/releases/latest`, and the page renders on a phone-width screen
    without horizontal scrolling.

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
  وNominatim، خرائط الإعدادات، المصادر البديلة لبحث المساجد وتصنيف أخطائه، رقائق
  مسافة ١–١٠ كم، وعَلم تشغيل الأذان الذي يقود زر الإيقاف).
- **موثوقية بحث المساجد (`MosqueSearchTest`)** — تحليل استجابة Nominatim على عينة حقيقية
  من وسط القاهرة (ترشيح الإسلام يُخرج الكنائس ويُبقي مساجد landuse/religious ويُزيل
  التكرارات)، صندوق البحث المحيط بنقاط عشرية وترتيب أركان سليم، مهلة الإجمالية
  20 ثانية وحدود المنافذ أدنى منها، وترتيب أسباب الفشل (الانقطاع ثم المهلة ثم الانشغال).
- **عدّ التذكير التنازلي (`ReminderRulesTest`)** — الهدف هو وقت الصلاة بعدّاد حيّ نازل،
  ولا يُنشر تذكير فات وقته أصلًا، والمعرّفات ثابتة ومتمايزة، وتُنظّف إشعارات الصلوات
  التي لم تعد مجدولة.
- **اختبارات التحقق من التحديث (`UpdateParserTest`/`UpdateVerifierTest`)** — تحليل الوسم
  والإصدار، كشف الأحدث رقميًا، تحليل JSON للإصدار وروابط APK والبصمة، قالب sha256sum،
  قائمة النطاقات المسموحة، ومصفوفة القرار كاملة (الأحدث ← متاح، المثل/الأقدم ← محدّث،
  فشل الاتصال ← صمت تلقائي ورسالة يدويًا، المؤجَّل يُسكت تلقائيًا فقط)، ومصفوفة الرفض
  قبل التثبيت (بصمة غائبة/مخالفة، حزمة أخرى، إصدار غير أحدث، توقيع مخالف).

## مصفوفة الاختبار اليدوي (على هاتفك)

1. **تبديل اللغة** عربي/إنجليزي: انعكاس الواجهة، وصول الإشعارات باللغة المختارة،
   والثبات بعد إعادة التشغيل.
2. **رفض إذن الموقع:** يبقى التطبيق قابلًا للاستخدام مع البحث والإحداثيات اليدوية.
3. **الموقع التلقائي** بعد منح الإذن؛ وفي أول تشغيل تظهر رسالة توضيحية قبل نافذة
   الإذن، ويبدأ تحديد الموقع فور المنح دون إعادة فتح التطبيق.
4. **أول تشغيل:** بعد اختيار أول موقع من الإعدادات ينتقل التطبيق مباشرة إلى شاشة
   مواقيت الصلاة دون حاجة لإغلاقه وإعادة فتحه.
5. **وضع الطيران:** المواقيت والقبلة والهجري تعمل، ويظهر خطأ «لا اتصال بالإنترنت»
   مع زر إعادة المحاولة في بحث المساجد.
6. **متانة بحث المساجد:** على بيانات الهاتف ابحث في وسط مدينة — تصل النتائج سريعًا من
   مصدر Nominatim حتى لو تعذّرت كل نقاط Overpass (حالة v1.0.3)؛ لا يتجاوز انتظارك
   ٢٠ ثانية إجمالًا؛ مغادرة الشاشة أثناء البحث توقف المؤشر فورًا؛ وعند فشل كل المصادر
   يُذكر السبب مع زر إعادة المحاولة؛ وريقات المسافة تعرض ١–١٠ كم برقم ووحدة سليمين
   (عربي/إنجليزي) مع تحديد القيمة المختارة.
7. **إعادة تشغيل الهاتف:** تستمر الإشعارات دون فتح التطبيق.
8. **تغيير المنطقة الزمنية أو السفر.**
9. **الإشعارات:** التجربة، الصوت، الاهتزاز، التفعيل لكل صلاة، والتذكير المسبق الذي يجب
   أن يعرض **عدّادًا حيًّا يتنازل** كل ثانية دون إيقاظ التطبيق، وعند دخول الوقت
   يُستبدل بإشعار الصلاة أو الأذان فلا يبقى «باقي N دقيقة» جامدًا؛ وعلى
   أندرويد 13+ لا تتكرر نافذة الإذن بعد الرفض، والمنح لاحقًا من إعدادات النظام
   يُفعّل الإشعارات دون إعادة تثبيت.
10. **الأذان (أسلوب التنبيه الصوتي):** اختر «أذان كامل» في الإعدادات ثم اضغط «تجربة
   الأذان» — يجب أن يُرفَع الأذان العادي على مستوى صوت المنبّه مع إشعار مستمر فيه زر
   إيقاف، ويتحول زر الإعدادات نفسه إلى «إيقاف الأذان» أثناء التشغيل ويوقف الصوت
   والخدمة تمامًا (اختبر ذلك أيضًا مع رفض إذن الإشعارات حيث يكون الإشعار مخفيًا
   وزر الشاشة هو وسيلة الإيقاف الوحيدة). ثم تحقّق عند صلاة حقيقية: يُرفَع التسجيل
   الصحيح (أذان الفجر بعبارة «الصلاةُ خيرٌ من النوم» للفجر)، ويستمر التشغيل مع قفل
   الشاشة/إطفائها وفي وضع Doze، ويعمل زر
   الإيقاف من شاشة القفل، ويترك الإشعار سجلًا يُلغى باللمس بعد انتهاء الأذان، وتبقى
   الصلوات المعطّلة صامتة، ويبقى التذكير المسبق إشعارًا قصيرًا، ويظل الأذان يعمل بعد
   إعادة التشغيل. مع تصویر مستوى المنبّه إلى الصفر يسكت الصوت لكن يبقى الإشعار ظاهرًا.
11. **تحسين البطارية** على رومات الشركات.
12. **القبلة:** قارن رقم الاتجاه بقيمة معروفة لمدينتك، واستجابة البوصلة والمعايرة.
13. **المساجد:** المسافة والملاحة والإسناد.
14. **التاريخ الهجري** مع التعديل اليدوي.
15. **الترقية فوق نسخة قائمة** مع بقاء الإعدادات.
16. **التحقق من التحديث:** على أحدث إصدار يعرض «التحقق من وجود تحديثات» في الإعدادات ←
    حول التطبيق أنك محدّث؛ وعند نشر إصدار أحدث يظهر شريط هادئ في الرئيسية بعد الفتح،
    ويعرض الحوار الإصدار وملخص التغييرات، و«لاحقًا» يُسكت هذا الإصدار بعينه، ويظهر
    تقدم التنزيل ثم نافذة التثبيت (وافق على «تثبيت تطبيقات غير معروفة» مرة واحدة)،
    ويثبّت النظام الملف المتحقق منه فوق التطبيق ببقاء البيانات. وفي وضع الطيران لا
    يظهر شيء ولا تتأثر وظائف الصلاة.
17. **صفحة الهبوط:** افتح <https://ahmedzakefit.github.io/noor-al-sabeel/> — العربية هي
    الافتراضية، وزر اللغة يبدّل الاتجاه (RTL/LTR) ويحفظ الاختيار، وزر التحميل يقود إلى
    `/releases/latest`، والصفحة تتصرف على عرض الهاتف دون تمرير أفقي.
