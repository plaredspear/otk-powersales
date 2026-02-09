import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/domain/entities/favorite_product.dart';
import 'package:mobile/domain/repositories/favorite_product_repository.dart';
import 'package:mobile/presentation/providers/favorite_product_provider.dart';

/// Mock Repository (메모리 기반)
class MockFavoriteProductRepository implements FavoriteProductRepository {
  final Map<String, FavoriteProduct> _storage = {};

  @override
  Future<List<FavoriteProduct>> getAllFavorites() async {
    final list = _storage.values.toList();
    list.sort((a, b) => b.addedAt.compareTo(a.addedAt));
    return list;
  }

  @override
  Future<bool> isFavorite(String productId) async {
    return _storage.containsKey(productId);
  }

  @override
  Future<void> addFavorite(FavoriteProduct product) async {
    _storage[product.id] = product;
  }

  @override
  Future<void> removeFavorite(String productId) async {
    _storage.remove(productId);
  }

  @override
  Future<List<FavoriteProduct>> searchFavorites(String query) async {
    if (query.trim().isEmpty) {
      return getAllFavorites();
    }

    final allFavorites = await getAllFavorites();
    final lowerQuery = query.toLowerCase();

    return allFavorites
        .where((product) => product.productName.toLowerCase().contains(lowerQuery))
        .toList();
  }

  @override
  Future<void> clearAllFavorites() async {
    _storage.clear();
  }
}

void main() {
  group('FavoriteProductProvider', () {
    late MockFavoriteProductRepository mockRepository;
    late ProviderContainer container;

    setUp(() {
      mockRepository = MockFavoriteProductRepository();
      container = ProviderContainer(
        overrides: [
          favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
        ],
      );
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 로딩 상태이다', () {
      final state = container.read(favoriteProductsProvider);
      expect(state, isA<AsyncLoading>());
    });

    test('즐겨찾기 목록을 로드할 수 있다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);
      await notifier.loadFavorites();

      final state = container.read(favoriteProductsProvider);
      expect(state, isA<AsyncData<List<FavoriteProduct>>>());
      expect(state.value, isEmpty);
    });

    test('즐겨찾기를 추가할 수 있다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');

      final state = container.read(favoriteProductsProvider);
      expect(state.value?.length, 1);
      expect(state.value?.first.productName, '진라면');
    });

    test('즐겨찾기를 제거할 수 있다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');
      expect(container.read(favoriteProductsProvider).value?.length, 1);

      await notifier.removeFavorite('prod_001');
      expect(container.read(favoriteProductsProvider).value, isEmpty);
    });

    test('즐겨찾기를 토글할 수 있다 (추가)', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.toggleFavorite('prod_001', '진라면');

      final state = container.read(favoriteProductsProvider);
      expect(state.value?.length, 1);
      expect(state.value?.first.productName, '진라면');
    });

    test('즐겨찾기를 토글할 수 있다 (제거)', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');
      expect(container.read(favoriteProductsProvider).value?.length, 1);

      await notifier.toggleFavorite('prod_001', '진라면');
      expect(container.read(favoriteProductsProvider).value, isEmpty);
    });

    test('즐겨찾기를 검색할 수 있다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');
      await notifier.addFavorite('prod_002', '진라면 매운맛');
      await notifier.addFavorite('prod_003', '케첩');

      await notifier.searchFavorites('진라면');

      final state = container.read(favoriteProductsProvider);
      expect(state.value?.length, 2);
      expect(state.value?.every((f) => f.productName.contains('진라면')), true);
    });

    test('모든 즐겨찾기를 삭제할 수 있다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');
      await notifier.addFavorite('prod_002', '케첩');
      expect(container.read(favoriteProductsProvider).value?.length, 2);

      await notifier.clearAll();
      expect(container.read(favoriteProductsProvider).value, isEmpty);
    });

    test('isFavorite 메서드가 올바르게 동작한다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');

      expect(await notifier.isFavorite('prod_001'), true);
      expect(await notifier.isFavorite('prod_999'), false);
    });

    test('여러 즐겨찾기를 추가하면 최신순으로 정렬된다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      // 순차적으로 추가 (시간차를 두기 위해 약간의 지연)
      await notifier.addFavorite('prod_001', '진라면');
      await Future.delayed(const Duration(milliseconds: 10));
      await notifier.addFavorite('prod_002', '케첩');
      await Future.delayed(const Duration(milliseconds: 10));
      await notifier.addFavorite('prod_003', '카레');

      final state = container.read(favoriteProductsProvider);
      final favorites = state.value!;

      // 최신순 확인 (가장 마지막에 추가된 것이 첫 번째)
      expect(favorites[0].id, 'prod_003');
      expect(favorites[1].id, 'prod_002');
      expect(favorites[2].id, 'prod_001');
    });

    test('isFavoriteProvider가 올바르게 동작한다', () async {
      final notifier = container.read(favoriteProductsProvider.notifier);

      await notifier.addFavorite('prod_001', '진라면');

      final isFavorite1 = await container.read(isFavoriteProvider('prod_001').future);
      final isFavorite2 = await container.read(isFavoriteProvider('prod_999').future);

      expect(isFavorite1, true);
      expect(isFavorite2, false);
    });
  });
}
