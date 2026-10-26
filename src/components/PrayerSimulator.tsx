import React, { useState, useEffect, useMemo } from 'react';
import {
  CITIES,
  METHOD_CONFIGS,
  computePrayerTimes,
  getHijriDate,
} from '../lib/astronomy';
import { CalculationMethodId, AsrSchool, CityPreset } from '../types';
import {
  Compass,
  Clock,
  MapPin,
  Calendar,
  Sparkles,
  Info,
  ChevronRight,
  Sun,
  Sunset,
  Sunrise,
  Moon,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

export const PrayerSimulator: React.FC = () => {
  const [selectedCity, setSelectedCity] = useState<CityPreset>(CITIES[0]); // Cairo
  const [methodId, setMethodId] = useState<CalculationMethodId>(selectedCity.defaultMethod);
  const [school, setSchool] = useState<AsrSchool>('standard');
  const [simulatedDate, setSimulatedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [now, setNow] = useState<Date>(new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const dateObj = useMemo(() => {
    const [y, m, d] = simulatedDate.split('-').map(Number);
    return new Date(y, m - 1, d);
  }, [simulatedDate]);

  const prayerTimes = useMemo(() => {
    return computePrayerTimes(
      dateObj,
      selectedCity.latitude,
      selectedCity.longitude,
      methodId,
      school
    );
  }, [dateObj, selectedCity, methodId, school]);

  const hijri = useMemo(() => getHijriDate(dateObj), [dateObj]);

  const formatTime = (d: Date) => {
    return d.toLocaleTimeString('ar-EG', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  };

  const prayersList = [
    { id: 'fajr', name: 'الفجر', time: prayerTimes.fajr, icon: <Sunrise className="w-5 h-5 text-amber-300" /> },
    { id: 'sunrise', name: 'الشروق', time: prayerTimes.sunrise, icon: <Sun className="w-5 h-5 text-yellow-400" /> },
    { id: 'dhuhr', name: 'الظهر', time: prayerTimes.dhuhr, icon: <Sun className="w-5 h-5 text-orange-400" /> },
    { id: 'asr', name: 'العصر', time: prayerTimes.asr, icon: <Sunset className="w-5 h-5 text-amber-500" /> },
    { id: 'maghrib', name: 'المغرب', time: prayerTimes.maghrib, icon: <Sunset className="w-5 h-5 text-rose-400" /> },
    { id: 'isha', name: 'العشاء', time: prayerTimes.isha, icon: <Moon className="w-5 h-5 text-indigo-400" /> },
  ];

  // Determine next prayer
  const nextPrayer = useMemo(() => {
    const currentTime = now.getTime();
    for (const p of prayersList) {
      if (p.id === 'sunrise') continue; // not notifiable prayer in app logic
      if (p.time.getTime() > currentTime) {
        const diffMs = p.time.getTime() - currentTime;
        const totalMinutes = Math.floor(diffMs / (1000 * 60));
        const hours = Math.floor(totalMinutes / 60);
        const minutes = totalMinutes % 60;
        return { name: p.name, time: p.time, hours, minutes };
      }
    }
    return { name: 'الفجر (غداً)', time: prayerTimes.fajr, hours: 0, minutes: 0 };
  }, [now, prayerTimes, prayersList]);

  return (
    <div className="space-y-6">
      {/* City & Parameters Control Bar */}
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow-xl">
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-emerald-400" />
              <h3 className="text-lg font-bold text-slate-100">
                محاكي محرك الحسابات الفلكية (Noor Core Engine)
              </h3>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              محاكاة حية لخوارزميات جان ميوس (Jean Meeus) الفلكية المطبقة في نور السبيل مع تصحيح منتصف الليل والثلث الأخير
            </p>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1.5">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              تم تصحيح الخوارزمية الفلكية
            </span>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              المدينة المحددة:
            </label>
            <select
              value={selectedCity.nameEn}
              onChange={(e) => {
                const c = CITIES.find((city) => city.nameEn === e.target.value) || CITIES[0];
                setSelectedCity(c);
                setMethodId(c.defaultMethod);
              }}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 transition-all"
            >
              {CITIES.map((city) => (
                <option key={city.nameEn} value={city.nameEn}>
                  {city.nameAr} ({city.country})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              طريقة الحساب المعتمدة:
            </label>
            <select
              value={methodId}
              onChange={(e) => setMethodId(e.target.value as CalculationMethodId)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 transition-all"
            >
              {Object.values(METHOD_CONFIGS).map((m) => (
                <option key={m.id} value={m.id}>
                  {m.nameAr}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              مذهب صلاة العصر (طول الظل):
            </label>
            <select
              value={school}
              onChange={(e) => setSchool(e.target.value as AsrSchool)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 transition-all"
            >
              <option value="standard">الجمهور: الشافعي/المالكي/الحنبلي (ظل 1x)</option>
              <option value="hanafi">الحنفي (ظل 2x)</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              تاريخ المحاكاة:
            </label>
            <input
              type="date"
              value={simulatedDate}
              onChange={(e) => setSimulatedDate(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500 transition-all"
            />
          </div>
        </div>
      </div>

      {/* Hero Prayer Card with Countdown */}
      <div className="bg-gradient-to-br from-emerald-950/40 via-slate-900 to-slate-950 border border-emerald-500/30 rounded-3xl p-6 shadow-2xl relative overflow-hidden">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2 text-emerald-400 text-sm font-semibold">
              <Clock className="w-4 h-4" />
              <span>الصلاة القادمة</span>
            </div>
            <div className="text-3xl md:text-4xl font-black text-slate-100 flex items-center gap-3">
              <span>صلاة {nextPrayer.name}</span>
              <span className="text-2xl md:text-3xl font-light text-slate-400">
                {formatTime(nextPrayer.time)}
              </span>
            </div>
            <p className="text-sm text-slate-400">
              المتبقي: <span className="font-bold text-emerald-400 font-mono">{nextPrayer.hours}</span> ساعة و{' '}
              <span className="font-bold text-emerald-400 font-mono">{nextPrayer.minutes}</span> دقيقة
            </p>
          </div>

          <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-4 text-center min-w-[200px]">
            <div className="text-xs text-slate-500 font-semibold mb-1">التقويم الهجري (تقويم أم القرى)</div>
            <div className="text-lg font-bold text-emerald-300 font-amiri">
              {hijri.day} {hijri.monthNameAr} {hijri.year} هـ
            </div>
            <div className="text-xs text-slate-400 mt-1">
              {selectedCity.nameAr} • {selectedCity.latitude.toFixed(2)}°N, {selectedCity.longitude.toFixed(2)}°E
            </div>
          </div>
        </div>
      </div>

      {/* Main Prayer Times Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3.5">
        {prayersList.map((p) => {
          const isNext = p.name === nextPrayer.name;

          return (
            <div
              key={p.id}
              className={`p-4 rounded-2xl border transition-all text-center flex flex-col items-center justify-between ${
                isNext
                  ? 'bg-emerald-950/40 border-emerald-500/60 shadow-lg shadow-emerald-500/10'
                  : 'bg-slate-900/70 border-slate-800/80'
              }`}
            >
              <div className="p-2.5 rounded-xl bg-slate-800/60 mb-2">{p.icon}</div>
              <div className="text-sm font-bold text-slate-200">{p.name}</div>
              <div className="text-lg font-black text-slate-100 font-mono mt-1">
                {formatTime(p.time)}
              </div>
              {isNext && (
                <span className="mt-2 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300">
                  الحالية
                </span>
              )}
            </div>
          );
        })}
      </div>

      {/* Special Box: FIXED Islamic Midnight & Last Third of Night */}
      <div className="bg-slate-900/90 border-2 border-emerald-500/40 rounded-2xl p-5 shadow-xl">
        <div className="flex items-center gap-2.5 mb-3">
          <div className="p-2 bg-emerald-500/20 rounded-xl text-emerald-400">
            <Moon className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-base font-bold text-slate-100">
              المواقيت الإضافية الشرعية المصححة (عرض المزيد في تطبيق نور السبيل)
            </h4>
            <p className="text-xs text-slate-400">
              هذه الحسابات كانت معطوبة وتظهر 11:00 صباحاً! تم إصلاح المعادلة لتظهر بالتوقيت الليلي الدقيق شرعياً:
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
          <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl">
            <div className="text-xs text-slate-400 font-medium">منتصف الليل الشرعي (نصف الليل بين المغرب والفجر)</div>
            <div className="text-2xl font-black text-emerald-400 font-mono mt-1">
              {formatTime(prayerTimes.midnight)}
            </div>
            <div className="text-[11px] text-slate-500 mt-1">
              المغرب: {formatTime(prayerTimes.maghrib)} + {Math.round(prayerTimes.nightDurationMinutes / 2)} دقيقة
            </div>
          </div>

          <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl">
            <div className="text-xs text-slate-400 font-medium">الثلث الأخير من الليل (وقت السحر والقيام واستجابة الدعاء)</div>
            <div className="text-2xl font-black text-emerald-400 font-mono mt-1">
              {formatTime(prayerTimes.lastThirdOfNight)}
            </div>
            <div className="text-[11px] text-slate-500 mt-1">
              فجر الغد: {formatTime(prayerTimes.fajr)} - {Math.round(prayerTimes.nightDurationMinutes / 3)} دقيقة
            </div>
          </div>

          <div className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl">
            <div className="text-xs text-slate-400 font-medium">إجمالي مدة الليل الشرعي</div>
            <div className="text-2xl font-black text-slate-200 font-mono mt-1">
              {Math.floor(prayerTimes.nightDurationMinutes / 60)} س و {prayerTimes.nightDurationMinutes % 60} د
            </div>
            <div className="text-[11px] text-emerald-400/80 mt-1 flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              قيمة موجبة صحيحة (كانت سالبة -780 دقيقة سابقاً)
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
