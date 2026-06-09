import '../entities/pos_sales_result.dart';
import '../repositories/pos_sales_repository.dart';

/// POS 매출 조회 UseCase.
///
/// 거래처 1곳 + 기간 + 선택 바코드 기준 제품별 POS 매출을 조회한다 (합계금액 포함).
class GetPosSalesUseCase {
  final PosSalesRepository _repository;

  GetPosSalesUseCase(this._repository);

  /// POS 매출 조회 (기간 + 선택 바코드).
  Future<PosSalesResult> call({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) {
    return _repository.getPosSalesByRange(
      customerId: customerId,
      startDate: startDate,
      endDate: endDate,
      barcodes: barcodes,
    );
  }
}
