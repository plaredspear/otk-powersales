import '../../../domain/entities/pos_sales.dart';
import '../../../domain/repositories/pos_sales_repository.dart';
import '../../mock/pos_sales_mock_data.dart';

/// POS 매출 Mock Repository 구현체
///
/// PosSalesRepository 인터페이스를 구현하여 Mock 데이터를 반환합니다.
/// 실제 API 연동 전까지 UI 개발 및 테스트용으로 사용됩니다.
class PosSalesMockRepository implements PosSalesRepository {
  @override
  Future<List<PosSales>> getPosSales({
    required DateTime startDate,
    required DateTime endDate,
    String? storeName,
    String? productName,
  }) async {
    // Mock 데이터에서 날짜 범위로 필터링
    var results = PosSalesMockData.getByDateRange(startDate, endDate);

    // 매장명 필터링 (선택적)
    if (storeName != null && storeName.isNotEmpty) {
      results = results
          .where((sales) => sales.storeName.contains(storeName))
          .toList();
    }

    // 제품명 필터링 (선택적)
    if (productName != null && productName.isNotEmpty) {
      results = results
          .where((sales) => sales.productName.contains(productName))
          .toList();
    }

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 날짜순 정렬 (최신순)
    results.sort((a, b) => b.salesDate.compareTo(a.salesDate));

    return results;
  }

  @override
  Future<List<PosSales>> getPosSalesByProduct({
    required String productCode,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    // Mock 데이터에서 날짜 범위로 필터링
    var results = PosSalesMockData.getByDateRange(startDate, endDate);

    // 제품 코드로 필터링
    results = results
        .where((sales) => sales.productCode == productCode)
        .toList();

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 날짜순 정렬 (최신순)
    results.sort((a, b) => b.salesDate.compareTo(a.salesDate));

    return results;
  }

  @override
  Future<List<PosSales>> getPosSalesByStore({
    required String storeName,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    // Mock 데이터에서 날짜 범위로 필터링
    var results = PosSalesMockData.getByDateRange(startDate, endDate);

    // 매장명으로 필터링
    results = results
        .where((sales) => sales.storeName == storeName)
        .toList();

    // 실제 API 호출을 시뮬레이션하기 위한 딜레이
    await Future.delayed(const Duration(milliseconds: 300));

    // 날짜순 정렬 (최신순)
    results.sort((a, b) => b.salesDate.compareTo(a.salesDate));

    return results;
  }
}
