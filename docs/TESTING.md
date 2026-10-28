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
  churches out, keeps landuse/religious mosques, collapses exact duplicates), **Photon
  GeoJSON parsing** against another live fixture (junk streets and church entries dropped,
  lon/lat order respected, dedup), the bounded viewbox and the Photon bbox (documented
  minLon,minLat,maxLon,maxLat order), the keyword per UI language with `lang` provably
  never sent to Photon (it rejects unsupported values with HTTP 400), the **parallel race
  itself** (a stalled source never delays the healthy one — both directions; the loser's
  request is cancelled), the Overpass fallback when both independent sources fail, the
  honest empty answer, the overall 15 s deadline firing while everything stalls, the
  tier-1/mirror socket budgets beneath that deadline, failure-reason ranking
  (offline > timeout > busy), and the Overpass fallback parser.
- **Mosque location + layout regression tests (`MosquesLocationAndLayoutTest`)** — pins
  the fixes for the on-device reports: the search-plan decision (a live device fix whenever
  the permission is granted; otherwise the in-context permission request — never a silent
  saved-location search, which is reachable only as an explicit on-screen choice), the
  pre-search map fallback to the saved location (never claiming to be "current"), a
  request-level proof that searching after moving queries the **new** area (the old one is
  absent from the outgoing Nominatim viewbox and Photon bbox — there is no result caching),
  the radius-fitted map zoom (monotonic in the radius, clamped to the tile source's 3–19
  range), the adaptive map height for small/landscape screens (clamped 150–260 dp,
  monotonic, leaving a usable results area), and the human-scale locate budget before the
  explicit LocationFailed fallback.
- **Mosque permission/search audit tests (`MosquesDiagnosticAuditTest`)** — born from the
  2026-10-24 diagnostic round and kept as regression pins: without the grant the default
  search plan requires the permission (and issues no request), the explicit saved-fallback
  queries exactly the saved area, with the grant a fresh fix area — not the saved one — is
  what reaches the outgoing queries, the map-sync LaunchedEffect keys are
  recomposition-stable across distinct-but-equal instances (so plain recompositions cannot
  re-center the map), and no result caching exists (each search embeds its own center in
  the request). These are JVM-level pins of the decision and request layers only — the
  permission dialog, gestures and layout still need on-device verification.
- **Reminder countdown tests (`ReminderRulesTest`)** — the reminder spec targets the prayer
  time with a counting-down chronometer, stale reminders (target already passed) are never
  posted, notification IDs are distinct and stable, and prayers without scheduled alarms
  get their notifications cleaned up.
- **Update-check tests (`UpdateParserTest` / `UpdateVerifierTest`)** — tag/version parsing,
  version resolution from the `/releases/latest` redirect (the rate-limit-free pre-check
  path), newer-version detection (numeric, not lexicographic), release-JSON parsing (APK +
  checksum asset URLs, size, changelog summary stripping, and deterministic preference of
  the versioned APK asset over the stable-named copy in any asset order), sha256sum-format
  checksum parsing, the HTTPS host allowlist, and the full decision matrix: newer → available,
  same/older → up to date, connection failure → silent up-to-date in auto mode but a
  visible failure for manual checks, postponed version silenced in auto mode only. The
  verifier matrix blocks install on checksum unknown/mismatch, wrong package, non-newer
  versionCode, or a mismatched signing certificate. The API-free fallback pins the
  deterministic asset URLs (versioned pair first, stable pair fallback, every URL HTTPS on
  the allowlisted github.com, malformed/test versions rejected, and the constructed URLs
  byte-identical to a real release's published asset URLs), the v1.0.21 browser
  escape-hatch URL (the versioned APK asset URL handed to the system browser when the
  in-app download cannot be delivered, tag-shaped inputs normalised, malformed versions
  rejected), and the failure-mapping test
  pins that every typed download failure (network / HTTP / empty / checksum unavailable /
  checksum mismatch / package / not-newer / signature / disallowed source) maps to a
  distinct localized message so no cause is ever silently swallowed.
- **Update swap-race regression tests (`UpdateDownloadPolicyTest`, v1.0.17)** — pins the
  proven root cause of the v1.0.16 "SHA-256 mismatch" wave (the tag was re-published
  with different bytes while users were mid-update): checksum-family verdicts retry
  exactly once, package/version/signature verdicts never retry, a served Content-Length
  that differs from the release-asset size proves an asset swap and aborts before hashing
  (unknown sizes never abort), and end-to-end pure simulations cover the positive paths
  (a re-publish between the checksum fetch and the APK download resolves via one
  checksum re-fetch without re-downloading; a swapped server cache resolves via one
  re-download) and the negative paths (a genuinely corrupted file fails permanently after
  the single retry; an unavailable checksum retries once then reports unavailable).
- **Adhan playback state tests (`AdhanPlaybackStateTest`, extended v1.0.17)** — the
  live flag the settings screen drives its stop controls from, plus the new preview
  tracking: a test adhan reports itself as preview, the real prayer-time adhan reports
  itself as NOT preview, stopping clears the flag, and a start without explicit preview
  args defaults to real (a prayer-time start can never inherit a stale preview=true).
- **Adhan asset inventory (`AdhanAssetsTest`, extended v1.0.17)** — every selectable
  voice must bundle a valid regular + Fajr recording (a missing/invalid file would
  silently fall back to the default recording at prayer time), all bundled audio stays
  inside the APK size budget, and every recording is pinned by its exact SHA-256 in a
  reviewed inventory so audio assets can no longer change silently (the failure mode
  behind the mislabelled Cairo voice — see NOTICE.md's voices audit).
- **Post-release link check (`verify-download` workflow)** — after every release publish
  (and weekly) the fixed landing-page link `/releases/latest/download/noor-al-sabeel.apk`
  is downloaded through and verified: HTTP 200, ZIP magic bytes, plausible size, and SHA-256
  equal to both the versioned release asset and the published `.sha256` checksum. Only a
  verified release updates the landing page's `release.json` metadata.

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
   arrive within a few seconds via whichever of the two parallel sources (Nominatim /
   Photon) is healthy on that network, even when the other one and every Overpass endpoint
   are unreachable (v1.0.3/v1.0.4's failure mode); the total wait never exceeds ~15 s;
   leaving the screen mid-search stops the spinner immediately; when everything fails the
   error screen states the reason and offers Retry; in the Arabic UI the mosque names
   should mostly be Arabic; the radius chips show every value 1–10 km
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
   **v1.0.17 — live vs test state:** while the REAL prayer-time adhan is reciting, open
   Settings: the red card must show the **live adhan** wording (not “test adhan”), the
   per-voice test buttons are disabled while it plays (tapping one must not start a test
   or interrupt the real adhan), and after it finishes the test buttons work again. A
   manual test started from the buttons still shows the “test adhan” card and stop toggle.
   Known open defect (documented in NOTICE.md, NOT fixed in v1.0.17): the Cairo voice
   opens with TWO takbirs — verify you hear the currently-documented behaviour and see
   NOTICE.md before changing any bundled recording.
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
    install prompt (approve “install unknown apps” once; when the grant is missing the
    dialog now explains it and links to the system settings page for this app), and the
    system installs the verified APK over the existing app with data intact. Failures must
    say WHY (network / HTTP error / checksum unavailable or mismatch / signature mismatch /
    installer could not open) and offer a retry — including when the GitHub API is
    rate-limited (HTTP 403): the banner must still appear and the download must still
    verify, because the check falls back to the deterministic release asset URLs. In
    airplane mode nothing pops up and prayer functions are unaffected.
    **v1.0.17 — update from the published release itself:** after a release is published,
    update from the PUBLISHED assets (not a local build): the in-app update must download,
    verify and install cleanly; a release re-published mid-download (if ever possible
    again) must self-heal via the single checksum re-fetch instead of failing with a
    false checksum mismatch. Installing the downloaded APK may show Google Play
    Protect's sideloaded-app caution (a reputation heuristic for self-signed apps —
    see docs/PLAY_PROTECT.md); the app itself must never ask you to disable it, and the
    APK can always be verified against the published `.sha256` before installing.
17. **Landing page:** open <https://ahmedzakefit.github.io/noor-al-sabeel/> — Arabic by
    default, the language toggle flips direction (RTL/LTR) and persists, the download
    button downloads the APK file directly (no intermediate release page; on Android a
    download notification appears and the file opens into the install flow), the version /
    size / date line appears under the button (only when the release metadata was verified
    by the post-release workflow), the secondary “All releases” link opens the releases
    list, and the page renders on a phone-width screen without horizontal scrolling.

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
  التكرارات)، وتحليل GeoJSON لـ Photon على عينة حقيقية أخرى (إسقاط الشوارع والكنائس،
  ترتيب الطول/العرض، إزالة التكرار)، صندوقا البحث بنقاط عشرية وترتيب أركان سليم
  (ترتيب Nominatim وترتيب Photon الموثّق)، الكلمة المفتاحية حسب لغة الواجهة مع عدم
  إرسال lang إلى Photon أبدًا (تردّ 400)، **التسابق التوازي نفسه** (المصدر المتعطّل
  لا يؤخر السليم — في الاتجاهين، ويُلغى طلب الخاسر)، احتياطي Overpass عند فشل
  المصدرين، الإجابة الفارغة الصادقة، المهلة الإجمالية 15 ثانية مع تعطّل كل شيء،
  حدود المنافذ أدنى منها، وترتيب أسباب الفشل (الانقطاع ثم المهلة ثم الانشغال).
- **انحدارات موقع البحث والتخطيط (`MosquesLocationAndLayoutTest`)** — تُثبّت إصلاحات
  تقارير الاختبار الميداني: قرار خطة البحث (إصلاح حيّ للجهاز متى مُنح الإذن؛ وإلا
  فطلب الإذن في السياق — لا بحث صامت بالموقع المحفوظ أبدًا، فهو متاح فقط كخيار صريح
  على الشاشة)، احتياطي الخريطة قبل البحث نحو الموقع المحفوظ (دون ادعاء أنه «حالي»
  أبدًا)، وإثبات على مستوى الطلبات بأن البحث بعد الانتقال يستعلم **المنطقة الجديدة**
  (القديمة غائبة عن viewbox الخاص بـ Nominatim وbbox الخاص بـ Photon — لا تخزين مؤقت
  للنتائج)، وتقريب الخريطة الموائم للنطاق (رتيب مع النطاق ومحصور بين ٣–١٩)، وارتفاع
  الخريطة التكيّفي للشاشات الصغيرة والأفقية (محصور ١٥٠–٢٦٠ dp ورتيب ويترك مساحة
  قابلة للاستخدام للنتائج)، وميزانية تحديد الموقع البشرية قبل الاحتياطي الصريح
  لفشل الموقع.
- **اختبارات تدقيق الإذن والبحث للمساجد (`MosquesDiagnosticAuditTest`)** — وُلدت من
  جولة التشخيص في 2026-10-24 وبقيت كتثبيتات انحدار:
  دون الإذن تكون خطة البحث الافتراضية طلب الإذن (ولا يصدر أي طلب شبكة)، واحتياطي
  الموقع المحفوظ الصريح يستعلم منطقة المحفوظ تحديدًا، وبالإذن تصل منطقة الإصلاح
  الحي — لا المحفوظة — إلى الطلبات الصادرة، ومفاتيح مزامنة الخريطة مستقرة عبر
  إعادة التكوين حتى مع نسخ متساوية القيمة مختلفة الهوية (فلا تعيد الإعادة تكوين
  تمركز الخريطة)، ولا تخزين مؤقت للنتائج (كل بحث يدمج مركزه في الطلب). هذه تثبيتات
  على مستوى القرار والطلب في JVM فقط — نافذة الإذن والإيماءات والتخطيط ما زالت
  بحاجة إلى تحقق على الجهاز.
- **عدّ التذكير التنازلي (`ReminderRulesTest`)** — الهدف هو وقت الصلاة بعدّاد حيّ نازل،
  ولا يُنشر تذكير فات وقته أصلًا، والمعرّفات ثابتة ومتمايزة، وتُنظّف إشعارات الصلوات
  التي لم تعد مجدولة.
- **اختبارات التحقق من التحديث (`UpdateParserTest`/`UpdateVerifierTest`)** — تحليل الوسم
  والإصدار، كشف الأحدث رقميًا، تحليل JSON للإصدار وروابط APK والبصمة، قالب sha256sum،
  قائمة النطاقات المسموحة، ومصفوفة القرار كاملة (الأحدث ← متاح، المثل/الأقدم ← محدّث،
  فشل الاتصال ← صمت تلقائي ورسالة يدويًا، المؤجَّل يُسكت تلقائيًا فقط)، ومصفوفة الرفض
  قبل التثبيت (بصمة غائبة/مخالفة، حزمة أخرى، إصدار غير أحدث، توقيع مخالف)، وتفضيل
  ملف APK المُوسوم بالإصدار تحديدًا على النسخة ثابتة الاسم مهما كان ترتيب الأصول.
  كما تُثبّت اختبارات المسار البديل بدون API روابط الأصول الحتمية (الزوج المُوسوم
  أولًا ثم الثابت، كلها HTTPS على github.com المسموح، ورفض أسماء الإصدارات التجريبية،
  وتطابق الروابط المبنية بايتًا بايتًا مع روابط إصدار حقيقي منشور)، ويُثبّت اختبار
  الخرائط أن كل سبب فشل مُنمّط في التنزيل (شبكة/HTTP/ملف فارغ/بصمة غائبة أو مخالفة/
  حزمة/غير أحدث/توقيع/مصدر ممنوع) يقابل رسالة مترجمة متميزة فلا يُبتلع أي سبب صمتًا.
- **فحص رابط التنزيل بعد النشر (سير عمل `verify-download`)** — بعد كل نشر إصدار (وأسبوعيًا)
  يُنزَّل الملف عبر الرابط الثابت `/releases/latest/download/noor-al-sabeel.apk` ويُتحقق
  منه: HTTP 200 وبايتات ZIP الفعلية وحجم معقول وبصمة SHA-256 مطابقة لأصل الإصدار
  المُوسوم ولملف البصمة المنشور. ولا يُحدَّث `release.json` الذي تقرأه صفحة الهبوط
  إلا بعد نجاح هذا التحقق كله.

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
6. **متانة بحث المساجد:** على بيانات الهاتف ابحث في وسط مدينة — تصل النتائج خلال
   ثوانٍ من أيّ المصدرين المتوازيين (Nominatim / Photon) السليم على تلك الشبكة حتى
   لو تعذّر الآخر وكل نقاط Overpass (حالة v1.0.3/v1.0.4)؛ لا يتجاوز انتظارك
   ١٥ ثانية إجمالًا؛ مغادرة الشاشة أثناء البحث توقف المؤشر فورًا؛ وعند فشل كل
   المصادر يُذكر السبب مع زر إعادة المحاولة؛ وفي الواجهة العربية تكون أسماء
   المساجد عربية في الغالب؛ وريقات المسافة تعرض ١–١٠ كم برقم ووحدة سليمين
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
   **v1.0.17 — حالة الأذان الفعلي مقابل التجربة:** أثناء رفع الأذان الفعلي في موعد
   الصلاة افتح الإعدادات: يجب أن تُظهر البطاقة صياغة **«الأذان الفعلي»** (لا
   «تجربة الأذان»)، وتكون أزرار تجربة الأصوات معطّلة أثناء رفعه (الضغط عليها لا
   يبدأ تجربة ولا يقاطع الأذان الفعلي)، وبعد انتهائه تعمل أزرار التجربة مجددًا. أما
   التجربة اليدوية من الأزرار فتُظهر بطاقة «تجربة الأذان» وزر الإيقاف كالمعتاد.
   عيب معروف موثّق (NOTICE.md) وغير مُصلح بعد في v1.0.17: يبدأ صوت القاهرة
   بتكبيرتين — تحقق من السلوك الموثّق الحالي وراجع NOTICE.md قبل تغيير أي تسجيل.
11. **تحسين البطارية** على رومات الشركات.
12. **القبلة:** قارن رقم الاتجاه بقيمة معروفة لمدينتك، واستجابة البوصلة والمعايرة.
13. **المساجد:** المسافة والملاحة والإسناد.
14. **التاريخ الهجري** مع التعديل اليدوي.
15. **الترقية فوق نسخة قائمة** مع بقاء الإعدادات.
16. **التحقق من التحديث:** على أحدث إصدار يعرض «التحقق من وجود تحديثات» في الإعدادات ←
    حول التطبيق أنك محدّث؛ وعند نشر إصدار أحدث يظهر شريط هادئ في الرئيسية بعد الفتح،
    ويعرض الحوار الإصدار وملخص التغييرات، و«لاحقًا» يُسكت هذا الإصدار بعينه، ويظهر
    تقدم التنزيل ثم نافذة التثبيت (وافق على «تثبيت تطبيقات غير معروفة» مرة واحدة؛
    وإذا كان الإذن غير ممنوح يشرحه الحوار الآن ويربط بصفحة إعدادات النظام الخاصة
    بالتطبيق)، ويثبّت النظام الملف المتحقق منه فوق التطبيق ببقاء البيانات. وعند أي
    فشل يجب أن يُذكر السبب (شبكة/خطأ HTTP/بصمة غائبة أو مخالفة/توقيع مخالف/تعذّر فتح
    المُثبّت) مع إمكانية إعادة المحاولة — بما في ذلك عند تقييد واجهة GitHub‏ (HTTP 403):
    يجب أن يظل الشريط ظاهرًا والتنزيل متحققًا منه لأن الفحص يسقط على روابط أصول
    الإصدار الحتمية. وفي وضع الطيران لا يظهر شيء ولا تتأثر وظائف الصلاة.
    **v1.0.17 — التحديث من الإصدار المنشور نفسه:** بعد نشر أي إصدار جرّب التحديث من
    الأصول المنشورة فعليًا (لا من بناء محلي): يجب أن يُنزّل ويتحقق ويثبّت بلا أخطاء؛
    وإذا أُعيد نشر إصدار أثناء تنزيلٍ قائم (إن أمكن مجددًا) فيجب أن يتعافى المسار
    بإعادة جلب البصمة مرة واحدة بدل الفشل ببصمةٍ زائفة. وقد يُظهر تثبيت الـ APK
    تحذير Play Protect للتطبيقات المثبتة خارج المتجر (إجراء سمعة للتطبيقات موقّعة
    ذاتيًا — انظر docs/PLAY_PROTECT.md)؛ التطبيق نفسه لا يطلب تعطيله أبدًا، ويمكن
    دائمًا التحقق من الملف مقابل ملف ‏`.sha256` المنشور قبل التثبيت.
17. **صفحة الهبوط:** افتح <https://ahmedzakefit.github.io/noor-al-sabeel/> — العربية هي
    الافتراضية، وزر اللغة يبدّل الاتجاه (RTL/LTR) ويحفظ الاختيار، وزر التحميل يُنزّل ملف
    الـ APK مباشرة عبر الرابط الثابت `/releases/latest/download/noor-al-sabeel.apk` دون
    صفحة وسيطة (على أندرويد يظهر إشعار التنزيل ويُفتح الملف في مسار التثبيت)، ويظهر سطر
    الإصدار/الحجم/التاريخ أسفل الزر فقط بعد أن يتحقق سير عمل ما بعد النشر من بيانات
    الإصدار، ويفتح الرابط الثانوي «كل الإصدارات» قائمة الإصدارات، والصفحة تتصرف على عرض
    الهاتف دون تمرير أفقي.
