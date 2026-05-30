import '../../domain/repositories/product_repository.dart';
import '../datasources/product_remote_datasource.dart';

/// Product Repository 구현체
///
/// Remote DataSource에서 데이터를 가져와 Domain Entity로 변환한다.
class ProductRepositoryImpl implements ProductRepository {
  final ProductRemoteDataSource _remoteDataSource;

  ProductRepositoryImpl({
    required ProductRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<ProductSearchResult> searchProducts({
    required String query,
    String type = 'text',
    int page = 0,
    int size = 20,
  }) async {
    // 검색어 최소 길이 검증은 Notifier 의 canSearch 게이트(query.length >= 2)에서
    // 단일 관리한다 (SoT 중복 방지).
    final pageModel = await _remoteDataSource.searchProducts(
      query: query,
      type: type,
      page: page,
      size: size,
    );

    return pageModel.toEntity();
  }
}
