# Pull request / طلب الدمج

**English** | [العربية بالأسفل](#العربية)

## What & why / ماذا ولماذا

<!-- What does this change do, and why is it needed? Reference the issue number if one exists (e.g. "Fixes #123"). -->
<!-- ما الذي يغيّره هذا الطلب ولماذا هو مطلوب؟ أشر إلى رقم المشكلة إن وُجدت (مثل «Fixes #123»). -->

## Checklist / قائمة التحقق

<!-- Check what applies; anything unchecked should be explained below. -->
<!-- ضع علامة على ما ينطبق، واشرح أدناه أي بند غير محقق. -->

- [ ] Tests added or updated for calculation-engine changes — الاختبارات مضافة/محدّثة لتغييرات محرك الحسابات
- [ ] Every user-facing string exists in **both** `values/` (English) and `values-ar/` (Arabic) — كل نص للمستخدم موجود بالإنجليزية والعربية معًا
- [ ] Layout verified in **both directions** (RTL and LTR) — تم التحقق من التخطيط في الاتجاهين RTL وLTR
- [ ] No new proprietary dependency or network behaviour (offline-first respected) — لا تبعية مغلقة ولا سلوك شبكي جديد مع احترام مبدأ العمل دون إنترنت
- [ ] `./gradlew :core:test :app:testDebugUnitTest` passes locally — الاختبارات تنجح محليًا
- [ ] CI is green on this branch — CI أخضر على هذا الفرع

<!--
Small docs-only fixes don't need every box checked — say so below.
الإصلاحات التوثيقية الصغيرة لا تحتاج تحقيق كل البنود — اذكر ذلك أدناه.
-->
