import '../entities/order_detail.dart';
import '../repositories/order_repository.dart';

/// 주문 상세 조회 UseCase
///
/// 주문 ID를 기반으로 주문 상세 정보를 조회합니다.
class GetOrderDetail {
  final OrderRepository _repository;

  GetOrderDetail(this._repository);

  /// 주문 상세를 조회합니다.
  ///
  /// [orderId]: 조회할 주문 ID
  /// Returns: 주문 상세 정보
  Future<OrderDetail> call({required int orderId}) {
    return _repository.getOrderDetail(orderId: orderId);
  }
}
