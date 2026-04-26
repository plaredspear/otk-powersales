import '../repositories/order_repository.dart';

/// 즐겨찾기 추가 UseCase
///
/// 제품을 즐겨찾기에 추가합니다.
class AddToFavorites {
  final OrderRepository _repository;

  AddToFavorites(this._repository);

  /// 즐겨찾기 추가 실행
  ///
  /// [productCode]: 즐겨찾기에 추가할 제품코드
  Future<void> call({required String productCode}) async {
    await _repository.addToFavorites(productCode: productCode);
  }
}
