import React from 'react';
import { AlertTriangle, ShieldAlert, KeyRound, ExternalLink, CheckCircle2 } from 'lucide-react';

export const SecurityAlert: React.FC = () => {
  return (
    <div className="bg-gradient-to-r from-rose-950/80 via-red-900/60 to-rose-950/80 border-2 border-rose-500/50 rounded-2xl p-5 mb-8 shadow-2xl backdrop-blur-md relative overflow-hidden">
      <div className="absolute -left-10 -bottom-10 w-40 h-40 bg-rose-500/10 rounded-full blur-3xl pointer-events-none" />
      
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div className="flex items-start gap-3.5">
          <div className="p-3 bg-rose-500/20 border border-rose-500/30 rounded-xl text-rose-400 shrink-0 mt-0.5">
            <ShieldAlert className="w-7 h-7" />
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-rose-500 text-white">
                تنبيه أمني عاجل
              </span>
              <h3 className="text-lg font-bold text-rose-100">
                تسريب رمز الوصول الشخصي (GitHub Personal Access Token) في نص الرسالة!
              </h3>
            </div>
            <p className="text-sm text-rose-200/90 mt-1.5 leading-relaxed">
              لاحظنا أنك أرفقت التوكن الشخصي <code className="bg-rose-900/80 px-2 py-0.5 rounded text-rose-300 font-mono text-xs">ghp_0zPJ...2XhgEN</code> مباشرة في رابط المستودع. هذا التوكن يمنح صلاحيات كاملة لقراءة وكتابة وحذف الكود وإدارة المستودعات في حسابك على GitHub.
            </p>
          </div>
        </div>

        <a
          href="https://github.com/settings/tokens"
          target="_blank"
          rel="noopener noreferrer"
          className="shrink-0 flex items-center gap-2 px-4 py-2.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl font-medium text-sm transition-all shadow-lg hover:shadow-rose-500/25 active:scale-95"
        >
          <KeyRound className="w-4 h-4" />
          <span>إلغاء التوكن فوراً من GitHub</span>
          <ExternalLink className="w-4 h-4 opacity-75" />
        </a>
      </div>

      <div className="mt-4 pt-3.5 border-t border-rose-500/20 grid grid-cols-1 md:grid-cols-3 gap-3 text-xs text-rose-200/80">
        <div className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-rose-400 shrink-0" />
          <span>1. افتح إعدادات GitHub (Developer Settings → Personal Access Tokens)</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-rose-400 shrink-0" />
          <span>2. ابحث عن التوكن واضغط على زر Delete / Revoke</span>
        </div>
        <div className="flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-rose-400 shrink-0" />
          <span>3. أنشئ توكن جديد بصلاحيات محددة فقط (Fine-grained) عند الحاجة</span>
        </div>
      </div>
    </div>
  );
};
