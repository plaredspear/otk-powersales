import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:mobile/data/repositories/local/favorite_product_hive_repository.dart';
import 'package:mobile/domain/entities/favorite_product.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('FavoriteProductHiveRepository', () {
    late FavoriteProductHiveRepository repository;

    setUp(() async {
      // 테스트용 Hive 초기화 (메모리 기반)
      Hive.init('./test/temp');
      repository = FavoriteProductHiveRepository();
      await repository.init();

      // 테스트 시작 전 초기화
      await repository.clearAllFavorites();
    });

    tearDown(() async {
      await repository.clearAllFavorites();
      await repository.dispose();
    });

    test('초기 상태에서 즐겨찾기 목록이 비어있다', () async {
      final favorites = await repository.getAllFavorites();
      expect(favorites, isEmpty);
    });

    test('즐겨찾기를 추가할 수 있다', () async {
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime.now(),
      );

      await repository.addFavorite(favorite);

      final favorites = await repository.getAllFavorites();
      expect(favorites.length, 1);
      expect(favorites.first.id, 'prod_001');
      expect(favorites.first.productName, '진라면');
    });

    test('즐겨찾기 여부를 확인할 수 있다', () async {
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime.now(),
      );

      expect(await repository.isFavorite('prod_001'), false);

      await repository.addFavorite(favorite);

      expect(await repository.isFavorite('prod_001'), true);
      expect(await repository.isFavorite('prod_999'), false);
    });

    test('즐겨찾기를 제거할 수 있다', () async {
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime.now(),
      );

      await repository.addFavorite(favorite);
      expect(await repository.isFavorite('prod_001'), true);

      await repository.removeFavorite('prod_001');
      expect(await repository.isFavorite('prod_001'), false);

      final favorites = await repository.getAllFavorites();
      expect(favorites, isEmpty);
    });

    test('여러 개의 즐겨찾기를 추가할 수 있다', () async {
      final favorites = [
        FavoriteProduct(
          id: 'prod_001',
          productName: '진라면',
          addedAt: DateTime(2026, 2, 1, 10, 0),
        ),
        FavoriteProduct(
          id: 'prod_002',
          productName: '진라면 매운맛',
          addedAt: DateTime(2026, 2, 1, 11, 0),
        ),
        FavoriteProduct(
          id: 'prod_003',
          productName: '케첩',
          addedAt: DateTime(2026, 2, 1, 12, 0),
        ),
      ];

      for (var favorite in favorites) {
        await repository.addFavorite(favorite);
      }

      final result = await repository.getAllFavorites();
      expect(result.length, 3);
    });

    test('즐겨찾기 목록이 최신순으로 정렬된다', () async {
      final favorites = [
        FavoriteProduct(
          id: 'prod_001',
          productName: '진라면',
          addedAt: DateTime(2026, 2, 1, 10, 0),
        ),
        FavoriteProduct(
          id: 'prod_002',
          productName: '진라면 매운맛',
          addedAt: DateTime(2026, 2, 1, 12, 0),
        ),
        FavoriteProduct(
          id: 'prod_003',
          productName: '케첩',
          addedAt: DateTime(2026, 2, 1, 11, 0),
        ),
      ];

      for (var favorite in favorites) {
        await repository.addFavorite(favorite);
      }

      final result = await repository.getAllFavorites();

      expect(result[0].id, 'prod_002'); // 가장 최근
      expect(result[1].id, 'prod_003');
      expect(result[2].id, 'prod_001'); // 가장 오래됨
    });

    test('제품명으로 즐겨찾기를 검색할 수 있다', () async {
      final favorites = [
        FavoriteProduct(
          id: 'prod_001',
          productName: '진라면',
          addedAt: DateTime.now(),
        ),
        FavoriteProduct(
          id: 'prod_002',
          productName: '진라면 매운맛',
          addedAt: DateTime.now(),
        ),
        FavoriteProduct(
          id: 'prod_003',
          productName: '케첩',
          addedAt: DateTime.now(),
        ),
      ];

      for (var favorite in favorites) {
        await repository.addFavorite(favorite);
      }

      final result = await repository.searchFavorites('진라면');

      expect(result.length, 2);
      expect(result.every((f) => f.productName.contains('진라면')), true);
    });

    test('검색어가 비어있으면 모든 즐겨찾기를 반환한다', () async {
      final favorites = [
        FavoriteProduct(
          id: 'prod_001',
          productName: '진라면',
          addedAt: DateTime.now(),
        ),
        FavoriteProduct(
          id: 'prod_002',
          productName: '케첩',
          addedAt: DateTime.now(),
        ),
      ];

      for (var favorite in favorites) {
        await repository.addFavorite(favorite);
      }

      final result = await repository.searchFavorites('');

      expect(result.length, 2);
    });

    test('검색어가 대소문자를 구분하지 않는다', () async {
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime.now(),
      );

      await repository.addFavorite(favorite);

      final result1 = await repository.searchFavorites('진라면');
      final result2 = await repository.searchFavorites('진라면');

      expect(result1.length, 1);
      expect(result2.length, 1);
    });

    test('모든 즐겨찾기를 삭제할 수 있다', () async {
      final favorites = [
        FavoriteProduct(
          id: 'prod_001',
          productName: '진라면',
          addedAt: DateTime.now(),
        ),
        FavoriteProduct(
          id: 'prod_002',
          productName: '케첩',
          addedAt: DateTime.now(),
        ),
      ];

      for (var favorite in favorites) {
        await repository.addFavorite(favorite);
      }

      expect((await repository.getAllFavorites()).length, 2);

      await repository.clearAllFavorites();

      expect((await repository.getAllFavorites()), isEmpty);
    });

    test('동일한 ID로 다시 추가하면 덮어쓴다', () async {
      final favorite1 = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime(2026, 2, 1, 10, 0),
      );

      final favorite2 = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면 매운맛',
        addedAt: DateTime(2026, 2, 1, 11, 0),
      );

      await repository.addFavorite(favorite1);
      await repository.addFavorite(favorite2);

      final result = await repository.getAllFavorites();

      expect(result.length, 1);
      expect(result.first.productName, '진라면 매운맛');
    });
  });
}
