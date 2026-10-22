# Contributing to Noor Al-Sabeel — المساهمة في نور السبيل

**English** | [العربية بالأسفل](#العربية)

Thank you for considering a contribution! Noor Al-Sabeel is a small, focused project, and these
guidelines keep it healthy.

## Ground rules

1. **Keep it free.** No ads, no trackers, no accounts, no paid services, no proprietary
   dependencies (no Google Play services). Ask first before adding any dependency — it must
   be Apache-2.0 (or similarly permissive) compatible.
2. **Keep it offline-first.** Computation stays on the device. Network access is only for
   the map, mosque search and optional online city search.
3. **Bilingual by default.** Every user-facing string must exist in `values/` (English) and
   `values-ar/` (Arabic), including notifications. RTL/LTR layout must be verified for both.
4. **Tests before merges.** Any change to the calculation engine must come with or update
   tests. CI must be green.

## Workflow

1. Fork, create a branch (`feature/...` or `fix/...`).
2. Make your change; add tests where applicable.
3. Run locally:
   ```bash
   ./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
   ```
4. Open a pull request describing *what* and *why*. Reference the issue if one exists.

## Areas that especially need help

- Translations beyond Arabic/English.
- Verifying prayer-time conventions for specific countries (with sources).
- Bug reports with device model and Android version — sensors and vendor battery policies
  vary a lot.
- F-Droid metadata and reproducible build polish.

## Reporting bugs

Use the issue templates. For prayer-time discrepancies, please include: city, method,
the time Noor Al-Sabeel shows, the time your mosque/local source shows, and the date.

## Code of conduct

Be respectful. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). Security issues go to
[SECURITY.md](SECURITY.md), not the public tracker.

---

# العربية

شكرًا لاهتمامك بالمساهمة! نور السبيل مشروع صغير ومركّز، وهذه الإرشادات تحافظ على صحته.

## القواعد الأساسية

1. **ابقِه مجانيًا:** لا إعلانات ولا متتبعات ولا حسابات ولا خدمات مدفوعة ولا تبعيات
   مغلقة (ولا خدمات Google Play). استأذن قبل إضافة أي تبعية — يجب أن تكون متوافقة مع
   Apache-2.0 أو ما يماثلها.
2. **ابقِه يعمل دون إنترنت أولًا:** الحسابات على الجهاز، والشبكة فقط للخريطة والمساجد
   والبحث الاختياري.
3. **ثنائي اللغة افتراضيًا:** كل نص يظهر للمستخدم يجب أن يوجد في `values/` (إنجليزي)
   و`values-ar/` (عربي) بما يشمل الإشعارات، مع التحقق من اتجاهي RTL/LTR.
4. **الاختبارات قبل الدمج:** أي تغيير في محرك الحسابات يجب أن يأتي مع اختبارات، ويجب
   أن ينجح CI.

## خطوات العمل

1. اعمل Fork ثم أنشئ فرعًا (`feature/...` أو `fix/...`).
2. نفّذ التغيير وأضف الاختبارات المناسبة.
3. شغّل محليًا:
   ```bash
   ./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
   ```
4. افتح طلب دمج يصف *ماذا* و*لماذا*، مع الإشارة إلى المشكلة إن وُجدت.

## مجالات تحتاج مساعدة خاصة

- الترجمات إلى لغات غير العربية والإنجليزية.
- التحقق من عُرف حساب المواقيت لبلدان محددة (مع المصادر).
- تقارير الأخطاء مرفقة بطراز الجهاز وإصدار أندرويد — المستشعرات وسياسات البطارية
  تختلف كثيرًا بين الشركات.
- بيانات F-Droid وتحسين البناء القابل للتكرار.

## الإبلاغ عن الأخطاء

استخدم قوالب المشكلات. عند وجود فرق في المواقيت أرفق: المدينة، الطريقة، الوقت الذي
يعرضه نور السبيل، الوقت من مسجدك أو مصدرك المحلي، والتاريخ.

## قواعد السلوك

كن محترمًا، وانظر [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). مشكلات الأمان تُرسل وفق
[SECURITY.md](SECURITY.md) لا عبر المتتبع العام.
