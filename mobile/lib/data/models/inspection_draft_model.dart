import 'dart:io';

import '../../domain/entities/inspection_draft.dart';

/// 현장 점검 임시저장 Model (`GET /api/v1/mobile/inspections/draft` 응답).
///
/// scalar 필드는 [fromJson] 으로 파싱하고, 사진은 URL 리스트만 담는다.
/// 데이터소스가 URL 을 임시 파일로 내려받아 [withPhotos] 로 [File] 을 채운 뒤 [toEntity] 한다.
class InspectionDraftModel {
  const InspectionDraftModel({
    this.themeId,
    this.category,
    this.accountId,
    this.accountName,
    this.inspectionDate,
    this.fieldTypeCode,
    this.description,
    this.productCode,
    this.productName,
    this.competitorName,
    this.competitorActivity,
    this.competitorTasting,
    this.competitorProductName,
    this.competitorProductPrice,
    this.competitorSalesQuantity,
    this.photoUrls = const [],
    this.photos = const [],
  });

  final int? themeId;
  final String? category;
  final int? accountId;
  final String? accountName;
  final String? inspectionDate;
  final String? fieldTypeCode;
  final String? description;
  final String? productCode;
  final String? productName;
  final String? competitorName;
  final String? competitorActivity;
  final bool? competitorTasting;
  final String? competitorProductName;
  final int? competitorProductPrice;
  final int? competitorSalesQuantity;

  final List<String> photoUrls;

  /// 데이터소스가 내려받아 채운 사진 임시 파일
  final List<File> photos;

  factory InspectionDraftModel.fromJson(Map<String, dynamic> json) {
    final urls = (json['photoUrls'] as List<dynamic>?)
            ?.map((e) => e as String)
            .toList() ??
        const <String>[];
    return InspectionDraftModel(
      themeId: (json['themeId'] as num?)?.toInt(),
      category: json['category'] as String?,
      accountId: (json['accountId'] as num?)?.toInt(),
      accountName: json['accountName'] as String?,
      inspectionDate: json['inspectionDate'] as String?,
      fieldTypeCode: json['fieldTypeCode'] as String?,
      description: json['description'] as String?,
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      competitorName: json['competitorName'] as String?,
      competitorActivity: json['competitorActivity'] as String?,
      competitorTasting: json['competitorTasting'] as bool?,
      competitorProductName: json['competitorProductName'] as String?,
      competitorProductPrice: (json['competitorProductPrice'] as num?)?.toInt(),
      competitorSalesQuantity:
          (json['competitorSalesQuantity'] as num?)?.toInt(),
      photoUrls: urls,
    );
  }

  /// 내려받은 사진 파일을 채운 사본 반환
  InspectionDraftModel withPhotos(List<File> photos) {
    return InspectionDraftModel(
      themeId: themeId,
      category: category,
      accountId: accountId,
      accountName: accountName,
      inspectionDate: inspectionDate,
      fieldTypeCode: fieldTypeCode,
      description: description,
      productCode: productCode,
      productName: productName,
      competitorName: competitorName,
      competitorActivity: competitorActivity,
      competitorTasting: competitorTasting,
      competitorProductName: competitorProductName,
      competitorProductPrice: competitorProductPrice,
      competitorSalesQuantity: competitorSalesQuantity,
      photoUrls: photoUrls,
      photos: photos,
    );
  }

  InspectionDraft toEntity() {
    return InspectionDraft(
      themeId: themeId,
      category: category,
      accountId: accountId,
      accountName: accountName,
      inspectionDate: inspectionDate,
      fieldTypeCode: fieldTypeCode,
      description: description,
      productCode: productCode,
      productName: productName,
      competitorName: competitorName,
      competitorActivity: competitorActivity,
      competitorTasting: competitorTasting,
      competitorProductName: competitorProductName,
      competitorProductPrice: competitorProductPrice,
      competitorSalesQuantity: competitorSalesQuantity,
      photos: photos,
    );
  }
}
