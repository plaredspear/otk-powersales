import 'dart:convert';
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../../domain/entities/suggestion_form.dart';

/// 제안하기 등록 요청 Model (multipart/form-data)
///
/// Backend `SuggestionController.create` 는 `@RequestPart("request")` JSON +
/// `@RequestPart photos` files 두 part 구조를 받는다. fields 평면화 방식이
/// 아니므로 [toFormData] 는 JSON 직렬화한 단일 part 와 file parts 를 분리하여
/// 구성한다.
class SuggestionRegisterRequest {
  const SuggestionRegisterRequest({
    required this.category,
    this.productCode,
    required this.title,
    required this.content,
    this.photos = const [],
    this.accountId,
    this.sapAccountCode,
    this.claimType,
    this.claimDate,
    this.carNumber,
    this.logisticsResponsibility,
    this.duplicateProposalNum,
  });

  final String category;
  final String? productCode;
  final String title;
  final String content;
  final List<File> photos;
  final int? accountId;
  final String? sapAccountCode;
  final String? claimType;
  final DateTime? claimDate;
  final String? carNumber;
  final String? logisticsResponsibility;
  final String? duplicateProposalNum;

  /// Entity에서 변환
  factory SuggestionRegisterRequest.fromEntity(SuggestionRegisterForm form) {
    return SuggestionRegisterRequest(
      category: form.category.code,
      productCode: form.productCode,
      title: form.title,
      content: form.content,
      photos: form.photos,
      accountId: form.accountId,
      sapAccountCode: form.sapAccountCode,
      claimType: form.claimType,
      claimDate: form.claimDate,
      carNumber: form.carNumber,
      logisticsResponsibility: form.logisticsResponsibility,
      duplicateProposalNum: form.duplicateProposalNum,
    );
  }

  /// `request` part 본문 JSON 직렬화 결과
  ///
  /// null / 빈 문자열 필드는 키 자체를 제외하여 backend Bean Validation 의
  /// `@Size` 가드를 트리거하지 않는다.
  Map<String, dynamic> toJson() {
    final Map<String, dynamic> json = {
      'category': category,
      'title': title,
      'content': content,
    };
    if (productCode != null && productCode!.isNotEmpty) {
      json['productCode'] = productCode;
    }
    if (accountId != null) {
      json['accountId'] = accountId;
    }
    if (sapAccountCode != null && sapAccountCode!.isNotEmpty) {
      json['sapAccountCode'] = sapAccountCode;
    }
    if (claimType != null && claimType!.isNotEmpty) {
      json['claimType'] = claimType;
    }
    if (claimDate != null) {
      json['claimDate'] = _formatLocalDate(claimDate!);
    }
    if (carNumber != null && carNumber!.isNotEmpty) {
      json['carNumber'] = carNumber;
    }
    if (logisticsResponsibility != null &&
        logisticsResponsibility!.isNotEmpty) {
      json['logisticsResponsibility'] = logisticsResponsibility;
    }
    if (duplicateProposalNum != null && duplicateProposalNum!.isNotEmpty) {
      json['duplicateProposalNum'] = duplicateProposalNum;
    }
    return json;
  }

  /// FormData로 변환 (Dio multipart/form-data)
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

    for (var i = 0; i < photos.length && i < 2; i++) {
      final file = photos[i];
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
