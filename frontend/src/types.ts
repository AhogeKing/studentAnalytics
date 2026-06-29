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

export type ModelTrainMode = "quick" | "default";

export interface ModelTrainPayload {
  mode?: ModelTrainMode;
  activate?: boolean;
}

export interface ModelVersionUpdatePayload {
  versionNo?: string;
  version_no?: string;
}

export interface ModelVersionQuery {
  page_num?: number;
  page_size?: number;
  active?: boolean;
  start_time?: string;
  end_time?: string;
}

export interface ModelVersion {
  id?: number;
  modelVersionId?: number;
  model_version_id?: number;
  modelName?: string | null;
  model_name?: string | null;
  versionNo?: string | null;
  version_no?: string | null;
  algorithm?: string | null;
  accuracy?: number | string | null;
  precisionMacro?: number | string | null;
  precision_macro?: number | string | null;
  recallMacro?: number | string | null;
  recall_macro?: number | string | null;
  f1Macro?: number | string | null;
  f1_macro?: number | string | null;
  active?: boolean | null;
  is_active?: boolean | null;
  trainedAt?: string | null;
  trained_at?: string | null;
  trainingDurationMs?: number | null;
  training_duration_ms?: number | null;
  createdAt?: string | null;
  created_at?: string | null;
}

export interface ModelTrainResult extends ModelVersion {
  modelVersionId?: number;
  targetColumn?: string | null;
  target_column?: string | null;
  featureColumns?: unknown;
  feature_columns?: unknown;
  searchMode?: string | null;
  search_mode?: string | null;
  searchCandidates?: number | null;
  search_candidates?: number | null;
  trainRows?: number | null;
  train_rows?: number | null;
  testRows?: number | null;
  test_rows?: number | null;
  bestParameters?: unknown;
  best_parameters?: unknown;
  aucOvrMacro?: number | string | null;
  auc_ovr_macro?: number | string | null;
  confusionMatrix?: unknown;
  confusion_matrix?: unknown;
  modelPath?: string | null;
  model_path?: string | null;
}

export interface ModelVersionDetail extends ModelTrainResult {
  criterion?: string | null;
  maxDepth?: number | null;
  max_depth?: number | null;
  minSamplesLeaf?: number | null;
  min_samples_leaf?: number | null;
  metrics?: unknown;
}

export interface PredictionPayload {
  model_version_id?: number;
  modelVersionId?: number;
  generate_warning?: boolean;
  generateWarning?: boolean;
}

export type PredictionDatasetSplit = "TRAIN" | "TEST" | "NEW" | "UNKNOWN";

export interface PredictionEligibility {
  studentNo?: number;
  student_no?: number;
  modelVersionId?: number;
  model_version_id?: number;
  modelVersionNo?: string | null;
  model_version_no?: string | null;
  datasetSplit?: PredictionDatasetSplit | string | null;
  dataset_split?: PredictionDatasetSplit | string | null;
  datasetSplitLabel?: string | null;
  dataset_split_label?: string | null;
  canPredict?: boolean | null;
  can_predict?: boolean | null;
  reason?: string | null;
}

export interface PredictionProbability {
  gradeClass?: OptionVO<number>;
  grade_class?: OptionVO<number>;
  gradeLabel?: string | null;
  grade_label?: string | null;
  probability: number | string;
}

export interface ImportantFactor {
  feature?: string | null;
  label?: string | null;
  value?: unknown;
  importance?: number | string | null;
}

export interface PredictionResult {
  predictionResultId?: number;
  prediction_result_id?: number;
  modelVersionId?: number;
  model_version_id?: number;
  modelVersionNo?: string | null;
  model_version_no?: string | null;
  predictedGradeClass?: OptionVO<number>;
  predicted_grade_class?: OptionVO<number>;
  predictedGradeLabel?: string | null;
  predicted_grade_label?: string | null;
  datasetSplit?: PredictionDatasetSplit | string | null;
  dataset_split?: PredictionDatasetSplit | string | null;
  datasetSplitLabel?: string | null;
  dataset_split_label?: string | null;
  probabilities?: PredictionProbability[];
  importantFactors?: ImportantFactor[];
  important_factors?: ImportantFactor[];
  predictInput?: unknown;
  predict_input?: unknown;
  createdAt?: string | null;
  created_at?: string | null;
}

export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type WarningStatus = "UNPROCESSED" | "PROCESSING" | "DONE" | "IGNORED";

export interface WarningRecord {
  id: number;
  studentNo?: number;
  student_no?: number;
  studentName?: string | null;
  student_name?: string | null;
  classInfo?: ClassInfo | null;
  class_info?: ClassInfo | null;
  predictionResultId?: number | null;
  prediction_result_id?: number | null;
  riskScore?: number | null;
  risk_score?: number | null;
  riskLevel?: RiskLevel | string | null;
  risk_level?: RiskLevel | string | null;
  riskLevelLabel?: string | null;
  risk_level_label?: string | null;
  riskReasons?: string[];
  risk_reasons?: string[];
  suggestions?: string[];
  status?: WarningStatus | string | null;
  statusLabel?: string | null;
  status_label?: string | null;
  createdAt?: string | null;
  created_at?: string | null;
  updatedAt?: string | null;
  updated_at?: string | null;
}

export interface WarningDetail extends WarningRecord {
  modelVersionId?: number | null;
  model_version_id?: number | null;
  modelVersionNo?: string | null;
  model_version_no?: string | null;
  predictedGradeClass?: OptionVO<number>;
  predicted_grade_class?: OptionVO<number>;
  predictedGradeLabel?: string | null;
  predicted_grade_label?: string | null;
  handlerUserId?: number | null;
  handler_user_id?: number | null;
  handlerUsername?: string | null;
  handler_username?: string | null;
  handlerRealName?: string | null;
  handler_real_name?: string | null;
}

export interface StudentPrediction {
  studentNo?: number;
  student_no?: number;
  name?: string | null;
  classInfo?: ClassInfo | null;
  class_info?: ClassInfo | null;
  prediction?: PredictionResult | null;
  warning?: WarningRecord | null;
}

export interface WarningQuery {
  page_num?: number;
  page_size?: number;
  student_no?: number;
  student_name?: string;
  grade_level?: number;
  class_name?: string;
  risk_level?: RiskLevel | string;
  status?: WarningStatus | string;
  start_time?: string;
  end_time?: string;
}
