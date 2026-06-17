import 'package:flutter_test/flutter_test.dart';
import '../../helpers/fake_order_request_repository.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/usecases/add_to_favorites_usecase.dart';
import 'package:mobile/domain/usecases/get_favorite_products_usecase.dart';
import 'package:mobile/domain/usecases/remove_from_favorites_usecase.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';

// NOTE: 여신/임시저장/검증/제출/수정/바코드 usecase 는 신규 OrderFormRepository
// (#592/#594/#596) 경로로 대체되어 제거됨. 여기에는 add_product 화면이 사용하는
// 즐겨찾기/검색 usecase 만 남는다.

void main() {
  late FakeOrderRequestRepository repository;
  late GetFavoriteProducts getFavoriteProducts;
  late SearchProductsForOrder searchProductsForOrder;
  late AddToFavorites addToFavorites;
  late RemoveFromFavorites removeFromFavorites;

  setUp(() {
    repository = FakeOrderRequestRepository();
    getFavoriteProducts = GetFavoriteProducts(repository);
    searchProductsForOrder = SearchProductsForOrder(repository);
    addToFavorites = AddToFavorites(repository);
    removeFromFavorites = RemoveFromFavorites(repository);
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
