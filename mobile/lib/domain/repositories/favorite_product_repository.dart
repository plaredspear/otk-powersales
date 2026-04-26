import '../entities/favorite_product.dart';

abstract class FavoriteProductRepository {
  /// 모든 즐겨찾기 제품 목록 조회
  Future<List<FavoriteProduct>> getAllFavorites();

  /// 특정 제품이 즐겨찾기에 있는지 확인
  Future<bool> isFavorite(String productId);

  /// 즐겨찾기에 제품 추가
  Future<void> addFavorite(FavoriteProduct product);

  /// 즐겨찾기에서 제품 제거
  Future<void> removeFavorite(String productId);

  /// 제품명으로 즐겨찾기 검색
  Future<List<FavoriteProduct>> searchFavorites(String query);

  /// 모든 즐겨찾기 삭제
  Future<void> clearAllFavorites();
}
