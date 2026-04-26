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
      );
      state = state.copyWith(
        isLoading: false,
        items: items,
        hasSearched: true,
      );
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  void updateStartDate(DateTime date) {
    state = state.copyWith(startDate: date);
  }

  void updateEndDate(DateTime date) {
    state = state.copyWith(endDate: date);
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
