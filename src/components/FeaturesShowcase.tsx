import React from 'react';
import {
  MapPin,
  Clock,
  Compass,
  BellRing,
  Volume2,
  ShieldCheck,
  Zap,
  Smartphone,
} from 'lucide-react';

export const FeaturesShowcase: React.FC = () => {
  const features = [
    {
      icon: <MapPin className="w-6 h-6 text-emerald-400" />,
      titleAr: 'البحث عن أقرب مسجد (Nearby Mosques)',
      descAr: 'استعراض المساجد القريبة على الخريطة (باستخدام خريطة تفاعلية ومصادر متعددة مثل Nominatim و Overpass) مع حساب المسافة والاتجاه الدقيق.',
      badge: 'مفعل وقوي',
    },
    {
      icon: <Clock className="w-6 h-6 text-emerald-400" />,
      titleAr: 'مواقيت الصلاة الفلكية (100% بدون إنترنت)',
      descAr: 'حساب دقيق لمواقيت الصلاة لجميع مدن العالم باستخدام الطرق الفلكية الرسمية (رابطة العالم الإسلامي، أم القرى، المصري، الهيئة العامة، إلخ) مع ضبط التاريخ الهجري.',
      badge: 'دقيق فلكياً',
    },
    {
      icon: <Compass className="w-6 h-6 text-emerald-400" />,
      titleAr: 'بوصلة القبلة التفاعلية (Qibla Compass)',
      descAr: 'تحديد اتجاه الكعبة المشرفة بدقة عالية باستخدام مستشعرات البوصلة والموقع الجغرافي مع عرض المسافة المتبقية لمكة المكرمة.',
      badge: 'حساسات حية',
    },
    {
      icon: <Volume2 className="w-6 h-6 text-emerald-400" />,
      titleAr: 'أصوات أذان متعددة ومنقاة',
      descAr: 'اختيار صوت الأذان المفضل (الحرم المكي، المسجد النبوي، الجامع الأزهر بـ 4 تكبيرات، والمسجد الأقصى) مع تمييز كامل لأذان الفجر.',
      badge: 'أصوات عالية النقاء',
    },
    {
      icon: <BellRing className="w-6 h-6 text-emerald-400" />,
      titleAr: 'تنبيهات وأذان منضبط بالثانية',
      descAr: 'استخدام AlarmManager.setAlarmClock لضمان إطلاق الأذان في موعده بالضبط دون تأخير، حتى مع تفعيل وضع توفير الطاقة وDoze Mode.',
      badge: 'منبه فوري',
    },
    {
      icon: <ShieldCheck className="w-6 h-6 text-emerald-400" />,
      titleAr: 'خصوصية تامة بدون إعلانات أو تتبع',
      descAr: 'التطبيق مجاني تماماً ومفتوح المصدر ولا يجمع أي بيانات شخصية أو يرسل إحصائيات تتبع إلى أي خادم خارجي.',
      badge: 'خصوصية 100%',
    },
    {
      icon: <Zap className="w-6 h-6 text-emerald-400" />,
      titleAr: 'حجم تطبيق محسن وخفيف',
      descAr: 'تم ضغط الملفات الصوتية بتقنية Mono 64kbps لتقليص حجم التطبيق بنسبة تجاوزت 62% ليعمل بسلاسة على كافة أجهزة أندرويد.',
      badge: 'خفيف وسريع',
    },
    {
      icon: <Smartphone className="w-6 h-6 text-emerald-400" />,
      titleAr: 'دعم كامل للغة العربية والإنجليزية',
      descAr: 'واجهة مستخدم عصرية بتصميم Material Design 3 مع دعم الأرقام العربية والهندية والتثبيت المباشر من التحديثات الآمنة.',
      badge: 'ثنائي اللغة',
    },
  ];

  return (
    <div className="space-y-6">
      <div className="bg-gradient-to-br from-emerald-950/40 via-slate-900/80 to-slate-950 border border-emerald-500/20 rounded-3xl p-6 sm:p-8">
        <div className="max-w-3xl">
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
            ميزات تطبيق نور السبيل (v1.0.14)
          </span>
          <h2 className="text-2xl sm:text-3xl font-black text-white font-amiri mt-3 mb-2">
            كل ما يحتاجه المسلم في تطبيق واحد، بلا إعلانات ولا تتبع
          </h2>
          <p className="text-slate-300 text-sm leading-relaxed">
            يجمع تطبيق نور السبيل بين دقة الحسابات الفلكية، واختيار أصوات الأذان العذبة، والبحث عن المساجد المجاورة، وتنبيهات المنبه الدقيقة، مع الحفاظ التام على خصوصية المستخدم والعمل بدون إنترنت.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {features.map((feat, idx) => (
          <div
            key={idx}
            className="bg-slate-900/80 border border-slate-800 hover:border-emerald-500/40 rounded-2xl p-5 flex flex-col justify-between transition-all group"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl group-hover:scale-110 transition-transform">
                  {feat.icon}
                </div>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-800 text-emerald-400 border border-slate-700">
                  {feat.badge}
                </span>
              </div>
              <h3 className="text-base font-bold text-slate-100 mb-2">{feat.titleAr}</h3>
              <p className="text-xs text-slate-400 leading-relaxed">{feat.descAr}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
