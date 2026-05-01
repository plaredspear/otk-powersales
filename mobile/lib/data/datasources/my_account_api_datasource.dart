import 'package:dio/dio.dart';
import 'my_account_remote_datasource.dart';

/// 내 거래처 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트를 사용하여 실제 Backend API와 통신합니다.
class MyAccountApiDataSource implements MyAccountRemoteDataSource {
  final Dio _dio;

  MyAccountApiDataSource(this._dio);

  @override
  Future<MyAccountListResponse> getMyAccounts({String? keyword}) async {
    final queryParameters = <String, dynamic>{};
    if (keyword != null && keyword.isNotEmpty) {
      queryParameters['keyword'] = keyword;
    }

    final response = await _dio.get(
      '/api/v1/mobile/accounts/my',
      queryParameters: queryParameters.isNotEmpty ? queryParameters : null,
    );

    return MyAccountListResponse.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
