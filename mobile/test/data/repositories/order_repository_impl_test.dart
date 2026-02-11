import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/order_local_datasource.dart';
import 'package:mobile/data/datasources/order_remote_datasource.dart';
import 'package:mobile/data/models/client_order_model.dart';
import 'package:mobile/data/models/order_cancel_model.dart';
import 'package:mobile/data/models/order_detail_model.dart';
import 'package:mobile/data/models/order_draft_model.dart';
import 'package:mobile/data/models/order_model.dart';
import 'package:mobile/data/models/product_for_order_model.dart';
import 'package:mobile/data/models/validation_result_model.dart';
import 'package:mobile/data/repositories/order_repository_impl.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';

void main() {
  late OrderRepositoryImpl repository;
  late FakeOrderRemoteDataSource fakeRemoteDataSource;
  late FakeOrderLocalDataSource fakeLocalDataSource;

  setUp(() {
    fakeRemoteDataSource = FakeOrderRemoteDataSource();
    fakeLocalDataSource = FakeOrderLocalDataSource();
    repository = OrderRepositoryImpl(
      remoteDataSource: fakeRemoteDataSource,
      localDataSource: fakeLocalDataSource,
    );
  });

  group('OrderRepositoryImpl - F22 주문서 작성 기능', () {
    test('여신 잔액 조회: remote datasource를 호출하고 잔액을 반환한다', () async {
      // Given: remote datasource가 50,000,000원을 반환하도록 설정
      fakeRemoteDataSource.creditBalanceToReturn = 50000000;

      // When: 여신 잔액을 조회한다
      final balance = await repository.getCreditBalance(clientId: 100);

      // Then: remote datasource가 호출되고 잔액이 반환된다
      expect(fakeRemoteDataSource.getCreditBalanceCalls, 1);
      expect(fakeRemoteDataSource.lastCreditBalanceClientId, 100);
      expect(balance, 50000000);
    });

    test('즐겨찾기 제품 조회: remote datasource를 호출하고 엔티티로 변환한다', () async {
      // Given: remote datasource가 즐겨찾기 제품 목록을 반환하도록 설정
      fakeRemoteDataSource.favoriteProductsToReturn = [
        _sampleProductModel1,
        _sampleProductModel2,
      ];

      // When: 즐겨찾기 제품을 조회한다
      final products = await repository.getFavoriteProducts();

      // Then: remote datasource가 호출되고 엔티티로 변환된다
      expect(fakeRemoteDataSource.getFavoriteProductsCalls, 1);
      expect(products, isList);
      expect(products.length, 2);
      expect(products[0], isA<ProductForOrder>());
      expect(products[0].productCode, '01234567');
      expect(products[0].productName, '오뚜기 진라면');
      expect(products[0].isFavorite, true);
      expect(products[1].productCode, '01234568');
    });

    test('제품 검색: 검색 파라미터를 전달하고 결과를 엔티티로 변환한다', () async {
      // Given: remote datasource가 검색 결과를 반환하도록 설정
      fakeRemoteDataSource.searchResultsToReturn = [
        _sampleProductModel1,
      ];

      // When: 제품을 검색한다
      final products = await repository.searchProductsForOrder(
        query: '진라면',
        categoryMid: '라면',
        categorySub: '봉지라면',
      );

      // Then: remote datasource가 올바른 파라미터로 호출된다
      expect(fakeRemoteDataSource.searchProductsForOrderCalls, 1);
      expect(fakeRemoteDataSource.lastSearchQuery, '진라면');
      expect(fakeRemoteDataSource.lastSearchCategoryMid, '라면');
      expect(fakeRemoteDataSource.lastSearchCategorySub, '봉지라면');
      expect(products.length, 1);
      expect(products[0], isA<ProductForOrder>());
      expect(products[0].productName, '오뚜기 진라면');
    });

    test('바코드로 제품 조회: remote datasource를 호출하고 엔티티로 변환한다', () async {
      // Given: remote datasource가 제품을 반환하도록 설정
      fakeRemoteDataSource.barcodeProductToReturn = _sampleProductModel1;

      // When: 바코드로 제품을 조회한다
      final product = await repository.getProductByBarcode(
        barcode: '8801234567890',
      );

      // Then: remote datasource가 호출되고 엔티티로 변환된다
      expect(fakeRemoteDataSource.getProductByBarcodeCalls, 1);
      expect(fakeRemoteDataSource.lastBarcode, '8801234567890');
      expect(product, isA<ProductForOrder>());
      expect(product.productCode, '01234567');
      expect(product.barcode, '8801234567890');
    });

    test('주문서 임시저장: 엔티티를 모델로 변환하여 local datasource에 저장한다', () async {
      // Given: 주문서 엔티티
      final orderDraft = _sampleOrderDraft;

      // When: 주문서를 임시저장한다
      await repository.saveDraftOrder(orderDraft: orderDraft);

      // Then: local datasource에 JSON 형태로 저장된다
      expect(fakeLocalDataSource.savedDraft, isNotNull);
      expect(fakeLocalDataSource.savedDraft!['client_id'], 100);
      expect(fakeLocalDataSource.savedDraft!['client_name'], '테스트거래처');
      expect(fakeLocalDataSource.savedDraft!['delivery_date'], '2026-03-01');
      expect(fakeLocalDataSource.savedDraft!['items'], isList);
      expect((fakeLocalDataSource.savedDraft!['items'] as List).length, 1);
      expect(fakeLocalDataSource.savedDraft!['total_amount'], 72000);
      expect(fakeLocalDataSource.savedDraft!['is_draft'], false);
    });

    test('주문서 불러오기: local datasource에서 불러와 엔티티로 변환한다', () async {
      // Given: local datasource에 임시저장된 주문서가 있다
      fakeLocalDataSource.draftToReturn = {
        'client_id': 100,
        'client_name': '테스트거래처',
        'delivery_date': '2026-03-01',
        'items': [
          {
            'product_code': '01234567',
            'product_name': '오뚜기 진라면',
            'quantity_boxes': 5.0,
            'quantity_pieces': 10,
            'unit_price': 1200,
            'box_size': 20,
            'total_price': 72000,
            'is_selected': false,
          }
        ],
        'total_amount': 72000,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00.000Z',
      };

      // When: 주문서를 불러온다
      final loaded = await repository.loadDraftOrder();

      // Then: 엔티티로 변환되어 반환된다
      expect(loaded, isNotNull);
      expect(loaded, isA<OrderDraft>());
      expect(loaded!.clientId, 100);
      expect(loaded.clientName, '테스트거래처');
      expect(loaded.deliveryDate, DateTime(2026, 3, 1));
      expect(loaded.items.length, 1);
      expect(loaded.items[0].productCode, '01234567');
      expect(loaded.items[0].productName, '오뚜기 진라면');
      expect(loaded.totalAmount, 72000);
      expect(loaded.isDraft, true);
    });

    test('주문서 불러오기: 저장된 임시저장이 없으면 null을 반환한다', () async {
      // Given: local datasource에 임시저장이 없다
      fakeLocalDataSource.draftToReturn = null;

      // When: 주문서를 불러온다
      final loaded = await repository.loadDraftOrder();

      // Then: null이 반환된다
      expect(loaded, isNull);
    });

    test('주문서 삭제: local datasource의 deleteDraft를 호출한다', () async {
      // When: 주문서를 삭제한다
      await repository.deleteDraftOrder();

      // Then: local datasource의 deleteDraft가 호출된다
      expect(fakeLocalDataSource.deleteDraftCalls, 1);
    });

    test('주문서 유효성 검증: 엔티티를 모델로 변환하여 remote datasource를 호출하고 결과를 엔티티로 변환한다',
        () async {
      // Given: remote datasource가 유효성 검증 결과를 반환하도록 설정
      fakeRemoteDataSource.validationResultToReturn = const ValidationResultModel(
        isValid: false,
        errors: [
          ValidationErrorWithProductModel(
            productCode: '01234567',
            error: ValidationErrorModel(
              errorType: 'MIN_ORDER_QUANTITY',
              message: '최소 주문 수량은 10박스입니다',
              minOrderQuantity: 10,
            ),
          ),
        ],
      );

      // When: 주문서 유효성을 검증한다
      final result = await repository.validateOrder(
        orderDraft: _sampleOrderDraft,
      );

      // Then: remote datasource가 호출되고 결과가 엔티티로 변환된다
      expect(fakeRemoteDataSource.validateOrderCalls, 1);
      expect(result, isA<ValidationResult>());
      expect(result.isValid, false);
      expect(result.errors, isNotEmpty);
      expect(result.errors['01234567'], isNotNull);
      expect(result.errors['01234567']!.errorType, ValidationErrorType.minOrderQuantity);
      expect(result.errors['01234567']!.message, '최소 주문 수량은 10박스입니다');
      expect(result.errors['01234567']!.minOrderQuantity, 10);
    });

    test('주문서 전송: 엔티티를 모델로 변환하여 remote datasource를 호출하고 결과를 엔티티로 변환한다',
        () async {
      // Given: remote datasource가 전송 결과를 반환하도록 설정
      fakeRemoteDataSource.submitResultToReturn = const OrderSubmitResultModel(
        orderId: 1,
        orderRequestNumber: 'OP00000001',
        status: 'PENDING',
      );

      // When: 주문서를 전송한다
      final result = await repository.submitOrder(
        orderDraft: _sampleOrderDraft,
      );

      // Then: remote datasource가 호출되고 결과가 엔티티로 변환된다
      expect(fakeRemoteDataSource.submitOrderCalls, 1);
      expect(result, isA<OrderSubmitResult>());
      expect(result.orderId, 1);
      expect(result.orderRequestNumber, 'OP00000001');
      expect(result.status, 'PENDING');
    });

    test('주문서 수정: orderId와 엔티티를 전달하여 remote datasource를 호출하고 결과를 엔티티로 변환한다',
        () async {
      // Given: remote datasource가 수정 결과를 반환하도록 설정
      fakeRemoteDataSource.updateResultToReturn = const OrderSubmitResultModel(
        orderId: 123,
        orderRequestNumber: 'OP00000123',
        status: 'UPDATED',
      );

      // When: 주문서를 수정한다
      final result = await repository.updateOrder(
        orderId: 123,
        orderDraft: _sampleOrderDraft,
      );

      // Then: remote datasource가 올바른 파라미터로 호출된다
      expect(fakeRemoteDataSource.updateOrderCalls, 1);
      expect(fakeRemoteDataSource.lastUpdateOrderId, 123);
      expect(result, isA<OrderSubmitResult>());
      expect(result.orderId, 123);
      expect(result.orderRequestNumber, 'OP00000123');
      expect(result.status, 'UPDATED');
    });

    test('즐겨찾기 추가: remote datasource를 productCode와 함께 호출한다', () async {
      // When: 즐겨찾기에 추가한다
      await repository.addToFavorites(productCode: '01234567');

      // Then: remote datasource가 올바른 파라미터로 호출된다
      expect(fakeRemoteDataSource.addToFavoritesCalls, 1);
      expect(fakeRemoteDataSource.lastAddToFavoritesProductCode, '01234567');
    });

    test('즐겨찾기 삭제: remote datasource를 productCode와 함께 호출한다', () async {
      // When: 즐겨찾기에서 삭제한다
      await repository.removeFromFavorites(productCode: '01234567');

      // Then: remote datasource가 올바른 파라미터로 호출된다
      expect(fakeRemoteDataSource.removeFromFavoritesCalls, 1);
      expect(fakeRemoteDataSource.lastRemoveFromFavoritesProductCode, '01234567');
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake Implementations
// ──────────────────────────────────────────────────────────────────

/// FakeOrderRemoteDataSource
///
/// OrderRemoteDataSource 인터페이스를 구현한 테스트용 가짜 객체입니다.
/// mockito를 사용하지 않고 수동으로 작성한 fake입니다.
class FakeOrderRemoteDataSource implements OrderRemoteDataSource {
  // getCreditBalance
  int getCreditBalanceCalls = 0;
  int? lastCreditBalanceClientId;
  int creditBalanceToReturn = 50000000;

  // getFavoriteProducts
  int getFavoriteProductsCalls = 0;
  List<ProductForOrderModel> favoriteProductsToReturn = [];

  // searchProductsForOrder
  int searchProductsForOrderCalls = 0;
  String? lastSearchQuery;
  String? lastSearchCategoryMid;
  String? lastSearchCategorySub;
  List<ProductForOrderModel> searchResultsToReturn = [];

  // getProductByBarcode
  int getProductByBarcodeCalls = 0;
  String? lastBarcode;
  ProductForOrderModel? barcodeProductToReturn;

  // validateOrder
  int validateOrderCalls = 0;
  ValidationResultModel validationResultToReturn = const ValidationResultModel(
    isValid: true,
  );

  // submitOrder
  int submitOrderCalls = 0;
  OrderSubmitResultModel submitResultToReturn = const OrderSubmitResultModel(
    orderId: 1,
    orderRequestNumber: 'OP00000001',
    status: 'PENDING',
  );

  // updateOrder
  int updateOrderCalls = 0;
  int? lastUpdateOrderId;
  OrderSubmitResultModel updateResultToReturn = const OrderSubmitResultModel(
    orderId: 1,
    orderRequestNumber: 'OP00000001',
    status: 'PENDING',
  );

  // addToFavorites
  int addToFavoritesCalls = 0;
  String? lastAddToFavoritesProductCode;

  // removeFromFavorites
  int removeFromFavoritesCalls = 0;
  String? lastRemoveFromFavoritesProductCode;

  @override
  Future<int> getCreditBalance({required int clientId}) async {
    getCreditBalanceCalls++;
    lastCreditBalanceClientId = clientId;
    return creditBalanceToReturn;
  }

  @override
  Future<List<ProductForOrderModel>> getFavoriteProducts() async {
    getFavoriteProductsCalls++;
    return favoriteProductsToReturn;
  }

  @override
  Future<List<ProductForOrderModel>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    searchProductsForOrderCalls++;
    lastSearchQuery = query;
    lastSearchCategoryMid = categoryMid;
    lastSearchCategorySub = categorySub;
    return searchResultsToReturn;
  }

  @override
  Future<ProductForOrderModel> getProductByBarcode({
    required String barcode,
  }) async {
    getProductByBarcodeCalls++;
    lastBarcode = barcode;
    return barcodeProductToReturn!;
  }

  @override
  Future<void> saveDraftOrder({required OrderDraftModel draft}) async {
    throw UnimplementedError();
  }

  @override
  Future<OrderDraftModel?> loadDraftOrder() async {
    throw UnimplementedError();
  }

  @override
  Future<ValidationResultModel> validateOrder({
    required OrderDraftModel draft,
  }) async {
    validateOrderCalls++;
    return validationResultToReturn;
  }

  @override
  Future<OrderSubmitResultModel> submitOrder({
    required OrderDraftModel draft,
  }) async {
    submitOrderCalls++;
    return submitResultToReturn;
  }

  @override
  Future<OrderSubmitResultModel> updateOrder({
    required int orderId,
    required OrderDraftModel draft,
  }) async {
    updateOrderCalls++;
    lastUpdateOrderId = orderId;
    return updateResultToReturn;
  }

  @override
  Future<void> addToFavorites({required String productCode}) async {
    addToFavoritesCalls++;
    lastAddToFavoritesProductCode = productCode;
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    removeFromFavoritesCalls++;
    lastRemoveFromFavoritesProductCode = productCode;
  }

  // ─── 기존 메서드들 (F22와 무관, UnimplementedError 던짐) ─────────

  @override
  Future<OrderListResponseModel> getMyOrders({
    int? clientId,
    String? status,
    String? deliveryDateFrom,
    String? deliveryDateTo,
    String sortBy = 'orderDate',
    String sortDir = 'DESC',
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<OrderDetailModel> getOrderDetail({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<void> resendOrder({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderCancelResponseModel> cancelOrder({
    required int orderId,
    required OrderCancelRequestModel request,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<ClientOrderListResponseModel> getClientOrders({
    required int clientId,
    String? deliveryDate,
    int page = 0,
    int size = 20,
  }) {
    throw UnimplementedError();
  }

  @override
  Future<ClientOrderDetailModel> getClientOrderDetail({
    required String sapOrderNumber,
  }) {
    throw UnimplementedError();
  }
}

/// FakeOrderLocalDataSource
///
/// OrderLocalDataSource 인터페이스를 구현한 테스트용 가짜 객체입니다.
/// 실제로는 concrete class이지만 Dart에서는 implements로 구현 가능합니다.
class FakeOrderLocalDataSource implements OrderLocalDataSource {
  Map<String, dynamic>? savedDraft;
  Map<String, dynamic>? draftToReturn;
  int deleteDraftCalls = 0;

  @override
  Future<void> saveDraft(Map<String, dynamic> draftJson) async {
    savedDraft = draftJson;
  }

  @override
  Future<Map<String, dynamic>?> loadDraft() async {
    return draftToReturn;
  }

  @override
  Future<void> deleteDraft() async {
    deleteDraftCalls++;
    savedDraft = null;
    draftToReturn = null;
  }

  @override
  Future<bool> hasDraft() async {
    return draftToReturn != null;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample Test Data
// ──────────────────────────────────────────────────────────────────

const _sampleProductModel1 = ProductForOrderModel(
  productCode: '01234567',
  productName: '오뚜기 진라면',
  barcode: '8801234567890',
  storageType: '상온',
  shelfLife: '12개월',
  unitPrice: 1200,
  boxSize: 20,
  isFavorite: true,
  categoryMid: '라면',
  categorySub: '봉지라면',
);

const _sampleProductModel2 = ProductForOrderModel(
  productCode: '01234568',
  productName: '오뚜기 참깨라면',
  barcode: '8801234567891',
  storageType: '상온',
  shelfLife: '12개월',
  unitPrice: 1300,
  boxSize: 20,
  isFavorite: true,
  categoryMid: '라면',
  categorySub: '봉지라면',
);

final _sampleOrderDraft = OrderDraft(
  clientId: 100,
  clientName: '테스트거래처',
  deliveryDate: DateTime(2026, 3, 1),
  items: const [
    OrderDraftItem(
      productCode: '01234567',
      productName: '오뚜기 진라면',
      quantityBoxes: 5.0,
      quantityPieces: 10,
      unitPrice: 1200,
      boxSize: 20,
      totalPrice: 72000,
    ),
  ],
  totalAmount: 72000,
  isDraft: false,
  lastModified: DateTime(2026, 2, 10, 10, 30),
);
