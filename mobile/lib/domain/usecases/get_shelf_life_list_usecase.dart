import '../entities/shelf_life_item.dart';
import '../repositories/shelf_life_repository.dart';

/// 유통기한 목록 조회 UseCase
///
/// 검색 필터 조건에 따라 유통기한 목록을 조회합니다.
/// 날짜 범위 유효성을 검증하고, 결과를 그룹화하여 반환할 수 있습니다.
class GetShelfLifeList {
  final ShelfLifeRepository _repository;

  GetShelfLifeList(this._repository);

  /// 유통기한 목록 조회 실행
  ///
  /// [filter]: 검색 필터 (거래처, 기간)
  /// Returns: 유통기한 항목 목록
  /// Throws: [Exception] 검색 기간이 최대 6개월을 초과할 경우
  Future<List<ShelfLifeItem>> call(ShelfLifeFilter filter) async {
    if (!filter.isValidDateRange) {
      throw Exception('유통기한 검색 기간은 최대 6개월입니다');
    }

    return await _repository.getShelfLifeList(filter);
  }

  /// 유통기한 목록을 그룹화하여 반환
  ///
  /// "유통기한 지남" (isExpired == true)과 "유통기한 전" (isExpired == false)으로 분리합니다.
  /// 각 그룹 내에서 D-DAY 오름차순으로 정렬합니다.
  static Map<String, List<ShelfLifeItem>> groupByExpiry(
    List<ShelfLifeItem> items,
  ) {
    final expired = items.where((item) => item.isExpired).toList()
      ..sort((a, b) => a.dDay.compareTo(b.dDay));
    final notExpired = items.where((item) => !item.isExpired).toList()
      ..sort((a, b) => a.dDay.compareTo(b.dDay));

    return {
      'expired': expired,
      'notExpired': notExpired,
    };
  }
}
