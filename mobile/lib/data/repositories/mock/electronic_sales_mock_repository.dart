import '../../../domain/entities/electronic_sales.dart';
import '../../../domain/repositories/electronic_sales_repository.dart';
import '../../mock/electronic_sales_mock_data.dart';

/// 전산매출 Mock Repository 구현체
///
/// ElectronicSalesRepository 인터페이스를 구현하여 Mock 데이터를 반환합니다.
/// 실제 API 연동 전까지 UI 개발 및 테스트용으로 사용됩니다.
class ElectronicSalesMockRepository implements ElectronicSalesRepository {
  @override
  Future<List<ElectronicSales>> getElectronicSales({
    required String yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
  }) async {
    // Mock 데이터에서 년월로 필터링
    var results = ElectronicSalesMockData.getByYearMonth(yearMonth);

    // 거래처명 필터링 (선택적)
    if (customerName != null && customerName.isNotEmpty) {
      results = results
          .where((sales) => sales.customerName.contains(customerName))
          .toList();
    }

    // 제품명 필터링 (선택적)
    if (productName != null && productName.isNotEmpty) {
      results = results
          .where((sales) => sales.productName.contains(productName))
          .toList();
    }

    // 제품 코드 필터링 (선택적)
    if (productCode != null && productCode.isNotEmpty) {
      results = results
          .where((sales) => sales.productCode == productCode)
          .toList();
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 금액순 정렬 (내림차순)
    results.sort((a, b) => b.amount.compareTo(a.amount));

    return results;
  }

  @override
  Future<ElectronicSales?> getCustomerTotal({
    required String yearMonth,
    required String customerName,
  }) async {
    // Mock 데이터에서 년월 + 거래처로 필터링
    final results = ElectronicSalesMockData.getByYearMonthAndCustomer(
      yearMonth,
      customerName,
    );

    if (results.isEmpty) {
      return null;
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 거래처별 합계 계산
    final totalAmount = results.fold<int>(0, (sum, sales) => sum + sales.amount);
    final totalQuantity = results.fold<int>(0, (sum, sales) => sum + sales.quantity);

    // 전년 대비 계산 (전년 데이터가 있는 경우만)
    int? totalPreviousYearAmount;
    double? totalGrowthRate;

    final previousYearAmounts = results
        .where((sales) => sales.previousYearAmount != null)
        .map((sales) => sales.previousYearAmount!)
        .toList();

    if (previousYearAmounts.isNotEmpty) {
      totalPreviousYearAmount = previousYearAmounts.fold<int>(0, (sum, amount) => sum + amount);
      if (totalPreviousYearAmount > 0) {
        totalGrowthRate = ((totalAmount - totalPreviousYearAmount) / totalPreviousYearAmount) * 100;
      }
    }

    return ElectronicSales(
      yearMonth: yearMonth,
      customerName: customerName,
      productName: '전체 합계',
      productCode: 'TOTAL',
      amount: totalAmount,
      quantity: totalQuantity,
      previousYearAmount: totalPreviousYearAmount,
      growthRate: totalGrowthRate,
    );
  }

  @override
  Future<ElectronicSales?> getProductTotal({
    required String yearMonth,
    required String productCode,
  }) async {
    // Mock 데이터에서 년월로 필터링 후 제품 코드로 필터링
    final allResults = ElectronicSalesMockData.getByYearMonth(yearMonth);
    final results = allResults
        .where((sales) => sales.productCode == productCode)
        .toList();

    if (results.isEmpty) {
      return null;
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 제품별 합계 계산
    final totalAmount = results.fold<int>(0, (sum, sales) => sum + sales.amount);
    final totalQuantity = results.fold<int>(0, (sum, sales) => sum + sales.quantity);

    // 전년 대비 계산 (전년 데이터가 있는 경우만)
    int? totalPreviousYearAmount;
    double? totalGrowthRate;

    final previousYearAmounts = results
        .where((sales) => sales.previousYearAmount != null)
        .map((sales) => sales.previousYearAmount!)
        .toList();

    if (previousYearAmounts.isNotEmpty) {
      totalPreviousYearAmount = previousYearAmounts.fold<int>(0, (sum, amount) => sum + amount);
      if (totalPreviousYearAmount > 0) {
        totalGrowthRate = ((totalAmount - totalPreviousYearAmount) / totalPreviousYearAmount) * 100;
      }
    }

    // 첫 번째 결과에서 제품명 가져오기
    final productName = results.first.productName;

    return ElectronicSales(
      yearMonth: yearMonth,
      customerName: '전체 거래처',
      productName: productName,
      productCode: productCode,
      amount: totalAmount,
      quantity: totalQuantity,
      previousYearAmount: totalPreviousYearAmount,
      growthRate: totalGrowthRate,
    );
  }
}
