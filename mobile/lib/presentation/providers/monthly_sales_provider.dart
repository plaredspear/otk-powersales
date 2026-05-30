import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/network/dio_provider.dart';
import '../../data/datasources/monthly_sales_api_datasource.dart';
import '../../data/datasources/monthly_sales_remote_datasource.dart';
import '../../data/repositories/monthly_sales_repository_impl.dart';
import '../../domain/repositories/monthly_sales_repository.dart';
import '../../domain/usecases/get_monthly_sales.dart';
import 'monthly_sales_state.dart';

// --- Dependency Providers ---

/// MonthlySales 원격 데이터소스 Provider (실 API)
final monthlySalesRemoteDataSourceProvider =
    Provider<MonthlySalesRemoteDataSource>((ref) {
  return MonthlySalesApiDataSource(ref.watch(dioProvider));
});

/// MonthlySales Repository Provider (실 API — MonthlySalesController)
final monthlySalesRepositoryProvider = Provider<MonthlySalesRepository>((ref) {
  return MonthlySalesRepositoryImpl(
    remoteDataSource: ref.watch(monthlySalesRemoteDataSourceProvider),
  );
});

/// GetMonthlySales UseCase Provider
final getMonthlySalesUseCaseProvider = Provider<GetMonthlySalesUseCase>((ref) {
  final repository = ref.watch(monthlySalesRepositoryProvider);
  return GetMonthlySalesUseCase(repository);
});

// --- MonthlySalesNotifier ---

/// 월매출 화면 상태 관리 Notifier
///
/// 거래처 선택, 연월 이동, 월매출 조회 기능을 관리합니다.
class MonthlySalesNotifier extends StateNotifier<MonthlySalesState> {
  final GetMonthlySalesUseCase _getMonthlySales;

  MonthlySalesNotifier({
    required GetMonthlySalesUseCase getMonthlySales,
  })  : _getMonthlySales = getMonthlySales,
        super(MonthlySalesState.initial());

  /// 초기 데이터 로딩
  Future<void> initialize() async {
    await loadMonthlySales();
  }

  /// 월매출 조회
  Future<void> loadMonthlySales() async {
    try {
      state = state.toLoading();

      final result = await _getMonthlySales.call(
        customerId: state.selectedCustomerId,
        yearMonth: state.yearMonth,
      );

      state = state.copyWith(
        isLoading: false,
        monthlySales: result,
      );
    } catch (e) {
      state = state.toError(e.toString());
    }
  }

  /// 거래처 선택
  Future<void> setCustomer(String? customerId) async {
    state = state.copyWith(
      selectedCustomerId: customerId,
    );
    await loadMonthlySales();
  }

  /// 거래처 필터 초기화
  Future<void> clearCustomerFilter() async {
    state = state.copyWith(
      clearCustomerFilter: true,
    );
    await loadMonthlySales();
  }

  /// 이전 월로 이동
  Future<void> goToPreviousMonth() async {
    if (!state.canGoToPreviousMonth || state.isLoading) return;

    final previousMonth = state.getPreviousMonth();
    state = state.copyWith(yearMonth: previousMonth);
    await loadMonthlySales();
  }

  /// 다음 월로 이동
  Future<void> goToNextMonth() async {
    if (!state.canGoToNextMonth || state.isLoading) return;

    final nextMonth = state.getNextMonth();
    state = state.copyWith(yearMonth: nextMonth);
    await loadMonthlySales();
  }

  /// 특정 연월로 이동
  Future<void> goToYearMonth(String yearMonth) async {
    state = state.copyWith(yearMonth: yearMonth);
    await loadMonthlySales();
  }

  /// 새로고침
  Future<void> refresh() async {
    await loadMonthlySales();
  }
}

/// Monthly Sales Provider
final monthlySalesProvider =
    StateNotifierProvider<MonthlySalesNotifier, MonthlySalesState>((ref) {
  final getMonthlySales = ref.watch(getMonthlySalesUseCaseProvider);
  return MonthlySalesNotifier(getMonthlySales: getMonthlySales);
});
