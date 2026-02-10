import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/presentation/providers/order_form_state.dart';

void main() {
  group('OrderFormState', () {
    group('initial()', () {
      test('creates correct default state', () {
        // Arrange & Act
        final state = OrderFormState.initial();

        // Assert
        expect(state.orderDraft.items, isEmpty);
        expect(state.orderDraft.clientId, isNull);
        expect(state.orderDraft.deliveryDate, isNull);
        expect(state.orderDraft.totalAmount, 0);
        expect(state.clients, isEmpty);
        expect(state.hasDraft, false);
        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.errorMessage, isNull);
        expect(state.successMessage, isNull);
        expect(state.validationErrors, isEmpty);
        expect(state.submitResult, isNull);
      });
    });

    group('toLoading()', () {
      test('sets isLoading true and clears error', () {
        // Arrange
        final state = OrderFormState.initial().copyWith(
          errorMessage: 'Test error',
          isLoading: false,
        );

        // Act
        final result = state.toLoading();

        // Assert
        expect(result.isLoading, true);
        expect(result.errorMessage, isNull);
      });
    });

    group('toError()', () {
      test('sets isLoading false, isSubmitting false, sets error message', () {
        // Arrange
        final state = OrderFormState.initial().copyWith(
          isLoading: true,
          isSubmitting: true,
        );

        // Act
        final result = state.toError('Test error message');

        // Assert
        expect(result.isLoading, false);
        expect(result.isSubmitting, false);
        expect(result.errorMessage, 'Test error message');
      });
    });

    group('copyWith()', () {
      test('with no args returns equivalent state', () {
        // Arrange
        final state = OrderFormState.initial();

        // Act
        final result = state.copyWith();

        // Assert
        expect(result.orderDraft, state.orderDraft);
        expect(result.clients, state.clients);
        expect(result.hasDraft, state.hasDraft);
        expect(result.isLoading, state.isLoading);
        expect(result.isSubmitting, state.isSubmitting);
        expect(result.errorMessage, state.errorMessage);
        expect(result.successMessage, state.successMessage);
        expect(result.validationErrors, state.validationErrors);
        expect(result.submitResult, state.submitResult);
      });

      test('with specific fields changes only those', () {
        // Arrange
        final state = OrderFormState.initial();
        final newDraft = _createTestDraft(clientId: 100);

        // Act
        final result = state.copyWith(
          orderDraft: newDraft,
          isLoading: true,
          hasDraft: true,
        );

        // Assert
        expect(result.orderDraft, newDraft);
        expect(result.isLoading, true);
        expect(result.hasDraft, true);
        // Other fields unchanged
        expect(result.clients, state.clients);
        expect(result.isSubmitting, state.isSubmitting);
        expect(result.errorMessage, state.errorMessage);
      });

      test('with clearError: true sets errorMessage to null', () {
        // Arrange
        final state = OrderFormState.initial().copyWith(
          errorMessage: 'Test error',
        );

        // Act
        final result = state.copyWith(clearError: true);

        // Assert
        expect(result.errorMessage, isNull);
      });

      test('with clearSuccess: true sets successMessage to null', () {
        // Arrange
        final state = OrderFormState.initial().copyWith(
          successMessage: 'Test success',
        );

        // Act
        final result = state.copyWith(clearSuccess: true);

        // Assert
        expect(result.successMessage, isNull);
      });

      test('with clearValidationErrors: true sets validationErrors to empty', () {
        // Arrange
        final errors = {
          '12345678': const ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: 'Test error',
          ),
        };
        final state = OrderFormState.initial().copyWith(
          validationErrors: errors,
        );

        // Act
        final result = state.copyWith(clearValidationErrors: true);

        // Assert
        expect(result.validationErrors, isEmpty);
      });

      test('with clearSubmitResult: true sets submitResult to null', () {
        // Arrange
        const submitResult = OrderSubmitResult(
          orderId: 1,
          orderRequestNumber: 'OP00000001',
          status: 'PENDING',
        );
        final state = OrderFormState.initial().copyWith(
          submitResult: submitResult,
        );

        // Act
        final result = state.copyWith(clearSubmitResult: true);

        // Assert
        expect(result.submitResult, isNull);
      });
    });

    group('computed getters', () {
      test('totalAmount returns orderDraft.calculatedTotalAmount', () {
        // Arrange
        final item1 = _createTestItem(
          productCode: '01234567',
          totalPrice: 72000,
        );
        final item2 = _createTestItem(
          productCode: '87654321',
          totalPrice: 48000,
        );
        final draft = _createTestDraft(
          items: [item1, item2],
          totalAmount: 120000, // ignored, calculated from items
        );
        final state = OrderFormState.initial().copyWith(orderDraft: draft);

        // Act & Assert
        expect(state.totalAmount, 72000 + 48000);
      });

      test('selectedClientId returns orderDraft.clientId', () {
        // Arrange
        final draft = _createTestDraft(clientId: 999);
        final state = OrderFormState.initial().copyWith(orderDraft: draft);

        // Act & Assert
        expect(state.selectedClientId, 999);
      });

      test('hasItems returns true when items exist', () {
        // Arrange
        final item = _createTestItem();
        final draft = _createTestDraft(items: [item]);
        final state = OrderFormState.initial().copyWith(orderDraft: draft);

        // Act & Assert
        expect(state.hasItems, true);
      });

      test('hasItems returns false when items empty', () {
        // Arrange
        final state = OrderFormState.initial();

        // Act & Assert
        expect(state.hasItems, false);
      });

      test('isEditMode returns true when orderDraft.id != null', () {
        // Arrange
        final draft = _createTestDraft(id: 42);
        final state = OrderFormState.initial().copyWith(orderDraft: draft);

        // Act & Assert
        expect(state.isEditMode, true);
      });

      test('isEditMode returns false when orderDraft.id == null', () {
        // Arrange
        final draft = _createTestDraft(id: null);
        final state = OrderFormState.initial().copyWith(orderDraft: draft);

        // Act & Assert
        expect(state.isEditMode, false);
      });

      test('hasValidationErrors returns true when validationErrors not empty', () {
        // Arrange
        final errors = {
          '12345678': const ValidationError(
            errorType: ValidationErrorType.minOrderQuantity,
            message: 'Test error',
          ),
        };
        final state = OrderFormState.initial().copyWith(
          validationErrors: errors,
        );

        // Act & Assert
        expect(state.hasValidationErrors, true);
      });

      test('hasValidationErrors returns false when validationErrors empty', () {
        // Arrange
        final state = OrderFormState.initial();

        // Act & Assert
        expect(state.hasValidationErrors, false);
      });

      test('allItemsSelected matches orderDraft.allItemsSelected', () {
        // Arrange
        final item1 = _createTestItem(productCode: '01234567', isSelected: true);
        final item2 = _createTestItem(productCode: '87654321', isSelected: true);
        final draft = _createTestDraft(items: [item1, item2]);
        final state = OrderFormState.initial().copyWith(orderDraft: draft);

        // Act & Assert
        expect(state.allItemsSelected, true);
        expect(state.allItemsSelected, draft.allItemsSelected);
      });
    });
  });
}

// --- Test Helper Functions ---

OrderDraft _createTestDraft({
  int? id,
  int? clientId,
  String? clientName,
  int? creditBalance,
  DateTime? deliveryDate,
  List<OrderDraftItem> items = const [],
  int totalAmount = 0,
}) {
  return OrderDraft(
    id: id,
    clientId: clientId,
    clientName: clientName,
    creditBalance: creditBalance,
    deliveryDate: deliveryDate,
    items: items,
    totalAmount: totalAmount,
    isDraft: true,
    lastModified: DateTime(2026, 2, 10),
  );
}

OrderDraftItem _createTestItem({
  String productCode = '01234567',
  String productName = '진라면',
  double quantityBoxes = 5.0,
  int quantityPieces = 0,
  int unitPrice = 1200,
  int boxSize = 20,
  int totalPrice = 72000,
  bool isSelected = false,
}) {
  return OrderDraftItem(
    productCode: productCode,
    productName: productName,
    quantityBoxes: quantityBoxes,
    quantityPieces: quantityPieces,
    unitPrice: unitPrice,
    boxSize: boxSize,
    totalPrice: totalPrice,
    isSelected: isSelected,
  );
}
