import '../../domain/entities/pos_product.dart';
import '../../domain/repositories/pos_product_repository.dart';
import '../datasources/pos_product_api_datasource.dart';

/// POS 제품 검색 Repository 실 API 구현체.
class PosProductRepositoryImpl implements PosProductRepository {
  final PosProductApiDataSource _remoteDataSource;

  PosProductRepositoryImpl({
    required PosProductApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<PosProduct>> searchByText(String query) {
    return _remoteDataSource.searchByText(query);
  }

  @override
  Future<PosProduct?> findByBarcode(String barcode) {
    return _remoteDataSource.findByBarcode(barcode);
  }
}
