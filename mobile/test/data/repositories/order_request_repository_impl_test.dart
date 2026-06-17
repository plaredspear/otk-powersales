import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/order_request_remote_datasource.dart';
import 'package:mobile/data/models/client_order_model.dart';
import 'package:mobile/data/models/order_cancel_model.dart';
import 'package:mobile/data/models/order_request_detail_model.dart';
import 'package:mobile/data/models/product_for_order_model.dart';
import 'package:mobile/data/repositories/order_request_repository_impl.dart';
import 'package:mobile/domain/entities/product_for_order.dart';

// NOTE: 여신/임시저장/검증/제출/수정/바코드는 신규 OrderFormRepository
// (#592/#594/#596) 경로로 대체되어 제거됨. 여기에는 add_product 화면이 사용하는
// 즐겨찾기/검색 경로만 남는다.

void main() {
  late OrderRequestRepositoryImpl repository;
  late FakeOrderRemoteDataSource fakeRemoteDataSource;

  setUp(() {
    fakeRemoteDataSource = FakeOrderRemoteDataSource();
    repository = OrderRequestRepositoryImpl(
      remoteDataSource: fakeRemoteDataSource,
    );
  });

  group('OrderRequestRepositoryImpl - 즐겨찾기/검색', () {
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
/// OrderRequestRemoteDataSource 인터페이스를 구현한 테스트용 가짜 객체입니다.
/// mockito를 사용하지 않고 수동으로 작성한 fake입니다.
class FakeOrderRemoteDataSource implements OrderRequestRemoteDataSource {
  // getFavoriteProducts
  int getFavoriteProductsCalls = 0;
  List<ProductForOrderModel> favoriteProductsToReturn = [];

  // searchProductsForOrder
  int searchProductsForOrderCalls = 0;
  String? lastSearchQuery;
  String? lastSearchCategoryMid;
  String? lastSearchCategorySub;
  List<ProductForOrderModel> searchResultsToReturn = [];

  // addToFavorites
  int addToFavoritesCalls = 0;
  String? lastAddToFavoritesProductCode;

  // removeFromFavorites
  int removeFromFavoritesCalls = 0;
  String? lastRemoveFromFavoritesProductCode;

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
  Future<void> addToFavorites({required String productCode}) async {
    addToFavoritesCalls++;
    lastAddToFavoritesProductCode = productCode;
  }

  @override
  Future<void> removeFromFavorites({required String productCode}) async {
    removeFromFavoritesCalls++;
    lastRemoveFromFavoritesProductCode = productCode;
  }

  // ─── 기존 메서드들 (즐겨찾기/검색과 무관, UnimplementedError 던짐) ─────────

  @override
  Future<OrderRequestListResponseModel> getMyOrderRequests({
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
  Future<OrderRequestDetailModel> getOrderRequestDetail({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<void> resendOrderRequest({required int orderId}) {
    throw UnimplementedError();
  }

  @override
  Future<OrderCancelResponseModel> cancelOrderRequest({
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
