import '../repositories/order_repository.dart';

/// 즐겨찾기 삭제 UseCase
///
/// 제품을 즐겨찾기에서 삭제합니다.
class RemoveFromFavorites {
  final OrderRepository _repository;

  RemoveFromFavorites(this._repository);

  /// 즐겨찾기 삭제 실행
  ///
  /// [productCode]: 즐겨찾기에서 삭제할 제품코드
  Future<void> call({required String productCode}) async {
    await _repository.removeFromFavorites(productCode: productCode);
  }
}
