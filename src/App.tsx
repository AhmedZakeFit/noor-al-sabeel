import React, { useState } from 'react';
import { SecurityAlert } from './components/SecurityAlert';
import { AuditReport } from './components/AuditReport';
import { PrayerSimulator } from './components/PrayerSimulator';
import { QiblaSimulator } from './components/QiblaSimulator';
import { DeviceMatrix } from './components/DeviceMatrix';
import { PatchViewer } from './components/PatchViewer';
import { FeaturesShowcase } from './components/FeaturesShowcase';
import {
  FileSearch,
  Compass,
  Clock,
  Smartphone,
  GitPullRequest,
  Github,
  Sparkles,
  Shield,
  Layers,
  HeartHandshake,
  CheckCircle2,
  ExternalLink,
  MapPin,
} from 'lucide-react';

export default function App() {
  const [activeTab, setActiveTab] = useState<
    'features' | 'audit' | 'prayer_sim' | 'qibla_sim' | 'device_matrix' | 'patches'
  >('features');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col antialiased">
      {/* Top Ambient Glow */}
      <div className="fixed top-0 left-1/2 -translate-x-1/2 w-[800px] h-64 bg-emerald-500/10 blur-[120px] rounded-full pointer-events-none -z-10" />

      {/* Main Header */}
      <header className="border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-400 flex items-center justify-center shadow-lg shadow-emerald-500/20 text-slate-950 font-black text-xl shrink-0">
              ن
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-black tracking-tight text-white font-amiri">
                  نور السبيل <span className="text-emerald-400 font-sans text-sm font-bold">| Noor Al-Sabeel</span>
                </h1>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  v1.0.14 Audited
                </span>
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                تطبيق الأذان والمواقيت والقبلة المفتوح المصدر وخالي من الإعلانات والتتبع للأندرويد
              </p>
            </div>
          </div>

          {/* Quick External Actions */}
          <div className="flex items-center gap-2.5">
            <a
              href="https://github.com/AhmedZakeFit/noor-al-sabeel"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-slate-900 border border-slate-800 hover:border-slate-700 text-slate-300 hover:text-white text-xs font-semibold transition-all shadow-sm"
            >
              <Github className="w-4 h-4 text-emerald-400" />
              <span>مستودع المشروع</span>
              <ExternalLink className="w-3.5 h-3.5 opacity-60" />
            </a>

            <a
              href="https://ahmedzakefit.github.io/noor-al-sabeel/"
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-emerald-600/90 hover:bg-emerald-500 text-slate-950 text-xs font-bold transition-all shadow-md active:scale-95"
            >
              <span>صفحة المشروع والتحميل</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="flex items-center gap-1.5 overflow-x-auto py-2 scrollbar-none border-t border-slate-800/50">
             {[
              {
                id: 'features',
                label: 'ميزات التطبيق والبحث عن المساجد',
                icon: <MapPin className="w-4 h-4" />,
                badge: 'الجديد',
              },
              {
                id: 'audit',
                label: 'تقرير الفحص والتشخيص الشامل',
                icon: <FileSearch className="w-4 h-4" />,
                badge: '8 نقاط',
              },
              {
                id: 'prayer_sim',
                label: 'محاكي المواقيت الفلكية والتقويم',
                icon: <Clock className="w-4 h-4" />,
                badge: 'مصحح',
              },
              {
                id: 'qibla_sim',
                label: 'محاكي بوصلة القبلة',
                icon: <Compass className="w-4 h-4" />,
              },
              {
                id: 'device_matrix',
                label: 'مصفوفة الأجهزة وإدارة البطارية',
                icon: <Smartphone className="w-4 h-4" />,
              },
              {
                id: 'patches',
                label: 'مركز الترقيعات و Git Patches',
                icon: <GitPullRequest className="w-4 h-4" />,
                badge: 'جاهز',
              },
            ].map((tab) => {
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-bold transition-all whitespace-nowrap ${
                    isActive
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 shadow-sm'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                  }`}
                >
                  {tab.icon}
                  <span>{tab.label}</span>
                  {tab.badge && (
                    <span
                      className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                        isActive
                          ? 'bg-emerald-500 text-slate-950'
                          : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      {tab.badge}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 space-y-6">
        {/* Security Warning (Immediate Priority) */}
        <SecurityAlert />

        {/* Tab Views */}
        {activeTab === 'features' && <FeaturesShowcase />}
        {activeTab === 'audit' && <AuditReport />}
        {activeTab === 'prayer_sim' && <PrayerSimulator />}
        {activeTab === 'qibla_sim' && <QiblaSimulator />}
        {activeTab === 'device_matrix' && <DeviceMatrix />}
        {activeTab === 'patches' && <PatchViewer />}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950 py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <span>مشروع نور السبيل (Noor Al-Sabeel)</span>
            <span>•</span>
            <span className="text-slate-400">المدرب أحمد زكي (AhmedZakeFit)</span>
            <span>•</span>
            <span>مرخص تحت رخصة Apache-2.0</span>
          </div>

          <div className="flex items-center gap-3 text-slate-400">
            <span>خوارزميات فلكية 100% بدون إنترنت (Offline-First)</span>
            <span>•</span>
            <span className="text-emerald-400 font-semibold">خصوصية تامة</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
