import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/providers/order_cancel_state.dart';

void main() {
  // 테스트용 제품 데이터
  final item1 = OrderedItem(
    productCode: '01101123',
    productName: '갈릭 아이올리소스 240g',
    totalQuantityBoxes: 5,
    totalQuantityPieces: 100,
    isCancelled: false,
  );

  final item2 = OrderedItem(
    productCode: '01101456',
    productName: '토마토 케첩 500g',
    totalQuantityBoxes: 10,
    totalQuantityPieces: 200,
    isCancelled: false,
  );

  final cancelledItem = OrderedItem(
    productCode: '01101789',
    productName: '마요네즈 300g',
    totalQuantityBoxes: 3,
    totalQuantityPieces: 60,
    isCancelled: true,
  );

  group('OrderCancelState', () {
    group('initial factory', () {
      test('should filter out cancelled items', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, cancelledItem, item2],
        );

        expect(state.orderId, 1);
        expect(state.cancellableItems.length, 2);
        expect(state.cancellableItems[0], item1);
        expect(state.cancellableItems[1], item2);
        expect(state.selectedProductCodes, isEmpty);
        expect(state.isCancelling, false);
        expect(state.errorMessage, isNull);
        expect(state.cancelSuccess, false);
      });

      test('should return empty list when all items are cancelled', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [cancelledItem],
        );

        expect(state.cancellableItems, isEmpty);
        expect(state.hasNoCancellableItems, true);
      });

      test('should include all items when none are cancelled', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        );

        expect(state.cancellableItems.length, 2);
      });
    });

    group('derived states', () {
      test('isAllSelected should be false when no items selected', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        );

        expect(state.isAllSelected, false);
      });

      test('isAllSelected should be true when all cancellable items selected', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        ).copyWith(
          selectedProductCodes: {'01101123', '01101456'},
        );

        expect(state.isAllSelected, true);
      });

      test('isAllSelected should be false with empty cancellable items', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [cancelledItem],
        );

        expect(state.isAllSelected, false);
      });

      test('selectedCount should return correct count', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        ).copyWith(
          selectedProductCodes: {'01101123'},
        );

        expect(state.selectedCount, 1);
      });

      test('canCancel should be true when items selected and not cancelling', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        ).copyWith(
          selectedProductCodes: {'01101123'},
        );

        expect(state.canCancel, true);
      });

      test('canCancel should be false when no items selected', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        );

        expect(state.canCancel, false);
      });

      test('canCancel should be false when cancelling', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        ).copyWith(
          selectedProductCodes: {'01101123'},
          isCancelling: true,
        );

        expect(state.canCancel, false);
      });

      test('hasNoCancellableItems should be true when empty', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [],
        );

        expect(state.hasNoCancellableItems, true);
      });

      test('hasNoCancellableItems should be false when items exist', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        );

        expect(state.hasNoCancellableItems, false);
      });
    });

    group('state transitions', () {
      test('toLoading should set isCancelling true and clear error', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        ).copyWith(
          selectedProductCodes: {'01101123'},
          errorMessage: 'previous error',
        );

        final loading = state.toLoading();

        expect(loading.isCancelling, true);
        expect(loading.errorMessage, isNull);
        expect(loading.selectedProductCodes, {'01101123'});
      });

      test('toError should set error message and stop loading', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        ).copyWith(isCancelling: true);

        final error = state.toError('취소 실패');

        expect(error.isCancelling, false);
        expect(error.errorMessage, '취소 실패');
      });

      test('toSuccess should set cancelSuccess true and clear error', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        ).copyWith(isCancelling: true);

        final success = state.toSuccess();

        expect(success.isCancelling, false);
        expect(success.cancelSuccess, true);
        expect(success.errorMessage, isNull);
      });
    });

    group('copyWith', () {
      test('should copy with new values', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1, item2],
        );

        final copied = state.copyWith(
          selectedProductCodes: {'01101123'},
          isCancelling: true,
        );

        expect(copied.orderId, 1);
        expect(copied.cancellableItems.length, 2);
        expect(copied.selectedProductCodes, {'01101123'});
        expect(copied.isCancelling, true);
        expect(copied.errorMessage, isNull);
        expect(copied.cancelSuccess, false);
      });

      test('should preserve existing values when not specified', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        ).copyWith(
          selectedProductCodes: {'01101123'},
          errorMessage: 'test error',
        );

        final copied = state.copyWith(isCancelling: true);

        expect(copied.selectedProductCodes, {'01101123'});
        expect(copied.errorMessage, 'test error');
        expect(copied.isCancelling, true);
      });

      test('clearError should set errorMessage to null', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        ).copyWith(errorMessage: 'error');

        final cleared = state.copyWith(clearError: true);

        expect(cleared.errorMessage, isNull);
      });

      test('clearError should override provided errorMessage', () {
        final state = OrderCancelState.initial(
          orderId: 1,
          allItems: [item1],
        ).copyWith(errorMessage: 'old error');

        final cleared = state.copyWith(
          clearError: true,
          errorMessage: 'new error',
        );

        expect(cleared.errorMessage, isNull);
      });
    });
  });
}
