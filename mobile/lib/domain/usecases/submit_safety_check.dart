import '../entities/safety_check_submit_result.dart';
import '../repositories/safety_check_repository.dart';

/// 안전점검 제출 UseCase
class SubmitSafetyCheck {
  final SafetyCheckRepository _repository;

  SubmitSafetyCheck(this._repository);

  Future<SafetyCheckSubmitResult> call({
    required DateTime startTime,
    required DateTime completeTime,
    required List<EquipmentAnswer> equipments,
    List<String>? precautions,
  }) async {
    if (equipments.isEmpty) {
      throw ArgumentError('장비 항목 응답이 없습니다.');
    }

    return await _repository.submit(
      startTime: startTime,
      completeTime: completeTime,
      equipments: equipments,
      precautions: precautions,
    );
  }
}
