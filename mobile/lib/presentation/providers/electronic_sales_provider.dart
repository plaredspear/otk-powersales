import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/electronic_sales_api_datasource.dart';
import '../../data/repositories/electronic_sales_repository_impl.dart';
import '../../domain/repositories/electronic_sales_repository.dart';
import '../../domain/usecases/get_electronic_sales.dart';
import 'electronic_sales_state.dart';

/// 전산매출 DataSource Provider (실 API)
final electronicSalesRemoteDataSourceProvider =
    Provider<ElectronicSalesApiDataSource>((ref) {
  return ElectronicSalesApiDataSource(ref.watch(dioProvider));
});

/// 전산매출 Repository Provider (실 API — MonthlySalesController)
final electronicSalesRepositoryProvider =
    Provider<ElectronicSalesRepository>((ref) {
  return ElectronicSalesRepositoryImpl(
    remoteDataSource: ref.watch(electronicSalesRemoteDataSourceProvider),
  );
});

/// 전산매출 UseCase Provider
final getElectronicSalesUseCaseProvider = Provider<GetElectronicSales>((ref) {
  return GetElectronicSales(ref.watch(electronicSalesRepositoryProvider));
});

/// 전산매출 상태 관리 Notifier
class ElectronicSalesNotifier extends StateNotifier<ElectronicSalesState> {
  ElectronicSalesNotifier(this._getElectronicSales)
      : super(ElectronicSalesState.initial());

  final GetElectronicSales _getElectronicSales;

  /// 전산매출 조회 (거래처 + 연월).
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
      yearMonth: yearMonth,
    );

    try {
      final sales = await _getElectronicSales.call(
        customerId: customerId,
        yearMonth: yearMonth,
      );

      final growthRate = _getElectronicSales.calculateAverageGrowthRate(sales);
      state = state.copyWith(
        sales: sales,
        isLoading: false,
        totalAmount: _getElectronicSales.calculateTotalAmount(sales),
        totalQuantity: _getElectronicSales.calculateTotalQuantity(sales),
        averageGrowthRate: growthRate,
        clearAverageGrowthRate: growthRate == null,
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

  /// 조회 결과 초기화 (거래처 선택 해제 포함).
  void reset() {
    state = ElectronicSalesState.initial();
  }
}

/// 전산매출 StateNotifier Provider
final electronicSalesProvider =
    StateNotifierProvider<ElectronicSalesNotifier, ElectronicSalesState>((ref) {
  return ElectronicSalesNotifier(ref.watch(getElectronicSalesUseCaseProvider));
});
