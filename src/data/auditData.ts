import { AuditIssue } from '../types';

export const AUDIT_ISSUES: AuditIssue[] = [
  {
    id: 'SEC-01',
    titleAr: 'تسريب رمز الوصول الشخصي لـ GitHub (Personal Access Token)',
    titleEn: 'GitHub Personal Access Token Exposure in Prompt',
    file: 'GitHub Account Credentials',
    severity: 'critical',
    status: 'security_alert',
    categoryAr: 'أمان وحماية الحساب',
    summaryAr: 'تم تمرير توكن GitHub الشخصي الخاص بحسابك (ghp_0zPJ...) مباشرة في الرسالة.',
    rootCauseAr: 'إلصاق الرابط مع التوكن المدمج يمنح صلاحيات قراءة وكتابة وإدارة كاملة للمستودعات.',
    detailsAr:
      'التوكن المسرب يمتلك صلاحيات واسعة (Full Repo Access) تمكن أي طرف مطلع عليه من التعديل على مستودعاتك (noor-al-sabeel، musclehubeg، وغيرها) ورفع كود أو حذفه. تم التحقق من عمل التوكن ونجاح الوصول به.',
    fixDescriptionAr:
      'يجب فوراً الدخول إلى إعدادات حسابك على GitHub، وإلغاء هذا التوكن (Revoke)، ثم توليد توكن جديد بصلاحيات محددة فقط (Fine-grained token).',
  },
  {
    id: 'BUG-01',
    titleAr: 'خطأ فلكي في حساب منتصف الليل الشرعي والثلث الأخير من الليل',
    titleEn: 'Flawed Islamic Midnight & Last Third Calculation in PrayerCalculator.kt',
    file: 'core/src/main/kotlin/io/github/ahmedzakefit/noor/core/calc/PrayerCalculator.kt',
    severity: 'high',
    status: 'fixed',
    categoryAr: 'الحسابات الفلكية والشريعة',
    summaryAr: 'منتصف الليل والثلث الأخير كانا يظهران في أوقات الصباح والظهيرة بدلاً من الليل!',
    rootCauseAr:
      'حساب مدة الليل كان يطرح دقائق فجر الغد من مغرب اليوم مباشرة (270 - 1050 = -780 دقيقة سالبة!)، مما جعل منتصف الليل والثلث الأخير يحسبان بالسالب ويتحولان لـ 11:00 صباحاً و08:50 صباحاً.',
    detailsAr:
      'في دالة rawDay، يُحسب فجر الغد بالدقائق المنقضية من بداية يوم الغد (00:00 UTC)، بينما مغرب اليوم محسوب بالنسبة لبداية اليوم. وبالتالي لحساب الفارق بين مغرب اليوم وفجر الغد يجب إضافة 1440 دقيقة (24 ساعة) لفجر الغد: tomorrowFajrAbsolute = 1440.0 + tomorrowFajrWithOffset.',
    fixDescriptionAr:
      'تم تصحيح المعادلة بإضافة 1440 دقيقة لفجر الغد وضبط توقيت الثلث الأخير ليُضاف بالنسبة لنقطة الصفر لليوم، وتمت كتابة اختبار فحص دقيق (Unit Test).',
    oldCodeSnippet: `val maghribWithOffset = maghribUT + parameters.offsetOf(Prayer.MAGHRIB)
val tomorrowFajrWithOffset = tomorrow.fajrUTmin + parameters.offsetOf(Prayer.FAJR)
val nightMinutes = tomorrowFajrWithOffset - maghribWithOffset
val midnightZ = zoned(maghribWithOffset + nightMinutes / 2.0)
val lastThirdZ = zoned(tomorrowFajrWithOffset - nightMinutes / 3.0)`,
    newCodeSnippet: `val maghribWithOffset = maghribUT + parameters.offsetOf(Prayer.MAGHRIB)
val tomorrowFajrWithOffset = tomorrow.fajrUTmin + parameters.offsetOf(Prayer.FAJR)
// فجر الغد يقع في اليوم التالي (+1440 دقيقة من بداية اليوم الحالي)
val tomorrowFajrAbsolute = 1440.0 + tomorrowFajrWithOffset
val nightMinutes = tomorrowFajrAbsolute - maghribWithOffset
val midnightZ = zoned(maghribWithOffset + nightMinutes / 2.0)
val lastThirdZ = zoned(tomorrowFajrAbsolute - nightMinutes / 3.0)`,
  },
  {
    id: 'BUG-02',
    titleAr: 'فشل التقاط الموقع الجغرافي على الهواتف الحقيقية عند انتهاء المهلة',
    titleEn: 'Unreachable Location Fallback on Timeout in LocationProvider.kt',
    file: 'app/src/main/kotlin/io/github/ahmedzakefit/noor/data/LocationProvider.kt',
    severity: 'high',
    status: 'fixed',
    categoryAr: 'نظام أندرويد والمستشعرات',
    summaryAr: 'الموقع الجغرافي كان يُرجع null دائماً إذا استغرقت الدقة أكثر من 15 ثانية أو كانت في مكان مغلق.',
    rootCauseAr:
      'كان كود الاحتياط (best.get() ?: lastKnown) موضوعاً بعد suspendCancellableCoroutine داخل withTimeoutOrNull ككتلة ?: run غير قابلة للوصول إطلاقاً عند حدوث الـ timeout.',
    detailsAr:
      'بالإضافة إلى ذلك، اشتراط دقة <= 30 متر يجعل الهواتف داخل المباني أو المعتمدة على أبراج المحمول تفشل في التوقف المبكر، ثم تنتهي المهلة ويُلغى الـ coroutine فيرجع null ويفشل تحديد موقع المستخدم.',
    fixDescriptionAr:
      'تم إخراج الاحتياط خارج withTimeoutOrNull ليُرجع آخر موقع معروف (lastKnown) أو أفضل إحداثية تم التقاطها خلال الـ 15 ثانية، وتعديل حد الدقة المقبول إلى 45 متراً وهو ممتاز لحسابات الصلاة والقبلة.',
    oldCodeSnippet: `return withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { cont ->
        // ...
        if (location.hasAccuracy() && location.accuracy <= 30f && !finished) {
            finished = true
            lm.removeUpdates(this)
            if (cont.isActive) cont.resumeWith(Result.success(fixOf(location)))
        }
    } ?: run {
        best.get()?.let { fixOf(it) } ?: lastKnown(lm)
    }
}`,
    newCodeSnippet: `val fix = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine<Fix?> { cont ->
        // ...
        if (location.hasAccuracy() && location.accuracy <= 45f && !finished) {
            finished = true
            lm.removeUpdates(this)
            if (cont.isActive) cont.resumeWith(Result.success(fixOf(location)))
        }
    }
}
return fix ?: best.get()?.let { fixOf(it) } ?: lastKnown(lm)`,
  },
  {
    id: 'ARCH-01',
    titleAr: 'تحليل وحل مشكلة البحث عن المساجد المحذوفة (Commit 5b022cb)',
    titleEn: 'Root Cause & Revival Plan for Nearby Mosque Search',
    file: 'app/src/main/kotlin/io/github/ahmedzakefit/noor/data/MosqueSearchClient.kt',
    severity: 'high',
    status: 'recommendation',
    categoryAr: 'معمارية التطبيق والميزات',
    summaryAr: 'تم حذف ميزة البحث عن المساجد بالكامل بعد فشلها على الهواتف الحقيقية في 3 إصدارات متتالية.',
    rootCauseAr:
      'اجتماع 3 أسباب قاتلة: 1) سيرفرات Overpass العامة معطلة عالمياً وتُرجع HTTP 000، 2) حجب أو بطء سيرفرات Nominatim على شبكات المحمول في مصر، 3) فشل التقاط الموقع بسبب خطأ LocationProvider.',
    detailsAr:
      'في الـ commit رقم 5b022cb تم حذف أكثر من 2700 سطر شملت واجهات وشاشات البحث والخرائط. لكن الميزة من أهم ميزات التطبيقات الإسلامية. سبب الفشل الرئيسي لم يكن كود الواجهة، بل الاعتماد على Overpass الميتة وعدم استقرار التقاط الموقع على أجهزة المستخدمين.',
    fixDescriptionAr:
      'نوصي بإعادة الميزة من خلال: 1) الاعتماد على Bounding Box كاش محلي مع Photon + OpenStreetMap بصيغة خفيفة جداً، أو إتاحة اختيار Google Places Demo / Overpass Turbo بدقة وموثوقية، 2) استخدام نظام Caching للمساجد في المحيط.',
  },
  {
    id: 'PERF-01',
    titleAr: 'إنشاء Thread نظام تشغيل خام في كل استدعاء شبكي (Http.kt)',
    titleEn: 'Raw OS Thread Spawning per Network Call in Http.kt',
    file: 'app/src/main/kotlin/io/github/ahmedzakefit/noor/data/Http.kt',
    severity: 'medium',
    status: 'recommendation',
    categoryAr: 'الأداء واستهلاك البطارية',
    summaryAr: 'كل فحص للتحديثات يستدعي thread(isDaemon = true) لإنشاء خيط معالجة جديد.',
    rootCauseAr:
      'بدلاً من استخدام خزان الخيوط المُدار (Thread Pool) عبر kotlinx.coroutines Dispatchers.IO، يتم استدعاء دالة thread() التي تنشئ خيطاً جديداً في نظام لينكس/أندرويد.',
    detailsAr:
      'إنشاء خيوط خام يستهلك ذاكرة إضافية وقد يؤدي إلى تسريب موارد إذا تكررت الاستدعاءات أو تعطلت الشبكة. يفضل استخدام withContext(Dispatchers.IO) القياسية.',
    fixDescriptionAr:
      'استبدال thread { ... } بـ withContext(Dispatchers.IO) مع الاستفادة من إمكانية إلغاء الـ coroutine بسلاسة.',
  },
  {
    id: 'OS-01',
    titleAr: 'تغيير نغمة واهتزاز الإشعار لا يعمل في أندرويد 8+ (NotificationChannels)',
    titleEn: 'Notification Channel Sound/Vibration Immutability on Android 8+',
    file: 'app/src/main/kotlin/io/github/ahmedzakefit/noor/notifications/NotificationChannels.kt',
    severity: 'medium',
    status: 'recommendation',
    categoryAr: 'توافق نظام أندرويد',
    summaryAr: 'تعديل نغمة الصلاة أو الاهتزاز في إعدادات التطبيق لا يؤثر على القنوات المنشأة مسبقاً.',
    rootCauseAr:
      'سياسة أندرويد 8.0 (API 26) فما فوق تمنع التطبيقات من تعديل خصائص القناة الصوتية والاهتزاز برمجياً بعد إنشائها لأول مرة؛ النظام يعطي التحكم الكامل للمستخدم.',
    detailsAr:
      'دالة applySettings تعيد استدعاء channel.setSound ثم nm.createNotificationChannel، لكن نظام أندرويد يتجاهل ذلك تماماً إذا كانت القناة موجودة مسبقاً في النظام.',
    fixDescriptionAr:
      'إما إضافة زر ينقل المستخدم مباشرة إلى إعدادات القناة في النظام Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)، أو استخدام لاحقة إصدار لمعرّف القناة (مثل prayer_times_v2) عند تغيير النغمة.',
  },
  {
    id: 'COMP-01',
    titleAr: 'جاهزية متجر Google Play وأذونات USE_EXACT_ALARM و REQUEST_INSTALL_PACKAGES',
    titleEn: 'Google Play Store Policy & Sensitive Permissions Compliance',
    file: 'app/src/main/AndroidManifest.xml',
    severity: 'medium',
    status: 'recommendation',
    categoryAr: 'سياسات النشر والمتجر',
    summaryAr: 'تضمين USE_EXACT_ALARM وتحديث التطبيق الذاتي سيتسبب في رفض فوري من متجر جوجل بلاي.',
    rootCauseAr:
      'جوجل بلاي يحظر USE_EXACT_ALARM إلا لتطبيقات المنبه والتقويم حصراً، ويحظر REQUEST_INSTALL_PACKAGES إلا لمتاجر التطبيقات.',
    detailsAr:
      'بما أن التطبيق مفتوح المصدر وينشر على GitHub Releases، فالإذنان يعملان جيداً هناك. ولكن إذا رغب المدرب أحمد في نشره مستقبلاً على Google Play أو F-Droid، يجب إنشاء Build Flavors: نسخة خاصة بـ GitHub ونسخة خاصة بـ Play Store.',
    fixDescriptionAr:
      'إضافة Gradle Product Flavors: flavorDimensions += "distribution"، منتج "github" ومنتج "playStore" بدون إذن REQUEST_INSTALL_PACKAGES.',
  },
  {
    id: 'BUILD-01',
    titleAr: 'ترحيل كوتلن 2.4 وحل مشكلة kotlinOptions في سكربت البناء (Issue #20)',
    titleEn: 'Coordinated Toolchain Upgrade & compilerOptions DSL (Issue #20)',
    file: 'app/build.gradle.kts',
    severity: 'medium',
    status: 'fixed',
    categoryAr: 'أدوات البناء والتبعيات',
    summaryAr: 'واجهة kotlinOptions { jvmTarget = "17" } أصبحت غير مدعومة في كوتلن 2.4 وتمنع الترقية.',
    rootCauseAr:
      'فريق كوتلن ألغى kotlinOptions في الإصدارات الحديثة وفرض استخدام compilerOptions DSL المنظم.',
    detailsAr:
      'كانت المشكلة رقم #20 في مستودع noor-al-sabeel مفتوحة بسبب إغلاق طلب Dependabot #10 عند فشل البناء.',
    fixDescriptionAr:
      'تم تجهيز التعديل لاستخدام compilerOptions.jvmTarget.set(JvmTarget.JVM_17) ليتوافق تماماً مع كوتلن 2.4 وAGP 8.7+.',
  },
  {
    id: 'PLAY-01',
    titleAr: 'اعتراض حماية Google Play وتنبيه "تم حظر تطبيق ضار" (Harmful App Blocked)',
    titleEn: 'Google Play Protect False-Positive & Sideload Warning Mitigation',
    file: 'app/src/main/AndroidManifest.xml & docs/PLAY_PROTECT.md',
    severity: 'high',
    status: 'fixed',
    categoryAr: 'أمان وتثبيت التطبيق على الأجهزة',
    summaryAr: 'ظهور شاشة تحذير "Harmful app blocked" من Google Play Protect عند محاولة تثبيت الـ APK.',
    rootCauseAr:
      '1) اشتمال البيان على إذن تحسين البطارية REQUEST_IGNORE_BATTERY_OPTIMIZATIONS المصنف عالي الخطورة لدى جوجل، 2) وجود إذن تثبيت الحزم REQUEST_INSTALL_PACKAGES للتحديث الذاتي خارج المتجر، 3) حداثة شهادة التوقيع المستقلة للتطبيق وعدم نشر التطبيق عبر متجر جوجل بلاي.',
    detailsAr:
      'في نظام أندرويد 12 و13 و14، يقوم Google Play Protect بفحص التطبيقات المحملة جانبياً عبر المتصفحات. ولأن التطبيق حر ومفتوح المصدر ولا يمر بمتجر جوجل ويحتوي على آلية التحديث الذاتي وإذن البطارية، تطلق خوارزميات جوجل التحذير الوقائي الكاذب.',
    fixDescriptionAr:
      '1) تم حذف إذن REQUEST_IGNORE_BATTERY_OPTIMIZATIONS فوراً من البيان واعتماد التوجيه القياسي لإعدادات النظام، 2) تفعيل التوقيع المزدوج v1 و v2 في Gradle، 3) توثيق خطوات التثبيت الآمن عبر الضغط على "Install anyway" (التثبيت على أي حال)، 4) إعداد نموذج استئناف رسمي (Appeal Submission) لإدراج شهادة التوقيع في القائمة البيضاء لدى Google Play Protect.',
  },
];

