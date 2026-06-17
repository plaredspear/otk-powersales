import '../models/staff_evaluation_model.dart';

/// 여사원 평가조회 원격 데이터소스 인터페이스
abstract class StaffEvaluationRemoteDataSource {
  /// 여사원 평가조회 API 호출
  ///
  /// GET /api/v1/mobile/staff-evaluation
  ///
  /// [yearMonth]: 조회 연월 (YYYYMM). null 이면 서버 기본값(전월).
  Future<StaffEvaluationModel> getStaffEvaluation({String? yearMonth});
}
