import '../entities/product_for_order.dart';
import '../repositories/order_request_repository.dart';

/// 주문용 제품 검색 UseCase
///
/// 중분류/소분류/제품명/제품코드로 제품을 검색합니다.
class SearchProductsForOrder {
  final OrderRequestRepository _repository;

  SearchProductsForOrder(this._repository);

  /// 제품 검색 실행
  ///
  /// [query]: 검색어 (제품명 또는 제품코드)
  /// [categoryMid]: 중분류 카테고리 (선택)
  /// [categorySub]: 소분류 카테고리 (선택)
  /// Returns: 검색 결과 제품 목록
  Future<List<ProductForOrder>> call({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    // 검색어가 없어도 중/소분류가 지정되면 해당 분류 전체를 조회한다(분류검색 정합).
    // 검색어·분류가 모두 없을 때만 서버 호출 없이 빈 결과를 반환한다.
    final hasCategory = (categoryMid != null && categoryMid.trim().isNotEmpty) ||
        (categorySub != null && categorySub.trim().isNotEmpty);
    if (query.trim().isEmpty && !hasCategory) {
      return [];
    }
    return await _repository.searchProductsForOrder(
      query: query.trim(),
      categoryMid: categoryMid,
      categorySub: categorySub,
    );
  }
}
