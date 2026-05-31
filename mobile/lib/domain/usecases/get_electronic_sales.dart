import '../entities/electronic_sales.dart';
import '../repositories/electronic_sales_repository.dart';

/// 전산매출(ABC) 조회 UseCase.
///
/// 거래처 1곳 + 연월 기준 제품별 전산매출을 조회하고, 합계 산출을 보조한다.
class GetElectronicSales {
  final ElectronicSalesRepository _repository;

  GetElectronicSales(this._repository);

  /// 전산매출 조회 (거래처 + 연월).
  Future<List<ElectronicSales>> call({
    required int customerId,
    required String yearMonth,
  }) {
    return _repository.getElectronicSales(
      customerId: customerId,
      yearMonth: yearMonth,
    );
  }

  /// 총 금액 합계.
  int calculateTotalAmount(List<ElectronicSales> salesList) =>
      salesList.fold(0, (sum, sales) => sum + sales.amount);

  /// 총 수량 합계.
  int calculateTotalQuantity(List<ElectronicSales> salesList) =>
      salesList.fold(0, (sum, sales) => sum + sales.quantity);

  /// 평균 증감율 (전년 비교가 있는 항목만). 없으면 null.
  double? calculateAverageGrowthRate(List<ElectronicSales> salesList) {
    final rates = salesList
        .where((s) => s.growthRate != null)
        .map((s) => s.growthRate!)
        .toList();
    if (rates.isEmpty) return null;
    return rates.fold(0.0, (sum, rate) => sum + rate) / rates.length;
  }
}
