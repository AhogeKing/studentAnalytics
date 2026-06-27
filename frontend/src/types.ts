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

export interface PageResult<T> {
  total: number;
  records: T[];
}

export interface AdminUser {
  id: number;
  username: string;
  real_name?: string | null;
  realName?: string | null;
  role: "ADMIN" | "TEACHER" | string;
  status: number;
  created_at?: string | null;
  updated_at?: string | null;
}

export interface AdminUserCreatePayload {
  username: string;
  password: string;
  real_name?: string;
  role: "ADMIN" | "TEACHER";
}

export interface AdminUserUpdatePayload {
  real_name?: string;
  role?: "ADMIN" | "TEACHER";
  status?: number;
}

export interface AdminUserStatusPayload {
  status: number;
}

export interface AdminUserPasswordPayload {
  new_password: string;
}

export interface OptionVO<T> {
  value: T | null;
  label: string;
}

export interface OperationLog {
  id: number;
  userId?: number | null;
  user_id?: number | null;
  username?: string | null;
  realName?: string | null;
  real_name?: string | null;
  userRole?: string | null;
  user_role?: string | null;
  userRoleLabel?: string | null;
  user_role_label?: string | null;
  moduleName?: string | null;
  module_name?: string | null;
  moduleLabel?: string | null;
  module_label?: string | null;
  operationType?: string | null;
  operation_type?: string | null;
  operationTypeLabel?: string | null;
  operation_type_label?: string | null;
  operationResult?: string | null;
  operation_result?: string | null;
  operationResultLabel?: string | null;
  operation_result_label?: string | null;
  targetType?: string | null;
  target_type?: string | null;
  targetTypeLabel?: string | null;
  target_type_label?: string | null;
  targetId?: string | null;
  target_id?: string | null;
  businessKey?: string | null;
  business_key?: string | null;
  operationSummary?: string | null;
  operation_summary?: string | null;
  requestMethod?: string | null;
  request_method?: string | null;
  requestUri?: string | null;
  request_uri?: string | null;
  ipAddress?: string | null;
  ip_address?: string | null;
  requestParams?: string | null;
  request_params?: string | null;
  requestBody?: string | null;
  request_body?: string | null;
  createdAt?: string | null;
  created_at?: string | null;
}

export interface OperationLogQuery {
  page_num?: number;
  page_size?: number;
  username?: string;
  user_role?: string;
  module_name?: string;
  operation_type?: string;
  operation_result?: string;
  target_type?: string;
  target_id?: string;
  business_key?: string;
  start_time?: string;
  end_time?: string;
  keyword?: string;
}

export interface OperationLogOptions {
  modules: OptionVO<string>[];
  operationTypes?: OptionVO<string>[];
  operation_types?: OptionVO<string>[];
  results: OptionVO<string>[];
  roles: OptionVO<string>[];
  targetTypes?: OptionVO<string>[];
  target_types?: OptionVO<string>[];
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

export interface StudentCreatePayload {
  student_no: number;
  name?: string;
  age: number;
  gender: number;
  ethnicity: number;
  parental_education: number;
  class_name: string;
}

export interface StudentPerformanceUpsertPayload {
  study_time_weekly: number;
  absences: number;
  tutoring: boolean;
  parental_support: number;
  extracurricular: boolean;
  sports: boolean;
  music: boolean;
  volunteering: boolean;
  gpa: number;
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
