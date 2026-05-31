import 'dart:io';

import '../../domain/entities/daily_sales_form.dart';
import '../../domain/repositories/promotion_daily_sales_repository.dart';
import '../datasources/daily_sales_api_datasource.dart';

/// 일매출 마감/임시저장 Repository 구현체.
class PromotionDailySalesRepositoryImpl implements PromotionDailySalesRepository {
  final DailySalesApiDataSource _remoteDataSource;

  PromotionDailySalesRepositoryImpl({
    required DailySalesApiDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<DailySalesForm> getForm(int promotionEmployeeId) {
    return _remoteDataSource.getForm(promotionEmployeeId);
  }

  @override
  Future<DailySalesCloseResult> close(
    int promotionEmployeeId,
    DailySalesInput input,
    File? photo,
  ) {
    return _remoteDataSource.close(promotionEmployeeId, input, photo);
  }

  @override
  Future<DailySalesForm> saveDraft(
    int promotionEmployeeId,
    DailySalesInput input,
    File? photo,
  ) {
    return _remoteDataSource.saveDraft(promotionEmployeeId, input, photo);
  }

  @override
  Future<void> deleteDraft(int promotionEmployeeId) {
    return _remoteDataSource.deleteDraft(promotionEmployeeId);
  }
}
