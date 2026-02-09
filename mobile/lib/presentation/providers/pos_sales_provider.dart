import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/repositories/mock/pos_sales_mock_repository.dart';
import '../../domain/repositories/pos_sales_repository.dart';
import '../../domain/usecases/get_pos_sales.dart';
import 'pos_sales_state.dart';

/// POS 매출 Repository Provider
final posSalesRepositoryProvider = Provider<PosSalesRepository>((ref) {
  return PosSalesMockRepository();
});

/// POS 매출 UseCase Provider
final getPosSalesUseCaseProvider = Provider<GetPosSalesUseCase>((ref) {
  final repository = ref.watch(posSalesRepositoryProvider);
  return GetPosSalesUseCase(repository);
});

/// POS 매출 상태 관리 Provider
class PosSalesNotifier extends StateNotifier<PosSalesState> {
  PosSalesNotifier(this._getPosSales) : super(PosSalesState.initial());

  final GetPosSalesUseCase _getPosSales;

  /// POS 매출 조회
  Future<void> fetchSales({
    DateTime? startDate,
    DateTime? endDate,
    String? storeName,
    String? productName,
  }) async {
    // 로딩 상태로 전환
    state = state.copyWith(
      isLoading: true,
      errorMessage: null,
    );

    try {
      // 필터 업데이트
      final filter = PosSalesFilter(
        startDate: startDate ?? state.filter.startDate,
        endDate: endDate ?? state.filter.endDate,
        storeName: storeName,
        productName: productName,
      );

      // UseCase 호출
      final sales = await _getPosSales.call(
        startDate: filter.startDate,
        endDate: filter.endDate,
        storeName: filter.storeName,
        productName: filter.productName,
      );

      // 합계 계산
      final totalAmount = _getPosSales.calculateTotalAmount(sales);
      final totalQuantity = _getPosSales.calculateTotalQuantity(sales);

      // 성공 상태로 전환
      state = state.copyWith(
        sales: sales,
        filter: filter,
        isLoading: false,
        totalAmount: totalAmount,
        totalQuantity: totalQuantity,
      );
    } catch (e) {
      // 에러 상태로 전환
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 매장별 POS 매출 조회
  Future<void> fetchSalesByStore({
    required DateTime startDate,
    required DateTime endDate,
    required String storeName,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final sales = await _getPosSales.getByStore(
        startDate: startDate,
        endDate: endDate,
        storeName: storeName,
      );

      final totalAmount = _getPosSales.calculateTotalAmount(sales);
      final totalQuantity = _getPosSales.calculateTotalQuantity(sales);

      state = state.copyWith(
        sales: sales,
        filter: PosSalesFilter(
          startDate: startDate,
          endDate: endDate,
          storeName: storeName,
        ),
        isLoading: false,
        totalAmount: totalAmount,
        totalQuantity: totalQuantity,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 제품별 POS 매출 조회
  Future<void> fetchSalesByProduct({
    required DateTime startDate,
    required DateTime endDate,
    required String productName,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final sales = await _getPosSales.getByProduct(
        startDate: startDate,
        endDate: endDate,
        productName: productName,
      );

      final totalAmount = _getPosSales.calculateTotalAmount(sales);
      final totalQuantity = _getPosSales.calculateTotalQuantity(sales);

      state = state.copyWith(
        sales: sales,
        filter: PosSalesFilter(
          startDate: startDate,
          endDate: endDate,
          productName: productName,
        ),
        isLoading: false,
        totalAmount: totalAmount,
        totalQuantity: totalQuantity,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 필터 업데이트 및 재조회
  Future<void> updateFilter(PosSalesFilter filter) async {
    await fetchSales(
      startDate: filter.startDate,
      endDate: filter.endDate,
      storeName: filter.storeName,
      productName: filter.productName,
    );
  }

  /// 필터 초기화
  Future<void> resetFilter() async {
    final defaultFilter = PosSalesFilter.defaultFilter();
    await updateFilter(defaultFilter);
  }
}

/// POS 매출 StateNotifier Provider
final posSalesProvider =
    StateNotifierProvider<PosSalesNotifier, PosSalesState>((ref) {
  final useCase = ref.watch(getPosSalesUseCaseProvider);
  return PosSalesNotifier(useCase);
});
