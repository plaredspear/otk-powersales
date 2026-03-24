import '../../domain/entities/product_expiration_form.dart';
import '../../domain/entities/product_expiration_item.dart';
import '../../domain/repositories/product_expiration_repository.dart';
import '../datasources/product_expiration_remote_datasource.dart';
import '../models/product_expiration_register_request.dart';
import '../models/product_expiration_update_request.dart';

/// 유통기한 Repository 구현체
///
/// ProductExpirationRemoteDataSource를 사용하여 API를 호출하고,
/// 응답 데이터를 도메인 엔티티로 변환합니다.
class ProductExpirationRepositoryImpl implements ProductExpirationRepository {
  final ProductExpirationRemoteDataSource _remoteDataSource;

  ProductExpirationRepositoryImpl({
    required ProductExpirationRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<List<ProductExpirationItem>> getProductExpirationList(ProductExpirationFilter filter) async {
    final response = await _remoteDataSource.getProductExpirationList(
      accountCode: filter.accountCode,
      fromDate: filter.fromDate.toIso8601String().substring(0, 10),
      toDate: filter.toDate.toIso8601String().substring(0, 10),
    );

    return response.map((model) => model.toEntity()).toList();
  }

  @override
  Future<ProductExpirationItem> registerProductExpiration(ProductExpirationRegisterForm form) async {
    final request = ProductExpirationRegisterRequest.fromForm(form);
    final response = await _remoteDataSource.registerProductExpiration(request);
    return response.toEntity();
  }

  @override
  Future<ProductExpirationItem> updateProductExpiration(
    int seq,
    ProductExpirationUpdateForm form,
  ) async {
    final request = ProductExpirationUpdateRequest.fromForm(form);
    final response = await _remoteDataSource.updateProductExpiration(seq, request);
    return response.toEntity();
  }

  @override
  Future<void> deleteProductExpiration(int seq) async {
    await _remoteDataSource.deleteProductExpiration(seq);
  }

  @override
  Future<int> deleteProductExpirationBatch(List<int> seqs) async {
    final response = await _remoteDataSource.deleteProductExpirationBatch(seqs);
    return response.deletedCount;
  }
}
