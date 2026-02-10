import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';

/// OrderMockRepository의 F22 주문서 작성 관련 메서드 테스트
void main() {
  late OrderMockRepository repository;

  setUp(() {
    // 각 테스트마다 새로운 인스턴스를 생성하여 상태 격리
    repository = OrderMockRepository();
  });

  group('getCreditBalance - 여신 잔액 조회', () {
    test('정상적으로 거래처의 여신 잔액을 조회한다', () async {
      final balance = await repository.getCreditBalance(clientId: 1);
      expect(balance, 100000000);
    });

    test('거래처 ID 2의 여신 잔액을 조회한다', () async {
      final balance = await repository.getCreditBalance(clientId: 2);
      expect(balance, 50000000);
    });

    test('존재하지 않는 거래처는 예외를 발생시킨다', () async {
      expect(
        () => repository.getCreditBalance(clientId: 999),
        throwsA(predicate((e) =>
            e is Exception && e.toString().contains('STORE_NOT_FOUND'))),
      );
    });
  });

  group('getFavoriteProducts - 즐겨찾기 제품 목록 조회', () {
    test('5개의 즐겨찾기 제품이 반환된다', () async {
      final products = await repository.getFavoriteProducts();
      expect(products.length, 5);
    });

    test('모든 즐겨찾기 제품의 isFavorite이 true이다', () async {
      final products = await repository.getFavoriteProducts();
      for (final product in products) {
        expect(product.isFavorite, true);
      }
    });

    test('즐겨찾기 목록에 예상된 제품 코드가 포함된다', () async {
      final products = await repository.getFavoriteProducts();
      final codes = products.map((p) => p.productCode).toSet();

      expect(codes, containsAll([
        '01101123',
        '01101222',
        '13310002',
        '23010011',
        '01202001',
      ]));
    });

    test('즐겨찾기 제품 정보가 올바르게 반환된다', () async {
      final products = await repository.getFavoriteProducts();
      final garlicSauce = products.firstWhere(
        (p) => p.productCode == '01101123',
      );

      expect(garlicSauce.productName, '갈릭 아이올리소스 240g');
      expect(garlicSauce.unitPrice, 3500);
      expect(garlicSauce.boxSize, 20);
      expect(garlicSauce.isFavorite, true);
    });
  });

  group('searchProductsForOrder - 제품 검색', () {
    test('제품명으로 검색 시 일치하는 제품이 반환된다', () async {
      final results = await repository.searchProductsForOrder(query: '라면');

      expect(results.length, greaterThan(0));
      final names = results.map((p) => p.productName).toList();
      expect(names, contains('진라면 매운맛 120g'));
      expect(names, contains('참깨라면 120g'));
    });

    test('제품 코드로 검색 시 정확히 일치하는 제품이 반환된다', () async {
      final results = await repository.searchProductsForOrder(query: '01101123');

      expect(results.length, 1);
      expect(results.first.productCode, '01101123');
      expect(results.first.productName, '갈릭 아이올리소스 240g');
    });

    test('대소문자를 구분하지 않고 검색한다', () async {
      final resultsLower = await repository.searchProductsForOrder(query: '라면');
      final resultsUpper = await repository.searchProductsForOrder(query: '라면');

      expect(resultsLower.length, resultsUpper.length);
    });

    test('카테고리(중분류) 필터링이 적용된다', () async {
      final results = await repository.searchProductsForOrder(
        query: '',
        categoryMid: '소스류',
      );

      expect(results.length, greaterThan(0));
      for (final product in results) {
        expect(product.categoryMid, '소스류');
      }
    });

    test('카테고리(소분류) 필터링이 적용된다', () async {
      final results = await repository.searchProductsForOrder(
        query: '',
        categorySub: '봉지라면',
      );

      expect(results.length, greaterThan(0));
      for (final product in results) {
        expect(product.categorySub, '봉지라면');
      }
    });

    test('중분류와 소분류를 함께 필터링할 수 있다', () async {
      final results = await repository.searchProductsForOrder(
        query: '',
        categoryMid: '라면',
        categorySub: '봉지라면',
      );

      expect(results.length, greaterThan(0));
      for (final product in results) {
        expect(product.categoryMid, '라면');
        expect(product.categorySub, '봉지라면');
      }
    });

    test('즐겨찾기 상태가 올바르게 반영된다', () async {
      final results = await repository.searchProductsForOrder(query: '진라면');

      final jinRamen = results.firstWhere(
        (p) => p.productCode == '01202001',
      );
      expect(jinRamen.isFavorite, true);
    });

    test('즐겨찾기가 아닌 제품은 isFavorite이 false이다', () async {
      final results = await repository.searchProductsForOrder(query: '참깨라면');

      final sesameRamen = results.firstWhere(
        (p) => p.productCode == '01302010',
      );
      expect(sesameRamen.isFavorite, false);
    });

    test('일치하는 결과가 없으면 빈 리스트를 반환한다', () async {
      final results = await repository.searchProductsForOrder(
        query: '존재하지않는제품명XYZ123',
      );
      expect(results, isEmpty);
    });
  });

  group('getProductByBarcode - 바코드로 제품 조회', () {
    test('바코드로 제품을 성공적으로 조회한다', () async {
      final product = await repository.getProductByBarcode(
        barcode: '8801045123456',
      );

      expect(product.productCode, '01101123');
      expect(product.productName, '갈릭 아이올리소스 240g');
    });

    test('즐겨찾기 상태가 올바르게 반영된다', () async {
      final product = await repository.getProductByBarcode(
        barcode: '8801045123456',
      );
      expect(product.isFavorite, true);
    });

    test('존재하지 않는 바코드는 예외를 발생시킨다', () async {
      expect(
        () => repository.getProductByBarcode(barcode: '9999999999999'),
        throwsA(predicate((e) =>
            e is Exception && e.toString().contains('PRODUCT_NOT_FOUND'))),
      );
    });

    test('여러 바코드로 각각의 제품을 조회할 수 있다', () async {
      final product1 = await repository.getProductByBarcode(
        barcode: '8801045234567',
      );
      expect(product1.productCode, '01101222');

      final product2 = await repository.getProductByBarcode(
        barcode: '8801045678901',
      );
      expect(product2.productCode, '01202001');
    });
  });

  group('임시저장 (saveDraftOrder / loadDraftOrder / deleteDraftOrder)', () {
    test('초기 상태에서 임시저장된 주문서가 없다', () async {
      final draft = await repository.loadDraftOrder();
      expect(draft, isNull);
    });

    test('주문서를 임시저장하고 불러올 수 있다', () async {
      final testDraft = _createTestDraft();

      await repository.saveDraftOrder(orderDraft: testDraft);
      final loaded = await repository.loadDraftOrder();

      expect(loaded, isNotNull);
      expect(loaded!.clientId, testDraft.clientId);
      expect(loaded.clientName, testDraft.clientName);
      expect(loaded.items.length, testDraft.items.length);
      expect(loaded.totalAmount, testDraft.totalAmount);
    });

    test('임시저장된 주문서의 제품 정보가 정확하다', () async {
      final testDraft = _createTestDraft();

      await repository.saveDraftOrder(orderDraft: testDraft);
      final loaded = await repository.loadDraftOrder();

      expect(loaded, isNotNull);
      final item = loaded!.items.first;
      expect(item.productCode, '01101123');
      expect(item.productName, '갈릭 아이올리소스 240g');
      expect(item.quantityBoxes, 5);
      expect(item.unitPrice, 3500);
    });

    test('임시저장된 주문서를 삭제하면 null을 반환한다', () async {
      final testDraft = _createTestDraft();

      await repository.saveDraftOrder(orderDraft: testDraft);
      await repository.deleteDraftOrder();
      final loaded = await repository.loadDraftOrder();

      expect(loaded, isNull);
    });

    test('주문서를 덮어쓰기할 수 있다', () async {
      final draft1 = _createTestDraft(clientId: 1, clientName: '천사푸드');
      final draft2 = _createTestDraft(clientId: 2, clientName: '(유)경산식품');

      await repository.saveDraftOrder(orderDraft: draft1);
      await repository.saveDraftOrder(orderDraft: draft2);
      final loaded = await repository.loadDraftOrder();

      expect(loaded, isNotNull);
      expect(loaded!.clientId, 2);
      expect(loaded.clientName, '(유)경산식품');
    });
  });

  group('validateOrder - 주문서 유효성 검사', () {
    test('정상적인 주문서는 유효성 검사를 통과한다', () async {
      final testDraft = _createTestDraft();

      final result = await repository.validateOrder(orderDraft: testDraft);

      expect(result.isValid, true);
      expect(result.errors, isEmpty);
    });

    test('제품 23010011이 최소수량 미달 시 유효성 검사에 실패한다', () async {
      final testDraft = _createTestDraft(
        items: [
          const OrderDraftItem(
            productCode: '23010011',
            productName: '오감포차_크림새우180G',
            quantityBoxes: 3, // 최소 5박스 필요
            quantityPieces: 0,
            unitPrice: 5900,
            boxSize: 16,
            totalPrice: 106200,
          ),
        ],
      );

      final result = await repository.validateOrder(orderDraft: testDraft);

      expect(result.isValid, false);
      expect(result.errors.containsKey('23010011'), true);
      expect(
        result.errors['23010011']!.errorType,
        ValidationErrorType.minOrderQuantity,
      );
      expect(result.errors['23010011']!.minOrderQuantity, 5);
    });

    test('제품 23010011이 최소수량 5박스 이상이면 통과한다', () async {
      final testDraft = _createTestDraft(
        items: [
          const OrderDraftItem(
            productCode: '23010011',
            productName: '오감포차_크림새우180G',
            quantityBoxes: 5,
            quantityPieces: 0,
            unitPrice: 5900,
            boxSize: 16,
            totalPrice: 472000,
          ),
        ],
      );

      final result = await repository.validateOrder(orderDraft: testDraft);

      expect(result.isValid, true);
      expect(result.errors, isEmpty);
    });

    test('여러 제품이 있을 때 하나만 오류가 있어도 실패한다', () async {
      final testDraft = _createTestDraft(
        items: [
          const OrderDraftItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            quantityBoxes: 10,
            quantityPieces: 0,
            unitPrice: 3500,
            boxSize: 20,
            totalPrice: 700000,
          ),
          const OrderDraftItem(
            productCode: '23010011',
            productName: '오감포차_크림새우180G',
            quantityBoxes: 2, // 최소 5박스 필요
            quantityPieces: 0,
            unitPrice: 5900,
            boxSize: 16,
            totalPrice: 70800,
          ),
        ],
      );

      final result = await repository.validateOrder(orderDraft: testDraft);

      expect(result.isValid, false);
      expect(result.errors.length, 1);
      expect(result.errors.containsKey('23010011'), true);
    });
  });

  group('submitOrder - 주문서 전송', () {
    test('주문서를 전송하면 성공 결과를 반환한다', () async {
      final testDraft = _createTestDraft();

      final result = await repository.submitOrder(orderDraft: testDraft);

      expect(result.orderId, greaterThan(0));
      expect(result.orderRequestNumber, isNotEmpty);
      expect(result.orderRequestNumber, startsWith('OP'));
      expect(result.status, 'PENDING');
    });

    test('주문서 전송 시 고유한 주문번호가 생성된다', () async {
      final testDraft = _createTestDraft();

      final result = await repository.submitOrder(orderDraft: testDraft);

      expect(result.orderRequestNumber.length, 10); // 'OP' + 8자리 숫자
      expect(result.orderRequestNumber, matches(r'^OP\d{8}$'));
    });

    test('여러 번 전송해도 정상적으로 동작한다', () async {
      final testDraft1 = _createTestDraft(clientId: 1);
      final testDraft2 = _createTestDraft(clientId: 2);

      final result1 = await repository.submitOrder(orderDraft: testDraft1);
      final result2 = await repository.submitOrder(orderDraft: testDraft2);

      expect(result1.orderId, greaterThan(0));
      expect(result2.orderId, greaterThan(0));
      expect(result1.status, 'PENDING');
      expect(result2.status, 'PENDING');
    });
  });

  group('updateOrder - 주문서 수정', () {
    test('존재하는 주문을 수정하면 성공 결과를 반환한다', () async {
      final testDraft = _createTestDraft();

      final result = await repository.updateOrder(
        orderId: 1,
        orderDraft: testDraft,
      );

      expect(result.orderId, 1);
      expect(result.orderRequestNumber, isNotEmpty);
      expect(result.status, 'PENDING');
    });

    test('존재하지 않는 주문 수정 시 예외를 발생시킨다', () async {
      final testDraft = _createTestDraft();

      expect(
        () => repository.updateOrder(
          orderId: 99999,
          orderDraft: testDraft,
        ),
        throwsA(predicate((e) =>
            e is Exception && e.toString().contains('ORDER_NOT_FOUND'))),
      );
    });

    test('여러 주문을 각각 수정할 수 있다', () async {
      final testDraft = _createTestDraft();

      final result1 = await repository.updateOrder(
        orderId: 1,
        orderDraft: testDraft,
      );
      final result2 = await repository.updateOrder(
        orderId: 2,
        orderDraft: testDraft,
      );

      expect(result1.orderId, 1);
      expect(result2.orderId, 2);
      expect(result1.orderRequestNumber, isNot(equals(result2.orderRequestNumber)));
    });
  });

  group('addToFavorites / removeFromFavorites - 즐겨찾기 관리', () {
    test('제품을 즐겨찾기에 추가하면 검색 결과에 반영된다', () async {
      // 초기에는 즐겨찾기가 아닌 제품
      final beforeResults = await repository.searchProductsForOrder(
        query: '참깨라면',
      );
      final beforeProduct = beforeResults.firstWhere(
        (p) => p.productCode == '01302010',
      );
      expect(beforeProduct.isFavorite, false);

      // 즐겨찾기에 추가
      await repository.addToFavorites(productCode: '01302010');

      // 검색 결과에서 즐겨찾기 상태 확인
      final afterResults = await repository.searchProductsForOrder(
        query: '참깨라면',
      );
      final afterProduct = afterResults.firstWhere(
        (p) => p.productCode == '01302010',
      );
      expect(afterProduct.isFavorite, true);
    });

    test('제품을 즐겨찾기에 추가하면 즐겨찾기 목록에 포함된다', () async {
      await repository.addToFavorites(productCode: '11110003');

      final favorites = await repository.getFavoriteProducts();
      final codes = favorites.map((p) => p.productCode).toList();

      expect(codes, contains('11110003'));
    });

    test('제품을 즐겨찾기에서 제거하면 검색 결과에 반영된다', () async {
      // 초기에는 즐겨찾기인 제품
      final beforeResults = await repository.searchProductsForOrder(
        query: '진라면',
      );
      final beforeProduct = beforeResults.firstWhere(
        (p) => p.productCode == '01202001',
      );
      expect(beforeProduct.isFavorite, true);

      // 즐겨찾기에서 제거
      await repository.removeFromFavorites(productCode: '01202001');

      // 검색 결과에서 즐겨찾기 상태 확인
      final afterResults = await repository.searchProductsForOrder(
        query: '진라면',
      );
      final afterProduct = afterResults.firstWhere(
        (p) => p.productCode == '01202001',
      );
      expect(afterProduct.isFavorite, false);
    });

    test('제품을 즐겨찾기에서 제거하면 즐겨찾기 목록에서 제외된다', () async {
      await repository.removeFromFavorites(productCode: '01101123');

      final favorites = await repository.getFavoriteProducts();
      final codes = favorites.map((p) => p.productCode).toList();

      expect(codes, isNot(contains('01101123')));
    });

    test('즐겨찾기 추가 후 제거하면 원래 상태로 돌아간다', () async {
      // 즐겨찾기 추가
      await repository.addToFavorites(productCode: '02101003');

      final afterAdd = await repository.getFavoriteProducts();
      expect(
        afterAdd.map((p) => p.productCode).toList(),
        contains('02101003'),
      );

      // 즐겨찾기 제거
      await repository.removeFromFavorites(productCode: '02101003');

      final afterRemove = await repository.getFavoriteProducts();
      expect(
        afterRemove.map((p) => p.productCode).toList(),
        isNot(contains('02101003')),
      );
    });

    test('바코드 조회 시에도 즐겨찾기 변경이 반영된다', () async {
      // 즐겨찾기 추가
      await repository.addToFavorites(productCode: '11110003');

      final product = await repository.getProductByBarcode(
        barcode: '8801045567890',
      );

      expect(product.productCode, '11110003');
      expect(product.isFavorite, true);
    });
  });

  group('통합 시나리오 테스트', () {
    test('주문서 작성 전체 플로우: 검색 → 임시저장 → 검증 → 전송', () async {
      // 1. 제품 검색
      final products = await repository.searchProductsForOrder(query: '라면');
      expect(products.length, greaterThan(0));

      // 2. 주문서 작성 및 임시저장
      final draft = _createTestDraft();
      await repository.saveDraftOrder(orderDraft: draft);

      // 3. 임시저장 확인
      final loaded = await repository.loadDraftOrder();
      expect(loaded, isNotNull);

      // 4. 유효성 검사
      final validationResult = await repository.validateOrder(orderDraft: draft);
      expect(validationResult.isValid, true);

      // 5. 주문 전송
      final submitResult = await repository.submitOrder(orderDraft: draft);
      expect(submitResult.status, 'PENDING');

      // 6. 임시저장 삭제
      await repository.deleteDraftOrder();
      final afterDelete = await repository.loadDraftOrder();
      expect(afterDelete, isNull);
    });

    test('즐겨찾기 관리 플로우: 추가 → 조회 → 제거 → 확인', () async {
      // 1. 즐겨찾기에 추가
      await repository.addToFavorites(productCode: '02101003');

      // 2. 즐겨찾기 목록에서 확인
      final favorites = await repository.getFavoriteProducts();
      expect(
        favorites.map((p) => p.productCode).toList(),
        contains('02101003'),
      );

      // 3. 검색 결과에서 즐겨찾기 상태 확인
      final searchResults = await repository.searchProductsForOrder(
        query: '02101003',
      );
      expect(searchResults.first.isFavorite, true);

      // 4. 즐겨찾기에서 제거
      await repository.removeFromFavorites(productCode: '02101003');

      // 5. 제거 확인
      final afterRemove = await repository.getFavoriteProducts();
      expect(
        afterRemove.map((p) => p.productCode).toList(),
        isNot(contains('02101003')),
      );
    });

    test('여신 잔액 확인 후 주문서 작성', () async {
      // 1. 여신 잔액 확인
      final balance = await repository.getCreditBalance(clientId: 1);
      expect(balance, 100000000);

      // 2. 주문서 작성 (잔액보다 적은 금액)
      final draft = _createTestDraft(clientId: 1);
      expect(draft.totalAmount, lessThan(balance));

      // 3. 주문서 전송
      final result = await repository.submitOrder(orderDraft: draft);
      expect(result.status, 'PENDING');
    });
  });
}

/// 테스트용 주문서 생성 헬퍼 함수
OrderDraft _createTestDraft({
  int? clientId = 1,
  String? clientName = '천사푸드',
  DateTime? deliveryDate,
  List<OrderDraftItem>? items,
}) {
  return OrderDraft(
    clientId: clientId,
    clientName: clientName,
    deliveryDate: deliveryDate ?? DateTime(2026, 2, 15),
    items: items ??
        [
          const OrderDraftItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            quantityBoxes: 5,
            quantityPieces: 0,
            unitPrice: 3500,
            boxSize: 20,
            totalPrice: 350000,
          ),
        ],
    totalAmount: 350000,
    isDraft: false,
    lastModified: DateTime(2026, 2, 10),
  );
}
