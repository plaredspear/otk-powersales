import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/presentation/providers/client_order_detail_state.dart';

void main() {
  group('ClientOrderDetailState', () {
    test('initial() has correct defaults', () {
      // Act
      final state = ClientOrderDetailState.initial();

      // Assert
      expect(state.isLoading, false);
      expect(state.errorMessage, isNull);
      expect(state.orderDetail, isNull);
    });

    test('hasData returns false when orderDetail is null', () {
      // Arrange
      final state = ClientOrderDetailState.initial();

      // Assert
      expect(state.hasData, false);
    });

    test('hasData returns true when orderDetail is set', () {
      // Arrange
      final orderDetail = ClientOrderDetail(
        sapOrderNumber: '300011396',
        clientId: 2,
        clientName: '(유)경산식품',
        clientDeadlineTime: '14:00',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 3763740,
        orderedItemCount: 2,
        orderedItems: [
          const ClientOrderItem(
            productCode: 'P001',
            productName: '진라면',
            deliveredQuantity: '10 BOX',
            deliveryStatus: DeliveryStatus.waiting,
          ),
        ],
      );
      final state = ClientOrderDetailState.initial().copyWith(
        orderDetail: orderDetail,
      );

      // Assert
      expect(state.hasData, true);
    });

    test('toLoading() sets isLoading true and clears error', () {
      // Arrange
      final state = ClientOrderDetailState.initial().copyWith(
        errorMessage: 'Previous error',
      );

      // Act
      final loadingState = state.toLoading();

      // Assert
      expect(loadingState.isLoading, true);
      expect(loadingState.errorMessage, isNull);
    });

    test('toError() sets errorMessage and isLoading false', () {
      // Arrange
      final state = ClientOrderDetailState.initial().toLoading();
      const errorMessage = 'Failed to load order detail';

      // Act
      final errorState = state.toError(errorMessage);

      // Assert
      expect(errorState.isLoading, false);
      expect(errorState.errorMessage, errorMessage);
    });

    test('copyWith preserves fields', () {
      // Arrange
      final orderDetail = ClientOrderDetail(
        sapOrderNumber: '300011396',
        clientId: 2,
        clientName: '(유)경산식품',
        clientDeadlineTime: '14:00',
        orderDate: DateTime(2026, 2, 5),
        deliveryDate: DateTime(2026, 2, 8),
        totalApprovedAmount: 3763740,
        orderedItemCount: 2,
        orderedItems: [
          const ClientOrderItem(
            productCode: 'P001',
            productName: '진라면',
            deliveredQuantity: '10 BOX',
            deliveryStatus: DeliveryStatus.waiting,
          ),
        ],
      );
      final state = ClientOrderDetailState.initial().copyWith(
        isLoading: true,
        orderDetail: orderDetail,
      );

      // Act - copyWith with no args preserves isLoading and orderDetail
      // Note: errorMessage is not preserved by copyWith() since it passes null directly
      final copiedState = state.copyWith();

      // Assert
      expect(copiedState.isLoading, state.isLoading);
      expect(copiedState.errorMessage, isNull); // errorMessage is not preserved by design
      expect(copiedState.orderDetail, state.orderDetail);
    });
  });
}
