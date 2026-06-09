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

  /// 전산매출 조회 (거래처 + 기간 + 매출 조회 제품 바코드).
  ///
  /// [barcodes] 가 비어 있으면 합계금액만(레거시 `abcSumAmount`), 있으면 해당 제품별 실적을
  /// 조회한다(레거시 `abcAmount`).
  Future<void> fetchSales({
    required int customerId,
    required String startDate,
    required String endDate,
    List<String> barcodes = const [],
  }) async {
    state = state.copyWith(
      isLoading: true,
      clearErrorMessage: true,
      hasSearched: true,
    );

    try {
      final result = await _getElectronicSales.call(
        customerId: customerId,
        startDate: startDate,
        endDate: endDate,
        barcodes: barcodes,
      );

      state = state.copyWith(
        sales: result.items,
        totalAmount: result.totalAmount,
        isLoading: false,
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

  /// 조회 결과 초기화.
  void reset() {
    state = ElectronicSalesState.initial();
  }
}

/// 전산매출 StateNotifier Provider
final electronicSalesProvider =
    StateNotifierProvider<ElectronicSalesNotifier, ElectronicSalesState>((ref) {
  return ElectronicSalesNotifier(ref.watch(getElectronicSalesUseCaseProvider));
});
