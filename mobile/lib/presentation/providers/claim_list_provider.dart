import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/network/dio_provider.dart';
import '../../data/datasources/claim_api_datasource.dart';
import '../../data/repositories/claim_repository_impl.dart';
import '../../domain/repositories/claim_repository.dart';
import '../../domain/usecases/get_claims_usecase.dart';
import 'claim_list_state.dart';

// ============================================
// 1. Dependency Providers
// ============================================

final claimApiDataSourceProvider = Provider<ClaimApiDataSource>((ref) {
  final dio = ref.watch(dioProvider);
  return ClaimApiDataSource(dio);
});

final claimQueryRepositoryProvider = Provider<ClaimRepository>((ref) {
  final dataSource = ref.watch(claimApiDataSourceProvider);
  return ClaimRepositoryImpl(dataSource);
});

final getClaimsUseCaseProvider = Provider<GetClaimsUseCase>((ref) {
  return GetClaimsUseCase(ref.watch(claimQueryRepositoryProvider));
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

class ClaimListNotifier extends StateNotifier<ClaimListState> {
  final GetClaimsUseCase _getClaims;

  ClaimListNotifier({required GetClaimsUseCase getClaims})
      : _getClaims = getClaims,
        super(ClaimListState.initial());

  Future<void> loadClaims() async {
    state = state.toLoading();
    try {
      final startDate =
          '${state.startDate.year}-${state.startDate.month.toString().padLeft(2, '0')}-${state.startDate.day.toString().padLeft(2, '0')}';
      final endDate =
          '${state.endDate.year}-${state.endDate.month.toString().padLeft(2, '0')}-${state.endDate.day.toString().padLeft(2, '0')}';

      final items = await _getClaims.call(
        startDate: startDate,
        endDate: endDate,
        accountId: state.selectedAccountId,
      );
      state = state.copyWith(
        isLoading: false,
        items: items,
        hasSearched: true,
        visibleCount: ClaimListState.pageSize,
      );
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  /// 다음 페이지 노출 (클라이언트 페이징 — 이미 받아둔 목록에서 20건씩 추가 표시).
  void loadMore() {
    if (!state.hasMore) return;
    final next = (state.visibleCount + ClaimListState.pageSize)
        .clamp(0, state.items.length);
    state = state.copyWith(visibleCount: next);
  }

  /// 시작일 변경. 종료일과의 간격이 최대 일수를 넘으면 종료일을 자동 보정한다.
  void updateStartDate(DateTime date) {
    const maxDays = ClaimListState.maxRangeDays;
    var end = state.endDate;
    if (date.isAfter(end)) {
      end = date;
    } else if (end.difference(date).inDays > maxDays) {
      end = date.add(const Duration(days: maxDays));
    }
    state = state.copyWith(startDate: date, endDate: end);
  }

  /// 종료일 변경. 시작일과의 간격이 최대 일수를 넘으면 시작일을 자동 보정한다.
  void updateEndDate(DateTime date) {
    const maxDays = ClaimListState.maxRangeDays;
    var start = state.startDate;
    if (date.isBefore(start)) {
      start = date;
    } else if (date.difference(start).inDays > maxDays) {
      start = date.subtract(const Duration(days: maxDays));
    }
    state = state.copyWith(startDate: start, endDate: date);
  }

  /// 거래처 필터 선택 (특정 거래처).
  void selectAccount(int accountId, String accountName) {
    state = state.copyWith(
      selectedAccountId: accountId,
      selectedAccountName: accountName,
    );
  }

  /// 거래처 필터 해제 (거래처 전체).
  void clearAccount() {
    state = state.copyWith(clearAccount: true);
  }

  void clearError() {
    state = state.copyWith(clearErrorMessage: true);
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

final claimListProvider =
    StateNotifierProvider<ClaimListNotifier, ClaimListState>((ref) {
  return ClaimListNotifier(
    getClaims: ref.watch(getClaimsUseCaseProvider),
  );
});
