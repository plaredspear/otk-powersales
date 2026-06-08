import '../entities/pos_sales.dart';
import '../repositories/pos_sales_repository.dart';

/// POS 매출 조회 UseCase.
///
/// 거래처 1곳 + 연월 기준 제품별 POS 매출을 조회하고, 합계 산출을 보조한다.
class GetPosSalesUseCase {
  final PosSalesRepository _repository;

  GetPosSalesUseCase(this._repository);

  /// POS 매출 조회 (거래처 + 연월).
  Future<List<PosSales>> call({
    required int customerId,
    required String yearMonth,
  }) {
    return _repository.getPosSales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
  }

  /// 총 금액 합계.
  int calculateTotalAmount(List<PosSales> salesList) =>
      salesList.fold(0, (sum, sales) => sum + sales.amount);

  /// 총 수량 합계.
  int calculateTotalQuantity(List<PosSales> salesList) =>
      salesList.fold(0, (sum, sales) => sum + sales.quantity);
}
