import '../entities/inspection_list_item.dart';
import '../entities/inspection_form.dart';
import '../repositories/inspection_repository.dart';

/// 현장 점검 등록 UseCase
///
/// 현장 점검 등록 폼 데이터를 검증하고 등록합니다.
class RegisterInspectionUseCase {
  final InspectionRepository _repository;

  RegisterInspectionUseCase(this._repository);

  /// 현장 점검 등록 실행
  ///
  /// [form]: 등록 폼 데이터
  /// Returns: 등록된 현장 점검 항목
  /// Throws: [Exception] 폼 유효성 검증 실패 시
  Future<InspectionListItem> call(InspectionRegisterForm form) async {
    // 폼 유효성 검증
    final validationResult = form.validate();
    if (!validationResult.isValid) {
      throw Exception(validationResult.firstError ?? '입력 정보가 올바르지 않습니다');
    }

    return await _repository.registerInspection(form);
  }
}
