import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/delete_draft_order_usecase.dart';
import 'package:mobile/domain/usecases/get_credit_balance_usecase.dart';
import 'package:mobile/domain/usecases/load_draft_order_usecase.dart';
import 'package:mobile/domain/usecases/save_draft_order_usecase.dart';
import 'package:mobile/domain/usecases/submit_order_usecase.dart';
import 'package:mobile/domain/usecases/update_order_usecase.dart';
import 'package:mobile/domain/usecases/validate_order_usecase.dart';
import 'package:mobile/presentation/providers/order_form_provider.dart';
import 'package:mobile/presentation/providers/order_form_state.dart';

void main() {
  group('OrderFormNotifier', () {
    late FakeOrderRepository fakeRepo;
    late OrderFormNotifier notifier;

    OrderFormNotifier createNotifier() {
      return OrderFormNotifier(
        getCreditBalance: GetCreditBalance(fakeRepo),
        loadDraftOrder: LoadDraftOrder(fakeRepo),
        saveDraftOrder: SaveDraftOrder(fakeRepo),
        deleteDraftOrder: DeleteDraftOrder(fakeRepo),
        validateOrder: ValidateOrder(fakeRepo),
        submitOrder: SubmitOrder(fakeRepo),
        updateOrder: UpdateOrder(fakeRepo),
      );
    }

    setUp(() {
      fakeRepo = FakeOrderRepository();
      notifier = createNotifier();
    });

    group('initialize', () {
      test('임시저장 데이터 있으면 hasDraft = true', () async {
        // Arrange
        fakeRepo.draftToReturn = _createTestDraft(clientId: 100);

        // Act
        await notifier.initialize();

        // Assert
        expect(notifier.state.hasDraft, true);
      });

      test('임시저장 데이터 없으면 hasDraft = false', () async {
        // Arrange
        fakeRepo.draftToReturn = null;

        // Act
        await notifier.initialize();

        // Assert
        expect(notifier.state.hasDraft, false);
      });

      test('거래처 목록 설정', () async {
        // Arrange
        final clients = {100: '거래처A', 200: '거래처B'};

        // Act
        await notifier.initialize(clients: clients);

        // Assert
        expect(notifier.state.clients, clients);
      });

      test('수정 모드 시 orderId 받음', () async {
        // Arrange & Act
        await notifier.initialize(orderId: 42);

        // Assert - just verify no crash
        expect(notifier.state, isNotNull);
      });
    });

    group('loadDraftOrder', () {
      test('성공 시 orderDraft 설정', () async {
        // Arrange
        final draft = _createTestDraft(clientId: 100, clientName: '거래처A');
        fakeRepo.draftToReturn = draft;

        // Act
        await notifier.loadDraftOrder();

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.orderDraft, draft);
        expect(notifier.state.hasDraft, false);
        expect(notifier.state.errorMessage, isNull);
      });

      test('데이터 없으면 에러 메시지', () async {
        // Arrange
        fakeRepo.draftToReturn = null;

        // Act
        await notifier.loadDraftOrder();

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '불러올 임시저장 데이터가 없습니다.');
      });
    });

    group('initializeNewOrder', () {
      test('orderDraft를 빈 주문서로 초기화', () async {
        // Arrange - set some state first
        final draft = _createTestDraft(clientId: 100);
        notifier.state = notifier.state.copyWith(
          orderDraft: draft,
          hasDraft: true,
        );

        // Act
        notifier.initializeNewOrder();

        // Assert
        expect(notifier.state.orderDraft.items, isEmpty);
        expect(notifier.state.orderDraft.clientId, isNull);
        expect(notifier.state.hasDraft, false);
      });
    });

    group('selectClient', () {
      test('clientId와 clientName 설정', () async {
        // Arrange
        fakeRepo.creditBalanceToReturn = 50000000;

        // Act
        notifier.selectClient(100, '거래처A');

        // Wait for async fetchCreditBalance to complete
        await Future.delayed(Duration.zero);

        // Assert
        expect(notifier.state.orderDraft.clientId, 100);
        expect(notifier.state.orderDraft.clientName, '거래처A');
      });

      test('여신 잔액 조회도 실행', () async {
        // Arrange
        fakeRepo.creditBalanceToReturn = 50000000;

        // Act
        notifier.selectClient(100, '거래처A');

        // Wait for async fetchCreditBalance to complete
        await Future.delayed(Duration.zero);

        // Assert
        expect(notifier.state.orderDraft.creditBalance, 50000000);
      });
    });

    group('setDeliveryDate', () {
      test('납기일 설정', () {
        // Arrange
        final date = DateTime(2026, 3, 1);

        // Act
        notifier.setDeliveryDate(date);

        // Assert
        expect(notifier.state.orderDraft.deliveryDate, date);
      });
    });

    group('addProductToOrder', () {
      test('제품 추가', () {
        // Arrange
        final item = _createTestItem(productCode: '01234567');

        // Act
        notifier.addProductToOrder(item);

        // Assert
        expect(notifier.state.orderDraft.items.length, 1);
        expect(notifier.state.orderDraft.items[0], item);
      });

      test('중복 제품 추가 시 무시', () {
        // Arrange
        final item1 = _createTestItem(productCode: '01234567');
        final item2 = _createTestItem(productCode: '01234567', productName: '다른이름');

        // Act
        notifier.addProductToOrder(item1);
        notifier.addProductToOrder(item2);

        // Assert
        expect(notifier.state.orderDraft.items.length, 1);
        expect(notifier.state.orderDraft.items[0], item1);
      });

      test('totalAmount 재계산', () {
        // Arrange
        final item1 = _createTestItem(productCode: '01234567', totalPrice: 72000);
        final item2 = _createTestItem(productCode: '87654321', totalPrice: 48000);

        // Act
        notifier.addProductToOrder(item1);
        notifier.addProductToOrder(item2);

        // Assert
        expect(notifier.state.orderDraft.totalAmount, 120000);
      });
    });

    group('removeSelectedProducts', () {
      test('선택된 제품만 삭제', () {
        // Arrange
        final item1 = _createTestItem(productCode: '01234567', isSelected: true);
        final item2 = _createTestItem(productCode: '87654321', isSelected: false);
        notifier.addProductToOrder(item1);
        notifier.addProductToOrder(item2);

        // Act
        notifier.removeSelectedProducts();

        // Assert
        expect(notifier.state.orderDraft.items.length, 1);
        expect(notifier.state.orderDraft.items[0].productCode, '87654321');
      });

      test('totalAmount 재계산', () {
        // Arrange
        final item1 = _createTestItem(
          productCode: '01234567',
          totalPrice: 72000,
          isSelected: true,
        );
        final item2 = _createTestItem(
          productCode: '87654321',
          totalPrice: 48000,
          isSelected: false,
        );
        notifier.addProductToOrder(item1);
        notifier.addProductToOrder(item2);

        // Act
        notifier.removeSelectedProducts();

        // Assert
        expect(notifier.state.orderDraft.totalAmount, 48000);
      });
    });

    group('updateProductQuantity', () {
      test('수량 변경 및 총액 재계산', () {
        // Arrange
        final item = _createTestItem(
          productCode: '01234567',
          quantityBoxes: 5.0,
          quantityPieces: 0,
          unitPrice: 1200,
          boxSize: 20,
          totalPrice: 6000,
        );
        notifier.addProductToOrder(item);

        // Act - change to 10 boxes
        notifier.updateProductQuantity('01234567', 10.0, 0);

        // Assert
        final updatedItem = notifier.state.orderDraft.items[0];
        expect(updatedItem.quantityBoxes, 10.0);
        expect(updatedItem.quantityPieces, 0);
        // 10 boxes * 20 pieces/box = 200 pieces
        // 200 pieces * 1200 won/piece / 20 = 12000
        expect(updatedItem.totalPrice, 12000);
        expect(notifier.state.orderDraft.totalAmount, 12000);
      });

      test('validationError 클리어', () {
        // Arrange
        const error = ValidationError(
          errorType: ValidationErrorType.minOrderQuantity,
          message: 'Test error',
        );
        final item = _createTestItem(productCode: '01234567').copyWith(
          validationError: error,
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(items: [item]),
          validationErrors: {'01234567': error},
        );

        // Act
        notifier.updateProductQuantity('01234567', 10.0, 0);

        // Assert
        expect(notifier.state.validationErrors, isEmpty);
        expect(notifier.state.orderDraft.items[0].validationError, isNull);
      });
    });

    group('toggleProductSelection', () {
      test('선택 토글', () {
        // Arrange
        final item = _createTestItem(productCode: '01234567', isSelected: false);
        notifier.addProductToOrder(item);

        // Act
        notifier.toggleProductSelection('01234567');

        // Assert
        expect(notifier.state.orderDraft.items[0].isSelected, true);

        // Act - toggle again
        notifier.toggleProductSelection('01234567');

        // Assert
        expect(notifier.state.orderDraft.items[0].isSelected, false);
      });
    });

    group('toggleSelectAllProducts', () {
      test('전체 선택', () {
        // Arrange
        final item1 = _createTestItem(productCode: '01234567', isSelected: false);
        final item2 = _createTestItem(productCode: '87654321', isSelected: false);
        notifier.addProductToOrder(item1);
        notifier.addProductToOrder(item2);

        // Act
        notifier.toggleSelectAllProducts();

        // Assert
        expect(notifier.state.orderDraft.items[0].isSelected, true);
        expect(notifier.state.orderDraft.items[1].isSelected, true);
      });

      test('전체 해제', () {
        // Arrange
        final item1 = _createTestItem(productCode: '01234567', isSelected: true);
        final item2 = _createTestItem(productCode: '87654321', isSelected: true);
        notifier.addProductToOrder(item1);
        notifier.addProductToOrder(item2);

        // Act
        notifier.toggleSelectAllProducts();

        // Assert
        expect(notifier.state.orderDraft.items[0].isSelected, false);
        expect(notifier.state.orderDraft.items[1].isSelected, false);
      });
    });

    group('validateAndSubmitOrder', () {
      test('유효성 통과 → 전송 성공', () async {
        // Arrange
        final item = _createTestItem(
          productCode: '01234567',
          quantityBoxes: 5.0,
          quantityPieces: 10,
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: _createTestDraft(
            clientId: 100,
            clientName: '거래처A',
            deliveryDate: DateTime(2026, 3, 1),
            items: [item],
          ),
        );
        fakeRepo.validationResultToReturn = const ValidationResult(isValid: true);
        fakeRepo.submitResultToReturn = const OrderSubmitResult(
          orderId: 1,
          orderRequestNumber: 'OP00000001',
          status: 'PENDING',
        );

        // Act
        await notifier.validateAndSubmitOrder();

        // Assert
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.submitResult, isNotNull);
        expect(notifier.state.submitResult!.orderId, 1);
        expect(notifier.state.successMessage, '주문서가 성공적으로 전송되었습니다.');
        expect(notifier.state.errorMessage, isNull);
        expect(fakeRepo.submitOrderCalled, true);
      });

      test('유효성 실패 → 에러 적용', () async {
        // Arrange
        final item = _createTestItem(
          productCode: '01234567',
          quantityBoxes: 5.0,
          quantityPieces: 10,
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: _createTestDraft(
            clientId: 100,
            clientName: '거래처A',
            deliveryDate: DateTime(2026, 3, 1),
            items: [item],
          ),
        );
        const error = ValidationError(
          errorType: ValidationErrorType.minOrderQuantity,
          message: '최소 주문 수량 미달',
          minOrderQuantity: 10,
        );
        fakeRepo.validationResultToReturn = const ValidationResult(
          isValid: false,
          errors: {'01234567': error},
        );

        // Act
        await notifier.validateAndSubmitOrder();

        // Assert
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.validationErrors, isNotEmpty);
        expect(notifier.state.validationErrors['01234567'], error);
        expect(notifier.state.errorMessage, '유효성 검증 실패');
        expect(fakeRepo.submitOrderCalled, false);
      });

      test('전송 에러 → 에러 메시지', () async {
        // Arrange
        final item = _createTestItem(
          productCode: '01234567',
          quantityBoxes: 5.0,
          quantityPieces: 10,
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: _createTestDraft(
            clientId: 100,
            clientName: '거래처A',
            deliveryDate: DateTime(2026, 3, 1),
            items: [item],
          ),
        );
        fakeRepo.validationResultToReturn = const ValidationResult(isValid: true);
        fakeRepo.shouldThrowOnSubmit = true;
        fakeRepo.errorMessage = '전송 실패';

        // Act
        await notifier.validateAndSubmitOrder();

        // Assert
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.errorMessage, '전송 실패');
        expect(notifier.state.submitResult, isNull);
      });

      test('수정 모드에서는 updateOrder 호출', () async {
        // Arrange
        final item = _createTestItem(
          productCode: '01234567',
          quantityBoxes: 5.0,
          quantityPieces: 10,
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: _createTestDraft(
            id: 42,
            clientId: 100,
            clientName: '거래처A',
            deliveryDate: DateTime(2026, 3, 1),
            items: [item],
          ),
        );
        fakeRepo.validationResultToReturn = const ValidationResult(isValid: true);
        fakeRepo.submitResultToReturn = const OrderSubmitResult(
          orderId: 42,
          orderRequestNumber: 'OP00000042',
          status: 'PENDING',
        );

        // Act
        await notifier.validateAndSubmitOrder();

        // Assert
        expect(fakeRepo.updateOrderCalled, true);
        expect(fakeRepo.lastUpdatedOrderId, 42);
        expect(fakeRepo.submitOrderCalled, false);
      });
    });

    group('saveDraft', () {
      test('성공 시 성공 메시지', () async {
        // Arrange
        final item = _createTestItem(productCode: '01234567');
        notifier.state = notifier.state.copyWith(
          orderDraft: _createTestDraft(
            clientId: 100,
            items: [item],
          ),
        );

        // Act
        await notifier.saveDraft();

        // Assert
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.successMessage, '임시저장되었습니다.');
        expect(notifier.state.errorMessage, isNull);
        expect(fakeRepo.saveDraftCalled, true);
      });

      test('실패 시 에러 메시지', () async {
        // Arrange
        fakeRepo.shouldThrow = true;
        fakeRepo.errorMessage = '저장 실패';

        // Act
        await notifier.saveDraft();

        // Assert
        expect(notifier.state.isSubmitting, false);
        expect(notifier.state.errorMessage, '저장 실패');
        expect(notifier.state.successMessage, isNull);
      });
    });

    group('deleteOrder', () {
      test('상태 초기화', () async {
        // Arrange - set some state
        final item = _createTestItem(productCode: '01234567');
        notifier.state = notifier.state.copyWith(
          orderDraft: _createTestDraft(
            clientId: 100,
            items: [item],
          ),
          hasDraft: true,
        );

        // Act
        await notifier.deleteOrder();

        // Assert
        expect(notifier.state.orderDraft.items, isEmpty);
        expect(notifier.state.orderDraft.clientId, isNull);
        expect(notifier.state.hasDraft, false);
        expect(fakeRepo.deleteDraftCalled, true);
      });
    });

    group('clearError', () {
      test('에러 메시지 제거', () {
        // Arrange
        notifier.state = notifier.state.copyWith(errorMessage: 'Test error');

        // Act
        notifier.clearError();

        // Assert
        expect(notifier.state.errorMessage, isNull);
      });
    });

    group('clearSuccess', () {
      test('성공 메시지 제거', () {
        // Arrange
        notifier.state = notifier.state.copyWith(successMessage: 'Test success');

        // Act
        notifier.clearSuccess();

        // Assert
        expect(notifier.state.successMessage, isNull);
      });
    });

    group('clearValidationErrors', () {
      test('유효성 에러 제거', () {
        // Arrange
        const error = ValidationError(
          errorType: ValidationErrorType.minOrderQuantity,
          message: 'Test error',
        );
        final item = _createTestItem(productCode: '01234567').copyWith(
          validationError: error,
        );
        notifier.state = notifier.state.copyWith(
          orderDraft: notifier.state.orderDraft.copyWith(items: [item]),
          validationErrors: {'01234567': error},
        );

        // Act
        notifier.clearValidationErrors();

        // Assert
        expect(notifier.state.validationErrors, isEmpty);
        expect(notifier.state.orderDraft.items[0].validationError, isNull);
      });
    });
  });
}

// --- Fake Repository ---

class FakeOrderRepository implements OrderRepository {
  // --- Controllable returns ---
  int creditBalanceToReturn = 50000000;
  OrderDraft? draftToReturn;
  ValidationResult validationResultToReturn = const ValidationResult(isValid: true);
  OrderSubmitResult submitResultToReturn = const OrderSubmitResult(
    orderId: 1,
    orderRequestNumber: 'OP00000001',
    status: 'PENDING',
  );
  bool shouldThrow = false;
  bool shouldThrowOnSubmit = false;
  String errorMessage = '테스트 에러';

  // --- Call tracking ---
  bool saveDraftCalled = false;
  bool deleteDraftCalled = false;
  bool submitOrderCalled = false;
  bool updateOrderCalled = false;
  OrderDraft? lastSavedDraft;
  OrderDraft? lastSubmittedDraft;
  int? lastUpdatedOrderId;

  @override
  Future<int> getCreditBalance({required int clientId}) async {
    if (shouldThrow) throw Exception(errorMessage);
    return creditBalanceToReturn;
  }

  @override
  Future<OrderDraft?> loadDraftOrder() async {
    if (shouldThrow) throw Exception(errorMessage);
    return draftToReturn;
  }

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) async {
    if (shouldThrow) throw Exception(errorMessage);
    saveDraftCalled = true;
    lastSavedDraft = orderDraft;
  }

  @override
  Future<void> deleteDraftOrder() async {
    if (shouldThrow) throw Exception(errorMessage);
    deleteDraftCalled = true;
  }

  @override
  Future<ValidationResult> validateOrder({required OrderDraft orderDraft}) async {
    if (shouldThrow) throw Exception(errorMessage);
    return validationResultToReturn;
  }

  @override
  Future<OrderSubmitResult> submitOrder({required OrderDraft orderDraft}) async {
    if (shouldThrowOnSubmit) throw Exception(errorMessage);
    submitOrderCalled = true;
    lastSubmittedDraft = orderDraft;
    return submitResultToReturn;
  }

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) async {
    if (shouldThrow) throw Exception(errorMessage);
    updateOrderCalled = true;
    lastUpdatedOrderId = orderId;
    lastSubmittedDraft = orderDraft;
    return submitResultToReturn;
  }

  // --- Non-F22 methods - stub ---
  @override
  Future<OrderListResult> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) async =>
      throw UnimplementedError();

  @override
  Future<OrderDetail> getOrderDetail({required int orderId}) async =>
      throw UnimplementedError();

  @override
  Future<void> resendOrder({required int orderId}) async =>
      throw UnimplementedError();

  @override
  Future<OrderCancelResult> cancelOrder({
    required int orderId,
    required List<String> productCodes,
  }) async =>
      throw UnimplementedError();

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() async =>
      throw UnimplementedError();

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async =>
      throw UnimplementedError();

  @override
  Future<ProductForOrder> getProductByBarcode({required String barcode}) async =>
      throw UnimplementedError();

  @override
  Future<void> addToFavorites({required String productCode}) async =>
      throw UnimplementedError();

  @override
  Future<void> removeFromFavorites({required String productCode}) async =>
      throw UnimplementedError();

  @override
  Future<ClientOrderListResult> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) async =>
      throw UnimplementedError();

  @override
  Future<ClientOrderDetail> getClientOrderDetail({
    required String sapOrderNumber,
  }) async =>
      throw UnimplementedError();
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
