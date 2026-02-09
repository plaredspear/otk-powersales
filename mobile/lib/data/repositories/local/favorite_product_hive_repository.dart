import 'package:hive/hive.dart';
import '../../../domain/entities/favorite_product.dart';
import '../../../domain/repositories/favorite_product_repository.dart';

class FavoriteProductHiveRepository implements FavoriteProductRepository {
  static const String _boxName = 'favorite_products';
  Box<Map>? _box;

  /// Hive Box 초기화
  Future<void> init() async {
    if (_box == null || !_box!.isOpen) {
      _box = await Hive.openBox<Map>(_boxName);
    }
  }

  /// Box가 초기화되었는지 확인
  void _ensureInitialized() {
    if (_box == null || !_box!.isOpen) {
      throw StateError('FavoriteProductHiveRepository is not initialized. Call init() first.');
    }
  }

  @override
  Future<List<FavoriteProduct>> getAllFavorites() async {
    _ensureInitialized();

    final List<FavoriteProduct> favorites = [];
    for (var key in _box!.keys) {
      final map = _box!.get(key);
      if (map != null) {
        try {
          favorites.add(FavoriteProduct.fromJson(Map<String, dynamic>.from(map)));
        } catch (e) {
          // 잘못된 데이터는 무시
          continue;
        }
      }
    }

    // 추가된 시간 역순으로 정렬 (최신순)
    favorites.sort((a, b) => b.addedAt.compareTo(a.addedAt));

    return favorites;
  }

  @override
  Future<bool> isFavorite(String productId) async {
    _ensureInitialized();
    return _box!.containsKey(productId);
  }

  @override
  Future<void> addFavorite(FavoriteProduct product) async {
    _ensureInitialized();
    await _box!.put(product.id, product.toJson());
  }

  @override
  Future<void> removeFavorite(String productId) async {
    _ensureInitialized();
    await _box!.delete(productId);
  }

  @override
  Future<List<FavoriteProduct>> searchFavorites(String query) async {
    _ensureInitialized();

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
    _ensureInitialized();
    await _box!.clear();
  }

  /// Repository 종료 (테스트용)
  Future<void> dispose() async {
    if (_box != null && _box!.isOpen) {
      await _box!.close();
      _box = null;
    }
  }
}
