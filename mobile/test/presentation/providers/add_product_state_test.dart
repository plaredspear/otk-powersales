import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/presentation/providers/add_product_state.dart';

void main() {
  group('AddProductState', () {
    group('initial()', () {
      test('기본값 설정', () {
        // Act
        final state = AddProductState.initial();

        // Assert
        expect(state.currentTab, AddProductTab.favorites);
        expect(state.favoriteProducts, isEmpty);
        expect(state.searchResults, isEmpty);
        expect(state.orderHistoryGroups, isEmpty);
        expect(state.selectedProductCodes, isEmpty);
        expect(state.searchQuery, '');
        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
        expect(state.successMessage, isNull);
      });

      test('historyDateFrom은 3일 전', () {
        // Act
        final state = AddProductState.initial();
        final now = DateTime.now();

        // Assert
        expect(state.historyDateFrom, isNotNull);
        expect(state.historyDateFrom!.isBefore(now), true);
        expect(
          state.historyDateFrom!.isAfter(
            now.subtract(const Duration(days: 4)),
          ),
          true,
        );
      });

      test('historyDateTo는 현재 시간', () {
        // Act
        final state = AddProductState.initial();
        final now = DateTime.now();

        // Assert
        expect(state.historyDateTo, isNotNull);
        expect(
          state.historyDateTo!.difference(now).inSeconds.abs() < 2,
          true,
        );
      });
    });

    group('toLoading()', () {
      test('isLoading true로 설정', () {
        // Arrange
        final state = AddProductState.initial();

        // Act
        final newState = state.toLoading();

        // Assert
        expect(newState.isLoading, true);
      });

      test('에러 메시지 클리어', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          errorMessage: 'Test error',
        );

        // Act
        final newState = state.toLoading();

        // Assert
        expect(newState.errorMessage, isNull);
      });
    });

    group('toError()', () {
      test('isLoading false로 설정', () {
        // Arrange
        final state = AddProductState.initial().copyWith(isLoading: true);

        // Act
        final newState = state.toError('Test error');

        // Assert
        expect(newState.isLoading, false);
      });

      test('에러 메시지 설정', () {
        // Arrange
        final state = AddProductState.initial();

        // Act
        final newState = state.toError('Test error');

        // Assert
        expect(newState.errorMessage, 'Test error');
      });
    });

    group('copyWith()', () {
      test('인자 없으면 동일한 상태 반환', () {
        // Arrange
        final state = AddProductState.initial();

        // Act
        final newState = state.copyWith();

        // Assert
        expect(newState.currentTab, state.currentTab);
        expect(newState.favoriteProducts, state.favoriteProducts);
        expect(newState.searchResults, state.searchResults);
        expect(newState.orderHistoryGroups, state.orderHistoryGroups);
        expect(newState.selectedProductCodes, state.selectedProductCodes);
        expect(newState.searchQuery, state.searchQuery);
        expect(newState.historyDateFrom, state.historyDateFrom);
        expect(newState.historyDateTo, state.historyDateTo);
        expect(newState.isLoading, state.isLoading);
        expect(newState.errorMessage, state.errorMessage);
        expect(newState.successMessage, state.successMessage);
      });

      test('특정 필드만 변경', () {
        // Arrange
        final state = AddProductState.initial();

        // Act
        final newState = state.copyWith(
          currentTab: AddProductTab.search,
          searchQuery: 'test',
        );

        // Assert
        expect(newState.currentTab, AddProductTab.search);
        expect(newState.searchQuery, 'test');
        expect(newState.favoriteProducts, state.favoriteProducts);
        expect(newState.isLoading, state.isLoading);
      });

      test('clearError 플래그로 에러 제거', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          errorMessage: 'Test error',
        );

        // Act
        final newState = state.copyWith(clearError: true);

        // Assert
        expect(newState.errorMessage, isNull);
      });

      test('clearSuccess 플래그로 성공 메시지 제거', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          successMessage: 'Test success',
        );

        // Act
        final newState = state.copyWith(clearSuccess: true);

        // Assert
        expect(newState.successMessage, isNull);
      });

      test('clearError false면 에러 유지', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          errorMessage: 'Test error',
        );

        // Act
        final newState = state.copyWith(
          isLoading: true,
          clearError: false,
        );

        // Assert
        expect(newState.errorMessage, 'Test error');
      });
    });

    group('Computed getters', () {
      test('selectedCount - 선택 없음', () {
        // Arrange
        final state = AddProductState.initial();

        // Assert
        expect(state.selectedCount, 0);
      });

      test('selectedCount - 여러 선택', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          selectedProductCodes: {'P001', 'P002', 'P003'},
        );

        // Assert
        expect(state.selectedCount, 3);
      });

      test('hasSelection - false', () {
        // Arrange
        final state = AddProductState.initial();

        // Assert
        expect(state.hasSelection, false);
      });

      test('hasSelection - true', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          selectedProductCodes: {'P001'},
        );

        // Assert
        expect(state.hasSelection, true);
      });

      test('currentTabProducts - favorites 탭', () {
        // Arrange
        final products = [_createTestProduct(productCode: 'P001')];
        final state = AddProductState.initial().copyWith(
          currentTab: AddProductTab.favorites,
          favoriteProducts: products,
        );

        // Assert
        expect(state.currentTabProducts, products);
      });

      test('currentTabProducts - search 탭', () {
        // Arrange
        final products = [_createTestProduct(productCode: 'P002')];
        final state = AddProductState.initial().copyWith(
          currentTab: AddProductTab.search,
          searchResults: products,
        );

        // Assert
        expect(state.currentTabProducts, products);
      });

      test('currentTabProducts - orderHistory 탭', () {
        // Arrange
        final product1 = _createTestProduct(productCode: 'P001');
        final product2 = _createTestProduct(productCode: 'P002');
        final groups = [
          OrderHistoryGroup(
            orderId: 1,
            orderDate: '2026-02-01',
            clientName: '거래처A',
            products: [product1],
          ),
          OrderHistoryGroup(
            orderId: 2,
            orderDate: '2026-02-02',
            clientName: '거래처B',
            products: [product2],
          ),
        ];
        final state = AddProductState.initial().copyWith(
          currentTab: AddProductTab.orderHistory,
          orderHistoryGroups: groups,
        );

        // Assert
        expect(state.currentTabProducts.length, 2);
        expect(state.currentTabProducts[0], product1);
        expect(state.currentTabProducts[1], product2);
      });

      test('isProductSelected - true', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          selectedProductCodes: {'P001', 'P002'},
        );

        // Assert
        expect(state.isProductSelected('P001'), true);
      });

      test('isProductSelected - false', () {
        // Arrange
        final state = AddProductState.initial().copyWith(
          selectedProductCodes: {'P001', 'P002'},
        );

        // Assert
        expect(state.isProductSelected('P003'), false);
      });
    });
  });

  group('AddProductTab', () {
    test('라벨 값 확인', () {
      expect(AddProductTab.favorites.label, '즐겨찾기');
      expect(AddProductTab.search.label, '제품 검색');
      expect(AddProductTab.orderHistory.label, '주문 이력');
    });
  });

  group('OrderHistoryGroup', () {
    test('copyWith - 값 변경', () {
      // Arrange
      final group = OrderHistoryGroup(
        orderId: 1,
        orderDate: '2026-02-01',
        clientName: '거래처A',
        products: [],
        isExpanded: false,
      );

      // Act
      final newGroup = group.copyWith(isExpanded: true);

      // Assert
      expect(newGroup.orderId, 1);
      expect(newGroup.orderDate, '2026-02-01');
      expect(newGroup.clientName, '거래처A');
      expect(newGroup.products, isEmpty);
      expect(newGroup.isExpanded, true);
    });

    test('copyWith - 값 유지', () {
      // Arrange
      final products = [_createTestProduct(productCode: 'P001')];
      final group = OrderHistoryGroup(
        orderId: 1,
        orderDate: '2026-02-01',
        clientName: '거래처A',
        products: products,
        isExpanded: true,
      );

      // Act
      final newGroup = group.copyWith();

      // Assert
      expect(newGroup.orderId, 1);
      expect(newGroup.orderDate, '2026-02-01');
      expect(newGroup.clientName, '거래처A');
      expect(newGroup.products, products);
      expect(newGroup.isExpanded, true);
    });
  });
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
