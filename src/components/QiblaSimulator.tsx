import React, { useState, useMemo } from 'react';
import { CITIES, computeQibla, KAABA_COORDS } from '../lib/astronomy';
import { CityPreset } from '../types';
import {
  Compass,
  Navigation,
  MapPin,
  RotateCw,
  CheckCircle2,
  Info,
  Maximize2,
  Sliders,
} from 'lucide-react';

export const QiblaSimulator: React.FC = () => {
  const [selectedCity, setSelectedCity] = useState<CityPreset>(CITIES[0]); // Cairo
  const [deviceHeading, setDeviceHeading] = useState<number>(0); // 0 to 360 deg
  const [customLat, setCustomLat] = useState<string>('30.0444');
  const [customLon, setCustomLon] = useState<string>('31.2357');
  const [isCustom, setIsCustom] = useState<boolean>(false);

  const activeLat = isCustom ? parseFloat(customLat) || 0 : selectedCity.latitude;
  const activeLon = isCustom ? parseFloat(customLon) || 0 : selectedCity.longitude;

  const qiblaData = useMemo(() => {
    return computeQibla(activeLat, activeLon);
  }, [activeLat, activeLon]);

  // Relative needle angle
  const needleAngle = useMemo(() => {
    let diff = qiblaData.bearing - deviceHeading;
    while (diff > 180) diff -= 360;
    while (diff < -180) diff += 360;
    return diff;
  }, [qiblaData.bearing, deviceHeading]);

  const isAligned = Math.abs(needleAngle) <= 3;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Controls Column */}
        <div className="lg:col-span-5 space-y-4">
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow-xl space-y-4">
            <div className="flex items-center gap-2 pb-3 border-b border-slate-800">
              <Compass className="w-5 h-5 text-emerald-400" />
              <h3 className="text-base font-bold text-slate-100">
                حساب اتجاه القبلة والمسافة العظمى (Qibla Math)
              </h3>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setIsCustom(false)}
                  className={`flex-1 py-2 px-3 rounded-xl font-medium text-xs transition-all ${
                    !isCustom ? 'bg-emerald-500 text-slate-950 font-bold' : 'bg-slate-800 text-slate-300'
                  }`}
                >
                  اختيار مدينة جاهزة
                </button>
                <button
                  onClick={() => setIsCustom(true)}
                  className={`flex-1 py-2 px-3 rounded-xl font-medium text-xs transition-all ${
                    isCustom ? 'bg-emerald-500 text-slate-950 font-bold' : 'bg-slate-800 text-slate-300'
                  }`}
                >
                  إحداثيات GPS مخصصة
                </button>
              </div>

              {!isCustom ? (
                <div>
                  <label className="block text-xs font-semibold text-slate-400 mb-1">المدينة:</label>
                  <select
                    value={selectedCity.nameEn}
                    onChange={(e) => {
                      const c = CITIES.find((city) => city.nameEn === e.target.value) || CITIES[0];
                      setSelectedCity(c);
                    }}
                    className="w-full bg-slate-950 border border-slate-700/80 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-emerald-500"
                  >
                    {CITIES.map((c) => (
                      <option key={c.nameEn} value={c.nameEn}>
                        {c.nameAr} - {c.country}
                      </option>
                    ))}
                  </select>
                </div>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">خط العرض (Lat):</label>
                    <input
                      type="number"
                      step="any"
                      value={customLat}
                      onChange={(e) => setCustomLat(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-slate-400 mb-1">خط الطول (Lon):</label>
                    <input
                      type="number"
                      step="any"
                      value={customLon}
                      onChange={(e) => setCustomLon(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200"
                    />
                  </div>
                </div>
              )}

              {/* Slider for device rotation */}
              <div className="pt-2">
                <div className="flex items-center justify-between text-xs mb-1.5">
                  <span className="text-slate-400 font-semibold flex items-center gap-1.5">
                    <Sliders className="w-3.5 h-3.5 text-cyan-400" />
                    محاكاة تدوير الهاتف (Heading):
                  </span>
                  <span className="font-mono text-emerald-400 font-bold">{deviceHeading}°</span>
                </div>
                <input
                  type="range"
                  min="0"
                  max="359"
                  value={deviceHeading}
                  onChange={(e) => setDeviceHeading(parseInt(e.target.value, 10))}
                  className="w-full accent-emerald-500 bg-slate-800 rounded-lg h-2 cursor-pointer"
                />
                <div className="flex justify-between text-[10px] text-slate-500 mt-1 font-mono">
                  <span>0° (شمال)</span>
                  <span>90° (شرق)</span>
                  <span>180° (جنوب)</span>
                  <span>270° (غرب)</span>
                </div>
              </div>
            </div>
          </div>

          {/* Stats Box */}
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 text-center">
              <div className="text-xs text-slate-400 font-semibold">زاوية القبلة الحقيقية</div>
              <div className="text-2xl font-black text-emerald-400 font-mono mt-1">
                {qiblaData.bearing.toFixed(1)}°
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">من الشمال الحقيقي</div>
            </div>

            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 text-center">
              <div className="text-xs text-slate-400 font-semibold">المسافة إلى الكعبة المشرفة</div>
              <div className="text-2xl font-black text-cyan-400 font-mono mt-1">
                {Math.round(qiblaData.distanceKm).toLocaleString('ar-EG')}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">كيلومتر عبر الدائرة العظمى</div>
            </div>
          </div>
        </div>

        {/* Compass Visualization Column */}
        <div className="lg:col-span-7 bg-slate-900/80 border border-slate-800 rounded-2xl p-6 shadow-xl flex flex-col items-center justify-center relative min-h-[380px]">
          {/* Status Indicator */}
          <div className="mb-4">
            {isAligned ? (
              <div className="flex items-center gap-2 px-4 py-1.5 rounded-full bg-emerald-500/20 border border-emerald-500 text-emerald-300 font-bold text-sm shadow-lg shadow-emerald-500/20 animate-pulse">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                <span>أنت الآن في مواجهة القبلة تماماً! (Aligned)</span>
              </div>
            ) : (
              <div className="flex items-center gap-2 px-4 py-1.5 rounded-full bg-slate-800/80 border border-slate-700 text-slate-300 text-xs">
                <span>أدر الهاتف {needleAngle > 0 ? 'يميناً' : 'يساراً'} بمقدار</span>
                <span className="font-mono font-bold text-amber-400">
                  {Math.abs(Math.round(needleAngle))}°
                </span>
              </div>
            )}
          </div>

          {/* Graphical Compass Rose */}
          <div className="relative w-64 h-64 md:w-72 md:h-72 rounded-full border-4 border-slate-800 bg-slate-950/90 shadow-2xl flex items-center justify-center p-4">
            {/* Compass Dial markings */}
            <div
              className="absolute inset-0 rounded-full transition-transform duration-300 flex items-center justify-center"
              style={{ transform: `rotate(${-deviceHeading}deg)` }}
            >
              {/* North Marker */}
              <div className="absolute top-2 text-rose-500 font-black text-sm tracking-wider">ش</div>
              {/* East */}
              <div className="absolute right-3 text-slate-400 font-bold text-xs">ق</div>
              {/* South */}
              <div className="absolute bottom-2 text-slate-400 font-bold text-xs">ج</div>
              {/* West */}
              <div className="absolute left-3 text-slate-400 font-bold text-xs">غ</div>

              {/* Tick marks */}
              {[0, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330].map((deg) => (
                <div
                  key={deg}
                  className="absolute w-0.5 h-full"
                  style={{ transform: `rotate(${deg}deg)` }}
                >
                  <div
                    className={`w-0.5 ${
                      deg % 90 === 0 ? 'h-3 bg-slate-400' : 'h-1.5 bg-slate-700'
                    }`}
                  />
                </div>
              ))}
            </div>

            {/* Qibla Direction Indicator / Pointer */}
            <div
              className="absolute inset-0 flex items-center justify-center pointer-events-none transition-transform duration-300"
              style={{ transform: `rotate(${qiblaData.bearing - deviceHeading}deg)` }}
            >
              <div className="w-1 h-full relative flex flex-col items-center justify-between">
                {/* Kaaba pointer tip */}
                <div className="w-8 h-8 -mt-2 bg-emerald-500 rounded-full flex items-center justify-center shadow-lg shadow-emerald-500/50 text-slate-950 font-black text-xs border-2 border-white">
                  🕋
                </div>
                {/* Opposite tail */}
                <div className="w-2 h-2 bg-emerald-500/30 rounded-full" />
              </div>
            </div>

            {/* Center Pivot Pin */}
            <div className="relative z-10 w-6 h-6 rounded-full bg-slate-800 border-2 border-slate-600 flex items-center justify-center shadow-md">
              <div className="w-2 h-2 rounded-full bg-emerald-400" />
            </div>
          </div>

          <p className="text-[11px] text-slate-500 mt-4 text-center max-w-sm">
            يحاكي هذا المخطط مستشعر الدوران <code className="text-slate-400">ROTATION_VECTOR</code> ومعادلات الانحراف المغناطيسي <code className="text-slate-400">GeomagneticField</code> المطبقة في نور السبيل.
          </p>
        </div>
      </div>
    </div>
  );
};
