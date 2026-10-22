name: Bug report / تقرير خطأ
description: Report something that does not work — أبلغ عن مشكلة
labels: [bug]
body:
  - type: textarea
    id: what-happened
    attributes:
      label: What happened? / ما الذي حدث؟
      description: Also tell us what you expected. / وأخبرنا ما الذي توقعته أيضًا.
    validations:
      required: true
  - type: input
    id: version
    attributes:
      label: App version / إصدار التطبيق
      placeholder: "1.0.0"
    validations:
      required: true
  - type: input
    id: device
    attributes:
      label: Device & Android version / الجهاز وإصدار أندرويد
      placeholder: "e.g. Samsung A52, Android 13"
    validations:
      required: true
  - type: textarea
    id: prayer-details
    attributes:
      label: If a prayer time looks wrong / إذا بدا وقت صلاة خاطئًا
      description: City, method, date, time Noor Al-Sabeel shows, time your mosque shows / المدينة والطريقة والتاريخ ووقت نور السبيل ووقت مسجدك
  - type: textarea
    id: logs
    attributes:
      label: Anything else? / أي شيء آخر؟
