import 'package:dio/dio.dart';

import '../models/suggestion_register_request.dart';
import '../models/suggestion_register_result_model.dart';
import 'suggestion_remote_datasource.dart';

/// 제안하기 API DataSource 구현체
///
/// `POST /api/v1/mobile/suggestions` 에 multipart/form-data 로 등록한다.
/// backend `SuggestionController.create` 는 `@RequestPart("request")` JSON +
/// `@RequestPart photos` files 구조를 받으므로 [SuggestionRegisterRequest.toFormData]
/// 의 결과를 그대로 전송한다.
class SuggestionApiDataSource implements SuggestionRemoteDataSource {
  final Dio _dio;

  SuggestionApiDataSource(this._dio);

  @override
  Future<SuggestionRegisterResultModel> registerSuggestion(
      SuggestionRegisterRequest request) async {
    final formData = await request.toFormData();
    final response = await _dio.post(
      '/api/v1/mobile/suggestions',
      data: formData,
      options: Options(contentType: 'multipart/form-data'),
    );

    return SuggestionRegisterResultModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
