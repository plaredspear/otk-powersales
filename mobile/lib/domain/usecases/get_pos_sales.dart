import '../entities/pos_sales.dart';
import '../repositories/pos_sales_repository.dart';

/// POS 매출 조회 UseCase
///
/// 날짜 범위, 매장명, 제품명으로 POS 매출을 조회합니다.
class GetPosSalesUseCase {
  final PosSalesRepository _repository;

  GetPosSalesUseCase(this._repository);

  /// POS 매출 조회
  ///
  /// [startDate]: 조회 시작일 (필수)
  /// [endDate]: 조회 종료일 (필수)
  /// [storeName]: 매장명 (선택)
  /// [productName]: 제품명 (선택)
  ///
  /// Returns: POS 매출 목록 (날짜순 최신순 정렬)
  Future<List<PosSales>> call({
    required DateTime startDate,
    required DateTime endDate,
    String? storeName,
    String? productName,
  }) async {
    // 날짜 범위 검증
    if (startDate.isAfter(endDate)) {
      throw ArgumentError('시작일은 종료일보다 이전이어야 합니다.');
    }

    // Repository에서 데이터 조회
    final sales = await _repository.getPosSales(
      startDate: startDate,
      endDate: endDate,
      storeName: storeName,
      productName: productName,
    );

    // 이미 Repository에서 정렬되어 반환되므로 그대로 반환
    return sales;
  }

  /// 매장별 POS 매출 조회
  ///
  /// 특정 매장의 POS 매출을 조회합니다.
  ///
  /// [startDate]: 조회 시작일
  /// [endDate]: 조회 종료일
  /// [storeName]: 매장명 (필수)
  ///
  /// Returns: 해당 매장의 POS 매출 목록
  Future<List<PosSales>> getByStore({
    required DateTime startDate,
    required DateTime endDate,
    required String storeName,
  }) async {
    if (storeName.isEmpty) {
      throw ArgumentError('매장명은 필수입니다.');
    }

    return call(
      startDate: startDate,
      endDate: endDate,
      storeName: storeName,
    );
  }

  /// 제품별 POS 매출 조회
  ///
  /// 특정 제품의 POS 매출을 조회합니다.
  ///
  /// [startDate]: 조회 시작일
  /// [endDate]: 조회 종료일
  /// [productName]: 제품명 (필수)
  ///
  /// Returns: 해당 제품의 POS 매출 목록
  Future<List<PosSales>> getByProduct({
    required DateTime startDate,
    required DateTime endDate,
    required String productName,
  }) async {
    if (productName.isEmpty) {
      throw ArgumentError('제품명은 필수입니다.');
    }

    return call(
      startDate: startDate,
      endDate: endDate,
      productName: productName,
    );
  }

  /// 매장 + 제품별 POS 매출 조회
  ///
  /// 특정 매장의 특정 제품 POS 매출을 조회합니다.
  ///
  /// [startDate]: 조회 시작일
  /// [endDate]: 조회 종료일
  /// [storeName]: 매장명 (필수)
  /// [productName]: 제품명 (필수)
  ///
  /// Returns: 해당 매장/제품의 POS 매출 목록
  Future<List<PosSales>> getByStoreAndProduct({
    required DateTime startDate,
    required DateTime endDate,
    required String storeName,
    required String productName,
  }) async {
    if (storeName.isEmpty) {
      throw ArgumentError('매장명은 필수입니다.');
    }
    if (productName.isEmpty) {
      throw ArgumentError('제품명은 필수입니다.');
    }

    return call(
      startDate: startDate,
      endDate: endDate,
      storeName: storeName,
      productName: productName,
    );
  }

  /// POS 매출 합계 계산
  ///
  /// 조회된 POS 매출의 총 판매금액을 계산합니다.
  ///
  /// [sales]: POS 매출 목록
  ///
  /// Returns: 총 판매금액
  int calculateTotalAmount(List<PosSales> sales) {
    return sales.fold<int>(
      0,
      (sum, sale) => sum + sale.amount,
    );
  }

  /// POS 매출 수량 합계 계산
  ///
  /// 조회된 POS 매출의 총 판매수량을 계산합니다.
  ///
  /// [sales]: POS 매출 목록
  ///
  /// Returns: 총 판매수량
  int calculateTotalQuantity(List<PosSales> sales) {
    return sales.fold<int>(
      0,
      (sum, sale) => sum + sale.quantity,
    );
  }
}
