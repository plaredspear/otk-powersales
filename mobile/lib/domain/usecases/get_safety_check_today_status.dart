import '../entities/safety_check_today_status.dart';
import '../repositories/safety_check_repository.dart';

/// 오늘 안전점검 상태 조회 UseCase
///
/// 오늘 날짜의 안전점검 완료 여부를 확인합니다.
/// 홈화면에서 "등록" 버튼 클릭 시 안전점검 화면 표시 여부를 결정하는 데 사용됩니다.
class GetSafetyCheckTodayStatus {
  final SafetyCheckRepository _repository;

  GetSafetyCheckTodayStatus(this._repository);

  /// 오늘 안전점검 상태 조회 실행
  ///
  /// Returns: 오늘 안전점검 상태 (완료 여부 + 완료 시각)
  Future<SafetyCheckTodayStatus> call() async {
    return await _repository.getTodayStatus();
  }
}
