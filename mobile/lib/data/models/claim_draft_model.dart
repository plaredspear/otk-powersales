import 'dart:io';

import '../../domain/entities/claim_draft.dart';

/// 클레임 임시저장 Model (진입 폼 `GET /api/v1/mobile/claims/form` 의 `draft` 필드)
///
/// scalar 필드는 [fromJson] 으로 파싱하고, 사진은 URL 만 담는다.
/// 데이터소스가 URL 을 임시 파일로 내려받아 [withPhotos] 로 [File] 을 채운 뒤 [toEntity] 한다.
class ClaimDraftModel {
  const ClaimDraftModel({
    this.accountId,
    this.accountName,
    this.productCode,
    this.productName,
    this.dateType,
    this.date,
    this.claimType1,
    this.claimType2,
    this.defectDescription,
    this.defectQuantity,
    this.purchaseAmount,
    this.purchaseMethodCode,
    this.requestTypeCode,
    this.defectPhotoUrl,
    this.labelPhotoUrl,
    this.receiptPhotoUrl,
    this.defectPhoto,
    this.labelPhoto,
    this.receiptPhoto,
  });

  final int? accountId;
  final String? accountName;
  final String? productCode;
  final String? productName;
  final String? dateType;
  final String? date;
  final String? claimType1;
  final String? claimType2;
  final String? defectDescription;
  final int? defectQuantity;
  final int? purchaseAmount;
  final String? purchaseMethodCode;
  final String? requestTypeCode;

  final String? defectPhotoUrl;
  final String? labelPhotoUrl;
  final String? receiptPhotoUrl;

  // 데이터소스가 URL 을 내려받아 채운 임시 파일
  final File? defectPhoto;
  final File? labelPhoto;
  final File? receiptPhoto;

  factory ClaimDraftModel.fromJson(Map<String, dynamic> json) {
    return ClaimDraftModel(
      accountId: (json['accountId'] as num?)?.toInt(),
      accountName: json['accountName'] as String?,
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      dateType: json['dateType'] as String?,
      date: json['date'] as String?,
      claimType1: json['claimType1'] as String?,
      claimType2: json['claimType2'] as String?,
      defectDescription: json['defectDescription'] as String?,
      defectQuantity: (json['defectQuantity'] as num?)?.toInt(),
      purchaseAmount: (json['purchaseAmount'] as num?)?.toInt(),
      purchaseMethodCode: json['purchaseMethodCode'] as String?,
      requestTypeCode: json['requestTypeCode'] as String?,
      defectPhotoUrl: json['defectPhotoUrl'] as String?,
      labelPhotoUrl: json['labelPhotoUrl'] as String?,
      receiptPhotoUrl: json['receiptPhotoUrl'] as String?,
    );
  }

  /// 내려받은 사진 파일을 채운 사본 반환
  ClaimDraftModel withPhotos({
    File? defectPhoto,
    File? labelPhoto,
    File? receiptPhoto,
  }) {
    return ClaimDraftModel(
      accountId: accountId,
      accountName: accountName,
      productCode: productCode,
      productName: productName,
      dateType: dateType,
      date: date,
      claimType1: claimType1,
      claimType2: claimType2,
      defectDescription: defectDescription,
      defectQuantity: defectQuantity,
      purchaseAmount: purchaseAmount,
      purchaseMethodCode: purchaseMethodCode,
      requestTypeCode: requestTypeCode,
      defectPhotoUrl: defectPhotoUrl,
      labelPhotoUrl: labelPhotoUrl,
      receiptPhotoUrl: receiptPhotoUrl,
      defectPhoto: defectPhoto,
      labelPhoto: labelPhoto,
      receiptPhoto: receiptPhoto,
    );
  }

  ClaimDraft toEntity() {
    return ClaimDraft(
      accountId: accountId,
      accountName: accountName,
      productCode: productCode,
      productName: productName,
      dateType: dateType,
      date: date,
      claimType1: claimType1,
      claimType2: claimType2,
      defectDescription: defectDescription,
      defectQuantity: defectQuantity,
      purchaseAmount: purchaseAmount,
      purchaseMethodCode: purchaseMethodCode,
      requestTypeCode: requestTypeCode,
      defectPhoto: defectPhoto,
      labelPhoto: labelPhoto,
      receiptPhoto: receiptPhoto,
    );
  }
}
