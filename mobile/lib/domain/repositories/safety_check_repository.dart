import '../entities/safety_check_category.dart';
import '../entities/safety_check_today_status.dart';
import '../entities/safety_check_submit_result.dart';

/// 안전점검 Repository 인터페이스
///
/// 안전점검 체크리스트 조회, 오늘 점검 상태 확인, 안전점검 제출 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class SafetyCheckRepository {
  /// 안전점검 체크리스트 항목 조회
  ///
  /// GET /api/v1/safety-check/items
  ///
  /// Returns: 카테고리 목록 (각 카테고리에 체크 항목 포함)
  Future<List<SafetyCheckCategory>> getItems();

  /// 오늘 안전점검 완료 여부 조회
  ///
  /// GET /api/v1/safety-check/today
  ///
  /// Returns: 오늘 안전점검 상태 (완료 여부 + 완료 시각)
  Future<SafetyCheckTodayStatus> getTodayStatus();

  /// 안전점검 제출
  ///
  /// POST /api/v1/safety-check/submit
  ///
  /// [checkedItemIds]: 체크된 항목 ID 목록
  ///
  /// Returns: 제출 결과 (제출 ID, 제출 시각, 완료 여부)
  Future<SafetyCheckSubmitResult> submit(List<int> checkedItemIds);
}
