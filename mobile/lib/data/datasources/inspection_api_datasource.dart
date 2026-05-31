import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../models/inspection_detail_model.dart';
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
}
