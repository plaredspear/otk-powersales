import 'package:dio/dio.dart';
import 'home_remote_datasource.dart';

/// 홈 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class HomeApiDataSource implements HomeRemoteDataSource {
  final Dio _dio;

  HomeApiDataSource(this._dio);

  @override
  Future<HomeResponseModel> getHomeData() async {
    final response = await _dio.get('/api/v1/home');

    return HomeResponseModel.fromJson(
      response.data as Map<String, dynamic>,
    );
  }
}
