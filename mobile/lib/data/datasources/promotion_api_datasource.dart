import 'package:dio/dio.dart';

import '../../domain/entities/promotion.dart';
import '../../domain/repositories/promotion_repository.dart';

/// 행사 API DataSource
class PromotionApiDataSource {
  final Dio _dio;

  PromotionApiDataSource(this._dio);

  /// 행사 목록 조회
  Future<PromotionListResult> getPromotions({
    String? startDate,
    String? endDate,
    String? keyword,
    int? accountId,
    int page = 0,
    int size = 20,
  }) async {
    final queryParameters = <String, dynamic>{
      'page': page,
      'size': size,
    };
    if (startDate != null) queryParameters['startDate'] = startDate;
    if (endDate != null) queryParameters['endDate'] = endDate;
    if (keyword != null && keyword.isNotEmpty) {
      queryParameters['keyword'] = keyword;
    }
    if (accountId != null) queryParameters['accountId'] = accountId;

    final response = await _dio.get(
      '/api/v1/mobile/promotions',
      queryParameters: queryParameters,
    );

    final data = response.data['data'] as Map<String, dynamic>;
    final contentJson = data['content'] as List<dynamic>;
    final items = contentJson
        .map((e) => PromotionItem.fromJson(e as Map<String, dynamic>))
        .toList();

    final totalElements = data['totalElements'] as int;
    final totalPages = data['totalPages'] as int;
    final currentPage = data['page'] as int;

    return PromotionListResult(
      items: items,
      totalElements: totalElements,
      totalPages: totalPages,
      isLast: currentPage >= totalPages - 1,
    );
  }

  /// 행사 상세 조회
  Future<PromotionDetail> getPromotion(int id) async {
    final response = await _dio.get('/api/v1/mobile/promotions/$id');
    final data = response.data['data'] as Map<String, dynamic>;
    return PromotionDetail.fromJson(data);
  }

  /// 담당 행사 일람 조회 (date 미지정 시 서버 오늘 기준).
  Future<List<MyPromotionAssignment>> getMyAssignments({String? date}) async {
    final queryParameters = <String, dynamic>{};
    if (date != null) queryParameters['date'] = date;

    final response = await _dio.get(
      '/api/v1/mobile/promotions/my-assignments',
      queryParameters: queryParameters.isEmpty ? null : queryParameters,
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => MyPromotionAssignment.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
