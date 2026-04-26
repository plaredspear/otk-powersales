import 'dart:io';

import 'package:dio/dio.dart';
import 'package:http_parser/http_parser.dart';

import '../../domain/entities/suggestion_form.dart';

/// 제안하기 등록 요청 Model (multipart/form-data)
class SuggestionRegisterRequest {
  const SuggestionRegisterRequest({
    required this.category,
    this.productCode,
    required this.title,
    required this.content,
    this.photos = const [],
  });

  final String category; // SuggestionCategory의 code
  final String? productCode;
  final String title;
  final String content;
  final List<File> photos;

  /// Entity에서 변환
  factory SuggestionRegisterRequest.fromEntity(SuggestionRegisterForm form) {
    return SuggestionRegisterRequest(
      category: form.category.code,
      productCode: form.productCode,
      title: form.title,
      content: form.content,
      photos: form.photos,
    );
  }

  /// FormData로 변환 (Dio multipart/form-data)
  Future<FormData> toFormData() async {
    final Map<String, dynamic> fields = {
      'category': category,
      'title': title,
      'content': content,
    };

    // 선택 필드: productCode (기존제품 선택 시)
    if (productCode != null && productCode!.isNotEmpty) {
      fields['productCode'] = productCode!;
    }

    // 파일 추가 (최대 2장)
    if (photos.isNotEmpty) {
      final photoFiles = <MultipartFile>[];
      for (var i = 0; i < photos.length && i < 2; i++) {
        final file = photos[i];
        photoFiles.add(
          await MultipartFile.fromFile(
            file.path,
            filename: file.path.split('/').last,
            contentType: MediaType('image', 'jpeg'),
          ),
        );
      }
      fields['photos'] = photoFiles;
    }

    return FormData.fromMap(fields);
  }
}
