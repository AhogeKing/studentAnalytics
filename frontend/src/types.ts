export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export interface RegisterPayload extends LoginPayload {
  realName?: string;
  role?: string;
}

export interface UserProfile {
  token?: string;
  userId?: number;
  user_id?: number;
  userName?: string;
  user_name?: string;
  realName?: string;
  real_name?: string;
  role?: string;
}

export interface OptionVO<T> {
  value: T | null;
  label: string;
}

export interface ClassInfo {
  grade_level: number;
  class_name: string;
  raw_class_name: string;
}

export interface StudentOverviewItem {
  student_no: number;
  name: string;
  age: number;
  gender: OptionVO<number>;
  grade_level: number;
  class_info: ClassInfo;
  gpa: number;
  grade_class: OptionVO<number>;
  update_time: string;
}

export interface StudentOverview {
  class_name?: string;
  raw_class_name?: string;
  class_names?: string[];
  keyword?: string;
  min_gpa?: number;
  max_gpa?: number;
  grade_class?: number;
  grade_level?: number;
  gender?: number;
  sortField?: string;
  sortOrder?: string;
  student_count: number;
  students: StudentOverviewItem[];
}

export interface StudentFilterOptions {
  classes: ClassInfo[];
  genders: OptionVO<number>[];
  gradeClasses: OptionVO<number>[];
  min_gpa: number;
  max_gpa: number;
}

export interface StudentOverviewQuery {
  class_name?: string;
  class_names?: string[];
  keyword?: string;
  min_gpa?: number;
  max_gpa?: number;
  grade_class?: number;
  grade_level?: number;
  gender?: number;
  sort_field?: string;
  sort_order?: string;
}

export interface StudentOverviewUpdatePayload {
  name?: string;
  age?: number;
  gender?: number;
  class_name?: string;
  gpa?: number;
}

export interface StudentEditableItem {
  student_no: number;
  name: string;
  age: number;
  gender: OptionVO<number>;
  class_info: ClassInfo;
  gpa?: number | null;
}

export type StudentQueryMode = "filter" | "search";

export interface StudentFilterForm {
  class_name?: string;
  min_gpa?: number;
  max_gpa?: number;
  grade_class?: number;
  grade_level?: number;
  gender?: number;
}

export interface StudentSearchForm {
  keyword?: string;
}

export interface StudentBasicInfo {
  student_no: number;
  name: string;
  age: number;
  gender: OptionVO<number>;
  grade_level: number;
  class_info: ClassInfo;
}

export interface AcademicPerformance {
  gpa?: number | null;
  grade_class?: OptionVO<number> | null;
  study_time_weekly?: number | null;
  absences?: number | null;
}

export interface SupportStatus {
  tutoring?: boolean | null;
  parental_support?: OptionVO<number> | null;
}

export interface ActivityProfile {
  extracurricular?: boolean | null;
  sports?: boolean | null;
  music?: boolean | null;
  volunteering?: boolean | null;
}

export interface StudentDetail {
  basic_info: StudentBasicInfo;
  academic_performance?: AcademicPerformance | null;
  support_status?: SupportStatus | null;
  activity_profile?: ActivityProfile | null;
  performance_available: boolean;
}

export interface GpaDistributionItem {
  bucketIndex?: number;
  bucket_index?: number;
  label: string;
  minGpa?: number;
  min_gpa?: number;
  maxGpa?: number;
  max_gpa?: number;
  studentCount?: number;
  student_count?: number;
  percentage: number;
}

export interface GradeClassDistributionItem {
  gradeClass?: OptionVO<number>;
  grade_class?: OptionVO<number>;
  studentCount?: number;
  student_count?: number;
  percentage: number;
}

export interface PerformanceAnalysisPoint {
  studentNo?: number;
  student_no?: number;
  name: string;
  classInfo?: ClassInfo;
  class_info?: ClassInfo;
  studyTimeWeekly?: number;
  study_time_weekly?: number;
  absences: number;
  gpa: number;
  gradeClass?: OptionVO<number>;
  grade_class?: OptionVO<number>;
  gpaBucket?: OptionVO<number>;
  gpa_bucket?: OptionVO<number>;
}

export interface NormalizedGpaDistributionItem {
  bucketIndex: number;
  label: string;
  minGpa: number;
  maxGpa: number;
  studentCount: number;
  percentage: number;
}

export interface NormalizedGradeClassDistributionItem {
  gradeClass: OptionVO<number>;
  studentCount: number;
  percentage: number;
}

export interface NormalizedPerformanceAnalysisPoint {
  studentNo: number;
  name: string;
  classInfo?: ClassInfo | null;
  studyTimeWeekly: number;
  absences: number;
  gpa: number;
  gradeClass: OptionVO<number>;
  gpaBucket: OptionVO<number>;
}

export interface AnalysisScopeParams {
  grade_level?: number;
  class_name?: string[];
}

export type AnalysisScopeMode = "all" | "grade" | "classes";
export type AnalysisColorMode = "gradeClass" | "gpaBucket";
export type AnalysisDisplayMode = "dim" | "only";
