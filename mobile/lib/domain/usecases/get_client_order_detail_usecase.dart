import '../entities/client_order.dart';
import '../repositories/order_repository.dart';

/// 거래처별 주문 상세 조회 UseCase
///
/// SAP 주문번호를 기반으로 거래처별 주문의 상세 정보를 조회합니다.
/// 주문 정보, 제품 목록, 배송 상태 등을 포함합니다.
class GetClientOrderDetailUseCase {
  final OrderRepository _repository;

  GetClientOrderDetailUseCase(this._repository);

  /// 거래처별 주문 상세를 조회합니다.
  ///
  /// [sapOrderNumber]: SAP 주문번호 (예: 300011396)
  Future<ClientOrderDetail> call({
    required String sapOrderNumber,
  }) async {
    return await _repository.getClientOrderDetail(
      sapOrderNumber: sapOrderNumber,
    );
  }
}
