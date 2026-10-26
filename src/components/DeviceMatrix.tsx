import React, { useState } from 'react';
import { Smartphone, BatteryCharging, Bell, RefreshCw, CheckCircle, AlertTriangle, ShieldCheck } from 'lucide-react';

interface VendorGuide {
  vendor: string;
  nameAr: string;
  romName: string;
  iconBg: string;
  riskLevel: 'high' | 'medium' | 'low';
  behaviorAr: string;
  requiredStepsAr: string[];
}

const VENDOR_GUIDES: VendorGuide[] = [
  {
    vendor: 'Xiaomi / Poco / Redmi',
    nameAr: 'شاومي / بوكو / ريدمي',
    romName: 'MIUI / HyperOS',
    iconBg: 'bg-orange-500/20 text-orange-400 border-orange-500/30',
    riskLevel: 'high',
    behaviorAr: 'نظام إدارة البطارية يقوم بإنهاء الـ Alarms وإلغاء تسجيل Alarms في وضع السكون (Doze) العميق.',
    requiredStepsAr: [
      'تفعيل خيار "التشغيل التلقائي" (Autostart) لتطبيق نور السبيل من إدارة التطبيقات.',
      'ضبط توفير البطارية على "بلا قيود" (No restrictions).',
      'تثبيت التطبيق في شاشة التطبيقات الحديثة (Lock in Recents).',
      'منح إذن التنبيهات الدقيقة (Alarms & Reminders).',
    ],
  },
  {
    vendor: 'Samsung',
    nameAr: 'سامسونج',
    romName: 'One UI',
    iconBg: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
    riskLevel: 'medium',
    behaviorAr: 'سامسونج تضع التطبيقات غير المفتوحة لفترة في "وضع السكون العميق" (Deep sleeping apps).',
    requiredStepsAr: [
      'الدخول إلى الإعدادات > العناية بالجهاز > البطارية > حدود استخدام الخلفية.',
      'إضافة "نور السبيل" إلى قائمة "التطبيقات التي لن تنتقل لوضع السكون أبداً" (Never sleeping apps).',
      'التأكد من تفعيل "السماح بالنشاط في الخلفية" في تفاصيل التطبيق.',
    ],
  },
  {
    vendor: 'Huawei / Honor',
    nameAr: 'هواوي / هونر',
    romName: 'EMUI / MagicOS',
    iconBg: 'bg-red-500/20 text-red-400 border-red-500/30',
    riskLevel: 'high',
    behaviorAr: 'يقوم بقتل خدمات تشغيل الأذان الصوتي في الخلفية بعد دقائق من إطفاء الشاشة.',
    requiredStepsAr: [
      'الإعدادات > البطارية > تشغيل التطبيقات (App launch).',
      'البحث عن "نور السبيل" وتحويله من "إدارة تلقائية" إلى "إدارة يدوية".',
      'تفعيل: التشغيل التلقائي، التشغيل الثانوي، والتشغيل في الخلفية.',
    ],
  },
  {
    vendor: 'Google Pixel / Motorola / Nokia',
    nameAr: 'جوجل بيكسل / موتورولا / الخام',
    romName: 'Stock Android',
    iconBg: 'bg-emerald-500/20 text-emerald-400 border-emerald-500/30',
    riskLevel: 'low',
    behaviorAr: 'يلتزم بنظام أندرويد القياسي. Alarms الدقيقة تعمل بصورة ممتازة ما لم يتم تفعيل وضع توفير الطاقة القاسي.',
    requiredStepsAr: [
      'إعطاء إذن التنبيهات (Notification Permission) في أندرويد 13+.',
      'التحقق من إذن Schedule Exact Alarms.',
    ],
  },
  {
    vendor: 'Oppo / Realme / OnePlus',
    nameAr: 'أوبو / ريلمي / ون بلس',
    romName: 'ColorOS / OxygenOS',
    iconBg: 'bg-teal-500/20 text-teal-400 border-teal-500/30',
    riskLevel: 'high',
    behaviorAr: 'إيقاف تشغيل المقاطع الصوتية الطويلة للأذان عند إقفال الشاشة إذا اعتبرت خدمة وسائط غير معتمدة.',
    requiredStepsAr: [
      'إدارة التطبيقات > نور السبيل > استخدام البطارية > السماح بالنشاط في الخلفية وتنشيط التطبيق التلقائي.',
      'قفل التطبيق في قائمة المهام المتعددة (Task Manager Lock).',
    ],
  },
];

export const DeviceMatrix: React.FC = () => {
  const [selectedVendor, setSelectedVendor] = useState<string>(VENDOR_GUIDES[0].vendor);

  const activeGuide = VENDOR_GUIDES.find((v) => v.vendor === selectedVendor) || VENDOR_GUIDES[0];

  return (
    <div className="space-y-6">
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow-xl">
        <div className="flex items-center gap-3 pb-3 border-b border-slate-800">
          <div className="p-2.5 bg-emerald-500/20 rounded-xl text-emerald-400">
            <Smartphone className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-slate-100">
              دليل توافق الأجهزة وسياسات الشركات المصنعة (OEM Battery Matrix)
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              وفقاً لملاحظات الاختبار على الهواتف الحقيقية الواردة في Issue #15 ومستند docs/TESTING.md
            </p>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2.5 mt-4">
          {VENDOR_GUIDES.map((vg) => (
            <button
              key={vg.vendor}
              onClick={() => setSelectedVendor(vg.vendor)}
              className={`p-3 rounded-xl border text-right transition-all flex flex-col justify-between ${
                selectedVendor === vg.vendor
                  ? 'bg-emerald-950/40 border-emerald-500 text-white shadow-md'
                  : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700'
              }`}
            >
              <div className="text-xs font-bold">{vg.nameAr}</div>
              <div className="text-[10px] text-slate-400 mt-1">{vg.romName}</div>
              <div className="mt-2 text-[10px]">
                {vg.riskLevel === 'high' ? (
                  <span className="text-rose-400 font-semibold">تعديلات صارمة</span>
                ) : vg.riskLevel === 'medium' ? (
                  <span className="text-amber-400 font-semibold">تعديلات متوسطة</span>
                ) : (
                  <span className="text-emerald-400 font-semibold">نظام قياسي</span>
                )}
              </div>
            </button>
          ))}
        </div>
      </div>

      {/* Active Vendor Detail Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-lg font-bold text-slate-100">{activeGuide.nameAr}</span>
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
                {activeGuide.romName}
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">{activeGuide.behaviorAr}</p>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1.5">
              <BatteryCharging className="w-4 h-4" />
              توفير الطاقة المفرط
            </span>
          </div>
        </div>

        <div>
          <h4 className="text-xs font-bold uppercase text-slate-300 mb-3 flex items-center gap-2">
            <CheckCircle className="w-4 h-4 text-emerald-400" />
            الخطوات الإلزامية لضمان عمل الأذان والتنبيه في الدقيقة المحددة والشاشة مغلقة:
          </h4>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {activeGuide.requiredStepsAr.map((step, idx) => (
              <div
                key={idx}
                className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 flex items-start gap-3"
              >
                <span className="w-6 h-6 rounded-lg bg-emerald-500/20 text-emerald-300 text-xs font-bold flex items-center justify-center shrink-0">
                  {idx + 1}
                </span>
                <p className="text-xs text-slate-200 leading-relaxed mt-0.5">{step}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
