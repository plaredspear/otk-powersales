import 'dart:io';

import 'package:dio/dio.dart';

import '../../domain/entities/suggestion_draft.dart';
import '../models/suggestion_detail_model.dart';
import '../models/suggestion_draft_model.dart';
import '../models/suggestion_draft_request.dart';
import '../models/suggestion_list_item_model.dart';
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

  @override
  Future<SuggestionListPageModel> getSuggestions({
    int page = 0,
    int size = 20,
    String? category,
  }) async {
    final response = await _dio.get(
      '/api/v1/mobile/suggestions',
      queryParameters: {
        'page': page,
        'size': size,
        'category': ?category,
      },
    );

    return SuggestionListPageModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<SuggestionDetailModel> getSuggestionDetail(int suggestionId) async {
    final response =
        await _dio.get('/api/v1/mobile/suggestions/$suggestionId');

    return SuggestionDetailModel.fromJson(
      response.data['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<void> saveDraft(SuggestionDraftRequest request) async {
    final formData = await request.toFormData();
    await _dio.post(
      '/api/v1/mobile/suggestions/draft',
      data: formData,
      options: Options(contentType: 'multipart/form-data'),
    );
  }

  @override
  Future<SuggestionDraft?> loadDraft() async {
    final response = await _dio.get('/api/v1/mobile/suggestions/draft');
    final data = response.data['data'];
    if (data == null) return null;

    final model = SuggestionDraftModel.fromJson(data as Map<String, dynamic>);

    // presigned URL 사진을 임시 파일로 내려받아 폼에 표시할 수 있게 한다
    // (실패 시 해당 사진만 생략). 최대 2장.
    final photos = <File>[];
    for (var i = 0; i < model.photoUrls.length && i < 2; i++) {
      final file = await _downloadToTemp(model.photoUrls[i], 'photo$i');
      if (file != null) photos.add(file);
    }

    return model.withPhotos(photos).toEntity();
  }

  @override
  Future<void> deleteDraft() async {
    await _dio.delete('/api/v1/mobile/suggestions/draft');
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

      final dir = await Directory.systemTemp.createTemp('suggestion_draft_');
      final file = File('${dir.path}/$tag.jpg');
      await file.writeAsBytes(bytes);
      return file;
    } catch (_) {
      return null;
    }
  }
}
