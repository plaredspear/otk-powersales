import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../data/repositories/mock/logistics_sales_mock_repository.dart';
import '../../domain/entities/logistics_sales.dart';
import '../../domain/repositories/logistics_sales_repository.dart';
import '../../domain/usecases/get_logistics_sales.dart';
import 'logistics_sales_state.dart';

/// 물류매출 Repository Provider
final logisticsSalesRepositoryProvider =
    Provider<LogisticsSalesRepository>((ref) {
  return LogisticsSalesMockRepository();
});

/// 물류매출 UseCase Provider
final getLogisticsSalesUseCaseProvider = Provider<GetLogisticsSales>((ref) {
  final repository = ref.watch(logisticsSalesRepositoryProvider);
  return GetLogisticsSales(repository);
});

/// 물류매출 상태 관리 Provider
class LogisticsSalesNotifier extends StateNotifier<LogisticsSalesState> {
  LogisticsSalesNotifier(this._getLogisticsSales)
      : super(LogisticsSalesState.initial());

  final GetLogisticsSales _getLogisticsSales;

  /// 물류매출 조회 (당월/이전월 자동 구분)
  Future<void> fetchSales({
    String? yearMonth,
    LogisticsCategory? category,
    String? customerName,
  }) async {
    // 로딩 상태로 전환
    state = state.copyWith(
      isLoading: true,
      errorMessage: null,
      clearErrorMessage: true,
    );

    try {
      // 필터 업데이트
      final filter = LogisticsSalesFilter(
        yearMonth: yearMonth ?? state.filter.yearMonth,
        category: category ?? state.filter.category,
        customerName: customerName ?? state.filter.customerName,
      );

      // 당월 여부 판단
      final isCurrentMonth = filter.yearMonth == state.currentYearMonth;

      // UseCase 호출 (당월/이전월 자동 구분)
      final sales = await _getLogisticsSales.getSalesByMonthType(
        yearMonth: filter.yearMonth,
        currentYearMonth: state.currentYearMonth,
        category: filter.category,
      );

      // 합계 계산
      final totalAmount = _getLogisticsSales.calculateTotalAmount(sales);
      final totalPreviousYearAmount =
          _getLogisticsSales.calculateTotalPreviousYearAmount(sales);
      final totalDifference =
          _getLogisticsSales.calculateTotalDifference(sales);
      final totalGrowthRate =
          _getLogisticsSales.calculateTotalGrowthRate(sales);

      // 카테고리별 합계 및 목록
      final categoryTotals = _getLogisticsSales.calculateCategoryTotals(sales);
      final categoryList = _getLogisticsSales.extractCategories(sales);

      // 성공 상태로 전환
      state = state.copyWith(
        sales: sales,
        filter: filter,
        isLoading: false,
        isCurrentMonth: isCurrentMonth,
        totalAmount: totalAmount,
        totalPreviousYearAmount: totalPreviousYearAmount,
        totalDifference: totalDifference,
        totalGrowthRate: totalGrowthRate,
        categoryTotals: categoryTotals,
        categoryList: categoryList,
      );
    } catch (e) {
      // 에러 상태로 전환
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 카테고리별 물류매출 조회
  Future<void> fetchSalesByCategory({
    required String yearMonth,
    required LogisticsCategory category,
  }) async {
    state = state.copyWith(
      isLoading: true,
      errorMessage: null,
      clearErrorMessage: true,
    );

    try {
      final sales = await _getLogisticsSales.getByCategory(
        yearMonth: yearMonth,
        category: category,
      );

      final isCurrentMonth = yearMonth == state.currentYearMonth;
      final totalAmount = _getLogisticsSales.calculateTotalAmount(sales);
      final totalPreviousYearAmount =
          _getLogisticsSales.calculateTotalPreviousYearAmount(sales);
      final totalDifference =
          _getLogisticsSales.calculateTotalDifference(sales);
      final totalGrowthRate =
          _getLogisticsSales.calculateTotalGrowthRate(sales);
      final categoryTotals = _getLogisticsSales.calculateCategoryTotals(sales);
      final categoryList = _getLogisticsSales.extractCategories(sales);

      state = state.copyWith(
        sales: sales,
        filter: LogisticsSalesFilter(
          yearMonth: yearMonth,
          category: category,
        ),
        isLoading: false,
        isCurrentMonth: isCurrentMonth,
        totalAmount: totalAmount,
        totalPreviousYearAmount: totalPreviousYearAmount,
        totalDifference: totalDifference,
        totalGrowthRate: totalGrowthRate,
        categoryTotals: categoryTotals,
        categoryList: categoryList,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 월별 추이 조회 (차트용)
  Future<List<LogisticsSales>> fetchTrend({
    required String startYearMonth,
    required String endYearMonth,
    required LogisticsCategory category,
  }) async {
    try {
      return await _getLogisticsSales.getTrend(
        startYearMonth: startYearMonth,
        endYearMonth: endYearMonth,
        category: category,
      );
    } catch (e) {
      state = state.copyWith(
        errorMessage: e.toString(),
      );
      return [];
    }
  }

  /// 당월 물류예상실적 조회
  Future<void> fetchCurrentMonthSales({
    LogisticsCategory? category,
  }) async {
    state = state.copyWith(
      isLoading: true,
      errorMessage: null,
      clearErrorMessage: true,
    );

    try {
      final sales = await _getLogisticsSales.getCurrentMonthSales(
        category: category,
      );

      final totalAmount = _getLogisticsSales.calculateTotalAmount(sales);
      final totalPreviousYearAmount =
          _getLogisticsSales.calculateTotalPreviousYearAmount(sales);
      final totalDifference =
          _getLogisticsSales.calculateTotalDifference(sales);
      final totalGrowthRate =
          _getLogisticsSales.calculateTotalGrowthRate(sales);
      final categoryTotals = _getLogisticsSales.calculateCategoryTotals(sales);
      final categoryList = _getLogisticsSales.extractCategories(sales);

      state = state.copyWith(
        sales: sales,
        filter: LogisticsSalesFilter(
          yearMonth: state.currentYearMonth,
          category: category,
        ),
        isLoading: false,
        isCurrentMonth: true,
        totalAmount: totalAmount,
        totalPreviousYearAmount: totalPreviousYearAmount,
        totalDifference: totalDifference,
        totalGrowthRate: totalGrowthRate,
        categoryTotals: categoryTotals,
        categoryList: categoryList,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 이전월 ABC물류배부 마감실적 조회
  Future<void> fetchClosedSales({
    required String yearMonth,
    LogisticsCategory? category,
  }) async {
    state = state.copyWith(
      isLoading: true,
      errorMessage: null,
      clearErrorMessage: true,
    );

    try {
      final sales = await _getLogisticsSales.getClosedSales(
        yearMonth: yearMonth,
        category: category,
      );

      final totalAmount = _getLogisticsSales.calculateTotalAmount(sales);
      final totalPreviousYearAmount =
          _getLogisticsSales.calculateTotalPreviousYearAmount(sales);
      final totalDifference =
          _getLogisticsSales.calculateTotalDifference(sales);
      final totalGrowthRate =
          _getLogisticsSales.calculateTotalGrowthRate(sales);
      final categoryTotals = _getLogisticsSales.calculateCategoryTotals(sales);
      final categoryList = _getLogisticsSales.extractCategories(sales);

      state = state.copyWith(
        sales: sales,
        filter: LogisticsSalesFilter(
          yearMonth: yearMonth,
          category: category,
        ),
        isLoading: false,
        isCurrentMonth: false,
        totalAmount: totalAmount,
        totalPreviousYearAmount: totalPreviousYearAmount,
        totalDifference: totalDifference,
        totalGrowthRate: totalGrowthRate,
        categoryTotals: categoryTotals,
        categoryList: categoryList,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// 필터 업데이트 및 재조회
  Future<void> updateFilter(LogisticsSalesFilter filter) async {
    await fetchSales(
      yearMonth: filter.yearMonth,
      category: filter.category,
      customerName: filter.customerName,
    );
  }

  /// 카테고리 필터 변경
  Future<void> updateCategory(LogisticsCategory? category) async {
    final newFilter = state.filter.copyWith(
      category: category,
      clearCategory: category == null,
    );
    await updateFilter(newFilter);
  }

  /// 년월 변경
  Future<void> updateYearMonth(String yearMonth) async {
    final newFilter = state.filter.copyWith(yearMonth: yearMonth);
    await updateFilter(newFilter);
  }

  /// 필터 초기화
  Future<void> resetFilter() async {
    final defaultFilter = LogisticsSalesFilter.defaultFilter();
    await updateFilter(defaultFilter);
  }
}

/// 물류매출 StateNotifier Provider
final logisticsSalesProvider =
    StateNotifierProvider<LogisticsSalesNotifier, LogisticsSalesState>((ref) {
  final useCase = ref.watch(getLogisticsSalesUseCaseProvider);
  return LogisticsSalesNotifier(useCase);
});
