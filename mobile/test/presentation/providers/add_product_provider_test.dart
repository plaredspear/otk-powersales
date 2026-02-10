import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/order_draft.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/validation_error.dart';
import 'package:mobile/domain/repositories/order_repository.dart';
import 'package:mobile/domain/usecases/add_to_favorites_usecase.dart';
import 'package:mobile/domain/usecases/get_favorite_products_usecase.dart';
import 'package:mobile/domain/usecases/remove_from_favorites_usecase.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';
import 'package:mobile/presentation/providers/add_product_provider.dart';
import 'package:mobile/presentation/providers/add_product_state.dart';

void main() {
  group('AddProductNotifier', () {
    late FakeOrderRepository fakeRepo;
    late AddProductNotifier notifier;

    AddProductNotifier createNotifier() {
      return AddProductNotifier(
        getFavoriteProducts: GetFavoriteProducts(fakeRepo),
        searchProductsForOrder: SearchProductsForOrder(fakeRepo),
        addToFavorites: AddToFavorites(fakeRepo),
        removeFromFavorites: RemoveFromFavorites(fakeRepo),
      );
    }

    setUp(() {
      fakeRepo = FakeOrderRepository();
      notifier = createNotifier();
    });

    group('initialize', () {
      test('즐겨찾기 제품 로드', () async {
        // Arrange
        final products = [
          _createTestProduct(productCode: 'P001'),
          _createTestProduct(productCode: 'P002'),
        ];
        fakeRepo.favoriteProductsResult = products;

        // Act
        await notifier.initialize();

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.favoriteProducts, products);
        expect(notifier.state.errorMessage, isNull);
        expect(fakeRepo.favoriteProductsCalled, true);
      });

      test('에러 발생 시 에러 상태', () async {
        // Arrange
        fakeRepo.shouldThrowOnFavorites = true;
        fakeRepo.errorMessage = '즐겨찾기 로드 실패';

        // Act
        await notifier.initialize();

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '즐겨찾기 로드 실패');
        expect(notifier.state.favoriteProducts, isEmpty);
      });
    });

    group('changeTab', () {
      test('탭 변경', () {
        // Act
        notifier.changeTab(AddProductTab.search);

        // Assert
        expect(notifier.state.currentTab, AddProductTab.search);

        // Act
        notifier.changeTab(AddProductTab.orderHistory);

        // Assert
        expect(notifier.state.currentTab, AddProductTab.orderHistory);
      });
    });

    group('loadFavoriteProducts', () {
      test('성공 시 제품 목록 설정', () async {
        // Arrange
        final products = [
          _createTestProduct(productCode: 'P001'),
          _createTestProduct(productCode: 'P002'),
        ];
        fakeRepo.favoriteProductsResult = products;

        // Act
        await notifier.loadFavoriteProducts();

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.favoriteProducts, products);
        expect(notifier.state.errorMessage, isNull);
      });

      test('실패 시 에러 설정', () async {
        // Arrange
        fakeRepo.shouldThrowOnFavorites = true;
        fakeRepo.errorMessage = '로드 실패';

        // Act
        await notifier.loadFavoriteProducts();

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '로드 실패');
      });
    });

    group('searchProducts', () {
      test('검색어 입력 시 결과 반환', () async {
        // Arrange
        final results = [
          _createTestProduct(productCode: 'P001', productName: '진라면'),
          _createTestProduct(productCode: 'P002', productName: '진짬뽕'),
        ];
        fakeRepo.searchProductsResult = results;

        // Act
        await notifier.searchProducts(query: '진라면');

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.searchQuery, '진라면');
        expect(notifier.state.searchResults, results);
        expect(notifier.state.errorMessage, isNull);
        expect(fakeRepo.searchProductsCalled, true);
        expect(fakeRepo.lastSearchQuery, '진라면');
      });

      test('빈 검색어면 결과 클리어', () async {
        // Arrange - set some results first
        notifier.state = notifier.state.copyWith(
          searchResults: [_createTestProduct(productCode: 'P001')],
        );

        // Act
        await notifier.searchProducts(query: '');

        // Assert
        expect(notifier.state.searchQuery, '');
        expect(notifier.state.searchResults, isEmpty);
        expect(fakeRepo.searchProductsCalled, false);
      });

      test('카테고리 필터와 함께 검색', () async {
        // Arrange
        fakeRepo.searchProductsResult = [
          _createTestProduct(productCode: 'P001'),
        ];

        // Act
        await notifier.searchProducts(
          query: '라면',
          categoryMid: '면류',
          categorySub: '라면',
        );

        // Assert
        expect(fakeRepo.searchProductsCalled, true);
        expect(fakeRepo.lastSearchQuery, '라면');
        expect(fakeRepo.lastCategoryMid, '면류');
        expect(fakeRepo.lastCategorySub, '라면');
      });

      test('검색 실패 시 에러', () async {
        // Arrange
        fakeRepo.shouldThrowOnSearch = true;
        fakeRepo.errorMessage = '검색 실패';

        // Act
        await notifier.searchProducts(query: 'test');

        // Assert
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '검색 실패');
      });
    });

    group('setOrderHistoryGroups', () {
      test('그룹 목록 설정', () {
        // Arrange
        final groups = [
          OrderHistoryGroup(
            orderId: 1,
            orderDate: '2026-02-01',
            clientName: '거래처A',
            products: [_createTestProduct(productCode: 'P001')],
          ),
          OrderHistoryGroup(
            orderId: 2,
            orderDate: '2026-02-02',
            clientName: '거래처B',
            products: [_createTestProduct(productCode: 'P002')],
          ),
        ];

        // Act
        notifier.setOrderHistoryGroups(groups);

        // Assert
        expect(notifier.state.orderHistoryGroups, groups);
      });
    });

    group('setHistoryDateRange', () {
      test('날짜 범위 설정', () {
        // Arrange
        final from = DateTime(2026, 2, 1);
        final to = DateTime(2026, 2, 10);

        // Act
        notifier.setHistoryDateRange(from, to);

        // Assert
        expect(notifier.state.historyDateFrom, from);
        expect(notifier.state.historyDateTo, to);
      });
    });

    group('toggleOrderHistoryExpansion', () {
      test('해당 주문의 isExpanded 토글', () {
        // Arrange
        final groups = [
          OrderHistoryGroup(
            orderId: 1,
            orderDate: '2026-02-01',
            clientName: '거래처A',
            products: [],
            isExpanded: false,
          ),
          OrderHistoryGroup(
            orderId: 2,
            orderDate: '2026-02-02',
            clientName: '거래처B',
            products: [],
            isExpanded: false,
          ),
        ];
        notifier.state = notifier.state.copyWith(
          orderHistoryGroups: groups,
        );

        // Act
        notifier.toggleOrderHistoryExpansion(1);

        // Assert
        expect(notifier.state.orderHistoryGroups[0].isExpanded, true);
        expect(notifier.state.orderHistoryGroups[1].isExpanded, false);

        // Act - toggle again
        notifier.toggleOrderHistoryExpansion(1);

        // Assert
        expect(notifier.state.orderHistoryGroups[0].isExpanded, false);
      });

      test('다른 그룹은 영향 없음', () {
        // Arrange
        final groups = [
          OrderHistoryGroup(
            orderId: 1,
            orderDate: '2026-02-01',
            clientName: '거래처A',
            products: [],
            isExpanded: true,
          ),
          OrderHistoryGroup(
            orderId: 2,
            orderDate: '2026-02-02',
            clientName: '거래처B',
            products: [],
            isExpanded: false,
          ),
        ];
        notifier.state = notifier.state.copyWith(
          orderHistoryGroups: groups,
        );

        // Act
        notifier.toggleOrderHistoryExpansion(2);

        // Assert
        expect(notifier.state.orderHistoryGroups[0].isExpanded, true);
        expect(notifier.state.orderHistoryGroups[1].isExpanded, true);
      });
    });

    group('toggleProductSelection', () {
      test('제품 코드 추가', () {
        // Act
        notifier.toggleProductSelection('P001');

        // Assert
        expect(notifier.state.selectedProductCodes, {'P001'});

        // Act
        notifier.toggleProductSelection('P002');

        // Assert
        expect(notifier.state.selectedProductCodes, {'P001', 'P002'});
      });

      test('이미 선택된 코드면 제거', () {
        // Arrange
        notifier.state = notifier.state.copyWith(
          selectedProductCodes: {'P001', 'P002'},
        );

        // Act
        notifier.toggleProductSelection('P001');

        // Assert
        expect(notifier.state.selectedProductCodes, {'P002'});
      });
    });

    group('clearSelection', () {
      test('모든 선택 제거', () {
        // Arrange
        notifier.state = notifier.state.copyWith(
          selectedProductCodes: {'P001', 'P002', 'P003'},
        );

        // Act
        notifier.clearSelection();

        // Assert
        expect(notifier.state.selectedProductCodes, isEmpty);
      });
    });

    group('addToFavorites', () {
      test('UseCase 호출 및 searchResults 업데이트', () async {
        // Arrange
        final searchResults = [
          _createTestProduct(productCode: 'P001', isFavorite: false),
          _createTestProduct(productCode: 'P002', isFavorite: false),
        ];
        notifier.state = notifier.state.copyWith(
          searchResults: searchResults,
        );

        // Act
        await notifier.addToFavorites('P001');

        // Assert
        expect(fakeRepo.addToFavoritesCalled, true);
        expect(fakeRepo.lastAddedProductCode, 'P001');
        expect(notifier.state.searchResults[0].isFavorite, true);
        expect(notifier.state.searchResults[1].isFavorite, false);
        expect(notifier.state.successMessage, '즐겨찾기에 추가되었습니다.');
      });

      test('에러 발생 시 에러 상태', () async {
        // Arrange
        fakeRepo.shouldThrowOnAddFavorites = true;
        fakeRepo.errorMessage = '추가 실패';

        // Act
        await notifier.addToFavorites('P001');

        // Assert
        expect(notifier.state.errorMessage, '추가 실패');
        expect(notifier.state.successMessage, isNull);
      });
    });

    group('removeFromFavorites', () {
      test('UseCase 호출 및 favoriteProducts에서 제거', () async {
        // Arrange
        final favorites = [
          _createTestProduct(productCode: 'P001', isFavorite: true),
          _createTestProduct(productCode: 'P002', isFavorite: true),
        ];
        notifier.state = notifier.state.copyWith(
          favoriteProducts: favorites,
        );

        // Act
        await notifier.removeFromFavorites('P001');

        // Assert
        expect(fakeRepo.removeFromFavoritesCalled, true);
        expect(fakeRepo.lastRemovedProductCode, 'P001');
        expect(notifier.state.favoriteProducts.length, 1);
        expect(notifier.state.favoriteProducts[0].productCode, 'P002');
        expect(notifier.state.successMessage, '즐겨찾기에서 삭제되었습니다.');
      });

      test('searchResults의 isFavorite도 업데이트', () async {
        // Arrange
        final favorites = [
          _createTestProduct(productCode: 'P001', isFavorite: true),
        ];
        final searchResults = [
          _createTestProduct(productCode: 'P001', isFavorite: true),
          _createTestProduct(productCode: 'P002', isFavorite: false),
        ];
        notifier.state = notifier.state.copyWith(
          favoriteProducts: favorites,
          searchResults: searchResults,
        );

        // Act
        await notifier.removeFromFavorites('P001');

        // Assert
        expect(notifier.state.favoriteProducts, isEmpty);
        expect(notifier.state.searchResults[0].isFavorite, false);
        expect(notifier.state.searchResults[1].isFavorite, false);
      });

      test('에러 발생 시 에러 상태', () async {
        // Arrange
        fakeRepo.shouldThrowOnRemoveFavorites = true;
        fakeRepo.errorMessage = '삭제 실패';

        // Act
        await notifier.removeFromFavorites('P001');

        // Assert
        expect(notifier.state.errorMessage, '삭제 실패');
        expect(notifier.state.successMessage, isNull);
      });
    });

    group('getSelectedOrderDraftItems', () {
      test('선택된 제품들을 OrderDraftItem으로 변환', () {
        // Arrange
        final favorites = [
          _createTestProduct(
            productCode: 'P001',
            productName: '진라면',
            unitPrice: 5000,
            boxSize: 20,
          ),
          _createTestProduct(
            productCode: 'P002',
            productName: '진짬뽕',
            unitPrice: 6000,
            boxSize: 24,
          ),
        ];
        notifier.state = notifier.state.copyWith(
          favoriteProducts: favorites,
          selectedProductCodes: {'P001', 'P002'},
        );

        // Act
        final items = notifier.getSelectedOrderDraftItems();

        // Assert
        expect(items.length, 2);
        expect(items[0].productCode, 'P001');
        expect(items[0].productName, '진라면');
        expect(items[0].quantityBoxes, 0);
        expect(items[0].quantityPieces, 0);
        expect(items[0].unitPrice, 5000);
        expect(items[0].boxSize, 20);
        expect(items[0].totalPrice, 0);

        expect(items[1].productCode, 'P002');
        expect(items[1].productName, '진짬뽕');
        expect(items[1].quantityBoxes, 0);
        expect(items[1].quantityPieces, 0);
        expect(items[1].unitPrice, 6000);
        expect(items[1].boxSize, 24);
        expect(items[1].totalPrice, 0);
      });

      test('모든 탭의 제품을 통합하여 수집', () {
        // Arrange
        final favorites = [
          _createTestProduct(productCode: 'P001', productName: '제품1'),
        ];
        final searchResults = [
          _createTestProduct(productCode: 'P002', productName: '제품2'),
        ];
        final orderHistoryGroups = [
          OrderHistoryGroup(
            orderId: 1,
            orderDate: '2026-02-01',
            clientName: '거래처A',
            products: [
              _createTestProduct(productCode: 'P003', productName: '제품3'),
            ],
          ),
        ];
        notifier.state = notifier.state.copyWith(
          favoriteProducts: favorites,
          searchResults: searchResults,
          orderHistoryGroups: orderHistoryGroups,
          selectedProductCodes: {'P001', 'P002', 'P003'},
        );

        // Act
        final items = notifier.getSelectedOrderDraftItems();

        // Assert
        expect(items.length, 3);
        expect(items.map((e) => e.productCode).toSet(), {'P001', 'P002', 'P003'});
      });

      test('선택되지 않은 제품은 제외', () {
        // Arrange
        final favorites = [
          _createTestProduct(productCode: 'P001'),
          _createTestProduct(productCode: 'P002'),
        ];
        notifier.state = notifier.state.copyWith(
          favoriteProducts: favorites,
          selectedProductCodes: {'P001'},
        );

        // Act
        final items = notifier.getSelectedOrderDraftItems();

        // Assert
        expect(items.length, 1);
        expect(items[0].productCode, 'P001');
      });

      test('중복 제품 코드는 한 번만 포함', () {
        // Arrange
        final product = _createTestProduct(productCode: 'P001');
        final favorites = [product];
        final searchResults = [product];
        notifier.state = notifier.state.copyWith(
          favoriteProducts: favorites,
          searchResults: searchResults,
          selectedProductCodes: {'P001'},
        );

        // Act
        final items = notifier.getSelectedOrderDraftItems();

        // Assert
        expect(items.length, 1);
        expect(items[0].productCode, 'P001');
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
  });
}

// --- Fake Repository ---

class FakeOrderRepository implements OrderRepository {
  // --- Controllable returns ---
  List<ProductForOrder> favoriteProductsResult = [];
  List<ProductForOrder> searchProductsResult = [];
  bool shouldThrowOnFavorites = false;
  bool shouldThrowOnSearch = false;
  bool shouldThrowOnAddFavorites = false;
  bool shouldThrowOnRemoveFavorites = false;
  String errorMessage = '테스트 에러';

  // --- Call tracking ---
  bool favoriteProductsCalled = false;
  bool searchProductsCalled = false;
  bool addToFavoritesCalled = false;
  bool removeFromFavoritesCalled = false;
  String? lastSearchQuery;
  String? lastCategoryMid;
  String? lastCategorySub;
  String? lastAddedProductCode;
  String? lastRemovedProductCode;

  @override
  Future<List<ProductForOrder>> getFavoriteProducts() async {
    favoriteProductsCalled = true;
    if (shouldThrowOnFavorites) throw Exception(errorMessage);
    return favoriteProductsResult;
  }

  @override
  Future<List<ProductForOrder>> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
  }) async {
    searchProductsCalled = true;
    lastSearchQuery = query;
    lastCategoryMid = categoryMid;
    lastCategorySub = categorySub;
    if (shouldThrowOnSearch) throw Exception(errorMessage);
    return searchProductsResult;
  }

  @override
  Future<void> addToFavorites({required String productCode}) async {
    addToFavoritesCalled = true;
    lastAddedProductCode = productCode;
    if (shouldThrowOnAddFavorites) throw Exception(errorMessage);
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    removeFromFavoritesCalled = true;
    lastRemovedProductCode = productCode;
    if (shouldThrowOnRemoveFavorites) throw Exception(errorMessage);
  }

  // --- Non-AddProduct methods - stub ---
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
  Future<ProductForOrder> getProductByBarcode({required String barcode}) async =>
      throw UnimplementedError();

  @override
  Future<int> getCreditBalance({required int clientId}) async =>
      throw UnimplementedError();

  @override
  Future<OrderDraft?> loadDraftOrder() async => throw UnimplementedError();

  @override
  Future<void> saveDraftOrder({required OrderDraft orderDraft}) async =>
      throw UnimplementedError();

  @override
  Future<void> deleteDraftOrder() async => throw UnimplementedError();

  @override
  Future<ValidationResult> validateOrder({
    required OrderDraft orderDraft,
  }) async =>
      throw UnimplementedError();

  @override
  Future<OrderSubmitResult> submitOrder({
    required OrderDraft orderDraft,
  }) async =>
      throw UnimplementedError();

  @override
  Future<OrderSubmitResult> updateOrder({
    required int orderId,
    required OrderDraft orderDraft,
  }) async =>
      throw UnimplementedError();
}

// --- Test Helper Functions ---

ProductForOrder _createTestProduct({
  String productCode = 'P001',
  String productName = '오뚜기 진라면',
  String barcode = '8801234567890',
  String storageType = '상온',
  String shelfLife = '12개월',
  int unitPrice = 5000,
  int boxSize = 20,
  bool isFavorite = true,
  String? categoryMid,
  String? categorySub,
}) {
  return ProductForOrder(
    productCode: productCode,
    productName: productName,
    barcode: barcode,
    storageType: storageType,
    shelfLife: shelfLife,
    unitPrice: unitPrice,
    boxSize: boxSize,
    isFavorite: isFavorite,
    categoryMid: categoryMid,
    categorySub: categorySub,
  );
}
