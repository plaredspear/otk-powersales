import '../entities/inspection_detail.dart';
import '../repositories/inspection_repository.dart';

/// 현장 점검 상세 조회 UseCase
///
/// 점검 ID로 현장 점검 상세 정보를 조회합니다.
class GetInspectionDetailUseCase {
  final InspectionRepository _repository;

  GetInspectionDetailUseCase(this._repository);

  /// 현장 점검 상세 조회 실행
  ///
  /// [id]: 점검 ID
  /// Returns: 현장 점검 상세 정보
  /// Throws: [Exception] ID가 유효하지 않거나 점검을 찾을 수 없을 경우
  Future<InspectionDetail> call(int id) async {
    if (id <= 0) {
      throw Exception('유효하지 않은 점검 ID입니다');
    }

    return await _repository.getInspectionDetail(id);
  }
}
