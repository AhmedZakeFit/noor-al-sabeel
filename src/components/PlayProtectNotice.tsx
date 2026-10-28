import React, { useState } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  CheckCircle2,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  Info,
} from 'lucide-react';

/**
 * Play Protect notice — v1.0.17 rewrite.
 *
 * Earlier versions of this component told users to tap "Install anyway" as
 * step 1 and claimed facts that do not hold (a "v1+v2 strict dual signing"
 * that the shipped APK does not carry, a battery-permission "main cause",
 * and a docs/PLAY_PROTECT.md appeal document that did not exist). This
 * version reports only what has actually been verified, explains WHICH
 * warning the user is seeing, and points at the real resolution path —
 * it never instructs the user to bypass the warning and never suggests
 * disabling Play Protect.
 */
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
                فحص Google Play Protect
              </span>
              <h3 className="text-lg font-bold text-amber-100">
                لماذا قد يظهر تحذير عند تثبيت التطبيق؟
              </h3>
            </div>
            <p className="text-sm text-amber-200/90 mt-1.5 leading-relaxed">
              تطبيق نور السبيل يُوزَّع من GitHub Releases وليس من متجر Google Play، لذا يفحصه
              Play Protect ويُظهر أحيانًا تحذيرًا وقائيًا للتطبيقات غير المفهرسة لديه. هذا التحذير
              <strong> لا يعني بالضرورة وجود ضرر</strong>، ولا نطلب منك تجاهله — بل نعرض لك هنا كيف
              تتحقق بنفسك من سلامة الملف الذي نزّلته قبل أي قرار.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 shrink-0">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="flex items-center gap-1.5 px-4 py-2 bg-amber-600/30 hover:bg-amber-600/40 text-amber-200 border border-amber-500/30 rounded-xl font-bold text-xs transition-all active:scale-95"
          >
            <span>{isExpanded ? 'إخفاء التفاصيل' : 'التحقق والتفاصيل'}</span>
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* Verification facts banner */}
      <div className="mt-4 pt-3.5 border-t border-amber-500/20 grid grid-cols-1 md:grid-cols-3 gap-3 text-xs text-amber-200/80">
        <div className="flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <span>
            بصمة SHA-256 لكل APK منشور مرفوعة بجانبه في ملف <code>.sha256</code> — قارنها بنفسك قبل التثبيت
          </span>
        </div>
        <div className="flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <span>
            شهادة توقيع واحدة ثابتة عبر جميع الإصدارات المنشورة (تحقق بايت-ببايت من v1.0.13 حتى v1.0.16)
          </span>
        </div>
        <div className="flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
          <span>
            مسار التحديث داخل التطبيق يرفض أي ملف لا يطابق البصمة والشهادة واسم الحزمة ورقم الإصدار
          </span>
        </div>
      </div>

      {/* Expanded deep dive */}
      {isExpanded && (
        <div className="mt-5 pt-4 border-t border-amber-500/20 space-y-4 text-sm text-slate-300">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-2.5">
              <h4 className="font-bold text-white flex items-center gap-2 text-xs">
                <Info className="w-4 h-4 text-amber-400" />
                أي تحذير ترى بالضبط؟
              </h4>
              <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside leading-relaxed">
                <li>
                  <strong className="text-slate-300">تحذير مصدر التثبيت:</strong> «معطل تثبيت تطبيقات غير معروفة» — إعداد نظام يطلب منح التطبيق صلاحية «تثبيت تطبيقات غير معروفة» مرة واحدة، ولا علاقة له بالضرر.
                </li>
                <li>
                  <strong className="text-slate-300">فحص Play Protect:</strong> «قد يكون التطبيق ضارًا» — فحص سمعة للتطبيقات المثبتة خارج المتجر. تطبيق موقّع بشهادة مستقلة غير مفهرسة لدى Google يستدعي هذا الفحص تلقائيًا، دون أن يعني اكتشاف أي ضرر فعلي.
                </li>
                <li>
                  <strong className="text-slate-300">حجب فعلي:</strong> إذا رفض النظام التثبيت رفضًا تامًا فغالبًا لا تطابق بصمة التوقيع مع نسخة قائمة — وهذا مسار ممنوع في هذا التطبيق أصلًا (سنوات التحديث تعتمد على ثبات الشهادة).
                </li>
              </ul>
            </div>

            <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-2.5">
              <h4 className="font-bold text-white flex items-center gap-2 text-xs">
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
                ما الذي تم التحقق منه فعليًا؟
              </h4>
              <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside leading-relaxed">
                <li>
                  <strong className="text-slate-300">سلامة الملف:</strong> كل APK منشور يطابق بصمة SHA-256 المنشورة معه، وسير عمل النشر يتحقق من الرابط الثابت آليًا بعد كل إصدار.
                </li>
                <li>
                  <strong className="text-slate-300">ثبات التوقيع:</strong> تحليل كتلة توقيع APK (v2 Signature Scheme) أثبت أن الإصدارات v1.0.13 حتى v1.0.16 موقّعة بنفس الشهادة تمامًا — أي أن التحديث من أي إصدار سابق ممكن دون كسر التوافق.
                </li>
                <li>
                  <strong className="text-slate-300">منذ v1.0.17:</strong> يمنع سير عمل النشر استبدال بايتات إصدار منشور تحت نفس الوسم — السيناريو الذي جعل بعض المستخدمين يتلقون ملفًا لا يطابق بصمته أثناء تحديث v1.0.16.
                </li>
              </ul>
            </div>
          </div>

          <div className="bg-slate-950/70 border border-slate-800 rounded-xl p-4 space-y-2.5">
            <h4 className="font-bold text-white flex items-center gap-2 text-xs">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              كيف تتحقق بنفسك قبل التثبيت؟
            </h4>
            <ul className="text-xs text-slate-400 space-y-1.5 list-disc list-inside leading-relaxed">
              <li>
                نزّل الـ APK ثم نزّل ملف <code className="text-amber-300">.sha256</code> المرفق به في صفحة الإصدارات.
              </li>
              <li>
                على أندرويد/لينكس: <code className="text-amber-300">sha256sum noor-al-sabeel.apk</code> ثم قارن الناتج بالبصمة المرفوعة.
              </li>
              <li>
                في مسار التحديث داخل التطبيق يقوم التطبيق بهذه المقارنة تلقائيًا ويرفض أي ملف غير متطابق قبل فتح مثبّت النظام.
              </li>
            </ul>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 p-3 bg-slate-900/90 border border-amber-500/20 rounded-xl text-xs">
            <span className="text-slate-300">
              الحل الجذري المعتمد: فهرسة التطبيق لدى Google (نموذج استئناف Play Protect للمطورين) —
              <a
                href="https://support.google.com/googleplay/android-developer/contact/protectappeals"
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1.5 text-amber-400 hover:text-amber-300 font-bold underline inline-flex mr-1"
              >
                <span>نموذج الاستئناف الرسمي</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            </span>
          </div>

          <p className="text-xs text-slate-500 leading-relaxed">
            نحن لا نعطّل Google Play Protect ولا نطلب منك تعطيله، ولا يطلب التطبيق أي صلاحية لتعطيل
            الفحص. التفاصيل التقنية الكاملة وتحليل التوقيع موجودة في
            <code className="text-amber-300 mx-1">docs/PLAY_PROTECT.md</code>
            داخل المستودع.
          </p>
        </div>
      )}
    </div>
  );
};
