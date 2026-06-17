import '../entities/staff_evaluation.dart';

/// 여사원 평가조회 Repository 인터페이스
abstract class StaffEvaluationRepository {
  /// 여사원 평가조회
  ///
  /// [yearMonth]: 조회 연월 (YYYYMM 형식). null 이면 서버 기본값(전월).
  Future<StaffEvaluation> getStaffEvaluation({String? yearMonth});
}
