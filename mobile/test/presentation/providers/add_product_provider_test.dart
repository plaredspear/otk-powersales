import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_cancel.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/product_search_result.dart';
import 'package:mobile/domain/entities/product_order_history_group.dart';
import 'package:mobile/domain/repositories/order_request_repository.dart';
import 'package:mobile/domain/usecases/add_to_favorites_usecase.dart';
import 'package:mobile/domain/usecases/get_account_order_history_usecase.dart';
import 'package:mobile/domain/usecases/get_favorite_products_usecase.dart';
import 'package:mobile/domain/usecases/remove_from_favorites_usecase.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';
import 'package:mobile/presentation/providers/add_product_provider.dart';
import 'package:mobile/presentation/providers/add_product_state.dart';

void main() {
  group('AddProductNotifier', () {
    late FakeOrderRequestRepository fakeRepo;
    late AddProductNotifier notifier;

    AddProductNotifier createNotifier() {
      return AddProductNotifier(
        getFavoriteProducts: GetFavoriteProducts(fakeRepo),
        searchProductsForOrder: SearchProductsForOrder(fakeRepo),
        getAccountOrderHistory: GetAccountOrderHistory(fakeRepo),
        addToFavorites: AddToFavorites(fakeRepo),
        removeFromFavorites: RemoveFromFavorites(fakeRepo),
      );
    }

    setUp(() {
      fakeRepo = FakeOrderRequestRepository();
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

      test('더 불러올 결과가 없으면 hasMoreSearchResults=false', () async {
        // Arrange - 전체 1건, 첫 페이지에 1건 (전부 로드됨)
        fakeRepo.searchProductsResult = [
          _createTestProduct(productCode: 'P001'),
        ];

        // Act
        await notifier.searchProducts(query: '라면');

        // Assert
        expect(notifier.state.searchTotalCount, 1);
        expect(notifier.state.searchPage, 0);
        expect(notifier.state.hasMoreSearchResults, false);
      });

      test('전체 건수가 로드분보다 많으면 hasMoreSearchResults=true', () async {
        // Arrange - 첫 페이지 1건인데 전체는 5건
        fakeRepo.searchProductsResult = [
          _createTestProduct(productCode: 'P001'),
        ];
        fakeRepo.searchTotalCountOverride = 5;

        // Act
        await notifier.searchProducts(query: '라면');

        // Assert
        expect(notifier.state.searchTotalCount, 5);
        expect(notifier.state.hasMoreSearchResults, true);
      });

      test('빈 검색어로 클리어하면 페이지 상태도 초기화된다', () async {
        // Arrange - 먼저 더 불러올 게 있는 상태를 만든다
        fakeRepo.searchProductsResult = [
          _createTestProduct(productCode: 'P001'),
        ];
        fakeRepo.searchTotalCountOverride = 500;
        await notifier.searchProducts(query: '라면');
        expect(notifier.state.hasMoreSearchResults, true);

        // Act
        await notifier.searchProducts(query: '');

        // Assert
        expect(notifier.state.searchTotalCount, 0);
        expect(notifier.state.searchPage, 0);
        expect(notifier.state.hasMoreSearchResults, false);
      });
    });

    group('loadMoreSearchResults (무한스크롤)', () {
      test('다음 페이지를 기존 목록 뒤에 이어붙인다', () async {
        // Arrange - 2페이지로 나뉜 전체 4건
        fakeRepo.pagedSearchResults = [
          [
            _createTestProduct(productCode: 'P001'),
            _createTestProduct(productCode: 'P002'),
          ],
          [
            _createTestProduct(productCode: 'P003'),
            _createTestProduct(productCode: 'P004'),
          ],
        ];
        await notifier.searchProducts(query: '라면');
        expect(notifier.state.searchResults.length, 2);
        expect(notifier.state.hasMoreSearchResults, true);

        // Act
        await notifier.loadMoreSearchResults();

        // Assert - 누적 4건, 페이지 1, 전건 로드 완료
        expect(notifier.state.searchResults.length, 4);
        expect(
          notifier.state.searchResults.map((p) => p.productCode),
          ['P001', 'P002', 'P003', 'P004'],
        );
        expect(notifier.state.searchPage, 1);
        expect(notifier.state.hasMoreSearchResults, false);
        expect(notifier.state.isLoadingMore, false);
      });

      test('추가 로드 시 다음 페이지 번호와 누적 건수를 넘긴다', () async {
        // Arrange
        fakeRepo.pagedSearchResults = [
          [_createTestProduct(productCode: 'P001')],
          [_createTestProduct(productCode: 'P002')],
        ];
        await notifier.searchProducts(query: '라면');

        // Act
        await notifier.loadMoreSearchResults();

        // Assert
        expect(fakeRepo.lastSearchPage, 1);
        expect(fakeRepo.lastLoadedCount, 1);
      });

      test('더 불러올 결과가 없으면 API 를 호출하지 않는다', () async {
        // Arrange - 전체 1건이라 첫 페이지로 끝
        fakeRepo.searchProductsResult = [
          _createTestProduct(productCode: 'P001'),
        ];
        await notifier.searchProducts(query: '라면');
        expect(notifier.state.hasMoreSearchResults, false);
        final callsAfterSearch = fakeRepo.searchProductsCallCount;

        // Act
        await notifier.loadMoreSearchResults();

        // Assert
        expect(fakeRepo.searchProductsCallCount, callsAfterSearch);
      });

      test('추가 로드 실패 시 기존 목록을 유지하고 에러만 노출한다', () async {
        // Arrange - 첫 페이지 성공 후 다음 호출부터 실패
        fakeRepo.pagedSearchResults = [
          [_createTestProduct(productCode: 'P001')],
          [_createTestProduct(productCode: 'P002')],
        ];
        await notifier.searchProducts(query: '라면');
        fakeRepo.shouldThrowOnSearch = true;

        // Act
        await notifier.loadMoreSearchResults();

        // Assert - 누적분 보존
        expect(notifier.state.searchResults.length, 1);
        expect(notifier.state.errorMessage, isNotNull);
        expect(notifier.state.isLoadingMore, false);
      });

      test('새 검색을 하면 이전 누적 결과를 버리고 첫 페이지부터 다시 로드한다', () async {
        // Arrange - 2페이지까지 로드한 상태
        fakeRepo.pagedSearchResults = [
          [_createTestProduct(productCode: 'P001')],
          [_createTestProduct(productCode: 'P002')],
        ];
        await notifier.searchProducts(query: '라면');
        await notifier.loadMoreSearchResults();
        expect(notifier.state.searchResults.length, 2);

        // Act - 새 검색
        await notifier.searchProducts(query: '진라면');

        // Assert - 첫 페이지 결과만 남는다
        expect(notifier.state.searchResults.length, 1);
        expect(notifier.state.searchPage, 0);
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

      test('단건 선택 모드면 새 제품이 기존 선택을 대체', () {
        // Arrange - 단건 선택 모드 + 기존 선택
        notifier.state = notifier.state.copyWith(
          multiSelect: false,
          selectedProductCodes: {'P001'},
        );

        // Act - 다른 제품 선택
        notifier.toggleProductSelection('P002');

        // Assert - 기존 선택은 대체되고 1건만 유지
        expect(notifier.state.selectedProductCodes, {'P002'});

        // Act - 같은 제품 다시 탭하면 해제
        notifier.toggleProductSelection('P002');

        // Assert
        expect(notifier.state.selectedProductCodes, isEmpty);
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

    group('loadOrderHistory', () {
      test('거래처 ID가 없으면 조회하지 않고 빈 목록 유지', () async {
        // Arrange — accountId 미주입 상태로 initialize
        await notifier.initialize();
        fakeRepo.orderHistoryCalled = false;

        // Act
        await notifier.loadOrderHistory();

        // Assert
        expect(fakeRepo.orderHistoryCalled, false);
        expect(notifier.state.orderHistoryGroups, isEmpty);
      });

      test('거래처 ID가 있으면 주문일 그룹으로 조회', () async {
        // Arrange
        fakeRepo.orderHistoryResult = [
          ProductOrderHistoryGroup(
            orderDate: '2026-05-06',
            products: [_createTestProduct(productCode: 'P001')],
          ),
          ProductOrderHistoryGroup(
            orderDate: '2026-05-04',
            products: [_createTestProduct(productCode: 'P003')],
          ),
        ];
        await notifier.initialize(orderHistoryAccountId: 1071460);

        // Act
        await notifier.loadOrderHistory();

        // Assert
        expect(fakeRepo.orderHistoryCalled, true);
        expect(fakeRepo.lastOrderHistoryAccountId, 1071460);
        expect(notifier.state.orderHistoryGroups.length, 2);
        expect(notifier.state.orderHistoryGroups[0].orderDate, '2026-05-06');
        expect(notifier.state.orderHistoryGroups[0].isExpanded, true);
        expect(
          notifier.state.orderHistoryGroups[0].products.first.productCode,
          'P001',
        );
      });

      test('같은 거래처·기간이면 탭 재진입 시 재조회하지 않음', () async {
        // Arrange
        fakeRepo.orderHistoryResult = [
          ProductOrderHistoryGroup(
            orderDate: '2026-05-06',
            products: [_createTestProduct(productCode: 'P001')],
          ),
        ];
        await notifier.initialize(orderHistoryAccountId: 1071460);
        await notifier.loadOrderHistory();
        expect(fakeRepo.orderHistoryCallCount, 1);

        // Act — 다른 탭 갔다가 주문이력 탭 재진입
        notifier.changeTab(AddProductTab.favorites);
        notifier.changeTab(AddProductTab.orderHistory);
        await Future<void>.delayed(Duration.zero);

        // Assert — 캐시 재사용
        expect(fakeRepo.orderHistoryCallCount, 1);
        expect(notifier.state.orderHistoryGroups.length, 1);
      });

      test('재조회 시 펼쳐둔 주문의 확장 상태를 유지', () async {
        // Arrange — 2건 조회 후 두 번째 그룹만 펼침
        fakeRepo.orderHistoryResult = [
          ProductOrderHistoryGroup(
            orderDate: '2026-05-06',
            products: [_createTestProduct(productCode: 'P001')],
          ),
          ProductOrderHistoryGroup(
            orderDate: '2026-05-04',
            products: [_createTestProduct(productCode: 'P003')],
          ),
        ];
        await notifier.initialize(orderHistoryAccountId: 1071460);
        await notifier.loadOrderHistory();
        notifier.toggleOrderHistoryExpansion(1); // 2026-05-04 펼침

        // Act — 강제 재조회
        await notifier.loadOrderHistory(force: true);

        // Assert
        expect(fakeRepo.orderHistoryCallCount, 2);
        expect(notifier.state.orderHistoryGroups[1].orderDate, '2026-05-04');
        expect(notifier.state.orderHistoryGroups[1].isExpanded, true);
      });

      test('기간을 변경하면 재조회', () async {
        // Arrange
        fakeRepo.orderHistoryResult = [
          ProductOrderHistoryGroup(
            orderDate: '2026-05-06',
            products: [_createTestProduct(productCode: 'P001')],
          ),
        ];
        await notifier.initialize(orderHistoryAccountId: 1071460);
        await notifier.loadOrderHistory();

        // Act
        notifier.setHistoryDateRange(
          DateTime(2026, 4, 1),
          DateTime(2026, 4, 30),
        );
        await Future<void>.delayed(Duration.zero);

        // Assert
        expect(fakeRepo.orderHistoryCallCount, 2);
      });
    });

    group('getSelectedProducts', () {
      test('선택된 제품들을 ProductForOrder로 반환', () {
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
        final items = notifier.getSelectedProducts();

        // Assert
        expect(items.length, 2);
        final byCode = {for (final p in items) p.productCode: p};
        expect(byCode['P001']!.productName, '진라면');
        expect(byCode['P001']!.unitPrice, 5000);
        expect(byCode['P001']!.boxSize, 20);
        expect(byCode['P002']!.productName, '진짬뽕');
        expect(byCode['P002']!.unitPrice, 6000);
        expect(byCode['P002']!.boxSize, 24);
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
        final items = notifier.getSelectedProducts();

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
        final items = notifier.getSelectedProducts();

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
        final items = notifier.getSelectedProducts();

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

class FakeOrderRequestRepository implements OrderRequestRepository {
  // --- Controllable returns ---
  List<ProductForOrder> favoriteProductsResult = [];
  List<ProductForOrder> searchProductsResult = [];

  /// 검색 전체 건수 override — hasMore 판정 시나리오 재현용.
  int? searchTotalCountOverride;

  /// 페이지별 검색 결과 — 무한스크롤 추가 로드 재현용.
  /// null 이면 [searchProductsResult] 를 0페이지 결과로 쓴다.
  List<List<ProductForOrder>>? pagedSearchResults;

  int searchProductsCallCount = 0;
  int? lastSearchPage;
  int? lastLoadedCount;
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
  Future<ProductSearchResult> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
    int page = 0,
    int loadedCount = 0,
  }) async {
    searchProductsCalled = true;
    searchProductsCallCount++;
    lastSearchQuery = query;
    lastCategoryMid = categoryMid;
    lastCategorySub = categorySub;
    lastSearchPage = page;
    lastLoadedCount = loadedCount;
    if (shouldThrowOnSearch) throw Exception(errorMessage);

    // 페이지별 결과를 지정했으면 그것을, 아니면 searchProductsResult 를 0페이지로 쓴다.
    final products = pagedSearchResults != null
        ? (page < pagedSearchResults!.length
            ? pagedSearchResults![page]
            : <ProductForOrder>[])
        : (page == 0 ? searchProductsResult : <ProductForOrder>[]);

    return ProductSearchResult(
      products: products,
      totalCount: searchTotalCountOverride ??
          (pagedSearchResults?.expand((e) => e).length ??
              searchProductsResult.length),
      page: page,
      loadedCount: loadedCount + products.length,
    );
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

  // --- 주문이력 조회 ---
  List<ProductOrderHistoryGroup> orderHistoryResult = [];
  bool shouldThrowOnOrderHistory = false;
  bool orderHistoryCalled = false;
  int orderHistoryCallCount = 0;
  int? lastOrderHistoryAccountId;

  @override
  Future<List<ProductOrderHistoryGroup>> getAccountOrderHistory({
    required int accountId,
    required DateTime startDate,
    required DateTime endDate,
  }) async {
    orderHistoryCalled = true;
    orderHistoryCallCount++;
    lastOrderHistoryAccountId = accountId;
    if (shouldThrowOnOrderHistory) throw Exception(errorMessage);
    return orderHistoryResult;
  }

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

  // --- Non-AddProduct methods - stub ---
  @override
  Future<OrderRequestListResult> getMyOrderRequests({
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
  Future<OrderDetail> getOrderRequestDetail({required int orderId}) async =>
      throw UnimplementedError();

  @override
  Future<void> resendOrderRequest({required int orderId}) async =>
      throw UnimplementedError();

  @override
  Future<OrderCancelResult> cancelOrderRequest({
    required int orderId,
    required List<int> orderProductIds,
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
