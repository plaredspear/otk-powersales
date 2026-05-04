import 'package:dio/dio.dart';

import '../models/safety_check_category_model.dart';
import '../models/safety_check_today_status_model.dart';
import '../models/safety_check_submit_result_model.dart';
import 'safety_check_remote_datasource.dart';

/// 안전점검 API DataSource 구현체 (Dio)
class SafetyCheckApiDataSource implements SafetyCheckRemoteDataSource {
  final Dio _dio;

  SafetyCheckApiDataSource(this._dio);

  @override
  Future<List<SafetyCheckCategoryModel>> getItems() async {
    final response = await _dio.get('/api/v1/mobile/safety-check/items');

    final data = response.data['data'] as Map<String, dynamic>;
    final categoriesJson = data['categories'] as List<dynamic>;
    return categoriesJson
        .map((json) => SafetyCheckCategoryModel.fromJson(
            json as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<SafetyCheckTodayStatusModel> getTodayStatus() async {
    final response = await _dio.get('/api/v1/mobile/safety-check/today');

    final data = response.data['data'] as Map<String, dynamic>;
    return SafetyCheckTodayStatusModel.fromJson(data);
  }

  @override
  Future<SafetyCheckSubmitResultModel> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<Map<String, dynamic>> equipments,
    List<String>? precautions,
  }) async {
    final response = await _dio.post(
      '/api/v1/mobile/safety-check/submit',
      data: {
        'startTime': startTime.toIso8601String(),
        'completeTime': completeTime.toIso8601String(),
        'equipments': equipments,
        'precautions': precautions ?? [],
      },
    );

    final data = response.data['data'] as Map<String, dynamic>;
    return SafetyCheckSubmitResultModel.fromJson(data);
  }
}
