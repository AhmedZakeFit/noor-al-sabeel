name: Feature request / طلب ميزة
description: Suggest an idea for NOOR — اقترح فكرة لنور
labels: [enhancement]
body:
  - type: textarea
    id: problem
    attributes:
      label: What problem does this solve? / أي مشكلة تحلها هذه الميزة؟
      description: Keep NOOR free, offline-first and tracking-free / ابقِ نور مجانيًا يعمل دون إنترنت وبلا تتبع
    validations:
      required: true
  - type: textarea
    id: solution
    attributes:
      label: What do you suggest? / ما الذي تقترحه؟
    validations:
      required: true
