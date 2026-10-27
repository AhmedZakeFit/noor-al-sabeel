import React, { useState } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  FileText,
  Info,
} from 'lucide-react';

export const PlayProtectNotice: React.FC = () => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="bg-gradient-to-r from-amber-950/70 via-slate-900 to-amber-950/70 border-2 border-amber-500/40 rounded-2xl p-5 mb-6 shadow-xl backdrop-blur-md relative overflow-hidden">
      <div className="absolute -right-10 -bottom-10 w-44 h-44 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-start gap-3.5">
          <div className="p-3 bg-amber-500/20 border border-amber-500/30 rounded-xl text-amber-400 shrink-0 mt-0.5">
            <ShieldAlert className="w-7 h-7" />
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-amber-500 text-slate-950">
                إرشاد حماية Google Play
              </span>
              <h3 className="text-lg font-bold text-amber-100">
                حل تنبيه «تم حظر تطبيق ضار» (Harmful app blocked)
              </h3>
            </div>
            <p className="text-sm text-amber-200/90 mt-1.5 leading-relaxed">
              إذا ظهرت لك نافذة Google Play Protect التحذيرية عند تثبيت الـ APK، فهذا <strong>إنذار وقائي كاذب</strong> يظهر تلقائياً للتطبيقات الحرة مفتوحة المصدر المحملة خارج متجر جوجل.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex items-center gap-1.5 px-4 py-2 bg-amber-600/30 hover:bg-amber-600/40 text-amber-200 border border-amber-500/30 rounded-xl font-bold text-xs transition-all active:scale-95"
          >
            <span>{isExpanded ? 'إخفاء التفاصيل' : 'كيفية التثبيت والحل'}</span>
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Quick steps banner */}
      <div className="mt-4 pt-3.5 border-t border-amber-500/20 grid grid-cols-1 md:grid-cols-3 gap-3 text-xs text-amber-200/80">
        <div className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>1. في نافذة Play Protect، اضغط على <strong>«Install anyway» (التثبيت على أي حال)</strong></span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>2. تم تنظيف البيان (Manifest) في <strong>v1.0.16</strong> وحذف إذن البطارية المثير للشك</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>3. تم تجهيز استئناف رسمي لجوجل لإدراج شهادة التوقيع في القائمة البيضاء</span>
        </div>
      </div>

      {/* Expanded deep dive */}
      {isExpanded && (
        <div className="mt-5 pt-4 border-t border-amber-500/20 space-y-4 text-sm text-slate-300">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-2.5">
              <h4 className="font-bold text-white flex items-center gap-2 text-xs">
                <Info className="w-4 h-4 text-amber-400" />
                لماذا يعترض Google Play Protect التطبيق؟
              </h4>
              <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside leading-relaxed">
                <li>
                  <strong className="text-slate-300">تثبيت خارجي (Sideload):</strong> أندرويد 12+ يفحص كل APK محمل من المتصفح بنظام الفحص السحابي الوقائي.
                </li>
                <li>
                  <strong className="text-slate-300">إذن التحديث الداخلي:</strong> إذن <code className="text-amber-300">REQUEST_INSTALL_PACKAGES</code> الذي يتيح التحديث الذاتي يُصنف وقائياً كبرنامج تثبيت.
                </li>
                <li>
                  <strong className="text-slate-300">شهادة توقيع مستقلة:</strong> لم تُفهرس بعد في خوادم Google Play كجهة مطور معروفة تجارياً.
                </li>
              </ul>
            </div>

            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-2.5">
              <h4 className="font-bold text-white flex items-center gap-2 text-xs">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                الإصلاحات المنفذة في v1.0.16:
              </h4>
              <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside leading-relaxed">
                <li>
                  <strong className="text-emerald-300">حذف إذن تحسين البطارية:</strong> أزلنا <code className="text-emerald-300">REQUEST_IGNORE_BATTERY_OPTIMIZATIONS</code> المسبب الرئيسي لحظر Play Protect.
                </li>
                <li>
                  <strong className="text-emerald-300">توقيع مزدوج صارم:</strong> تفعيل v1 (JAR) و v2 (APK Signature Scheme) معاً.
                </li>
                <li>
                  <strong className="text-emerald-300">نموذج استئناف رسمي:</strong> متاح في <code className="text-emerald-300">docs/PLAY_PROTECT.md</code> لاعتماد شهادة التوقيع لدى جوجل.
                </li>
              </ul>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 p-3 bg-slate-900/90 border border-amber-500/20 rounded-xl text-xs">
            <span className="text-slate-300">
              للمزيد من التفاصيل ونموذج الاستئناف الرسمي الموجه لجوجل:
            </span>
            <a
              href="https://support.google.com/googleplay/android-developer/contact/protectappeals"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 text-amber-400 hover:text-amber-300 font-bold underline"
            >
              <span>رابط نموذج استئناف Google Play Protect</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>
      )}
    </div>
  );
};
