# Privacy Policy — سياسة الخصوصية

**English** | [العربية بالأسفل](#العربية)

*Last updated: 2026-10-24*

Noor Al-Sabeel (نور السبيل) is a free, open-source Android application. This policy describes exactly what
the app does with data. Because the app contains no analytics or crash reporting, this
document is based on the source code, which you can inspect yourself.

## What Noor Al-Sabeel stores

Everything Noor Al-Sabeel stores stays **on your device** in Android's private app storage:

- your selected location (coordinates, city label, time zone) — chosen by you via GPS,
  city search or manual entry;
- your calculation settings (method, angles, per-prayer offsets, reminders);
- your appearance preferences (theme, language, numerals, Hijri date adjustment);
- the version of an update you chose to postpone, and verified update files staged
  in the app cache until installed or discarded.

This data is never sent anywhere and never leaves the device. Uninstalling the app removes
it completely.

## What Noor Al-Sabeel sends

By default, nothing. The app uses the internet **only** when you actively use one of these
features:

- **Optional online city search** — the text you type is sent to the public Nominatim
  service (OpenStreetMap) when the offline catalog has no match.
- **Update check** — on app open (and when you tap "check for updates") the app contacts
  GitHub's API (`api.github.com` / `github.com`) to read the latest published release of
  this repository and compare version numbers. If you choose to download an update, the
  APK comes from the project's own GitHub Releases page. Nothing about you is sent — no
  identifiers, no usage data; it is a plain version lookup.

These services are operated by third parties (the OpenStreetMap Foundation and GitHub) and are
governed by their own policies. Noor Al-Sabeel sends no
identifiers, no advertising IDs and no usage statistics with these requests.

## Permissions and why

| Permission | Purpose |
|---|---|
| Location (coarse/fine) | Only used when you tap "use current location" to detect your position for prayer times and qibla |
| Notifications | Showing prayer-time and reminder notifications |
| Internet / network state | Optional online city search, update check |
| Receive boot completed | Re-scheduling prayer notifications after the phone restarts |
| Exact alarms | Ringing prayer notifications at the precise time |
| Vibrate | Notification vibration (optional) |
| Foreground service + media playback + wake lock | Playing the full adhan at prayer time (only when you choose that alert style); keeps it audible with the screen off |
| Request ignore battery optimizations | Optional reliability step some vendors require |
| Request install packages | Only for the in-app update flow: lets Android show you the standard "install unknown apps" consent for this app after the downloaded APK has been verified. Nothing is ever installed without your explicit approval |

Noor Al-Sabeel requests no contacts, no files, no camera, no microphone, no advertising IDs.

## No ads, no trackers, no accounts

There are no ads, no analytics SDKs, no crash reporters, no accounts and no payments in
Noor Al-Sabeel — the source code contains none.

## Changes

Material changes to this policy will be noted in the repository's CHANGELOG.

## Contact

Open an issue at <https://github.com/AhmedZakeFit/noor-al-sabeel/issues> for any privacy question.

---

# العربية

*آخر تحديث: 2026-10-24*

نور السبيل تطبيق أندرويد مجاني ومفتوح المصدر، وهذه السياسة تصف بالضبط ما يفعله التطبيق
بالبيانات. ولأن التطبيق لا يحتوي أي تحليلات أو تقارير أعطال، فإن هذا المستند مبني على
الشيفرة المصدرية التي يمكنك فحصها بنفسك.

## ما الذي يخزّنه نور السبيل؟

كل ما يخزّنه نور السبيل يبقى **على جهازك** في وحدة التخزين الخاصة بالتطبيق:

- موقعك المختار (الإحداثيات واسم المدينة والمنطقة الزمنية) — تختاره بنفسك عبر GPS أو
  البحث أو الإدخال اليدوي؛
- إعدادات الحساب (الطريقة، الزوايا، فروق التعديل، التذكيرات)؛
- تفضيلات المظهر (السمة واللغة والأرقام وتعديل التاريخ الهجري)؛
- رقم إصدار التحديث الذي اخترت تأجيله، وملفات التحديث المتحقق منها المؤقتة في ذاكرة
  التطبيق المؤقتة حتى تُثبّت أو تُحذف.

هذه البيانات لا تُرسل إلى أي مكان ولا تترك الجهاز أبدًا، وحذف التطبيق يمحوها تمامًا.

## ما الذي يرسله نور السبيل؟

افتراضيًا: لا شيء. يستخدم التطبيق الإنترنت **فقط** عند استخدامك إحدى هذه الميزات:

- **البحث الاختياري عن المدن عبر الإنترنت** — يُرسل النص الذي تكتبه إلى خدمة Nominatim
  العامة (OpenStreetMap) عندما لا يجد الفهرس المدمج نتيجة مطابقة.
- **التحقق من التحديثات** — عند فتح التطبيق (وعند ضغطك «التحقق من وجود تحديثات») يتصل
  التطبيق بواجهة GitHub‏ (api.github.com / github.com) لقراءة أحدث إصدار منشور من هذا
  المستودع ومقارنة رقم الإصدار. وإن اخترت تنزيل تحديث فيأتي ملف APK من صفحة GitHub
  Releases الخاصة بالمشروع نفسه. لا يُرسل شيء يخصك — لا معرّفات ولا بيانات استخدام؛
  إنها مطابقة رقم إصدار فحسب.

هذه الخدمات يشغّلها طرف ثالث (مؤسسة OpenStreetMap وGitHub) وتحكمها سياستهم
الخاصة. لا يرسل نور السبيل معها أي معرّفات ولا معرّفات إعلانية ولا إحصاءات استخدام.

## الأذونات وسببها

| الإذن | الغرض |
|---|---|
| الموقع (تقريبي/دقيق) | يُستخدم فقط عند ضغطك "استخدام الموقع الحالي" لتحديد موقعك للمواقيت والقبلة |
| الإشعارات | عرض إشعارات المواقيت والتذكيرات |
| الإنترنت وحالة الشبكة | البحث الاختياري عن المدن والتحقق من التحديثات |
| استقبال اكتمال الإقلاع | إعادة جدولة إشعارات الصلاة بعد إعادة تشغيل الهاتف |
| التنبيهات الدقيقة | رنين إشعارات الصلاة في الوقت المحدد بدقة |
| الاهتزاز | اهتزاز الإشعار (اختياري) |
| الخدمة الأمامية + تشغيل الوسائط + قفل التنبيه | رفع الأذان الكامل عند دخول الوقت (فقط عند اختيارك هذا الأسلوب)؛ يبقى مسموعًا والشاشة مغلقة |
| طلب تجاوز تحسين البطارية | خطوة موثوقية اختيارية تتطلبها بعض الشركات |
| طلب تثبيت الحزم | خاص بمسار التحديث داخل التطبيق فقط: يتيح لأندرويد عرض موافقة «تثبيت التطبيقات غير المعروفة» القياسية لهذا التطبيق بعد التحقق من ملف APK المنزّل. ولا يُثبَّت أي شيء قط دون موافقتك الصريحة |

لا يطلب نور السبيل أيّ اتصالات أو ملفات أو كاميرا أو ميكروفون أو معرّفات إعلانية.

## بلا إعلانات ولا متتبعات ولا حسابات

لا يوجد في نور السبيل إعلانات ولا أدوات تحليلات ولا مراسلات أعطال ولا حسابات ولا مدفوعات —
الشيفرة المصدرية لا تحتوي أيًّا منها.

## التغييرات

سيُدوَّن أي تغيير جوهري في هذه السياسة في سجل التغييرات الخاص بالمستودع.

## التواصل

افتح مشكلة في <https://github.com/AhmedZakeFit/noor-al-sabeel/issues> لأي سؤال يخص الخصوصية.
