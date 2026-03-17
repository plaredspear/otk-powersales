import '../../domain/entities/promotion.dart';
import '../../domain/repositories/promotion_repository.dart';
import '../datasources/promotion_api_datasource.dart';

/// 행사 Repository 구현체
class PromotionRepositoryImpl implements PromotionRepository {
  final PromotionApiDataSource _remoteDataSource;

  PromotionRepositoryImpl({
    required PromotionApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<PromotionListResult> getPromotions({
    String? startDate,
    String? endDate,
    String? keyword,
    int page = 0,
    int size = 20,
  }) async {
    return await _remoteDataSource.getPromotions(
      startDate: startDate,
      endDate: endDate,
      keyword: keyword,
      page: page,
      size: size,
    );
  }

  @override
  Future<PromotionDetail> getPromotion(int id) async {
    return await _remoteDataSource.getPromotion(id);
  }
}
