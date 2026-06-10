import 'dart:io';

import 'package:dio/dio.dart';
import '../models/claim_detail_model.dart';
import '../models/claim_draft_model.dart';
import '../models/claim_draft_request.dart';
import '../models/claim_form_data_model.dart';
import '../models/claim_form_model.dart';
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
  Future<ClaimFormModel> getForm() async {
    final response = await _dio.get('/api/v1/mobile/claims/form');
    final data = response.data['data'] as Map<String, dynamic>;

    final metadata = ClaimFormDataModel.fromJson(
      data['metadata'] as Map<String, dynamic>,
    );

    final draftJson = data['draft'];
    if (draftJson == null) {
      return ClaimFormModel(metadata: metadata, draft: null);
    }

    final draft = ClaimDraftModel.fromJson(draftJson as Map<String, dynamic>);
    // presigned URL 사진을 임시 파일로 내려받아 폼에 표시할 수 있게 한다(실패 시 해당 사진만 생략).
    return ClaimFormModel(
      metadata: metadata,
      draft: draft.withPhotos(
        defectPhoto: await _downloadToTemp(draft.defectPhotoUrl, 'defect'),
        labelPhoto: await _downloadToTemp(draft.labelPhotoUrl, 'label'),
        receiptPhoto: await _downloadToTemp(draft.receiptPhotoUrl, 'receipt'),
      ),
    );
  }

  @override
  Future<List<ClaimListItemModel>> getClaims({
    String? startDate,
    String? endDate,
    int? accountId,
  }) async {
    final queryParameters = <String, dynamic>{};
    if (startDate != null) queryParameters['startDate'] = startDate;
    if (endDate != null) queryParameters['endDate'] = endDate;
    if (accountId != null) queryParameters['accountId'] = accountId;

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

  @override
  Future<void> saveDraft(ClaimDraftRequest request) async {
    final formData = await request.toFormData();
    await _dio.post('/api/v1/mobile/claims/draft', data: formData);
  }

  @override
  Future<void> deleteDraft() async {
    await _dio.delete('/api/v1/mobile/claims/draft');
  }

  /// presigned URL 을 시스템 임시 디렉터리에 내려받는다. 실패하면 null.
  Future<File?> _downloadToTemp(String? url, String tag) async {
    if (url == null || url.isEmpty) return null;
    try {
      // S3 presigned URL 은 절대 URL 이므로 인증 인터셉터가 없는 별도 Dio 로 받는다.
      final response = await Dio().get<List<int>>(
        url,
        options: Options(responseType: ResponseType.bytes),
      );
      final bytes = response.data;
      if (bytes == null || bytes.isEmpty) return null;

      final dir = await Directory.systemTemp.createTemp('claim_draft_');
      final file = File('${dir.path}/$tag.jpg');
      await file.writeAsBytes(bytes);
      return file;
    } catch (_) {
      return null;
    }
  }
}
