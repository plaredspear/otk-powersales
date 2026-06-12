import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../../domain/entities/inspection_form.dart';
import '../../domain/entities/inspection_list_item.dart';
import '../models/inspection_detail_model.dart';
import '../models/inspection_draft_model.dart';
import '../models/inspection_field_type_model.dart';
import '../models/inspection_list_item_model.dart';
import '../models/inspection_register_request.dart';
import '../models/inspection_theme_model.dart';
import 'inspection_remote_datasource.dart';

/// 현장 점검 API 데이터소스 구현체 (Dio)
///
/// 백엔드 `/api/v1/mobile/inspections` 엔드포인트와 통신합니다.
class InspectionApiDataSource implements InspectionRemoteDataSource {
  final Dio _dio;

  InspectionApiDataSource(this._dio);

  static const String _basePath = '/api/v1/mobile/inspections';

  @override
  Future<List<InspectionListItemModel>> getInspectionList({
    int? accountId,
    String? category,
    required String fromDate,
    required String toDate,
  }) async {
    final queryParameters = <String, dynamic>{
      'fromDate': fromDate,
      'toDate': toDate,
    };
    if (accountId != null) queryParameters['accountId'] = accountId;
    if (category != null) queryParameters['category'] = category;

    final response = await _dio.get(
      _basePath,
      queryParameters: queryParameters,
    );

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => InspectionListItemModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<InspectionDetailModel> getInspectionDetail(int inspectionId) async {
    final response = await _dio.get('$_basePath/$inspectionId');

    return InspectionDetailModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<InspectionListItemModel> registerInspection(
    InspectionRegisterRequest request,
  ) async {
    // 백엔드는 multipart 의 "request" 파트를 JSON 으로, "photos" 파트를 파일 리스트로 받는다.
    final formData = FormData();
    formData.files.add(
      MapEntry(
        'request',
        MultipartFile.fromString(
          jsonEncode(request.toFormData()),
          contentType: MediaType('application', 'json'),
        ),
      ),
    );

    for (final photo in request.photos) {
      formData.files.add(
        MapEntry(
          'photos',
          await MultipartFile.fromFile(
            photo.path,
            filename: photo.path.split('/').last,
            contentType: MediaType('image', 'jpeg'),
          ),
        ),
      );
    }

    final response = await _dio.post(_basePath, data: formData);

    return InspectionListItemModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<List<InspectionThemeModel>> getThemes() async {
    final response = await _dio.get('$_basePath/themes');

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => InspectionThemeModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<List<InspectionFieldTypeModel>> getFieldTypes() async {
    final response = await _dio.get('$_basePath/field-types');

    final data = response.data['data'] as List<dynamic>;
    return data
        .map((e) => InspectionFieldTypeModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  @override
  Future<InspectionDraftModel?> getDraft() async {
    final response = await _dio.get('$_basePath/draft');
    final data = response.data['data'];
    if (data == null) return null;

    final draft = InspectionDraftModel.fromJson(data as Map<String, dynamic>);
    // 사진 URL 을 임시 파일로 내려받아 폼에 표시할 수 있게 한다(실패 시 해당 사진만 생략).
    final files = <File>[];
    for (var i = 0; i < draft.photoUrls.length; i++) {
      final file = await _downloadToTemp(draft.photoUrls[i], 'photo_$i');
      if (file != null) files.add(file);
    }
    return draft.withPhotos(files);
  }

  @override
  Future<void> saveDraft(
    InspectionRegisterForm form, {
    String? accountName,
    String? productName,
  }) async {
    // 백엔드는 multipart 의 "request" 파트를 JSON 으로, "photos" 파트를 파일 리스트로 받는다.
    final formData = FormData();
    formData.files.add(
      MapEntry(
        'request',
        MultipartFile.fromString(
          jsonEncode(_draftJson(form, accountName, productName)),
          contentType: MediaType('application', 'json'),
        ),
      ),
    );

    for (final photo in form.photos) {
      formData.files.add(
        MapEntry(
          'photos',
          await MultipartFile.fromFile(
            photo.path,
            filename: photo.path.split('/').last,
            contentType: MediaType('image', 'jpeg'),
          ),
        ),
      );
    }

    await _dio.post('$_basePath/draft', data: formData);
  }

  /// 임시저장 요청 JSON. 검증 없이 현재 폼 상태를 담되, 비어있는(미선택) 필드는 생략한다.
  Map<String, dynamic> _draftJson(
    InspectionRegisterForm form,
    String? accountName,
    String? productName,
  ) {
    return {
      if (form.themeId > 0) 'themeId': form.themeId,
      'category': form.category.toJson(),
      if (form.accountId > 0) 'accountId': form.accountId,
      if (accountName != null && accountName.isNotEmpty)
        'accountName': accountName,
      'inspectionDate': form.inspectionDate.toIso8601String().substring(0, 10),
      if (form.fieldTypeCode.isNotEmpty) 'fieldTypeCode': form.fieldTypeCode,
      if (form.description != null && form.description!.isNotEmpty)
        'description': form.description,
      if (form.productCode != null && form.productCode!.isNotEmpty)
        'productCode': form.productCode,
      if (productName != null && productName.isNotEmpty)
        'productName': productName,
      if (form.competitorName != null && form.competitorName!.isNotEmpty)
        'competitorName': form.competitorName,
      if (form.competitorActivity != null &&
          form.competitorActivity!.isNotEmpty)
        'competitorActivity': form.competitorActivity,
      if (form.competitorTasting != null)
        'competitorTasting': form.competitorTasting,
      if (form.competitorProductName != null &&
          form.competitorProductName!.isNotEmpty)
        'competitorProductName': form.competitorProductName,
      if (form.competitorProductPrice != null)
        'competitorProductPrice': form.competitorProductPrice,
      if (form.competitorSalesQuantity != null)
        'competitorSalesQuantity': form.competitorSalesQuantity,
    };
  }

  /// 사진 URL 을 시스템 임시 디렉터리에 내려받는다. 실패하면 null.
  Future<File?> _downloadToTemp(String? url, String tag) async {
    if (url == null || url.isEmpty) return null;
    try {
      // 절대 URL 이므로 인증 인터셉터가 없는 별도 Dio 로 받는다.
      final response = await Dio().get<List<int>>(
        url,
        options: Options(responseType: ResponseType.bytes),
      );
      final bytes = response.data;
      if (bytes == null || bytes.isEmpty) return null;

      final dir = await Directory.systemTemp.createTemp('inspection_draft_');
      final file = File('${dir.path}/$tag.jpg');
      await file.writeAsBytes(bytes);
      return file;
    } catch (_) {
      return null;
    }
  }
}
