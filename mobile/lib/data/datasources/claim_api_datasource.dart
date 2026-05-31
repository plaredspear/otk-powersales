import 'package:dio/dio.dart';
import '../models/claim_detail_model.dart';
import '../models/claim_form_data_model.dart';
import '../models/claim_list_item_model.dart';
import '../models/claim_register_request.dart';
import '../models/claim_register_result_model.dart';
import 'claim_remote_datasource.dart';

/// 클레임 API 데이터소스 구현체
class ClaimApiDataSource implements ClaimRemoteDataSource {
  final Dio _dio;

  ClaimApiDataSource(this._dio);

  @override
  Future<ClaimRegisterResultModel> registerClaim(
      ClaimRegisterRequest request) async {
    final formData = await request.toFormData();
    final response = await _dio.post(
      '/api/v1/mobile/claims',
      data: formData,
    );

    return ClaimRegisterResultModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<ClaimFormDataModel> getFormData() async {
    final response = await _dio.get('/api/v1/mobile/claims/form-data');

    return ClaimFormDataModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<List<ClaimListItemModel>> getClaims({
    String? startDate,
    String? endDate,
  }) async {
    final queryParameters = <String, dynamic>{};
    if (startDate != null) queryParameters['startDate'] = startDate;
    if (endDate != null) queryParameters['endDate'] = endDate;

    final response = await _dio.get(
      '/api/v1/mobile/claims',
      queryParameters: queryParameters,
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => ClaimListItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<ClaimDetailModel> getClaimDetail(int claimId) async {
    final response = await _dio.get('/api/v1/mobile/claims/$claimId');

    return ClaimDetailModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }
}
