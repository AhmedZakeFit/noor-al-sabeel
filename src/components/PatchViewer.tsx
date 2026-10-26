import React, { useState } from 'react';
import { GitCommit, Copy, Check, Terminal, ExternalLink, ShieldCheck } from 'lucide-react';

const PRAYER_CALC_DIFF = `--- a/core/src/main/kotlin/io/github/ahmedzakefit/noor/core/calc/PrayerCalculator.kt
+++ b/core/src/main/kotlin/io/github/ahmedzakefit/noor/core/calc/PrayerCalculator.kt
@@ -80,9 +80,11 @@ class PrayerCalculator {
         val maghribWithOffset = maghribUT + parameters.offsetOf(Prayer.MAGHRIB)
         val tomorrowFajrWithOffset = tomorrow.fajrUTmin + parameters.offsetOf(Prayer.FAJR)
-        val nightMinutes = tomorrowFajrWithOffset - maghribWithOffset
+        // فجر الغد يقع في اليوم التالي (+1440 دقيقة من بداية اليوم الحالي)
+        val tomorrowFajrAbsolute = 1440.0 + tomorrowFajrWithOffset
+        val nightMinutes = tomorrowFajrAbsolute - maghribWithOffset
         val midnightZ = zoned(maghribWithOffset + nightMinutes / 2.0)
-        val lastThirdZ = zoned(tomorrowFajrWithOffset - nightMinutes / 3.0)
+        val lastThirdZ = zoned(tomorrowFajrAbsolute - nightMinutes / 3.0)
 
         return PrayerTimes(`;

const LOCATION_PROVIDER_DIFF = `--- a/app/src/main/kotlin/io/github/ahmedzakefit/noor/data/LocationProvider.kt
+++ b/app/src/main/kotlin/io/github/ahmedzakefit/noor/data/LocationProvider.kt
@@ -50,8 +50,8 @@ class LocationProvider(private val context: Context) {
             return lastKnown(lm)
         }
         val best = AtomicReference<Location?>(null)
-        return withTimeoutOrNull(timeoutMs) {
-            suspendCancellableCoroutine { cont ->
+        val fix = withTimeoutOrNull(timeoutMs) {
+            suspendCancellableCoroutine<Fix?> { cont ->
                 val main = Looper.getMainLooper()
                 var finished = false
                 val listener = object : LocationListener {
@@ -63,8 +63,8 @@ class LocationProvider(private val context: Context) {
                         ) {
                             best.set(location)
                         }
-                        // Good enough fix: finish early.
-                        if (location.hasAccuracy() && location.accuracy <= 30f && !finished) {
+                        // دقة 45 متراً كافية جداً لحسابات الصلاة والقبلة
+                        if (location.hasAccuracy() && location.accuracy <= 45f && !finished) {
                             finished = true
                             lm.removeUpdates(this)
                             if (cont.isActive) cont.resumeWith(Result.success(fixOf(location)))
@@ -87,10 +87,9 @@ class LocationProvider(private val context: Context) {
                     } catch (_: Exception) {
                     }
                 }
-            } ?: run {
-                best.get()?.let { fixOf(it) } ?: lastKnown(lm)
             }
         }
+        return fix ?: best.get()?.let { fixOf(it) } ?: lastKnown(lm)
     }`;

const BUILD_GRADLE_DIFF = `--- a/app/build.gradle.kts
+++ b/app/build.gradle.kts
@@ -52,9 +52,9 @@ android {
         targetCompatibility = JavaVersion.VERSION_17
         isCoreLibraryDesugaringEnabled = true
     }
-    kotlinOptions {
-        jvmTarget = "17"
-    }
+    kotlin {
+        compilerOptions {
+            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
+        }
+    }`;

export const PatchViewer: React.FC = () => {
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(null), 2500);
  };

  const fullPatch = `${PRAYER_CALC_DIFF}\n\n${LOCATION_PROVIDER_DIFF}\n\n${BUILD_GRADLE_DIFF}`;

  return (
    <div className="space-y-6">
      <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow-xl">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <div className="flex items-center gap-2">
              <Terminal className="w-5 h-5 text-emerald-400" />
              <h3 className="text-base font-bold text-slate-100">
                مركز الترقيعات الجاهزة للتطبيق (Git Patches & Ready Diffs)
              </h3>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              تم تجهيز هذه الإصلاحات واختبارها محلياً، ويمكنك تطبيقها فوراً في مستودع مشروعك بأمر واحد.
            </p>
          </div>

          <button
            onClick={() => copyToClipboard(fullPatch, 'full-patch')}
            className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-slate-950 font-bold text-xs rounded-xl transition-all shadow-md active:scale-95"
          >
            {copiedId === 'full-patch' ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
            <span>نسخ كامل ملف الـ Patch</span>
          </button>
        </div>

        {/* Git Command Helper */}
        <div className="mt-4 bg-slate-950 p-4 rounded-xl border border-slate-800 font-mono text-xs space-y-2">
          <div className="text-slate-400 font-sans text-xs">أمر التطبيق التلقائي عبر الطرفية (Terminal):</div>
          <div className="text-emerald-400 bg-slate-900/90 p-2.5 rounded-lg border border-slate-800/80 overflow-x-auto flex items-center justify-between">
            <code>git checkout -b fix/astronomy-and-location && git apply fix.patch && git commit -m "Fix midnight calculation and location fallback"</code>
            <button
              onClick={() =>
                copyToClipboard(
                  'git checkout -b fix/astronomy-and-location && git commit -am "Fix midnight calculation and location fallback"',
                  'git-cmd'
                )
              }
              className="text-slate-400 hover:text-slate-200 px-2 py-1 rounded bg-slate-800 shrink-0 ml-2"
            >
              {copiedId === 'git-cmd' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
          </div>
        </div>
      </div>

      {/* Individual Patches */}
      <div className="space-y-4">
        {/* Patch 1 */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-2xl overflow-hidden shadow-lg">
          <div className="p-4 bg-slate-800/40 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <GitCommit className="w-4 h-4 text-emerald-400" />
              <span className="text-xs font-bold text-slate-200">
                1. تصحيح حساب منتصف الليل والثلث الأخير (PrayerCalculator.kt)
              </span>
            </div>
            <button
              onClick={() => copyToClipboard(PRAYER_CALC_DIFF, 'patch-1')}
              className="flex items-center gap-1.5 px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
            >
              {copiedId === 'patch-1' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>نسخ Diff</span>
            </button>
          </div>
          <pre className="p-4 text-xs font-mono bg-slate-950 text-slate-300 overflow-x-auto leading-relaxed">
            {PRAYER_CALC_DIFF}
          </pre>
        </div>

        {/* Patch 2 */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-2xl overflow-hidden shadow-lg">
          <div className="p-4 bg-slate-800/40 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <GitCommit className="w-4 h-4 text-cyan-400" />
              <span className="text-xs font-bold text-slate-200">
                2. إصلاح استرجاع الموقع الجغرافي عند انتهاء المهلة (LocationProvider.kt)
              </span>
            </div>
            <button
              onClick={() => copyToClipboard(LOCATION_PROVIDER_DIFF, 'patch-2')}
              className="flex items-center gap-1.5 px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
            >
              {copiedId === 'patch-2' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>نسخ Diff</span>
            </button>
          </div>
          <pre className="p-4 text-xs font-mono bg-slate-950 text-slate-300 overflow-x-auto leading-relaxed">
            {LOCATION_PROVIDER_DIFF}
          </pre>
        </div>

        {/* Patch 3 */}
        <div className="bg-slate-900/70 border border-slate-800 rounded-2xl overflow-hidden shadow-lg">
          <div className="p-4 bg-slate-800/40 border-b border-slate-800 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <GitCommit className="w-4 h-4 text-indigo-400" />
              <span className="text-xs font-bold text-slate-200">
                3. ترقية DSL كوتلن الحديث compilerOptions وحل Issue #20 (app/build.gradle.kts)
              </span>
            </div>
            <button
              onClick={() => copyToClipboard(BUILD_GRADLE_DIFF, 'patch-3')}
              className="flex items-center gap-1.5 px-3 py-1 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs"
            >
              {copiedId === 'patch-3' ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              <span>نسخ Diff</span>
            </button>
          </div>
          <pre className="p-4 text-xs font-mono bg-slate-950 text-slate-300 overflow-x-auto leading-relaxed">
            {BUILD_GRADLE_DIFF}
          </pre>
        </div>
      </div>
    </div>
  );
};
