import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/validation_error.dart';

void main() {
  group('OrderDraft', () {
    final testDateTime = DateTime(2024, 1, 15, 10, 30);
    final testDeliveryDate = DateTime(2024, 1, 20);

    final testItem1 = OrderDraftItem(
      productCode: 'P001',
      productName: '제품1',
      quantityBoxes: 2.0,
      quantityPieces: 5,
      unitPrice: 10000,
      boxSize: 20,
      totalPrice: 202500,
      isSelected: true,
    );

    final testItem2 = OrderDraftItem(
      productCode: 'P002',
      productName: '제품2',
      quantityBoxes: 1.0,
      quantityPieces: 0,
      unitPrice: 15000,
      boxSize: 10,
      totalPrice: 150000,
      isSelected: false,
    );

    test('OrderDraft 생성 테스트', () {
      final draft = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '테스트 거래처',
        creditBalance: 5000000,
        deliveryDate: testDeliveryDate,
        items: [testItem1, testItem2],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.id, 1);
      expect(draft.clientId, 100);
      expect(draft.clientName, '테스트 거래처');
      expect(draft.creditBalance, 5000000);
      expect(draft.deliveryDate, testDeliveryDate);
      expect(draft.items.length, 2);
      expect(draft.totalAmount, 352500);
      expect(draft.isDraft, true);
      expect(draft.lastModified, testDateTime);
    });

    test('OrderDraft.empty() 팩토리 생성 테스트', () {
      final emptyDraft = OrderDraft.empty();

      expect(emptyDraft.id, null);
      expect(emptyDraft.clientId, null);
      expect(emptyDraft.clientName, null);
      expect(emptyDraft.creditBalance, null);
      expect(emptyDraft.deliveryDate, null);
      expect(emptyDraft.items, isEmpty);
      expect(emptyDraft.totalAmount, 0);
      expect(emptyDraft.isDraft, true);
      expect(emptyDraft.lastModified, isNotNull);
    });

    test('copyWith 테스트', () {
      final original = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '테스트 거래처',
        creditBalance: 5000000,
        deliveryDate: testDeliveryDate,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final copied = original.copyWith(
        clientId: 200,
        clientName: '변경된 거래처',
        isDraft: false,
      );

      expect(copied.id, 1);
      expect(copied.clientId, 200);
      expect(copied.clientName, '변경된 거래처');
      expect(copied.creditBalance, 5000000);
      expect(copied.deliveryDate, testDeliveryDate);
      expect(copied.items, [testItem1]);
      expect(copied.totalAmount, 202500);
      expect(copied.isDraft, false);
      expect(copied.lastModified, testDateTime);
    });

    test('toJson/fromJson 왕복 변환 테스트', () {
      final original = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '테스트 거래처',
        creditBalance: 5000000,
        deliveryDate: testDeliveryDate,
        items: [testItem1, testItem2],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final json = original.toJson();
      final restored = OrderDraft.fromJson(json);

      expect(restored, original);
    });

    test('calculatedTotalAmount getter 테스트', () {
      final draft = OrderDraft(
        items: [testItem1, testItem2],
        totalAmount: 0, // 이 값과 무관하게 items에서 계산됨
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.calculatedTotalAmount, 202500 + 150000);
    });

    test('selectedItems getter 테스트', () {
      final draft = OrderDraft(
        items: [testItem1, testItem2],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final selected = draft.selectedItems;
      expect(selected.length, 1);
      expect(selected.first, testItem1);
    });

    test('allItemsSelected getter - 모두 선택된 경우', () {
      final item1Selected = testItem1.copyWith(isSelected: true);
      final item2Selected = testItem2.copyWith(isSelected: true);

      final draft = OrderDraft(
        items: [item1Selected, item2Selected],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.allItemsSelected, true);
    });

    test('allItemsSelected getter - 일부만 선택된 경우', () {
      final draft = OrderDraft(
        items: [testItem1, testItem2],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.allItemsSelected, false);
    });

    test('allItemsSelected getter - 빈 리스트인 경우', () {
      final draft = OrderDraft(
        items: [],
        totalAmount: 0,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.allItemsSelected, false);
    });

    test('itemsWithErrors getter 테스트', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 수량 미달',
        minOrderQuantity: 10,
      );

      final itemWithError = testItem1.copyWith(validationError: error);

      final draft = OrderDraft(
        items: [itemWithError, testItem2],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final errored = draft.itemsWithErrors;
      expect(errored.length, 1);
      expect(errored.first, itemWithError);
    });

    test('isRequiredFieldsFilled - 모든 필수 필드 채워진 경우', () {
      final draft = OrderDraft(
        clientId: 100,
        deliveryDate: testDeliveryDate,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.isRequiredFieldsFilled, true);
    });

    test('isRequiredFieldsFilled - clientId 없는 경우', () {
      final draft = OrderDraft(
        deliveryDate: testDeliveryDate,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.isRequiredFieldsFilled, false);
    });

    test('isRequiredFieldsFilled - deliveryDate 없는 경우', () {
      final draft = OrderDraft(
        clientId: 100,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.isRequiredFieldsFilled, false);
    });

    test('isRequiredFieldsFilled - items 비어있는 경우', () {
      final draft = OrderDraft(
        clientId: 100,
        deliveryDate: testDeliveryDate,
        items: [],
        totalAmount: 0,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft.isRequiredFieldsFilled, false);
    });

    test('equality 테스트 - 동일한 객체', () {
      final draft1 = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '테스트 거래처',
        creditBalance: 5000000,
        deliveryDate: testDeliveryDate,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final draft2 = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '테스트 거래처',
        creditBalance: 5000000,
        deliveryDate: testDeliveryDate,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft1, draft2);
      expect(draft1.hashCode, draft2.hashCode);
    });

    test('equality 테스트 - 다른 객체', () {
      final draft1 = OrderDraft(
        id: 1,
        clientId: 100,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final draft2 = OrderDraft(
        id: 2,
        clientId: 100,
        items: [testItem1],
        totalAmount: 202500,
        isDraft: true,
        lastModified: testDateTime,
      );

      expect(draft1, isNot(draft2));
    });

    test('toString 테스트', () {
      final draft = OrderDraft(
        id: 1,
        clientId: 100,
        clientName: '테스트 거래처',
        deliveryDate: testDeliveryDate,
        items: [testItem1, testItem2],
        totalAmount: 352500,
        isDraft: true,
        lastModified: testDateTime,
      );

      final str = draft.toString();
      expect(str, contains('OrderDraft'));
      expect(str, contains('id: 1'));
      expect(str, contains('clientId: 100'));
      expect(str, contains('items: 2'));
    });
  });

  group('OrderDraftItem', () {
    test('OrderDraftItem 생성 테스트', () {
      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.5,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 262500,
        isSelected: true,
      );

      expect(item.productCode, 'P001');
      expect(item.productName, '테스트 제품');
      expect(item.quantityBoxes, 2.5);
      expect(item.quantityPieces, 5);
      expect(item.unitPrice, 10000);
      expect(item.boxSize, 20);
      expect(item.totalPrice, 262500);
      expect(item.isSelected, true);
      expect(item.validationError, null);
    });

    test('OrderDraftItem 기본값 테스트', () {
      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 1.0,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 100000,
      );

      expect(item.isSelected, false);
      expect(item.validationError, null);
    });

    test('copyWith 테스트', () {
      final original = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 1.0,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 100000,
        isSelected: false,
      );

      final copied = original.copyWith(
        quantityBoxes: 2.0,
        quantityPieces: 10,
        isSelected: true,
      );

      expect(copied.productCode, 'P001');
      expect(copied.productName, '테스트 제품');
      expect(copied.quantityBoxes, 2.0);
      expect(copied.quantityPieces, 10);
      expect(copied.unitPrice, 10000);
      expect(copied.boxSize, 20);
      expect(copied.isSelected, true);
    });

    test('toJson/fromJson 왕복 변환 테스트', () {
      final original = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.5,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 262500,
        isSelected: true,
      );

      final json = original.toJson();
      final restored = OrderDraftItem.fromJson(json);

      expect(restored, original);
    });

    test('toJson/fromJson with ValidationError', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 수량 미달',
        minOrderQuantity: 10,
      );

      final original = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 1.0,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 100000,
        validationError: error,
      );

      final json = original.toJson();
      final restored = OrderDraftItem.fromJson(json);

      expect(restored, original);
      expect(restored.validationError, error);
    });

    test('calculatedTotalPrice 계산 테스트 - 정수 박스', () {
      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.0,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 0, // 실제 계산값과 무관
      );

      // (2.0 * 20 + 5) = 45개
      // 45 * 10000 / 20 = 22500
      expect(item.calculatedTotalPrice, 22500);
    });

    test('calculatedTotalPrice 계산 테스트 - 소수점 박스', () {
      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.5,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 0,
      );

      // (2.5 * 20 + 0) = 50개
      // 50 * 10000 / 20 = 25000
      expect(item.calculatedTotalPrice, 25000);
    });

    test('hasError getter 테스트 - 에러 있는 경우', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 수량 미달',
      );

      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 1.0,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 100000,
        validationError: error,
      );

      expect(item.hasError, true);
    });

    test('hasError getter 테스트 - 에러 없는 경우', () {
      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 1.0,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 100000,
      );

      expect(item.hasError, false);
    });

    test('clearValidationError 테스트', () {
      final error = ValidationError(
        errorType: ValidationErrorType.minOrderQuantity,
        message: '최소 수량 미달',
      );

      final itemWithError = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 1.0,
        quantityPieces: 0,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 100000,
        validationError: error,
      );

      final cleared = itemWithError.clearValidationError();

      expect(cleared.validationError, null);
      expect(cleared.hasError, false);
      expect(cleared.productCode, itemWithError.productCode);
      expect(cleared.quantityBoxes, itemWithError.quantityBoxes);
    });

    test('equality 테스트 - 동일한 객체', () {
      final item1 = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.0,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 202500,
        isSelected: true,
      );

      final item2 = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.0,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 202500,
        isSelected: true,
      );

      expect(item1, item2);
      expect(item1.hashCode, item2.hashCode);
    });

    test('equality 테스트 - 다른 객체', () {
      final item1 = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.0,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 202500,
      );

      final item2 = OrderDraftItem(
        productCode: 'P002',
        productName: '테스트 제품',
        quantityBoxes: 2.0,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 202500,
      );

      expect(item1, isNot(item2));
    });

    test('toString 테스트', () {
      final item = OrderDraftItem(
        productCode: 'P001',
        productName: '테스트 제품',
        quantityBoxes: 2.0,
        quantityPieces: 5,
        unitPrice: 10000,
        boxSize: 20,
        totalPrice: 202500,
        isSelected: true,
      );

      final str = item.toString();
      expect(str, contains('OrderDraftItem'));
      expect(str, contains('P001'));
      expect(str, contains('테스트 제품'));
    });
  });
}
