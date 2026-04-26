import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../domain/usecases/get_claim_detail_usecase.dart';
import 'claim_detail_state.dart';
import 'claim_list_provider.dart';

// ============================================
// 1. Dependency Providers
// ============================================

final getClaimDetailUseCaseProvider = Provider<GetClaimDetailUseCase>((ref) {
  return GetClaimDetailUseCase(ref.watch(claimQueryRepositoryProvider));
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

class ClaimDetailNotifier extends StateNotifier<ClaimDetailState> {
  final GetClaimDetailUseCase _getClaimDetail;

  ClaimDetailNotifier({required GetClaimDetailUseCase getClaimDetail})
      : _getClaimDetail = getClaimDetail,
        super(const ClaimDetailState());

  Future<void> loadDetail(int claimId) async {
    state = state.toLoading();
    try {
      final detail = await _getClaimDetail.call(claimId);
      state = state.copyWith(isLoading: false, detail: detail);
    } catch (e) {
      state = state.toError(e.toString().replaceFirst('Exception: ', ''));
    }
  }

  void clearError() {
    state = state.copyWith(clearErrorMessage: true);
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

final claimDetailProvider =
    StateNotifierProvider<ClaimDetailNotifier, ClaimDetailState>((ref) {
  return ClaimDetailNotifier(
    getClaimDetail: ref.watch(getClaimDetailUseCaseProvider),
  );
});
