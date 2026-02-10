import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/order_mock_repository.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/usecases/add_to_favorites_usecase.dart';
import 'package:mobile/domain/usecases/delete_draft_order_usecase.dart';
import 'package:mobile/domain/usecases/get_credit_balance_usecase.dart';
import 'package:mobile/domain/usecases/get_favorite_products_usecase.dart';
import 'package:mobile/domain/usecases/get_product_by_barcode_usecase.dart';
import 'package:mobile/domain/usecases/load_draft_order_usecase.dart';
import 'package:mobile/domain/usecases/remove_from_favorites_usecase.dart';
import 'package:mobile/domain/usecases/save_draft_order_usecase.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';
import 'package:mobile/domain/usecases/update_order_usecase.dart';

void main() {
  late OrderMockRepository repository;
  late GetCreditBalance getCreditBalance;
  late GetFavoriteProducts getFavoriteProducts;
  late SearchProductsForOrder searchProductsForOrder;
  late GetProductByBarcode getProductByBarcode;
  late SaveDraftOrder saveDraftOrder;
  late LoadDraftOrder loadDraftOrder;
  late DeleteDraftOrder deleteDraftOrder;
  late UpdateOrder updateOrder;
  late AddToFavorites addToFavorites;
  late RemoveFromFavorites removeFromFavorites;

  setUp(() {
    repository = OrderMockRepository();
    getCreditBalance = GetCreditBalance(repository);
    getFavoriteProducts = GetFavoriteProducts(repository);
    searchProductsForOrder = SearchProductsForOrder(repository);
    getProductByBarcode = GetProductByBarcode(repository);
    saveDraftOrder = SaveDraftOrder(repository);
    loadDraftOrder = LoadDraftOrder(repository);
    deleteDraftOrder = DeleteDraftOrder(repository);
    updateOrder = UpdateOrder(repository);
    addToFavorites = AddToFavorites(repository);
    removeFromFavorites = RemoveFromFavorites(repository);
  });

  group('GetCreditBalance UseCase', () {
    test('정상 조회', () async {
      // Given: 존재하는 거래처 ID
      const clientId = 1;

      // When: 여신 잔액 조회
      final balance = await getCreditBalance(clientId: clientId);

      // Then: 여신 잔액 반환
      expect(balance, isA<int>());
      expect(balance, greaterThan(0));
      expect(balance, 100000000); // Mock data: clientId 1 = 100000000
    });

    test('존재하지 않는 거래처 예외', () async {
      // Given: 존재하지 않는 거래처 ID
      const clientId = 999;

      // When & Then: 예외 발생
      expect(
        () => getCreditBalance(clientId: clientId),
        throwsException,
      );
    });
  });

  group('GetFavoriteProducts UseCase', () {
    test('즐겨찾기 목록 조회', () async {
      // When: 즐겨찾기 제품 목록 조회
      final products = await getFavoriteProducts();

      // Then: 즐겨찾기 제품 목록 반환
      expect(products, isA<List<ProductForOrder>>());
      expect(products.isNotEmpty, true);
      expect(products.every((p) => p.isFavorite), true);
    });
  });

  group('SearchProductsForOrder UseCase', () {
    test('검색 성공', () async {
      // Given: 검색어
      const query = '갈릭';

      // When: 제품 검색
      final products = await searchProductsForOrder(query: query);

      // Then: 검색 결과 반환
      expect(products, isA<List<ProductForOrder>>());
      expect(products.isNotEmpty, true);
      expect(
        products.any((p) => p.productName.contains('갈릭')),
        true,
      );
    });

    test('빈 검색어 시 빈 리스트 반환', () async {
      // Given: 빈 검색어
      const query = '';

      // When: 제품 검색
      final products = await searchProductsForOrder(query: query);

      // Then: 빈 리스트 반환
      expect(products, isEmpty);
    });

    test('공백만 있는 검색어 시 빈 리스트 반환', () async {
      // Given: 공백만 있는 검색어
      const query = '   ';

      // When: 제품 검색
      final products = await searchProductsForOrder(query: query);

      // Then: 빈 리스트 반환
      expect(products, isEmpty);
    });
  });

  group('GetProductByBarcode UseCase', () {
    test('바코드 조회 성공', () async {
      // Given: 유효한 바코드
      const barcode = '8801045123456';

      // When: 바코드로 제품 조회
      final product = await getProductByBarcode(barcode: barcode);

      // Then: 제품 정보 반환
      expect(product, isA<ProductForOrder>());
      expect(product.barcode, barcode);
      expect(product.productCode, '01101123');
    });

    test('빈 바코드 시 ArgumentError', () async {
      // Given: 빈 바코드
      const barcode = '';

      // When & Then: ArgumentError 발생
      expect(
        () => getProductByBarcode(barcode: barcode),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('공백만 있는 바코드 시 ArgumentError', () async {
      // Given: 공백만 있는 바코드
      const barcode = '   ';

      // When & Then: ArgumentError 발생
      expect(
        () => getProductByBarcode(barcode: barcode),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('존재하지 않는 바코드 시 예외', () async {
      // Given: 존재하지 않는 바코드
      const barcode = '9999999999999';

      // When & Then: Exception 발생
      expect(
        () => getProductByBarcode(barcode: barcode),
        throwsException,
      );
    });
  });

  group('SaveDraftOrder + LoadDraftOrder UseCase', () {
    test('저장 후 불러오기', () async {
      // Given: 저장할 주문서
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
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

      // When: 임시저장
      await saveDraftOrder(orderDraft: draft);
      final loaded = await loadDraftOrder();

      // Then: 저장된 데이터 불러오기 성공
      expect(loaded, isNotNull);
      expect(loaded?.clientId, draft.clientId);
      expect(loaded?.clientName, draft.clientName);
      expect(loaded?.deliveryDate, draft.deliveryDate);
      expect(loaded?.items.length, draft.items.length);
      expect(loaded?.totalAmount, draft.totalAmount);
      expect(loaded?.isDraft, true); // SaveDraftOrder가 자동으로 true로 설정
    });

    test('저장하지 않았을 때 null 반환', () async {
      // Given: 아무것도 저장하지 않은 상태
      // When: 불러오기
      final loaded = await loadDraftOrder();

      // Then: null 반환
      expect(loaded, isNull);
    });
  });

  group('DeleteDraftOrder UseCase', () {
    test('삭제 후 null 반환', () async {
      // Given: 임시저장된 주문서가 있는 상태
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
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
        isDraft: true,
        lastModified: DateTime(2026, 2, 10),
      );
      await saveDraftOrder(orderDraft: draft);

      // When: 임시저장 삭제
      await deleteDraftOrder();
      final loaded = await loadDraftOrder();

      // Then: null 반환
      expect(loaded, isNull);
    });
  });

  group('UpdateOrder UseCase', () {
    test('수정 성공', () async {
      // Given: 수정할 주문 ID와 주문서
      const orderId = 3; // Mock data에 존재하는 주문
      final draft = OrderDraft(
        clientId: 3,
        clientName: '대한식품유통',
        deliveryDate: DateTime(2026, 2, 20),
        items: const [
          OrderDraftItem(
            productCode: '01101123',
            productName: '갈릭 아이올리소스 240g',
            quantityBoxes: 10,
            quantityPieces: 0,
            unitPrice: 3500,
            boxSize: 20,
            totalPrice: 700000,
          ),
        ],
        totalAmount: 700000,
        isDraft: false,
        lastModified: DateTime(2026, 2, 10),
      );

      // When: 주문서 수정
      final result = await updateOrder(orderId: orderId, orderDraft: draft);

      // Then: 수정 결과 반환
      expect(result, isA<OrderSubmitResult>());
      expect(result.orderId, orderId);
      expect(result.orderRequestNumber, isNotEmpty);
      expect(result.status, 'PENDING');
    });

    test('존재하지 않는 주문 예외', () async {
      // Given: 존재하지 않는 주문 ID
      const orderId = 999;
      final draft = OrderDraft(
        clientId: 1,
        clientName: '천사푸드',
        deliveryDate: DateTime(2026, 2, 15),
        items: const [
          OrderDraftItem(
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

      // When & Then: 예외 발생
      expect(
        () => updateOrder(orderId: orderId, orderDraft: draft),
        throwsException,
      );
    });
  });

  group('AddToFavorites + RemoveFromFavorites UseCase', () {
    test('추가 후 목록 확인', () async {
      // Given: 즐겨찾기에 없는 제품
      const productCode = '11110003'; // 토마토케찹500G (초기에 즐겨찾기 아님)

      // When: 즐겨찾기 추가
      await addToFavorites(productCode: productCode);
      final favorites = await getFavoriteProducts();

      // Then: 목록에 추가됨
      expect(
        favorites.any((p) => p.productCode == productCode),
        true,
      );
    });

    test('삭제 후 목록 확인', () async {
      // Given: 즐겨찾기에 있는 제품
      const productCode = '01101123'; // 갈릭 아이올리소스 (초기에 즐겨찾기)

      // 초기 상태 확인
      var favorites = await getFavoriteProducts();
      expect(
        favorites.any((p) => p.productCode == productCode),
        true,
      );

      // When: 즐겨찾기 삭제
      await removeFromFavorites(productCode: productCode);
      favorites = await getFavoriteProducts();

      // Then: 목록에서 제거됨
      expect(
        favorites.any((p) => p.productCode == productCode),
        false,
      );
    });
  });
}
