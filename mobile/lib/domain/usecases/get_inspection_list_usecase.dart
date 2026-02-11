import '../entities/inspection_list_item.dart';
import '../repositories/inspection_repository.dart';

/// 현장 점검 목록 조회 UseCase
///
/// 검색 필터 조건에 따라 현장 점검 목록을 조회합니다.
/// 날짜 범위 유효성을 검증하고, 결과를 반환합니다.
class GetInspectionListUseCase {
  final InspectionRepository _repository;

  GetInspectionListUseCase(this._repository);

  /// 현장 점검 목록 조회 실행
  ///
  /// [filter]: 검색 필터 (거래처, 분류, 기간)
  /// Returns: 현장 점검 목록 항목 목록
  /// Throws: [Exception] 검색 기간이 유효하지 않을 경우 (시작일 > 종료일)
  Future<List<InspectionListItem>> call(InspectionFilter filter) async {
    if (!filter.isValidDateRange) {
      throw Exception('검색 시작일은 종료일보다 이전이어야 합니다');
    }

    return await _repository.getInspectionList(filter);
  }
}
