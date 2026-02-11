import 'dart:io';

import 'package:http_parser/http_parser.dart';
import 'package:dio/dio.dart';

import '../../domain/entities/claim_form.dart';

/// 클레임 등록 요청 Model (multipart/form-data)
class ClaimRegisterRequest {
  const ClaimRegisterRequest({
    required this.storeId,
    required this.productCode,
    required this.dateType,
    required this.date,
    required this.categoryId,
    required this.subcategoryId,
    required this.defectDescription,
    required this.defectQuantity,
    required this.defectPhoto,
    required this.labelPhoto,
    this.purchaseAmount,
    this.purchaseMethodCode,
    this.receiptPhoto,
    this.requestTypeCode,
  });

  final int storeId;
  final String productCode;
  final String dateType;
  final String date;
  final int categoryId;
  final int subcategoryId;
  final String defectDescription;
  final int defectQuantity;
  final File defectPhoto;
  final File labelPhoto;
  final int? purchaseAmount;
  final String? purchaseMethodCode;
  final File? receiptPhoto;
  final String? requestTypeCode;

  /// Entity에서 변환
  factory ClaimRegisterRequest.fromEntity(ClaimRegisterForm form) {
    return ClaimRegisterRequest(
      storeId: form.storeId,
      productCode: form.productCode,
      dateType: form.dateType.toJson(),
      date: form.date.toIso8601String().substring(0, 10), // YYYY-MM-DD
      categoryId: form.categoryId,
      subcategoryId: form.subcategoryId,
      defectDescription: form.defectDescription,
      defectQuantity: form.defectQuantity,
      defectPhoto: form.defectPhoto,
      labelPhoto: form.labelPhoto,
      purchaseAmount: form.purchaseAmount,
      purchaseMethodCode: form.purchaseMethodCode,
      receiptPhoto: form.receiptPhoto,
      requestTypeCode: form.requestTypeCode,
    );
  }

  /// FormData로 변환 (Dio multipart/form-data)
  Future<FormData> toFormData() async {
    final Map<String, dynamic> fields = {
      'storeId': storeId.toString(),
      'productCode': productCode,
      'dateType': dateType,
      'date': date,
      'categoryId': categoryId.toString(),
      'subcategoryId': subcategoryId.toString(),
      'defectDescription': defectDescription,
      'defectQuantity': defectQuantity.toString(),
    };

    // 선택 필드 추가
    if (purchaseAmount != null) {
      fields['purchaseAmount'] = purchaseAmount.toString();
    }
    if (purchaseMethodCode != null) {
      fields['purchaseMethodCode'] = purchaseMethodCode!;
    }
    if (requestTypeCode != null) {
      fields['requestTypeCode'] = requestTypeCode!;
    }

    // 파일 추가
    fields['defectPhoto'] = await MultipartFile.fromFile(
      defectPhoto.path,
      filename: defectPhoto.path.split('/').last,
      contentType: MediaType('image', 'jpeg'),
    );

    fields['labelPhoto'] = await MultipartFile.fromFile(
      labelPhoto.path,
      filename: labelPhoto.path.split('/').last,
      contentType: MediaType('image', 'jpeg'),
    );

    if (receiptPhoto != null) {
      fields['receiptPhoto'] = await MultipartFile.fromFile(
        receiptPhoto!.path,
        filename: receiptPhoto!.path.split('/').last,
        contentType: MediaType('image', 'jpeg'),
      );
    }

    return FormData.fromMap(fields);
  }
}
