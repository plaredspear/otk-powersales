import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../core/utils/error_utils.dart';
import '../../data/datasources/pos_sales_api_datasource.dart';
import '../../data/repositories/pos_sales_repository_impl.dart';
import '../../domain/repositories/pos_sales_repository.dart';
import '../../domain/usecases/get_pos_sales.dart';
import 'pos_sales_state.dart';

/// POS 매출 DataSource Provider (실 API)
final posSalesRemoteDataSourceProvider =
    Provider<PosSalesApiDataSource>((ref) {
  return PosSalesApiDataSource(ref.watch(dioProvider));
});

/// POS 매출 Repository Provider (실 API — MonthlySalesController)
final posSalesRepositoryProvider = Provider<PosSalesRepository>((ref) {
  return PosSalesRepositoryImpl(
    remoteDataSource: ref.watch(posSalesRemoteDataSourceProvider),
  );
});

/// POS 매출 UseCase Provider
final getPosSalesUseCaseProvider = Provider<GetPosSalesUseCase>((ref) {
  return GetPosSalesUseCase(ref.watch(posSalesRepositoryProvider));
});

/// POS 매출 상태 관리 Notifier
class PosSalesNotifier extends StateNotifier<PosSalesState> {
  PosSalesNotifier(this._getPosSales) : super(PosSalesState.initial());

  final GetPosSalesUseCase _getPosSales;

  /// POS 매출 조회 (거래처 + 연월).
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
      final sales = await _getPosSales.call(
        customerId: customerId,
        yearMonth: yearMonth,
      );

      state = state.copyWith(
        sales: sales,
        isLoading: false,
        totalAmount: _getPosSales.calculateTotalAmount(sales),
        totalQuantity: _getPosSales.calculateTotalQuantity(sales),
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
    state = PosSalesState.initial();
  }
}

/// POS 매출 StateNotifier Provider
final posSalesProvider =
    StateNotifierProvider<PosSalesNotifier, PosSalesState>((ref) {
  return PosSalesNotifier(ref.watch(getPosSalesUseCaseProvider));
});
