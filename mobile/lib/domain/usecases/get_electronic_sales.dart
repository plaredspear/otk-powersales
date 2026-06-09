import '../entities/electronic_sales.dart';
import '../repositories/electronic_sales_repository.dart';

/// 전산매출(ABC) 조회 UseCase.
///
/// 거래처 1곳 + 기간 + 매출 조회 제품(바코드) 기준 전산매출을 조회한다.
class GetElectronicSales {
  final ElectronicSalesRepository _repository;

  GetElectronicSales(this._repository);

  /// 전산매출 조회.
  Future<ElectronicSalesResult> call({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) {
    return _repository.getElectronicSales(
      customerId: customerId,
      startDate: startDate,
      endDate: endDate,
      barcodes: barcodes,
    );
  }
}
