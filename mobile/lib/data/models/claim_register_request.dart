import 'dart:io';

import 'package:http_parser/http_parser.dart';
import 'package:dio/dio.dart';

import '../../domain/entities/claim_form.dart';

/// 클레임 등록 요청 Model (multipart/form-data)
class ClaimRegisterRequest {
  const ClaimRegisterRequest({
    required this.accountId,
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

  final int accountId;
  final String productCode;
  final String dateType;
  final String date;
  /// 백엔드 ClaimCreateRequest.claimType1 (SF picklist value, 예: "A").
  final String categoryId;

  /// 백엔드 ClaimCreateRequest.claimType2 (SF picklist value, 예: "AA").
  final String subcategoryId;
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
      accountId: form.accountId,
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
      'accountId': accountId.toString(),
      'productCode': productCode,
      'dateType': dateType,
      'date': date,
      // 백엔드 ClaimCreateRequest 는 SF picklist value 를 claimType1/claimType2 로 받는다
      'claimType1': categoryId,
      'claimType2': subcategoryId,
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

    // 파일 추가 (항상 image/jpeg 로 전송하므로 filename 도 .jpg 확장자를 보장한다)
    fields['defectPhoto'] = await MultipartFile.fromFile(
      defectPhoto.path,
      filename: _imageFilename(defectPhoto.path),
      contentType: MediaType('image', 'jpeg'),
    );

    fields['labelPhoto'] = await MultipartFile.fromFile(
      labelPhoto.path,
      filename: _imageFilename(labelPhoto.path),
      contentType: MediaType('image', 'jpeg'),
    );

    if (receiptPhoto != null) {
      fields['receiptPhoto'] = await MultipartFile.fromFile(
        receiptPhoto!.path,
        filename: _imageFilename(receiptPhoto!.path),
        contentType: MediaType('image', 'jpeg'),
      );
    }

    return FormData.fromMap(fields);
  }

  /// 업로드 파일명을 이미지 확장자(.jpg)가 보장된 형태로 정규화한다.
  ///
  /// iOS(Simulator 포함) image_picker 임시 파일은 확장자가 없거나(`.../image_XXXX`)
  /// 비이미지 확장자(`.../XXXX.tmp`)로 넘어온다. 이 파일명이 그대로 SF 로 전달되면
  /// SF `FileExtensionGuard` 가 허용 확장자 목록에 없다며 등록을 거부한다
  /// (`FIELD_CUSTOM_VALIDATION_EXCEPTION, 허용되지 않는 파일 확장자입니다`).
  /// 업로드 콘텐츠는 항상 image/jpeg 이므로 확장자를 .jpg 로 통일한다.
  static String _imageFilename(String path) {
    final base = path.split('/').last;
    final dot = base.lastIndexOf('.');
    final stem = (dot > 0) ? base.substring(0, dot) : base;
    final safeStem = stem.isEmpty ? 'claim' : stem;
    return '$safeStem.jpg';
  }
}
