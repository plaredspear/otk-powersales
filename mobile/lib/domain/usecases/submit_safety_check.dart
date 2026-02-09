import '../entities/safety_check_submit_result.dart';
import '../repositories/safety_check_repository.dart';

/// 안전점검 제출 UseCase
///
/// 체크 완료된 안전점검 항목을 서버에 제출합니다.
class SubmitSafetyCheck {
  final SafetyCheckRepository _repository;

  SubmitSafetyCheck(this._repository);

  /// 안전점검 제출 실행
  ///
  /// [checkedItemIds]: 체크된 항목 ID 목록 (필수, 비어있으면 안 됨)
  ///
  /// Returns: 제출 결과 (제출 ID, 제출 시각, 완료 여부)
  ///
  /// Throws:
  /// - [ArgumentError] 체크된 항목이 없는 경우
  Future<SafetyCheckSubmitResult> call({
    required List<int> checkedItemIds,
  }) async {
    if (checkedItemIds.isEmpty) {
      throw ArgumentError('체크된 항목이 없습니다. 모든 필수 항목을 체크해주세요.');
    }

    return await _repository.submit(checkedItemIds);
  }
}
