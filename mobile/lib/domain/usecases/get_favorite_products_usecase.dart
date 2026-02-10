import '../entities/product_for_order.dart';
import '../repositories/order_repository.dart';

/// 즐겨찾기 제품 목록 조회 UseCase
///
/// 즐겨찾기에 등록된 제품 목록을 조회합니다.
class GetFavoriteProducts {
  final OrderRepository _repository;

  GetFavoriteProducts(this._repository);

  /// 즐겨찾기 제품 목록 조회 실행
  ///
  /// Returns: 즐겨찾기 제품 목록
  Future<List<ProductForOrder>> call() async {
    return await _repository.getFavoriteProducts();
  }
}
