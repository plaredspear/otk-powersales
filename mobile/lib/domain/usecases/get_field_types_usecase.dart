import '../entities/inspection_field_type.dart';
import '../repositories/inspection_repository.dart';

/// 현장 유형 목록 조회 UseCase
///
/// 현장 유형 코드 목록을 조회합니다.
class GetFieldTypesUseCase {
  final InspectionRepository _repository;

  GetFieldTypesUseCase(this._repository);

  /// 현장 유형 목록 조회 실행
  ///
  /// Returns: 현장 유형 목록
  Future<List<InspectionFieldType>> call() async {
    return await _repository.getFieldTypes();
  }
}
