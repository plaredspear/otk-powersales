import '../entities/safety_check_category.dart';
import '../repositories/safety_check_repository.dart';

/// 안전점검 체크리스트 항목 조회 UseCase
///
/// 안전점검 화면에 표시할 카테고리별 체크리스트 항목을 조회합니다.
class GetSafetyCheckItems {
  final SafetyCheckRepository _repository;

  GetSafetyCheckItems(this._repository);

  /// 체크리스트 항목 조회 실행
  ///
  /// Returns: 카테고리 목록 (각 카테고리에 체크 항목 포함)
  Future<List<SafetyCheckCategory>> call() async {
    return await _repository.getItems();
  }
}
