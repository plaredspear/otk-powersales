import '../models/safety_check_category_model.dart';
import '../models/safety_check_today_status_model.dart';
import '../models/safety_check_submit_result_model.dart';

/// 안전점검 원격 데이터소스 인터페이스
abstract class SafetyCheckRemoteDataSource {
  /// GET /api/v1/safety-check/items
  Future<List<SafetyCheckCategoryModel>> getItems();

  /// GET /api/v1/safety-check/today
  Future<SafetyCheckTodayStatusModel> getTodayStatus();

  /// POST /api/v1/safety-check/submit
  Future<SafetyCheckSubmitResultModel> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<Map<String, dynamic>> equipments,
    List<String>? precautions,
  });
}
