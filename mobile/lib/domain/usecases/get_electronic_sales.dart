import '../entities/electronic_sales.dart';
import '../repositories/electronic_sales_repository.dart';

/// 전산매출 조회 UseCase
///
/// 전산매출 조회 및 필터링 비즈니스 로직을 처리합니다.
class GetElectronicSales {
  final ElectronicSalesRepository _repository;

  GetElectronicSales(this._repository);

  /// 전산매출 조회 (기본)
  ///
  /// [yearMonth] 년월 (예: "202601")
  /// [customerName] 거래처명 필터 (선택)
  /// [productName] 제품명 필터 (선택)
  /// [productCode] 제품 코드 필터 (선택)
  ///
  /// Returns: 전산매출 목록
  Future<List<ElectronicSales>> call({
    required String yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
  }) async {
    return _repository.getElectronicSales(
      yearMonth: yearMonth,
      customerName: customerName,
      productName: productName,
      productCode: productCode,
    );
  }

  /// 거래처별 전산매출 조회
  ///
  /// [yearMonth] 년월
  /// [customerName] 거래처명
  ///
  /// Returns: 해당 거래처의 전산매출 목록
  Future<List<ElectronicSales>> getByCustomer({
    required String yearMonth,
    required String customerName,
  }) async {
    return _repository.getElectronicSales(
      yearMonth: yearMonth,
      customerName: customerName,
    );
  }

  /// 제품별 전산매출 조회
  ///
  /// [yearMonth] 년월
  /// [productCode] 제품 코드
  ///
  /// Returns: 해당 제품의 전산매출 목록
  Future<List<ElectronicSales>> getByProduct({
    required String yearMonth,
    required String productCode,
  }) async {
    return _repository.getElectronicSales(
      yearMonth: yearMonth,
      productCode: productCode,
    );
  }

  /// 거래처별 전산매출 합계 조회
  ///
  /// [yearMonth] 년월
  /// [customerName] 거래처명
  ///
  /// Returns: 거래처 전체 실적 합계
  Future<ElectronicSales?> getCustomerTotal({
    required String yearMonth,
    required String customerName,
  }) async {
    return _repository.getCustomerTotal(
      yearMonth: yearMonth,
      customerName: customerName,
    );
  }

  /// 제품별 전산매출 합계 조회
  ///
  /// [yearMonth] 년월
  /// [productCode] 제품 코드
  ///
  /// Returns: 제품 전체 실적 합계
  Future<ElectronicSales?> getProductTotal({
    required String yearMonth,
    required String productCode,
  }) async {
    return _repository.getProductTotal(
      yearMonth: yearMonth,
      productCode: productCode,
    );
  }

  /// 전산매출 목록의 총 금액 계산
  ///
  /// [salesList] 전산매출 목록
  ///
  /// Returns: 총 금액
  int calculateTotalAmount(List<ElectronicSales> salesList) {
    if (salesList.isEmpty) return 0;
    return salesList.fold(0, (sum, sales) => sum + sales.amount);
  }

  /// 전산매출 목록의 총 수량 계산
  ///
  /// [salesList] 전산매출 목록
  ///
  /// Returns: 총 수량
  int calculateTotalQuantity(List<ElectronicSales> salesList) {
    if (salesList.isEmpty) return 0;
    return salesList.fold(0, (sum, sales) => sum + sales.quantity);
  }

  /// 전산매출 목록의 평균 증감율 계산
  ///
  /// [salesList] 전산매출 목록
  ///
  /// Returns: 평균 증감율 (%), null이면 계산 불가
  double? calculateAverageGrowthRate(List<ElectronicSales> salesList) {
    if (salesList.isEmpty) return null;

    final validGrowthRates = salesList
        .where((sales) => sales.growthRate != null)
        .map((sales) => sales.growthRate!)
        .toList();

    if (validGrowthRates.isEmpty) return null;

    final sum = validGrowthRates.fold(0.0, (sum, rate) => sum + rate);
    return sum / validGrowthRates.length;
  }

  /// 전년 대비 증감율 계산
  ///
  /// [currentAmount] 당해 실적
  /// [previousAmount] 전년 실적
  ///
  /// Returns: 증감율 (%), null이면 전년 실적이 0
  double? calculateGrowthRate({
    required int currentAmount,
    required int previousAmount,
  }) {
    if (previousAmount == 0) return null;
    return ((currentAmount - previousAmount) / previousAmount) * 100;
  }

  /// 거래처 목록 추출
  ///
  /// [salesList] 전산매출 목록
  ///
  /// Returns: 중복 제거된 거래처명 목록
  List<String> extractCustomers(List<ElectronicSales> salesList) {
    final customers = salesList.map((sales) => sales.customerName).toSet();
    return customers.toList()..sort();
  }

  /// 제품 목록 추출
  ///
  /// [salesList] 전산매출 목록
  ///
  /// Returns: 중복 제거된 제품명 목록
  List<String> extractProducts(List<ElectronicSales> salesList) {
    final products = salesList.map((sales) => sales.productName).toSet();
    return products.toList()..sort();
  }
}
