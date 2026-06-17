import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../../domain/entities/suggestion_form.dart';

/// 제안하기 임시저장 요청 Model (multipart/form-data, POST /api/v1/mobile/suggestions/draft)
///
/// Backend `SuggestionController` draft 엔드포인트는 등록과 동일하게
/// `@RequestPart("request")` JSON + `@RequestPart photos` files 구조를 받는다.
/// 등록과 달리 검증이 없으므로 category/title/content 가 비어도 전송하며, null /
/// 빈 문자열 필드는 JSON 키 자체를 제외한다.
class SuggestionDraftRequest {
  const SuggestionDraftRequest({required this.form});

  final SuggestionRegisterForm? form;

  factory SuggestionDraftRequest.fromForm(SuggestionRegisterForm? form) =>
      SuggestionDraftRequest(form: form);

  /// `request` part 본문 JSON 직렬화 결과
  ///
  /// 임시저장은 검증이 없으므로 category 도 비어 있으면 키를 제외한다.
  Map<String, dynamic> toJson() {
    final Map<String, dynamic> json = {};
    final f = form;
    if (f == null) return json;

    json['category'] = f.category.code;
    if (f.title.isNotEmpty) json['title'] = f.title;
    if (f.content.isNotEmpty) json['content'] = f.content;
    if (f.productCode != null && f.productCode!.isNotEmpty) {
      json['productCode'] = f.productCode;
    }
    if (f.productName != null && f.productName!.isNotEmpty) {
      json['productName'] = f.productName;
    }
    if (f.accountId != null) json['accountId'] = f.accountId;
    if (f.accountName != null && f.accountName!.isNotEmpty) {
      json['accountName'] = f.accountName;
    }
    if (f.sapAccountCode != null && f.sapAccountCode!.isNotEmpty) {
      json['sapAccountCode'] = f.sapAccountCode;
    }
    if (f.claimType != null && f.claimType!.isNotEmpty) {
      json['claimType'] = f.claimType;
    }
    if (f.claimDate != null) {
      json['claimDate'] = _formatLocalDate(f.claimDate!);
    }
    if (f.carNumber != null && f.carNumber!.isNotEmpty) {
      json['carNumber'] = f.carNumber;
    }
    return json;
  }

  /// FormData 로 변환 (Dio multipart/form-data)
  ///
  /// 구성: `request` (JSON, application/json) + `photos` (jpeg files, 최대 2개).
  Future<FormData> toFormData() async {
    final formData = FormData();

    formData.files.add(
      MapEntry(
        'request',
        MultipartFile.fromString(
          jsonEncode(toJson()),
          contentType: MediaType('application', 'json'),
        ),
      ),
    );

    final photos = form?.photos ?? const <File>[];
    for (var i = 0; i < photos.length && i < 2; i++) {
      final file = photos[i];
      if (file.path.isEmpty) continue;
      formData.files.add(
        MapEntry(
          'photos',
          await MultipartFile.fromFile(
            file.path,
            filename: file.path.split('/').last,
            contentType: MediaType('image', 'jpeg'),
          ),
        ),
      );
    }

    return formData;
  }

  static String _formatLocalDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }
}
