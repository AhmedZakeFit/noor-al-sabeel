# Security Policy — سياسة الأمان

**English** | [العربية بالأسفل](#العربية)

## Supported versions

| Version | Supported |
|---|---|
| 1.0.x (latest release) | Yes |
| anything older | No — update to the latest release |

## Reporting a vulnerability

Please report vulnerabilities **privately** using GitHub's
["Report a vulnerability"](https://github.com/AhmedZakeFit/noor/security/advisories/new)
feature on the repository. If that is not possible, open a security advisory draft or
contact the repository owner through GitHub.

Please include:

- a description of the issue and its impact;
- steps or a proof of concept;
- the app version and Android version.

You should receive a response within a few days. Please avoid public disclosure until a fix
is released so users are not put at risk. Small bounties are not offered — this is a
volunteer open-source project — but you will be credited in the release notes and
CHANGELOG if you wish.

## Scope notes

- NOOR stores everything locally and sends no identifiers; the realistic attack surface is
  small: notification/alarm handling, the two read-only public APIs (Overpass, Nominatim)
  and map tile loading.
- Miscomputed prayer times are an accuracy issue (see docs/PRAYER_TIMES.md), not a security
  vulnerability, unless you can show code-level abuse.

---

# العربية

## الإصدارات المدعومة

| الإصدار | مدعوم؟ |
|---|---|
| 1.0.x (أحدث إصدار) | نعم |
| أي إصدار أقدم | لا — حدّث إلى الأحدث |

## الإبلاغ عن ثغرة

أبلغ عن الثغرات **بسرية** عبر خاصية
["Report a vulnerability"](https://github.com/AhmedZakeFit/noor/security/advisories/new)
في المستودع. أرفق وصفًا للأثر وخطوات الاستغلال وإصدار التطبيق وإصدار أندرويد.

ينبغي أن تتلقى ردًا خلال أيام قليلة. تجنّب الإفصاح العام حتى صدور الإصلاح حفاظًا على
المستخدمين. لا تُقدَّم مكافآت مالية — المشروع تطوعي — لكن سيُنسب الفضل لك في ملاحظات
الإصدار إن رغبت.

## ملاحظات النطاق

- يخزّن نور كل شيء محليًا ولا يرسل أي معرّفات، لذا سطح الهجوم صغير: معالجة الإشعارات
  والتنبيهات، والواجهتان العامتان للقراءة فقط (Overpass وNominatim)، وتحميل مربعات
  الخريطة.
- أخطاء حساب المواقيت مسألة دقة (انظر docs/PRAYER_TIMES.md) لا ثغرة أمنية، إلا إذا
  أثبتَ استغلالًا على مستوى الشيفرة.
