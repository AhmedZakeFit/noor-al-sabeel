# Prayer times: methods, sources, accuracy — المواقيت: الطرق والمصادر والدقة

**English** | [العربية بالأسفل](#العربية)

This document explains exactly how Noor Al-Sabeel computes prayer times, where the numbers come
from, and what the known limits are. It exists so users can make informed comparisons
with their mosques and other apps.

## Algorithm

All computation runs **on your device** in the `core` module (`PrayerCalculator.kt`,
`SolarMath.kt`):

1. **Solar position** — standard low-precision formulas from Jean Meeus,
   *Astronomical Algorithms* (2nd ed., ch. 25 & 28): mean/true longitude, equation of
   center with nutation and aberration corrections, apparent obliquity, declination and
   the equation of time. Typical accuracy: ~0.01°, i.e. well under one minute of time.
2. **Solar transit (true noon)** — solved in UTC, then refined once at the transit
   instant. `transit = 720 − 4·longitude − equationOfTime`.
3. **Hour angles** — each prayer is the moment the sun reaches a given altitude:
   - Sunrise/sunset: −0.833° (refraction + solar semi-diameter), with an elevation
     correction `−0.0347·√elevation`.
   - Fajr/Isha: the method's twilight angle (see table).
   - Asr: altitude where an object's shadow equals the factor × its height
     (`atan(1/(factor + tan(|lat − decl|)))`), factor 1 (Standard) or 2 (Hanafi).
4. **Time zone conversion** — times are converted via `java.time`, so DST transitions are
   handled correctly, including transition days.
5. **High latitudes** — when the sun never reaches the twilight angle (roughly above
   ~48.5° with the MWL angles, higher with smaller angles), a night-portion rule is
   applied (Angle-based by default; Middle-of-the-night and Seventh-of-the-night are
   selectable). Above the polar circles in mid-summer/-winter even sunrise/sunset can
   disappear; Noor Al-Sabeel then approximates the day length as 24h and flags the times as
   approximate on screen.
6. **User offsets** — the ±30-minute per-prayer adjustments are applied last.

Dhuhr is the refined transit (no hidden extra minutes). Midnight = sunset + (next fajr −
sunset)/2; the last third of night starts at next fajr − (next fajr − sunset)/3.

## Methods and their parameters

| Method | Fajr | Isha |
|---|---|---|
| Muslim World League | 18.0° | 17.0° |
| **Egyptian General Authority of Survey (default)** | 19.5° | 17.5° |
| ISNA | 15.0° | 15.0° |
| Umm al-Qura, Makkah | 18.5° | maghrib + 90 min (120 min in Ramadan per the Umm al-Qura calendar) |
| Karachi | 18.0° | 18.0° |
| Gulf region | 19.5° | maghrib + 90 min |
| Kuwait | 18.0° | 17.5° |
| Qatar | 18.0° | maghrib + 90 min |
| Singapore (MUIS) | 20.0° | 18.0° |
| Turkey (Diyanet) | 18.0° | 17.0° |
| France (UOIF) | 12.0° | 12.0° |
| Custom | your choice (10–21°, 0.5° steps) | your choice |

Asr defaults to the Standard (Shafi/Maliki/Hanbali) shadow factor; Hanafi is selectable.

## Verification (what CI actually checks)

The test suite compares Noor Al-Sabeel against independent references:

- **288 prayer-time rows** generated from the AlAdhan API (which uses the adhan-java
  engine) across 9 cities (Cairo, Makkah, Istanbul, London, New York, Jakarta, Sydney,
  Anchorage, Reykjavik), 6 methods, 5 dates including DST transitions, plus Hanafi-asr
  and Ramadan subsets. Median deviation: ≤1 minute; tolerance 2 minutes (4 where
  high-latitude clamping is active).
- **731 Hijri conversions** against the MIT-licensed hijridate library (Umm al-Qura),
  plus round-trip checks and known-event spot checks (1 Ramadan 1446 = 2025-03-01,
  Eid al-Fitr 1446 = 2025-03-30, Eid al-Adha 1446 = 2025-06-06).
- **14 qibla bearings** against AlAdhan (±0.5°).

## Known deviations and limits (documented, not hidden)

1. **Umm al-Qura isha:** AlAdhan's implementation uses maghrib + 120 minutes year-round
   (its Ramadan determination is a tabular calendar that can differ by a day at the
   boundaries). Noor Al-Sabeel follows the official Umm al-Qura convention: 90 minutes, 120 during
   Ramadan (per the Umm al-Qura table). Difference: 30 minutes on some days.
2. **Asr convention:** adhan-java/AlAdhan resolves the Asr shadow altitude against the
   sun's *apparent* altitude (including refraction, ~+0.15° near the horizon); Noor Al-Sabeel uses
   the *geometric* altitude, which the physical shadow definition requires. At normal
   latitudes the difference is under a minute; near the polar circles (where the winter
   Asr altitude is only a few degrees) it can reach 5–10 minutes.
3. **Solar model precision:** Noor Al-Sabeel's Meeus low-precision model differs from full
   SPA-based engines by up to ~0.05° in declination, which is amplified at extreme
   latitudes (minutes, not seconds). Sub-minute at normal latitudes.
4. **High-latitude clamping:** where twilight does not occur, results depend on the
   chosen night-portion rule; different apps pick different rules. The home screen shows
   an approximation notice whenever clamping is active.
5. **Rounding & conventions:** times are rounded to the minute; some authorities add 1–2
   minutes to dhuhr for zenith crossing. Use the per-prayer offsets to match your mosque.
6. **Above the polar circles** in deep summer/winter, sunrise/sunset themselves vanish;
   Noor Al-Sabeel approximates and flags it.
7. **The Hijri calendar** is the official Umm al-Qura civil calendar (1924–2076). Local
   moon sighting can differ by a day — use the ±2-day adjustment in settings. Outside
   the table's range an arithmetic approximation is flagged as approximate.

**Bottom line:** Noor Al-Sabeel is accurate to about a minute at normal latitudes for the standard
methods, and deliberately transparent about every edge case. Prayer times are a religious
obligation — when in doubt, follow your local mosque and use the offsets.

## Qibla

The qibla bearing is the standard great-circle initial bearing toward the Kaaba
(21.4225°N, 39.8262°E), computed locally. The compass screen corrects magnetic north to
true north using the platform's `GeomagneticField` model (WMM-based). Phone magnetometers
are affected by nearby metal, cases with magnets and interference — the app shows sensor
accuracy and calibration instructions, but cannot fix bad hardware. The bearing number
itself (shown on screen) does not depend on the sensors and is reliable everywhere.

---

# العربية

يشرح هذا المستند بدقة كيف يحسب نور السبيل المواقيت، ومن أين تأتي الأرقام، وما حدود الدقة
المعروفة، ليتمكن المستخدم من المقارنة المستنيرة مع مسجده والتطبيقات الأخرى.

## الخوارزمية

كل الحسابات تجري **على جهازك** في وحدة `core`:

1. **الموقع الشمسي** — معادلات Meeus القياسية من كتاب *Astronomical Algorithms*:
   الطول المتوسط والحقيقي ومعادلة المركز مع تصحيحات التزيح والزيغ، والميل الظاهري
   والانحراف ومعادلة الوقت، بدقة نموذجية ~0.01° (أقل من دقيقة زمنيًا).
2. **العبور (الزوال الحقيقي)** — يُحل بتوقيت UT ثم يُنقّح عند لحظة العبور.
3. **زوايا الساعة** — كل صلاة هي لحظة بلوغ الشمس ارتفاعًا محددًا:
   - الشروق/الغروب: −0.833° (انكسار + نصف قطر الشمس) مع تصحيح الارتفاع.
   - الفجر/العشاء: زاوية الشفق الخاصة بالطريقة (انظر الجدول).
   - العصر: ارتفاع يصبح فيه ظل الشيء = المعامل × طوله (1 قياسي أو 2 حنفي).
4. **التحويل الزمني** — عبر `java.time` فيُعالج التوقيت الصيفي — بما يشمل أيام
   التحويل — بصورة صحيحة.
5. **خطوط العرض العليا** — عند عدم بلوغ الشمس زاوية الشفق تُطبق قاعدة نصيب الليل
   (الزاوية افتراضيًا، أو منتصف الليل أو سُبعه)، وفوق الدوائر القطبية تُقرَّب مدة
   اليوم بـ24 ساعة مع إظهار تنبيه تقريب على الشاشة.
6. **فروق المستخدم** — تُطبق تعديلات ±30 دقيقة لكل صلاة في النهاية.

الظهر هو العبور المنقّح (بلا دقائق خفية إضافية). منتصف الليل = الغروب + (فجر الغد −
الغروب)/2، والثلث الأخير يبدأ عند فجر الغد − (فجر الغد − الغروب)/3.

## التحقق (ما يفحصه CI فعليًا)

- **288 صف مواقيت** من واجهة AlAdhan (محرك adhan-java) عبر 9 مدن و6 طرق و5 تواريخ
  تشمل تحويلات التوقيت الصيفي، مع مجموعات العصر الحنفي ورمضان. الانحراف الوسيط ≤
  دقيقة، والتسامح دقيقتان (4 دقائق عند تفعيل قواعد خطوط العرض العليا).
- **731 تحويلًا هجريًا** مقابل مكتبة hijridate (أم القرى) مع فحوص رجعية وتواريخ معروفة.
- **14 اتجاه قبلة** مقابل AlAdhan (±0.5°).

## الانحرافات والحدود المعروفة (موثقة لا مخفية)

1. **إيشا أم القرى:** يستخدم AlAdhan مغربًا+120 دقيقة طوال العام، بينما يتبع نور السبيل
   العُرف الرسمي (90 دقيقة، و120 في رمضان حسب جدول أم القرى) — فرق 30 دقيقة بعض
   الأيام.
2. **عُرف العصر:** يحسب adhan-java الارتفاع *الظاهري* (مع الانكسار) بينما يستخدم نور السبيل
   الارتفاع *الهندسي* الذي يتطلبه تعريف الظل فعليًا؛ الفرق أقل من دقيقة في العرض
   المعتاد وقد يبلغ 5–10 دقائق قرب الدوائر القطبية.
3. **دقة النموذج الشمسي:** نموذج Meeus منخفض الدقة يختلف عن محركات SPA الكاملة
   بـ~0.05° في الانحراف، ويتضخم أثرها في العروض المتطرفة (دقائق لا ثوانٍ).
4. **تقييد العروض العليا:** تختلف التطبيقات في اختيار قاعدة نصيب الليل، ويعرض
   الشاشة الرئيسية تنبيهًا عند التفعيل.
5. **التقريب والعُرف:** تُقرَّب الأوقات للدقيقة، وبعض الجهات تضيف دقيقة أو دقيقتين
   للظهر — استخدم فروق التعديل لمطابقة مسجدك.
6. **فوق الدوائر القطبية** قد يختفي الشروق نفسه فيُقرَّب مع التنبيه.
7. **التقويم الهجري** هو أم القرى الرسمي (1924–2076) وقد تختلف الرؤية المحلية بيوم —
   استخدم تعديل ±2 يوم، وخارج المدى يستخدم تقريب حسابي مع وسم "تقريبي".

**الخلاصة:** دقة نور السبيل تناهز الدقيقة في العروض المعتادة للطرق القياسية، مع شفافية
كاملة في كل الحالات الحدية. والمواقيت عبادة — عند الشك اتبع مسجدك واستخدم الفروق.

## القبلة

الاتجاه هو Bearing الدائرة العظمى الابتدائي نحو الكعبة (21.4225°N, 39.8262°E)
محسوبًا محليًا، وتصحّح الشاشة الشمال المغناطيسي إلى الحقيقي عبر نموذج
`GeomagneticField` المدمج في أندرويد. مستشعرات الهواتف تتأثر بالمعادن والأغطية
الممغنطة — يعرض التطبيق دقة المستشعر وإرشادات المعايرة لكنه لا يستطيع إصلاح عتاد
رديء. رقم الاتجاه المعروض لا يعتمد على المستشعرات وهو موثوق في كل مكان.
