export type CalculationMethodId =
  | 'EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY'
  | 'UMM_AL_QURA'
  | 'MUSLIM_WORLD_LEAGUE'
  | 'ISNA'
  | 'KARACHI'
  | 'GULF_REGION';

export interface MethodConfig {
  id: CalculationMethodId;
  nameAr: string;
  nameEn: string;
  fajrAngle: number;
  ishaAngle?: number;
  ishaMinutesAfterMaghrib?: number;
  descriptionAr: string;
}

export interface CityPreset {
  nameAr: string;
  nameEn: string;
  country: string;
  latitude: number;
  longitude: number;
  timeZoneId: string;
  defaultMethod: CalculationMethodId;
}

export type AsrSchool = 'standard' | 'hanafi';

export interface ComputedTimes {
  fajr: Date;
  sunrise: Date;
  dhuhr: Date;
  asr: Date;
  maghrib: Date;
  isha: Date;
  midnight: Date;
  lastThirdOfNight: Date;
  nightDurationMinutes: number;
  isHighLatitudeAdjusted: boolean;
}

export interface AuditIssue {
  id: string;
  titleAr: string;
  titleEn: string;
  file: string;
  severity: 'critical' | 'high' | 'medium' | 'improvement';
  status: 'fixed' | 'recommendation' | 'security_alert';
  categoryAr: string;
  summaryAr: string;
  detailsAr: string;
  rootCauseAr: string;
  fixDescriptionAr: string;
  oldCodeSnippet?: string;
  newCodeSnippet?: string;
}
