import '../entities/product_for_order.dart';
import '../repositories/order_repository.dart';

/// 주문용 제품 검색 UseCase
///
/// 중분류/소분류/제품명/제품코드로 제품을 검색합니다.
class SearchProductsForOrder {
  final OrderRepository _repository;

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
    if (query.trim().isEmpty) {
      return [];
    }
    return await _repository.searchProductsForOrder(
      query: query.trim(),
      categoryMid: categoryMid,
      categorySub: categorySub,
    );
  }
}
