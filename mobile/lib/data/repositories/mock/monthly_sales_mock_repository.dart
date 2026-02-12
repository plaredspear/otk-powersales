import '../../../domain/entities/monthly_sales.dart';
import '../../../domain/repositories/monthly_sales_repository.dart';
import '../../mock/monthly_sales_mock_data.dart';

/// MonthlySales Mock Repository
///
/// Backend API가 준비되기 전까지 Mock 데이터로 동작하는 Repository.
class MonthlySalesMockRepository implements MonthlySalesRepository {
  /// Mock 데이터 커스텀 (테스트용)
  Map<String, MonthlySales>? customMonthlySales;
  Exception? exceptionToThrow;

  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 500));
  }

  @override
  Future<MonthlySales> getMonthlySales({
    String? customerId,
    required String yearMonth,
  }) async {
    await _simulateDelay();

    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }

    // 커스텀 데이터 또는 Mock 데이터에서 조회
    MonthlySales? monthlySales;

    // 빈 문자열도 null로 취급
    final effectiveCustomerId = (customerId == null || customerId.isEmpty) ? null : customerId;
    final key = '${effectiveCustomerId ?? 'ALL'}-$yearMonth';

    if (customMonthlySales != null) {
      monthlySales = customMonthlySales![key];
    } else {
      monthlySales = MonthlySalesMockData.getMonthlySales(
        customerId: effectiveCustomerId,
        yearMonth: yearMonth,
      );
    }

    if (monthlySales == null) {
      throw Exception(
        'MONTHLY_SALES_NOT_FOUND: Sales data for customer=$customerId, yearMonth=$yearMonth not found',
      );
    }

    return monthlySales;
  }
}
