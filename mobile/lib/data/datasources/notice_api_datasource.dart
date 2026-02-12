import 'package:dio/dio.dart';
import '../models/notice_post_model.dart';
import '../models/notice_post_detail_model.dart';
import 'notice_remote_datasource.dart';

/// 공지사항 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
/// 현재는 코드 작성만 해두고, 실제 사용은 Backend 연동 시점에 시작합니다.
class NoticeApiDataSource implements NoticeRemoteDataSource {
  final Dio _dio;

  NoticeApiDataSource(this._dio);

  @override
  Future<NoticePostPageModel> getPosts({
    String? category,
    String? search,
    int page = 1,
    int size = 10,
  }) async {
    // 쿼리 파라미터 구성
    final queryParameters = <String, dynamic>{
      'page': page,
      'size': size,
    };

    if (category != null) {
      queryParameters['category'] = category;
    }

    if (search != null && search.isNotEmpty) {
      queryParameters['search'] = search;
    }

    final response = await _dio.get(
      '/api/v1/notices',
      queryParameters: queryParameters,
    );

    return NoticePostPageModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<NoticePostDetailModel> getPostDetail(int noticeId) async {
    final response = await _dio.get('/api/v1/notices/$noticeId');

    return NoticePostDetailModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
