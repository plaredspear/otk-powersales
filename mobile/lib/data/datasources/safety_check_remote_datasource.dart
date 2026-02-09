import '../models/safety_check_category_model.dart';
import '../models/safety_check_today_status_model.dart';
import '../models/safety_check_submit_result_model.dart';

/// 안전점검 원격 데이터소스 인터페이스
///
/// API 서버와의 안전점검 관련 통신을 추상화합니다.
abstract class SafetyCheckRemoteDataSource {
  /// 안전점검 체크리스트 항목 조회 API 호출
  ///
  /// GET /api/v1/safety-check/items
  ///
  /// Returns: 카테고리 모델 목록
  Future<List<SafetyCheckCategoryModel>> getItems();

  /// 오늘 안전점검 완료 여부 조회 API 호출
  ///
  /// GET /api/v1/safety-check/today
  ///
  /// Returns: 오늘 점검 상태 모델
  Future<SafetyCheckTodayStatusModel> getTodayStatus();

  /// 안전점검 제출 API 호출
  ///
  /// POST /api/v1/safety-check/submit
  ///
  /// [checkedItemIds]: 체크된 항목 ID 목록
  ///
  /// Returns: 제출 결과 모델
  Future<SafetyCheckSubmitResultModel> submit(List<int> checkedItemIds);
}
