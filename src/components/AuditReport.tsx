import React, { useState } from 'react';
import { AUDIT_ISSUES } from '../data/auditData';
import { AuditIssue } from '../types';
import {
  AlertCircle,
  CheckCircle,
  FileCode2,
  Filter,
  ShieldCheck,
  ChevronDown,
  ChevronUp,
  Sparkles,
  ArrowRight,
  Bug,
  Cpu,
  Layers,
  Smartphone,
} from 'lucide-react';

export const AuditReport: React.FC = () => {
  const [selectedFilter, setSelectedFilter] = useState<'all' | 'critical' | 'high' | 'medium' | 'fixed'>('all');
  const [expandedId, setExpandedId] = useState<string | null>('BUG-01');

  const filteredIssues = AUDIT_ISSUES.filter((issue) => {
    if (selectedFilter === 'all') return true;
    if (selectedFilter === 'fixed') return issue.status === 'fixed';
    return issue.severity === selectedFilter;
  });

  const getSeverityBadge = (severity: AuditIssue['severity'], status: AuditIssue['status']) => {
    if (status === 'security_alert') {
      return (
        <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1.5">
          <span className="w-1.5 h-1.5 rounded-full bg-rose-400 animate-pulse" />
          تحذير أمني حرج
        </span>
      );
    }
    if (status === 'fixed') {
      return (
        <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5">
          <CheckCircle className="w-3.5 h-3.5 text-emerald-400" />
          تم الإصلاح والاختبار
        </span>
      );
    }
    switch (severity) {
      case 'high':
        return (
          <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">
            أهمية عالية
          </span>
        );
      case 'medium':
        return (
          <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30">
            أهمية متوسطة
          </span>
        );
      default:
        return (
          <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-slate-500/20 text-slate-300 border border-slate-500/30">
            اقتراح تحسين
          </span>
        );
    }
  };

  const getCategoryIcon = (category: string) => {
    if (category.includes('فلكي') || category.includes('الشريعة')) return <Sparkles className="w-4 h-4 text-emerald-400" />;
    if (category.includes('أمان')) return <AlertCircle className="w-4 h-4 text-rose-400" />;
    if (category.includes('مستشعرات') || category.includes('أندرويد')) return <Smartphone className="w-4 h-4 text-cyan-400" />;
    if (category.includes('الأداء')) return <Cpu className="w-4 h-4 text-amber-400" />;
    return <Layers className="w-4 h-4 text-indigo-400" />;
  };

  return (
    <div className="space-y-6">
      {/* Overview Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 flex items-center gap-3.5">
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl text-emerald-400">
            <CheckCircle className="w-6 h-6" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-100">2</div>
            <div className="text-xs text-slate-400 font-medium">أخطاء حيوية تم إصلاحها</div>
          </div>
        </div>

        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 flex items-center gap-3.5">
          <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl text-amber-400">
            <Bug className="w-6 h-6" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-100">{AUDIT_ISSUES.length}</div>
            <div className="text-xs text-slate-400 font-medium">إجمالي نقاط الفحص والتدقيق</div>
          </div>
        </div>

        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 flex items-center gap-3.5">
          <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl text-indigo-400">
            <FileCode2 className="w-6 h-6" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-100">100%</div>
            <div className="text-xs text-slate-400 font-medium">تغطية كوتلن وأندرويد</div>
          </div>
        </div>

        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 flex items-center gap-3.5">
          <div className="p-3 bg-cyan-500/10 border border-cyan-500/20 rounded-xl text-cyan-400">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <div className="text-2xl font-black text-slate-100">v1.0.13</div>
            <div className="text-xs text-slate-400 font-medium">الإصدار المفحوص للمشروع</div>
          </div>
        </div>
      </div>

      {/* Filters Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-slate-900/60 p-3 rounded-2xl border border-slate-800">
        <div className="flex items-center gap-2 text-sm text-slate-300 font-medium">
          <Filter className="w-4 h-4 text-emerald-400" />
          <span>تصفية نتائج التدقيق:</span>
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          {[
            { id: 'all', label: 'الكل' },
            { id: 'fixed', label: 'تم إصلاحه (2)' },
            { id: 'critical', label: 'حرجة (1)' },
            { id: 'high', label: 'عالية (3)' },
            { id: 'medium', label: 'متوسطة (3)' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setSelectedFilter(tab.id as any)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                selectedFilter === tab.id
                  ? 'bg-emerald-500 text-slate-950 shadow-md'
                  : 'bg-slate-800/80 hover:bg-slate-800 text-slate-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Issues Accordion */}
      <div className="space-y-4">
        {filteredIssues.map((issue) => {
          const isExpanded = expandedId === issue.id;

          return (
            <div
              key={issue.id}
              className={`border rounded-2xl transition-all overflow-hidden ${
                issue.status === 'security_alert'
                  ? 'bg-rose-950/20 border-rose-500/40'
                  : issue.status === 'fixed'
                  ? 'bg-emerald-950/10 border-emerald-500/30'
                  : 'bg-slate-900/70 border-slate-800 hover:border-slate-700'
              }`}
            >
              <div
                onClick={() => setExpandedId(isExpanded ? null : issue.id)}
                className="p-5 cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4 select-none"
              >
                <div className="flex items-start gap-3.5">
                  <div className="p-2.5 rounded-xl bg-slate-800/80 border border-slate-700/60 shrink-0 mt-0.5">
                    {getCategoryIcon(issue.categoryAr)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2.5 flex-wrap">
                      <span className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-semibold">
                        {issue.id}
                      </span>
                      <h4 className="text-base font-bold text-slate-100">{issue.titleAr}</h4>
                      {getSeverityBadge(issue.severity, issue.status)}
                    </div>
                    <p className="text-xs text-slate-400 mt-1">{issue.summaryAr}</p>
                    <div className="flex items-center gap-3 mt-2 text-xs text-slate-500">
                      <span className="flex items-center gap-1 font-mono">
                        <FileCode2 className="w-3.5 h-3.5 text-slate-400" />
                        {issue.file}
                      </span>
                      <span>•</span>
                      <span>{issue.categoryAr}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end md:self-center">
                  <button className="text-slate-400 hover:text-slate-200 p-1.5 rounded-lg bg-slate-800/50">
                    {isExpanded ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                  </button>
                </div>
              </div>

              {isExpanded && (
                <div className="px-5 pb-5 pt-2 border-t border-slate-800/80 space-y-4 text-sm bg-slate-950/40">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                      <div className="text-xs font-bold text-rose-400 uppercase mb-1">السبب الجذري للمشكلة</div>
                      <p className="text-slate-300 text-xs leading-relaxed">{issue.rootCauseAr}</p>
                    </div>

                    <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                      <div className="text-xs font-bold text-emerald-400 uppercase mb-1">الحل والمعالجة المقترحة</div>
                      <p className="text-slate-300 text-xs leading-relaxed">{issue.fixDescriptionAr}</p>
                    </div>
                  </div>

                  <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                    <div className="text-xs font-bold text-slate-300 uppercase mb-1">التفاصيل الفنية والتحليلية</div>
                    <p className="text-slate-400 text-xs leading-relaxed">{issue.detailsAr}</p>
                  </div>

                  {issue.oldCodeSnippet && issue.newCodeSnippet && (
                    <div className="space-y-2">
                      <div className="text-xs font-bold text-slate-300">مقارنة الكود (Diff):</div>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 font-mono text-xs">
                        <div className="p-3 bg-rose-950/20 border border-rose-900/40 rounded-xl overflow-x-auto">
                          <div className="text-[11px] font-bold text-rose-400 mb-2 flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-rose-500" />
                            قبل الإصلاح (الكود الأصلي):
                          </div>
                          <pre className="text-rose-200/90 whitespace-pre leading-relaxed">{issue.oldCodeSnippet}</pre>
                        </div>

                        <div className="p-3 bg-emerald-950/20 border border-emerald-900/40 rounded-xl overflow-x-auto">
                          <div className="text-[11px] font-bold text-emerald-400 mb-2 flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-emerald-500" />
                            بعد الإصلاح في المستودع:
                          </div>
                          <pre className="text-emerald-200/90 whitespace-pre leading-relaxed">{issue.newCodeSnippet}</pre>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
