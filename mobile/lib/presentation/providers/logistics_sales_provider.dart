import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/logistics_sales_api_datasource.dart';
import '../../data/repositories/logistics_sales_repository_impl.dart';
import '../../domain/repositories/logistics_sales_repository.dart';
import '../../domain/usecases/get_logistics_sales.dart';
import 'logistics_sales_state.dart';

/// 물류매출 DataSource Provider (실 API)
final logisticsSalesRemoteDataSourceProvider =
    Provider<LogisticsSalesApiDataSource>((ref) {
  return LogisticsSalesApiDataSource(ref.watch(dioProvider));
});

/// 물류매출 Repository Provider (실 API — MonthlySalesController)
final logisticsSalesRepositoryProvider =
    Provider<LogisticsSalesRepository>((ref) {
  return LogisticsSalesRepositoryImpl(
    remoteDataSource: ref.watch(logisticsSalesRemoteDataSourceProvider),
  );
});

/// 물류매출 UseCase Provider
final getLogisticsSalesUseCaseProvider = Provider<GetLogisticsSales>((ref) {
  return GetLogisticsSales(ref.watch(logisticsSalesRepositoryProvider));
});

/// 물류매출 상태 관리 Notifier
class LogisticsSalesNotifier extends StateNotifier<LogisticsSalesState> {
  LogisticsSalesNotifier(this._getLogisticsSales)
      : super(LogisticsSalesState.initial());

  final GetLogisticsSales _getLogisticsSales;

  /// 물류매출 조회 (거래처 + 연월).
  Future<void> fetchSales({
    required int customerId,
    required String customerName,
    required String yearMonth,
  }) async {
    state = state.copyWith(
      isLoading: true,
      clearErrorMessage: true,
      selectedCustomerId: customerId,
      selectedCustomerName: customerName,
    );

    try {
      final sales = await _getLogisticsSales.call(
        customerId: customerId,
        yearMonth: yearMonth,
      );

      // 당월 여부: 모든 항목이 동일하므로 첫 항목 기준, 없으면 연월 비교 fallback.
      final isCurrentMonth = sales.isNotEmpty
          ? sales.first.isCurrentMonth
          : yearMonth == state.currentYearMonth;
      final growthRate = _getLogisticsSales.calculateTotalGrowthRate(sales);

      state = state.copyWith(
        sales: sales,
        filter: state.filter.copyWith(yearMonth: yearMonth),
        isLoading: false,
        isCurrentMonth: isCurrentMonth,
        totalAmount: _getLogisticsSales.calculateTotalAmount(sales),
        totalPreviousYearAmount:
            _getLogisticsSales.calculateTotalPreviousYearAmount(sales),
        totalDifference: _getLogisticsSales.calculateTotalDifference(sales),
        totalGrowthRate: growthRate,
        clearTotalGrowthRate: growthRate == null,
        categoryTotals: _getLogisticsSales.calculateCategoryTotals(sales),
        categoryList: _getLogisticsSales.extractCategories(sales),
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: extractErrorMessage(e),
      );
    }
  }

  /// 에러 메시지 제거.
  void clearError() {
    state = state.copyWith(clearErrorMessage: true);
  }

  /// 필터/조회 결과 초기화 (거래처·연월 선택 해제 포함).
  void reset() {
    state = LogisticsSalesState.initial();
  }
}

/// 물류매출 StateNotifier Provider
final logisticsSalesProvider =
    StateNotifierProvider<LogisticsSalesNotifier, LogisticsSalesState>((ref) {
  return LogisticsSalesNotifier(ref.watch(getLogisticsSalesUseCaseProvider));
});
