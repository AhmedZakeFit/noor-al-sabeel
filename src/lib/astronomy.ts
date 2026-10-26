import { CalculationMethodId, MethodConfig, CityPreset, AsrSchool, ComputedTimes } from '../types';

export const METHOD_CONFIGS: Record<CalculationMethodId, MethodConfig> = {
  EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY: {
    id: 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY',
    nameAr: 'الهيئة المصرية العامة للمساحة',
    nameEn: 'Egyptian General Authority of Survey',
    fajrAngle: 19.5,
    ishaAngle: 17.5,
    descriptionAr: 'المعتمد في مصر وشمال إفريقيا وبلاد الشام (فجر 19.5°، عشاء 17.5°)',
  },
  UMM_AL_QURA: {
    id: 'UMM_AL_QURA',
    nameAr: 'جامعة أم القرى (مكة المكرمة)',
    nameEn: 'Umm Al-Qura University, Makkah',
    fajrAngle: 18.5,
    ishaMinutesAfterMaghrib: 90,
    descriptionAr: 'المعتمد في المملكة العربية السعودية (فجر 18.5°، العشاء 90 دقيقة بعد المغرب و120 في رمضان)',
  },
  MUSLIM_WORLD_LEAGUE: {
    id: 'MUSLIM_WORLD_LEAGUE',
    nameAr: 'رابطة العالم الإسلامي',
    nameEn: 'Muslim World League',
    fajrAngle: 18.0,
    ishaAngle: 17.0,
    descriptionAr: 'معتمد في أوروبا والعديد من دول العالم الإسلامي (فجر 18°، عشاء 17°)',
  },
  ISNA: {
    id: 'ISNA',
    nameAr: 'الجمعية الإسلامية لأمريكا الشمالية (ISNA)',
    nameEn: 'Islamic Society of North America',
    fajrAngle: 15.0,
    ishaAngle: 15.0,
    descriptionAr: 'معتمد في أمريكا الشمالية والولايات المتحدة وكندا (فجر 15°، عشاء 15°)',
  },
  KARACHI: {
    id: 'KARACHI',
    nameAr: 'جامعة العلوم الإسلامية بكراتشي',
    nameEn: 'University of Islamic Sciences, Karachi',
    fajrAngle: 18.0,
    ishaAngle: 18.0,
    descriptionAr: 'معتمد في باكستان والهند وبنغلاديش وأفغانستان (فجر 18°، عشاء 18°)',
  },
  GULF_REGION: {
    id: 'GULF_REGION',
    nameAr: 'مجلس التعاون لدول الخليج',
    nameEn: 'Gulf Region',
    fajrAngle: 19.5,
    ishaMinutesAfterMaghrib: 90,
    descriptionAr: 'معتمد في بعض دول الخليج كالإمارات وعمان (فجر 19.5°، العشاء 90 دقيقة)',
  },
};

export const CITIES: CityPreset[] = [
  {
    nameAr: 'القاهرة',
    nameEn: 'Cairo',
    country: 'مصر',
    latitude: 30.0444,
    longitude: 31.2357,
    timeZoneId: 'Africa/Cairo',
    defaultMethod: 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY',
  },
  {
    nameAr: 'الإسكندرية',
    nameEn: 'Alexandria',
    country: 'مصر',
    latitude: 31.2001,
    longitude: 29.9187,
    timeZoneId: 'Africa/Cairo',
    defaultMethod: 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY',
  },
  {
    nameAr: 'المنصورة',
    nameEn: 'Mansoura',
    country: 'مصر',
    latitude: 31.0409,
    longitude: 31.3785,
    timeZoneId: 'Africa/Cairo',
    defaultMethod: 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY',
  },
  {
    nameAr: 'أسوان',
    nameEn: 'Aswan',
    country: 'مصر',
    latitude: 24.0889,
    longitude: 32.8998,
    timeZoneId: 'Africa/Cairo',
    defaultMethod: 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY',
  },
  {
    nameAr: 'مكة المكرمة',
    nameEn: 'Makkah',
    country: 'السعودية',
    latitude: 21.4225,
    longitude: 39.8262,
    timeZoneId: 'Asia/Riyadh',
    defaultMethod: 'UMM_AL_QURA',
  },
  {
    nameAr: 'المدينة المنورة',
    nameEn: 'Madinah',
    country: 'السعودية',
    latitude: 24.5247,
    longitude: 39.5692,
    timeZoneId: 'Asia/Riyadh',
    defaultMethod: 'UMM_AL_QURA',
  },
  {
    nameAr: 'الرياض',
    nameEn: 'Riyadh',
    country: 'السعودية',
    latitude: 24.7136,
    longitude: 46.6753,
    timeZoneId: 'Asia/Riyadh',
    defaultMethod: 'UMM_AL_QURA',
  },
  {
    nameAr: 'القدس الشريف',
    nameEn: 'Jerusalem',
    country: 'فلسطين',
    latitude: 31.7683,
    longitude: 35.2137,
    timeZoneId: 'Asia/Jerusalem',
    defaultMethod: 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY',
  },
  {
    nameAr: 'دبي',
    nameEn: 'Dubai',
    country: 'الإمارات',
    latitude: 25.2048,
    longitude: 55.2708,
    timeZoneId: 'Asia/Dubai',
    defaultMethod: 'GULF_REGION',
  },
  {
    nameAr: 'لندن',
    nameEn: 'London',
    country: 'المملكة المتحدة',
    latitude: 51.5074,
    longitude: -0.1278,
    timeZoneId: 'Europe/London',
    defaultMethod: 'MUSLIM_WORLD_LEAGUE',
  },
  {
    nameAr: 'نيويورك',
    nameEn: 'New York',
    country: 'الولايات المتحدة',
    latitude: 40.7128,
    longitude: -74.006,
    timeZoneId: 'America/New_York',
    defaultMethod: 'ISNA',
  },
];

export const KAABA_COORDS = {
  latitude: 21.4225,
  longitude: 39.8262,
};

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}

function toDeg(rad: number): number {
  return (rad * 180) / Math.PI;
}

function normalizeDegrees(deg: number): number {
  let v = deg % 360;
  if (v < 0) v += 360;
  return v;
}

export function julianDayAtNoonUT(year: number, month: number, day: number): number {
  let y = year;
  let m = month;
  if (m <= 2) {
    y -= 1;
    m += 12;
  }
  const a = Math.floor(y / 100);
  const b = 2 - a + Math.floor(a / 4);
  return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5 + 0.5;
}

export function solarCoordinates(jd: number): { declinationDeg: number; equationOfTimeMinutes: number } {
  const t = (jd - 2451545.0) / 36525.0;
  const l0Deg = normalizeDegrees(280.46645 + 36000.76983 * t + 0.0003032 * t * t);
  const mRad = toRad(357.5291 + 35999.0503 * t - 0.0001559 * t * t - 0.00000048 * t * t * t);
  const e = 0.016708617 - 0.000042037 * t - 0.0000001236 * t * t;

  const cDeg =
    (1.914602 - 0.004817 * t - 0.000014 * t * t) * Math.sin(mRad) +
    (0.019993 - 0.000101 * t) * Math.sin(2 * mRad) +
    0.00029 * Math.sin(3 * mRad);
  const trueLongDeg = l0Deg + cDeg;
  const omegaRad = toRad(125.04 - 1934.136 * t);
  const lambdaDeg = trueLongDeg - 0.00569 - 0.00478 * Math.sin(omegaRad);

  const eps0Deg =
    23.0 +
    26.0 / 60.0 +
    21.448 / 3600.0 -
    (46.815 * t + 0.00059 * t * t - 0.001813 * t * t * t) / 3600.0;
  const epsDeg = eps0Deg + 0.00256 * Math.cos(omegaRad);
  const declinationDeg = toDeg(Math.asin(Math.sin(toRad(epsDeg)) * Math.sin(toRad(lambdaDeg))));

  const y = Math.tan(toRad(epsDeg) / 2.0) ** 2;
  const eotRad =
    y * Math.sin(2 * toRad(l0Deg)) -
    2 * e * Math.sin(mRad) +
    4 * e * y * Math.sin(mRad) * Math.cos(2 * toRad(l0Deg)) -
    0.5 * y * y * Math.sin(4 * toRad(l0Deg)) -
    1.25 * e * e * Math.sin(2 * mRad);
  const equationOfTimeMinutes = 4.0 * toDeg(eotRad);

  return { declinationDeg, equationOfTimeMinutes };
}

export function hourAngleHours(latitudeDeg: number, declinationDeg: number, altitudeDeg: number): number | null {
  const lat = toRad(latitudeDeg);
  const dec = toRad(declinationDeg);
  const alt = toRad(altitudeDeg);
  const cosH = (Math.sin(alt) - Math.sin(lat) * Math.sin(dec)) / (Math.cos(lat) * Math.cos(dec));
  if (cosH < -1.0 || cosH > 1.0) return null;
  return toDeg(Math.acos(cosH)) / 15.0;
}

export function asrAltitudeDeg(latitudeDeg: number, declinationDeg: number, shadowFactor: number): number {
  const delta = Math.abs(latitudeDeg - declinationDeg);
  return toDeg(Math.atan(1.0 / (shadowFactor + Math.tan(toRad(delta)))));
}

interface RawDayTimes {
  transitUTmin: number;
  fajrUTmin: number;
  sunriseUTmin: number;
  sunsetUTmin: number;
  asrUTmin: number;
  ishaUTmin: number;
  highLatAdjusted: boolean;
}

function computeRawDay(
  year: number,
  month: number,
  day: number,
  latitude: number,
  longitude: number,
  methodConfig: MethodConfig,
  asrFactor: number
): RawDayTimes {
  const jd0 = julianDayAtNoonUT(year, month, day);
  let sc = solarCoordinates(jd0);
  let transit = 720.0 - 4.0 * longitude - sc.equationOfTimeMinutes;
  sc = solarCoordinates(jd0 + (transit - 720.0) / 1440.0);
  transit = 720.0 - 4.0 * longitude - sc.equationOfTimeMinutes;

  const decl = sc.declinationDeg;
  const riseSetAltDeg = -0.833; // standard refraction + semi-diameter

  let sunrise: number | null = null;
  let sunset: number | null = null;
  const riseSetH = hourAngleHours(latitude, decl, riseSetAltDeg);
  if (riseSetH !== null) {
    sunrise = transit - riseSetH * 60.0;
    sunset = transit + riseSetH * 60.0;
  }

  let fajr: number | null = null;
  const fajrH = hourAngleHours(latitude, decl, -methodConfig.fajrAngle);
  if (fajrH !== null) {
    fajr = transit - fajrH * 60.0;
  }

  let asr: number | null = null;
  const asrAlt = asrAltitudeDeg(latitude, decl, asrFactor);
  const asrH = hourAngleHours(latitude, decl, asrAlt);
  if (asrH !== null) {
    asr = transit + asrH * 60.0;
  }

  let ishaByAngle: number | null = null;
  if (methodConfig.ishaMinutesAfterMaghrib == null && methodConfig.ishaAngle != null) {
    const ishaH = hourAngleHours(latitude, decl, -methodConfig.ishaAngle);
    if (ishaH !== null) {
      ishaByAngle = transit + ishaH * 60.0;
    }
  }

  let highLatAdjusted = false;
  if (sunrise === null || sunset === null) {
    sunrise = transit - 720.0;
    sunset = transit + 720.0;
    highLatAdjusted = true;
  }

  const nightHours = (1440.0 - (sunset - sunrise)) / 60.0;
  const fajrPortion = methodConfig.fajrAngle / 60.0;
  const ishaPortion = (methodConfig.ishaAngle ?? methodConfig.fajrAngle) / 60.0;
  const safeFajr = sunrise - fajrPortion * nightHours * 60.0;
  const safeIsha = sunset + ishaPortion * nightHours * 60.0;

  if (fajr === null || fajr < safeFajr) {
    fajr = safeFajr;
    highLatAdjusted = true;
  }
  if (ishaByAngle === null || ishaByAngle > safeIsha) {
    ishaByAngle = safeIsha;
    highLatAdjusted = true;
  }

  if (asr === null) {
    asr = transit + (sunset - transit) / 2.0;
    highLatAdjusted = true;
  }

  const ishaUTmin =
    methodConfig.ishaMinutesAfterMaghrib != null
      ? sunset + methodConfig.ishaMinutesAfterMaghrib
      : ishaByAngle!;

  return {
    transitUTmin: transit,
    fajrUTmin: fajr,
    sunriseUTmin: sunrise,
    sunsetUTmin: sunset,
    asrUTmin: asr,
    ishaUTmin: ishaUTmin,
    highLatAdjusted,
  };
}

export function computePrayerTimes(
  date: Date,
  latitude: number,
  longitude: number,
  methodId: CalculationMethodId,
  school: AsrSchool = 'standard'
): ComputedTimes {
  const method = METHOD_CONFIGS[methodId];
  const asrFactor = school === 'hanafi' ? 2 : 1;

  const year = date.getUTCFullYear();
  const month = date.getUTCMonth() + 1;
  const day = date.getUTCDate();

  const today = computeRawDay(year, month, day, latitude, longitude, method, asrFactor);

  // Compute tomorrow for Fajr
  const tomorrowDate = new Date(Date.UTC(year, month - 1, day + 1));
  const tomorrow = computeRawDay(
    tomorrowDate.getUTCFullYear(),
    tomorrowDate.getUTCMonth() + 1,
    tomorrowDate.getUTCDate(),
    latitude,
    longitude,
    method,
    asrFactor
  );

  const baseUTCMillis = Date.UTC(year, month - 1, day, 0, 0, 0, 0);

  const makeDate = (utMinutes: number): Date => {
    return new Date(baseUTCMillis + Math.round(utMinutes * 60 * 1000));
  };

  const fajrDate = makeDate(today.fajrUTmin);
  const sunriseDate = makeDate(today.sunriseUTmin);
  const dhuhrDate = makeDate(today.transitUTmin);
  const asrDate = makeDate(today.asrUTmin);
  const maghribDate = makeDate(today.sunsetUTmin);
  const ishaDate = makeDate(today.ishaUTmin);

  // FIXED CALCULATION FOR ISLAMIC MIDNIGHT & LAST THIRD OF NIGHT
  // Tomorrow's Fajr is relative to tomorrow's midnight, so add 1440 minutes relative to today's base!
  const tomorrowFajrAbsolute = 1440.0 + tomorrow.fajrUTmin;
  const nightDurationMinutes = tomorrowFajrAbsolute - today.sunsetUTmin;

  // Islamic Midnight = Maghrib + (Night / 2)
  const midnightDate = makeDate(today.sunsetUTmin + nightDurationMinutes / 2.0);

  // Last Third of Night = Tomorrow Fajr - (Night / 3)
  const lastThirdDate = makeDate(tomorrowFajrAbsolute - nightDurationMinutes / 3.0);

  return {
    fajr: fajrDate,
    sunrise: sunriseDate,
    dhuhr: dhuhrDate,
    asr: asrDate,
    maghrib: maghribDate,
    isha: ishaDate,
    midnight: midnightDate,
    lastThirdOfNight: lastThirdDate,
    nightDurationMinutes: Math.round(nightDurationMinutes),
    isHighLatitudeAdjusted: today.highLatAdjusted,
  };
}

// Qibla Math
export function computeQibla(latitude: number, longitude: number): { bearing: number; distanceKm: number } {
  const phi1 = toRad(latitude);
  const phi2 = toRad(KAABA_COORDS.latitude);
  const deltaLambda = toRad(KAABA_COORDS.longitude - longitude);

  const theta = Math.atan2(
    Math.sin(deltaLambda),
    Math.cos(phi1) * Math.tan(phi2) - Math.sin(phi1) * Math.cos(deltaLambda)
  );
  const bearing = normalizeDegrees(toDeg(theta));

  // Haversine distance
  const r = 6371.0088; // Earth radius in km
  const dLat = toRad(KAABA_COORDS.latitude - latitude);
  const dLon = toRad(KAABA_COORDS.longitude - longitude);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLon / 2) ** 2;
  const distanceKm = 2 * r * Math.asin(Math.sqrt(a));

  return { bearing, distanceKm };
}

// Simple Umm Al-Qura month approximation
export function getHijriDate(date: Date): { year: number; monthNameAr: string; day: number } {
  // Approximate conversion for display
  const months = [
    'محرم',
    'صفر',
    'ربيع الأول',
    'ربيع الآخر',
    'جمادى الأولى',
    'جمادى الآخرة',
    'رجب',
    'شعبان',
    'رمضان',
    'شوال',
    'ذو القعدة',
    'ذو الحجة',
  ];

  const intl = new Intl.DateTimeFormat('ar-SA-u-ca-islamic-umalqura', {
    day: 'numeric',
    month: 'numeric',
    year: 'numeric',
  });
  
  try {
    const parts = intl.formatToParts(date);
    const day = parseInt(parts.find(p => p.type === 'day')?.value || '1', 10);
    const mNum = parseInt(parts.find(p => p.type === 'month')?.value || '1', 10);
    const year = parseInt(parts.find(p => p.type === 'year')?.value || '1448', 10);
    const monthNameAr = months[(mNum - 1 + 12) % 12];
    return { year, monthNameAr, day };
  } catch {
    return { year: 1448, monthNameAr: 'جمادى الأولى', day: 15 };
  }
}
