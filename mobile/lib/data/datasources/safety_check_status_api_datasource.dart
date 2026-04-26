import 'package:dio/dio.dart';
import '../models/safety_check_status_model.dart';

/// 안전점검 현황 API DataSource
class SafetyCheckStatusApiDataSource {
  final Dio _dio;

  SafetyCheckStatusApiDataSource(this._dio);

  Future<SafetyCheckStatusModel> getStatus({String? date}) async {
    final queryParams = <String, dynamic>{};
    if (date != null) {
      queryParams['date'] = date;
    }

    final response = await _dio.get(
      '/api/v1/safety-check/status',
      queryParameters: queryParams,
    );

    final data = response.data['data'] as Map<String, dynamic>;
    return SafetyCheckStatusModel.fromJson(data);
  }
}
