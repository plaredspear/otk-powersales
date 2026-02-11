import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/entities/shelf_life_item.dart';
import '../../domain/usecases/delete_shelf_life_batch_usecase.dart';
import 'shelf_life_delete_state.dart';
import 'shelf_life_list_provider.dart';

// ============================================
// 1. Dependency Providers
// ============================================

/// DeleteShelfLifeBatch UseCase Provider
final deleteShelfLifeBatchUseCaseProvider =
    Provider<DeleteShelfLifeBatch>((ref) {
  final repository = ref.watch(shelfLifeRepositoryProvider);
  return DeleteShelfLifeBatch(repository);
});

// ============================================
// 2. StateNotifier Implementation
// ============================================

/// 유통기한 삭제 상태 관리 Notifier
class ShelfLifeDeleteNotifier extends StateNotifier<ShelfLifeDeleteState> {
  final DeleteShelfLifeBatch _deleteBatch;

  ShelfLifeDeleteNotifier({
    required DeleteShelfLifeBatch deleteBatch,
  })  : _deleteBatch = deleteBatch,
        super(ShelfLifeDeleteState.initial());

  /// 삭제 화면 초기화 (관리 화면에서 전달받은 목록 설정)
  void setItems(List<ShelfLifeItem> items) {
    state = state.copyWith(items: items, selectedIds: {});
  }

  /// 개별 항목 선택/해제 토글
  void toggleItem(int id) {
    final selected = Set<int>.from(state.selectedIds);
    if (selected.contains(id)) {
      selected.remove(id);
    } else {
      selected.add(id);
    }
    state = state.copyWith(selectedIds: selected);
  }

  /// 전체 선택/해제 토글
  void toggleAll() {
    if (state.isAllSelected) {
      state = state.copyWith(selectedIds: {});
    } else {
      final allIds = state.items.map((e) => e.id).toSet();
      state = state.copyWith(selectedIds: allIds);
    }
  }

  /// 그룹 선택/해제 토글 (expired: true → 만료 그룹, false → 만료 전 그룹)
  void toggleGroup({required bool expired}) {
    final groupItems =
        expired ? state.expiredItems : state.activeItems;
    final groupIds = groupItems.map((e) => e.id).toSet();

    final selected = Set<int>.from(state.selectedIds);
    final isGroupSelected = groupIds.every(selected.contains);

    if (isGroupSelected) {
      selected.removeAll(groupIds);
    } else {
      selected.addAll(groupIds);
    }
    state = state.copyWith(selectedIds: selected);
  }

  /// 선택된 항목 일괄 삭제
  Future<void> deleteSelected() async {
    if (!state.canDelete) return;

    state = state.toLoading();

    try {
      await _deleteBatch.call(state.selectedIds.toList());

      state = state.copyWith(
        isLoading: false,
        isDeleted: true,
        errorMessage: null,
      );
    } catch (e) {
      state = state.toError(
        e.toString().replaceFirst('Exception: ', ''),
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

/// ShelfLifeDelete StateNotifier Provider
final shelfLifeDeleteProvider =
    StateNotifierProvider<ShelfLifeDeleteNotifier, ShelfLifeDeleteState>(
        (ref) {
  final useCase = ref.watch(deleteShelfLifeBatchUseCaseProvider);

  return ShelfLifeDeleteNotifier(
    deleteBatch: useCase,
  );
});
