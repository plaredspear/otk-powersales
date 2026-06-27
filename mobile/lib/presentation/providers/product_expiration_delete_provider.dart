import '../../core/utils/error_utils.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/product_expiration_item.dart';
import '../../domain/usecases/delete_product_expiration_batch_usecase.dart';
import 'product_expiration_delete_state.dart';
import 'product_expiration_list_provider.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// DeleteProductExpirationBatch UseCase Provider
final deleteProductExpirationBatchUseCaseProvider =
    Provider<DeleteProductExpirationBatch>((ref) {
  final repository = ref.watch(productExpirationRepositoryProvider);
  return DeleteProductExpirationBatch(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 소비기한 삭제 상태 관리 Notifier
class ProductExpirationDeleteNotifier extends StateNotifier<ProductExpirationDeleteState> {
  final DeleteProductExpirationBatch _deleteBatch;

  ProductExpirationDeleteNotifier({
    required DeleteProductExpirationBatch deleteBatch,
  })  : _deleteBatch = deleteBatch,
        super(ProductExpirationDeleteState.initial());

  /// 삭제 화면 초기화 (관리 화면에서 전달받은 목록 설정)
  void setItems(List<ProductExpirationItem> items) {
    state = state.copyWith(items: items, selectedSeqs: {});
  }

  /// 개별 항목 선택/해제 토글
  void toggleItem(int seq) {
    final selected = Set<int>.from(state.selectedSeqs);
    if (selected.contains(seq)) {
      selected.remove(seq);
    } else {
      selected.add(seq);
    }
    state = state.copyWith(selectedSeqs: selected);
  }

  /// 전체 선택/해제 토글
  void toggleAll() {
    if (state.isAllSelected) {
      state = state.copyWith(selectedSeqs: {});
    } else {
      final allSeqs = state.items.map((e) => e.seq).toSet();
      state = state.copyWith(selectedSeqs: allSeqs);
    }
  }

  /// 그룹 선택/해제 토글 (expired: true → 만료 그룹, false → 만료 전 그룹)
  void toggleGroup({required bool expired}) {
    final groupItems =
        expired ? state.expiredItems : state.activeItems;
    final groupSeqs = groupItems.map((e) => e.seq).toSet();

    final selected = Set<int>.from(state.selectedSeqs);
    final isGroupSelected = groupSeqs.every(selected.contains);

    if (isGroupSelected) {
      selected.removeAll(groupSeqs);
    } else {
      selected.addAll(groupSeqs);
    }
    state = state.copyWith(selectedSeqs: selected);
  }

  /// 선택된 항목 일괄 삭제
  Future<void> deleteSelected() async {
    if (!state.canDelete) return;

    state = state.toLoading();

    try {
      await _deleteBatch.call(state.selectedSeqs.toList());

      state = state.copyWith(
        isLoading: false,
        isDeleted: true,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        extractErrorMessage(e),
      );
    }
  }

  /// 에러 초기화
  void clearError() {
    state = state.copyWith(errorMessage: null);
  }
}

// ============================================
// 3. StateNotifier Provider Definition
// ============================================

/// ProductExpirationDelete StateNotifier Provider
final productExpirationDeleteProvider =
    StateNotifierProvider<ProductExpirationDeleteNotifier, ProductExpirationDeleteState>(
        (ref) {
  final useCase = ref.watch(deleteProductExpirationBatchUseCaseProvider);

  return ProductExpirationDeleteNotifier(
    deleteBatch: useCase,
  );
});
