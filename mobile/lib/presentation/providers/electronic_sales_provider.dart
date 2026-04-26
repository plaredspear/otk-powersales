import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/repositories/mock/electronic_sales_mock_repository.dart';
import '../../domain/repositories/electronic_sales_repository.dart';
import '../../domain/usecases/get_electronic_sales.dart';
import 'electronic_sales_state.dart';

/// 전산매출 Repository Provider
final electronicSalesRepositoryProvider =
    Provider<ElectronicSalesRepository>((ref) {
  return ElectronicSalesMockRepository();
});

/// 전산매출 UseCase Provider
final getElectronicSalesUseCaseProvider = Provider<GetElectronicSales>((ref) {
  final repository = ref.watch(electronicSalesRepositoryProvider);
  return GetElectronicSales(repository);
});

/// 전산매출 상태 관리 Provider
class ElectronicSalesNotifier extends StateNotifier<ElectronicSalesState> {
  ElectronicSalesNotifier(this._getElectronicSales)
      : super(ElectronicSalesState.initial());

  final GetElectronicSales _getElectronicSales;

  /// 전산매출 조회
  Future<void> fetchSales({
    String? yearMonth,
    String? customerName,
    String? productName,
    String? productCode,
  }) async {
    // 로딩 상태로 전환
    state = state.copyWith(
      isLoading: true,
      errorMessage: null,
    );

    try {
      // 필터 업데이트
      final filter = ElectronicSalesFilter(
        yearMonth: yearMonth ?? state.filter.yearMonth,
        customerName: customerName,
        productName: productName,
        productCode: productCode,
      );

      // UseCase 호출
      final sales = await _getElectronicSales.call(
        yearMonth: filter.yearMonth,
        customerName: filter.customerName,
        productName: filter.productName,
        productCode: filter.productCode,
      );

      // 합계 계산
      final totalAmount = _getElectronicSales.calculateTotalAmount(sales);
      final totalQuantity = _getElectronicSales.calculateTotalQuantity(sales);
      final averageGrowthRate =
          _getElectronicSales.calculateAverageGrowthRate(sales);

      // 거래처/제품 목록 추출
      final customerList = _getElectronicSales.extractCustomers(sales);
      final productList = _getElectronicSales.extractProducts(sales);

      // 성공 상태로 전환
      state = state.copyWith(
        sales: sales,
        filter: filter,
        isLoading: false,
        totalAmount: totalAmount,
        totalQuantity: totalQuantity,
        averageGrowthRate: averageGrowthRate,
        customerList: customerList,
        productList: productList,
      );
    } catch (e) {
      // 에러 상태로 전환
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 거래처별 전산매출 조회
  Future<void> fetchSalesByCustomer({
    required String yearMonth,
    required String customerName,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final sales = await _getElectronicSales.getByCustomer(
        yearMonth: yearMonth,
        customerName: customerName,
      );

      final totalAmount = _getElectronicSales.calculateTotalAmount(sales);
      final totalQuantity = _getElectronicSales.calculateTotalQuantity(sales);
      final averageGrowthRate =
          _getElectronicSales.calculateAverageGrowthRate(sales);
      final customerList = _getElectronicSales.extractCustomers(sales);
      final productList = _getElectronicSales.extractProducts(sales);

      state = state.copyWith(
        sales: sales,
        filter: ElectronicSalesFilter(
          yearMonth: yearMonth,
          customerName: customerName,
        ),
        isLoading: false,
        totalAmount: totalAmount,
        totalQuantity: totalQuantity,
        averageGrowthRate: averageGrowthRate,
        customerList: customerList,
        productList: productList,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 제품별 전산매출 조회
  Future<void> fetchSalesByProduct({
    required String yearMonth,
    required String productCode,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final sales = await _getElectronicSales.getByProduct(
        yearMonth: yearMonth,
        productCode: productCode,
      );

      final totalAmount = _getElectronicSales.calculateTotalAmount(sales);
      final totalQuantity = _getElectronicSales.calculateTotalQuantity(sales);
      final averageGrowthRate =
          _getElectronicSales.calculateAverageGrowthRate(sales);
      final customerList = _getElectronicSales.extractCustomers(sales);
      final productList = _getElectronicSales.extractProducts(sales);

      state = state.copyWith(
        sales: sales,
        filter: ElectronicSalesFilter(
          yearMonth: yearMonth,
          productCode: productCode,
        ),
        isLoading: false,
        totalAmount: totalAmount,
        totalQuantity: totalQuantity,
        averageGrowthRate: averageGrowthRate,
        customerList: customerList,
        productList: productList,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 거래처 합계 조회
  Future<void> fetchCustomerTotal({
    required String yearMonth,
    required String customerName,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final total = await _getElectronicSales.getCustomerTotal(
        yearMonth: yearMonth,
        customerName: customerName,
      );

      if (total != null) {
        state = state.copyWith(
          sales: [total],
          filter: ElectronicSalesFilter(
            yearMonth: yearMonth,
            customerName: customerName,
          ),
          isLoading: false,
          totalAmount: total.amount,
          totalQuantity: total.quantity,
        );
      } else {
        state = state.copyWith(
          sales: [],
          isLoading: false,
        );
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 제품 합계 조회
  Future<void> fetchProductTotal({
    required String yearMonth,
    required String productCode,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final total = await _getElectronicSales.getProductTotal(
        yearMonth: yearMonth,
        productCode: productCode,
      );

      if (total != null) {
        state = state.copyWith(
          sales: [total],
          filter: ElectronicSalesFilter(
            yearMonth: yearMonth,
            productCode: productCode,
          ),
          isLoading: false,
          totalAmount: total.amount,
          totalQuantity: total.quantity,
        );
      } else {
        state = state.copyWith(
          sales: [],
          isLoading: false,
        );
      }
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 필터 업데이트 및 재조회
  Future<void> updateFilter(ElectronicSalesFilter filter) async {
    await fetchSales(
      yearMonth: filter.yearMonth,
      customerName: filter.customerName,
      productName: filter.productName,
      productCode: filter.productCode,
    );
  }

  /// 필터 초기화
  Future<void> resetFilter() async {
    final defaultFilter = ElectronicSalesFilter.defaultFilter();
    await updateFilter(defaultFilter);
  }
}

/// 전산매출 StateNotifier Provider
final electronicSalesProvider =
    StateNotifierProvider<ElectronicSalesNotifier, ElectronicSalesState>((ref) {
  final useCase = ref.watch(getElectronicSalesUseCaseProvider);
  return ElectronicSalesNotifier(useCase);
});
