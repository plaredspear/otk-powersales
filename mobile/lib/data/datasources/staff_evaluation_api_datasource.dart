import 'package:dio/dio.dart';

import '../models/staff_evaluation_model.dart';
import 'staff_evaluation_remote_datasource.dart';

/// 여사원 평가조회 API 데이터소스 구현체
///
/// Dio HTTP 클라이언트로 Backend `StaffEvaluationController` 와 통신한다.
class StaffEvaluationApiDataSource implements StaffEvaluationRemoteDataSource {
  final Dio _dio;

  StaffEvaluationApiDataSource(this._dio);

  @override
  Future<StaffEvaluationModel> getStaffEvaluation({String? yearMonth}) async {
    final queryParameters = <String, dynamic>{};
    if (yearMonth != null && yearMonth.isNotEmpty) {
      queryParameters['yearMonth'] = yearMonth;
    }

    final response = await _dio.get(
      '/api/v1/mobile/staff-evaluation',
      queryParameters: queryParameters,
    );

    return StaffEvaluationModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
