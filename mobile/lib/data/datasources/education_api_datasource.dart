import 'package:dio/dio.dart';

import '../models/education_post_detail_model.dart';
import '../models/education_post_model.dart';
import 'education_remote_datasource.dart';

/// 교육 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API(EducationController)와 통신합니다.
class EducationApiDataSource implements EducationRemoteDataSource {
  final Dio _dio;

  EducationApiDataSource(this._dio);

  @override
  Future<EducationPostPageModel> getPosts({
    required String category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    final queryParameters = <String, dynamic>{
      'category': category,
      'page': page,
      'size': size,
    };
    if (search != null && search.isNotEmpty) {
      queryParameters['search'] = search;
    }

    final response = await _dio.get(
      '/api/v1/mobile/education/posts',
      queryParameters: queryParameters,
    );

    return EducationPostPageModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<EducationPostDetailModel> getPostDetail(String postId) async {
    final response = await _dio.get('/api/v1/mobile/education/posts/$postId');

    return EducationPostDetailModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
