import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../domain/entities/favorite_product.dart';
import '../../domain/repositories/favorite_product_repository.dart';
import '../../data/repositories/local/favorite_product_hive_repository.dart';

// Repository Provider
final favoriteProductRepositoryProvider = Provider<FavoriteProductRepository>((ref) {
  return FavoriteProductHiveRepository();
});

// 즐겨찾기 목록 상태 Provider
final favoriteProductsProvider = StateNotifierProvider<FavoriteProductNotifier, AsyncValue<List<FavoriteProduct>>>((ref) {
  final repository = ref.watch(favoriteProductRepositoryProvider);
  return FavoriteProductNotifier(repository);
});

// 특정 제품의 즐겨찾기 여부 확인 Provider
final isFavoriteProvider = FutureProvider.family<bool, String>((ref, productId) async {
  final repository = ref.watch(favoriteProductRepositoryProvider);
  return repository.isFavorite(productId);
});

class FavoriteProductNotifier extends StateNotifier<AsyncValue<List<FavoriteProduct>>> {
  final FavoriteProductRepository _repository;

  FavoriteProductNotifier(this._repository) : super(const AsyncValue.loading()) {
    loadFavorites();
  }

  /// 즐겨찾기 목록 로드
  Future<void> loadFavorites() async {
    state = const AsyncValue.loading();
    try {
      // Hive repository 초기화
      if (_repository is FavoriteProductHiveRepository) {
        await _repository.init();
      }

      final favorites = await _repository.getAllFavorites();
      state = AsyncValue.data(favorites);
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  /// 즐겨찾기 추가
  Future<void> addFavorite(String productId, String productName) async {
    try {
      final product = FavoriteProduct(
        id: productId,
        productName: productName,
        addedAt: DateTime.now(),
      );

      await _repository.addFavorite(product);
      await loadFavorites(); // 목록 새로고침
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  /// 즐겨찾기 제거
  Future<void> removeFavorite(String productId) async {
    try {
      await _repository.removeFavorite(productId);
      await loadFavorites(); // 목록 새로고침
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  /// 즐겨찾기 토글 (추가/제거)
  Future<void> toggleFavorite(String productId, String productName) async {
    try {
      final isFavorite = await _repository.isFavorite(productId);

      if (isFavorite) {
        await removeFavorite(productId);
      } else {
        await addFavorite(productId, productName);
      }
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  /// 검색
  Future<void> searchFavorites(String query) async {
    state = const AsyncValue.loading();
    try {
      final favorites = await _repository.searchFavorites(query);
      state = AsyncValue.data(favorites);
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  /// 모든 즐겨찾기 삭제
  Future<void> clearAll() async {
    try {
      await _repository.clearAllFavorites();
      await loadFavorites(); // 목록 새로고침
    } catch (e, stack) {
      state = AsyncValue.error(e, stack);
    }
  }

  /// 즐겨찾기 여부 확인 (동기)
  Future<bool> isFavorite(String productId) async {
    return _repository.isFavorite(productId);
  }
}
