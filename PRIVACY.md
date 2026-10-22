# Privacy Policy — سياسة الخصوصية

**English** | [العربية بالأسفل](#العربية)

*Last updated: 2026-10-23*

NOOR (نور) is a free, open-source Android application. This policy describes exactly what
the app does with data. Because the app contains no analytics or crash reporting, this
document is based on the source code, which you can inspect yourself.

## What NOOR stores

Everything NOOR stores stays **on your device** in Android's private app storage:

- your selected location (coordinates, city label, time zone) — chosen by you via GPS,
  city search or manual entry;
- your calculation settings (method, angles, per-prayer offsets, reminders);
- your appearance preferences (theme, language, numerals, Hijri date adjustment).

This data is never sent anywhere and never leaves the device. Uninstalling the app removes
it completely.

## What NOOR sends

By default, nothing. The app uses the internet **only** when you actively use one of these
features:

- **Mosque search / map screen** — your search center (the coordinates you selected) is sent
  to the public Overpass API to find mosques, and map tiles are fetched from
  OpenStreetMap servers for the visible area.
- **Optional online city search** — the text you type is sent to the public Nominatim
  service (OpenStreetMap) when the offline catalog has no match.

These services are operated by third parties (OpenStreetMap Foundation and Overpass API
maintainers) and are governed by their own policies. NOOR sends no identifiers, no
advertising IDs and no usage statistics with these requests.

## Permissions and why

| Permission | Purpose |
|---|---|
| Location (coarse/fine) | Only used when you tap "use current location" to detect your position for prayer times and qibla |
| Notifications | Showing prayer-time and reminder notifications |
| Internet / network state | Mosque search, map tiles, optional online city search |
| Receive boot completed | Re-scheduling prayer notifications after the phone restarts |
| Exact alarms | Ringing prayer notifications at the precise time |
| Vibrate | Notification vibration (optional) |
| Request ignore battery optimizations | Optional reliability step some vendors require |

NOOR requests no contacts, no files, no camera, no microphone, no advertising IDs.

## No ads, no trackers, no accounts

There are no ads, no analytics SDKs, no crash reporters, no accounts and no payments in
NOOR — the source code contains none.

## Changes

Material changes to this policy will be noted in the repository's CHANGELOG.

## Contact

Open an issue at <https://github.com/AhmedZakeFit/noor/issues> for any privacy question.

---

# العربية

*آخر تحديث: 2026-10-23*

نور تطبيق أندرويد مجاني ومفتوح المصدر، وهذه السياسة تصف بالضبط ما يفعله التطبيق
بالبيانات. ولأن التطبيق لا يحتوي أي تحليلات أو تقارير أعطال، فإن هذا المستند مبني على
الشيفرة المصدرية التي يمكنك فحصها بنفسك.

## ما الذي يخزّنه نور؟

كل ما يخزّنه نور يبقى **على جهازك** في وحدة التخزين الخاصة بالتطبيق:

- موقعك المختار (الإحداثيات واسم المدينة والمنطقة الزمنية) — تختاره بنفسك عبر GPS أو
  البحث أو الإدخال اليدوي؛
- إعدادات الحساب (الطريقة، الزوايا، فروق التعديل، التذكيرات)؛
- تفضيلات المظهر (السمة واللغة والأرقام وتعديل التاريخ الهجري).

هذه البيانات لا تُرسل إلى أي مكان ولا تترك الجهاز أبدًا، وحذف التطبيق يمحوها تمامًا.

## ما الذي يرسله نور؟

افتراضيًا: لا شيء. يستخدم التطبيق الإنترنت **فقط** عند استخدامك إحدى هذه الميزات:

- **البحث عن المساجد / شاشة الخريطة** — يُرسل مركز البحث (الإحداثيات التي اخترتها) إلى
  خدمة Overpass العامة للعثور على المساجد، وتُجلب مربعات الخريطة من خوادم
  OpenStreetMap للمنطقة الظاهرة.
- **البحث الاختياري عن المدن عبر الإنترنت** — يُرسل النص الذي تكتبه إلى خدمة Nominatim
  العامة (OpenStreetMap) عندما لا يجد الفهرس المدمج نتيجة مطابقة.

هذه الخدمات يشغّلها طرف ثالث (مؤسسة OpenStreetMap ومشغّلو Overpass) وتحكمها سياستهم
الخاصة. لا يرسل نور معها أي معرّفات ولا معرّفات إعلانية ولا إحصاءات استخدام.

## الأذونات وسببها

| الإذن | الغرض |
|---|---|
| الموقع (تقريبي/دقيق) | يُستخدم فقط عند ضغطك "استخدام الموقع الحالي" لتحديد موقعك للمواقيت والقبلة |
| الإشعارات | عرض إشعارات المواقيت والتذكيرات |
| الإنترنت وحالة الشبكة | البحث عن المساجد ومربعات الخريطة والبحث الاختياري عن المدن |
| استقبال اكتمال الإقلاع | إعادة جدولة إشعارات الصلاة بعد إعادة تشغيل الهاتف |
| التنبيهات الدقيقة | رنين إشعارات الصلاة في الوقت المحدد بدقة |
| الاهتزاز | اهتزاز الإشعار (اختياري) |
| طلب تجاوز تحسين البطارية | خطوة موثوقية اختيارية تتطلبها بعض الشركات |

لا يطلب نور أيّ اتصالات أو ملفات أو كاميرا أو ميكروفون أو معرّفات إعلانية.

## بلا إعلانات ولا متتبعات ولا حسابات

لا يوجد في نور إعلانات ولا أدوات تحليلات ولا مراسلات أعطال ولا حسابات ولا مدفوعات —
الشيفرة المصدرية لا تحتوي أيًّا منها.

## التغييرات

سيُدوَّن أي تغيير جوهري في هذه السياسة في سجل التغييرات الخاص بالمستودع.

## التواصل

افتح مشكلة في <https://github.com/AhmedZakeFit/noor/issues> لأي سؤال يخص الخصوصية.
