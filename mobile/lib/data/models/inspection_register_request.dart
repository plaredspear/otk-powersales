import 'dart:io';

import '../../domain/entities/inspection_form.dart';
import '../../domain/entities/inspection_list_item.dart';

/// 현장 점검 등록 요청 모델 (DTO)
///
/// Backend API로 전송하는 등록 요청 데이터입니다.
/// Multipart form-data 형식으로 전송됩니다.
class InspectionRegisterRequest {
  final int themeId;
  final String category;
  final int storeId;
  final String inspectionDate;
  final String fieldTypeCode;
  final String? description;
  final String? productCode;
  final String? competitorName;
  final String? competitorActivity;
  final bool? competitorTasting;
  final String? competitorProductName;
  final int? competitorProductPrice;
  final int? competitorSalesQuantity;
  final List<File> photos;

  const InspectionRegisterRequest({
    required this.themeId,
    required this.category,
    required this.storeId,
    required this.inspectionDate,
    required this.fieldTypeCode,
    this.description,
    this.productCode,
    this.competitorName,
    this.competitorActivity,
    this.competitorTasting,
    this.competitorProductName,
    this.competitorProductPrice,
    this.competitorSalesQuantity,
    required this.photos,
  });

  /// Domain Entity에서 생성
  factory InspectionRegisterRequest.fromEntity(InspectionRegisterForm entity) {
    return InspectionRegisterRequest(
      themeId: entity.themeId,
      category: entity.category.toJson(),
      storeId: entity.storeId,
      inspectionDate: entity.inspectionDate.toIso8601String().substring(0, 10),
      fieldTypeCode: entity.fieldTypeCode,
      description: entity.description,
      productCode: entity.productCode,
      competitorName: entity.competitorName,
      competitorActivity: entity.competitorActivity,
      competitorTasting: entity.competitorTasting,
      competitorProductName: entity.competitorProductName,
      competitorProductPrice: entity.competitorProductPrice,
      competitorSalesQuantity: entity.competitorSalesQuantity,
      photos: entity.photos,
    );
  }

  /// Multipart form fields (JSON 부분) 생성
  ///
  /// Retrofit에서 @Part("data")로 전송할 JSON 데이터
  Map<String, dynamic> toFormData() {
    return {
      'themeId': themeId,
      'category': category,
      'storeId': storeId,
      'inspectionDate': inspectionDate,
      'fieldTypeCode': fieldTypeCode,
      if (description != null) 'description': description,
      if (productCode != null) 'productCode': productCode,
      if (competitorName != null) 'competitorName': competitorName,
      if (competitorActivity != null) 'competitorActivity': competitorActivity,
      if (competitorTasting != null) 'competitorTasting': competitorTasting,
      if (competitorProductName != null)
        'competitorProductName': competitorProductName,
      if (competitorProductPrice != null)
        'competitorProductPrice': competitorProductPrice,
      if (competitorSalesQuantity != null)
        'competitorSalesQuantity': competitorSalesQuantity,
    };
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is InspectionRegisterRequest &&
        other.themeId == themeId &&
        other.category == category &&
        other.storeId == storeId &&
        other.inspectionDate == inspectionDate &&
        other.fieldTypeCode == fieldTypeCode &&
        other.description == description &&
        other.productCode == productCode &&
        other.competitorName == competitorName &&
        other.competitorActivity == competitorActivity &&
        other.competitorTasting == competitorTasting &&
        other.competitorProductName == competitorProductName &&
        other.competitorProductPrice == competitorProductPrice &&
        other.competitorSalesQuantity == competitorSalesQuantity &&
        _listEquals(other.photos, photos);
  }

  bool _listEquals<T>(List<T> a, List<T> b) {
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }

  @override
  int get hashCode {
    return Object.hash(
      themeId,
      category,
      storeId,
      inspectionDate,
      fieldTypeCode,
      description,
      productCode,
      competitorName,
      competitorActivity,
      competitorTasting,
      competitorProductName,
      competitorProductPrice,
      competitorSalesQuantity,
      Object.hashAll(photos),
    );
  }

  @override
  String toString() {
    return 'InspectionRegisterRequest(themeId: $themeId, category: $category, '
        'storeId: $storeId, inspectionDate: $inspectionDate, '
        'fieldTypeCode: $fieldTypeCode, description: $description, '
        'productCode: $productCode, competitorName: $competitorName, '
        'competitorActivity: $competitorActivity, competitorTasting: $competitorTasting, '
        'competitorProductName: $competitorProductName, competitorProductPrice: $competitorProductPrice, '
        'competitorSalesQuantity: $competitorSalesQuantity, photos: ${photos.length} files)';
  }
}
