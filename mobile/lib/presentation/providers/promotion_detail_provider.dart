import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/utils/error_utils.dart';
import '../../domain/entities/promotion.dart';
import '../../domain/repositories/promotion_repository.dart';
import 'promotion_list_provider.dart';

// ============================================
// 1. State
// ============================================

class PromotionDetailState {
  final bool isLoading;
  final String? errorMessage;
  final PromotionDetail? detail;

  const PromotionDetailState({
    this.isLoading = false,
    this.errorMessage,
    this.detail,
  });

  PromotionDetailState toLoading() =>
      PromotionDetailState(isLoading: true, detail: detail);

  PromotionDetailState toError(String message) =>
      PromotionDetailState(errorMessage: message, detail: detail);

  PromotionDetailState copyWith({
    bool? isLoading,
    String? errorMessage,
    PromotionDetail? detail,
  }) {
    return PromotionDetailState(
      isLoading: isLoading ?? this.isLoading,
      errorMessage: errorMessage,
      detail: detail ?? this.detail,
    );
  }
}

// ============================================
// 2. StateNotifier
// ============================================

class PromotionDetailNotifier extends StateNotifier<PromotionDetailState> {
  final PromotionRepository _repository;

  PromotionDetailNotifier({required PromotionRepository repository})
      : _repository = repository,
        super(const PromotionDetailState());

  Future<void> loadPromotion(int id) async {
    state = state.toLoading();

    try {
      final detail = await _repository.getPromotion(id);
      state = PromotionDetailState(detail: detail);
    } catch (e) {
      state = state.toError(extractErrorMessage(e));
    }
  }

  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

// ============================================
// 3. Provider
// ============================================

final promotionDetailProvider =
    StateNotifierProvider<PromotionDetailNotifier, PromotionDetailState>((ref) {
  final repository = ref.watch(promotionRepositoryProvider);
  return PromotionDetailNotifier(repository: repository);
});
