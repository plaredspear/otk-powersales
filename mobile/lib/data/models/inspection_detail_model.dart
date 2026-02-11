import '../../domain/entities/inspection_detail.dart';
import '../../domain/entities/inspection_list_item.dart';

/// 현장 점검 상세 모델 (DTO)
///
/// Backend API의 JSON을 파싱하여 InspectionDetail 엔티티로 변환합니다.
class InspectionDetailModel {
  final int id;
  final String category;
  final String storeName;
  final int storeId;
  final String themeName;
  final int themeId;
  final String inspectionDate;
  final String fieldType;
  final String fieldTypeCode;
  final String? description;
  final String? productCode;
  final String? productName;
  final String? competitorName;
  final String? competitorActivity;
  final bool? competitorTasting;
  final String? competitorProductName;
  final int? competitorProductPrice;
  final int? competitorSalesQuantity;
  final List<InspectionPhotoModel> photos;
  final String createdAt;

  const InspectionDetailModel({
    required this.id,
    required this.category,
    required this.storeName,
    required this.storeId,
    required this.themeName,
    required this.themeId,
    required this.inspectionDate,
    required this.fieldType,
    required this.fieldTypeCode,
    this.description,
    this.productCode,
    this.productName,
    this.competitorName,
    this.competitorActivity,
    this.competitorTasting,
    this.competitorProductName,
    this.competitorProductPrice,
    this.competitorSalesQuantity,
    required this.photos,
    required this.createdAt,
  });

  /// JSON에서 파싱
  factory InspectionDetailModel.fromJson(Map<String, dynamic> json) {
    return InspectionDetailModel(
      id: json['id'] as int,
      category: json['category'] as String,
      storeName: json['storeName'] as String,
      storeId: json['storeId'] as int,
      themeName: json['themeName'] as String,
      themeId: json['themeId'] as int,
      inspectionDate: json['inspectionDate'] as String,
      fieldType: json['fieldType'] as String,
      fieldTypeCode: json['fieldTypeCode'] as String,
      description: json['description'] as String?,
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      competitorName: json['competitorName'] as String?,
      competitorActivity: json['competitorActivity'] as String?,
      competitorTasting: json['competitorTasting'] as bool?,
      competitorProductName: json['competitorProductName'] as String?,
      competitorProductPrice: json['competitorProductPrice'] as int?,
      competitorSalesQuantity: json['competitorSalesQuantity'] as int?,
      photos: (json['photos'] as List<dynamic>)
          .map((photo) => InspectionPhotoModel.fromJson(photo as Map<String, dynamic>))
          .toList(),
      createdAt: json['createdAt'] as String,
    );
  }

  /// JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category,
      'storeName': storeName,
      'storeId': storeId,
      'themeName': themeName,
      'themeId': themeId,
      'inspectionDate': inspectionDate,
      'fieldType': fieldType,
      'fieldTypeCode': fieldTypeCode,
      if (description != null) 'description': description,
      if (productCode != null) 'productCode': productCode,
      if (productName != null) 'productName': productName,
      if (competitorName != null) 'competitorName': competitorName,
      if (competitorActivity != null) 'competitorActivity': competitorActivity,
      if (competitorTasting != null) 'competitorTasting': competitorTasting,
      if (competitorProductName != null)
        'competitorProductName': competitorProductName,
      if (competitorProductPrice != null)
        'competitorProductPrice': competitorProductPrice,
      if (competitorSalesQuantity != null)
        'competitorSalesQuantity': competitorSalesQuantity,
      'photos': photos.map((photo) => photo.toJson()).toList(),
      'createdAt': createdAt,
    };
  }

  /// Domain Entity로 변환
  InspectionDetail toEntity() {
    return InspectionDetail(
      id: id,
      category: InspectionCategoryExtension.fromJson(category),
      storeName: storeName,
      storeId: storeId,
      themeName: themeName,
      themeId: themeId,
      inspectionDate: DateTime.parse(inspectionDate),
      fieldType: fieldType,
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
      photos: photos.map((photo) => photo.toEntity()).toList(),
      createdAt: DateTime.parse(createdAt),
    );
  }

  /// Domain Entity에서 생성
  factory InspectionDetailModel.fromEntity(InspectionDetail entity) {
    return InspectionDetailModel(
      id: entity.id,
      category: entity.category.toJson(),
      storeName: entity.storeName,
      storeId: entity.storeId,
      themeName: entity.themeName,
      themeId: entity.themeId,
      inspectionDate: entity.inspectionDate.toIso8601String().substring(0, 10),
      fieldType: entity.fieldType,
      fieldTypeCode: entity.fieldTypeCode,
      description: entity.description,
      productCode: entity.productCode,
      productName: entity.productName,
      competitorName: entity.competitorName,
      competitorActivity: entity.competitorActivity,
      competitorTasting: entity.competitorTasting,
      competitorProductName: entity.competitorProductName,
      competitorProductPrice: entity.competitorProductPrice,
      competitorSalesQuantity: entity.competitorSalesQuantity,
      photos: entity.photos
          .map((photo) => InspectionPhotoModel.fromEntity(photo))
          .toList(),
      createdAt: entity.createdAt.toIso8601String(),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is InspectionDetailModel &&
        other.id == id &&
        other.category == category &&
        other.storeName == storeName &&
        other.storeId == storeId &&
        other.themeName == themeName &&
        other.themeId == themeId &&
        other.inspectionDate == inspectionDate &&
        other.fieldType == fieldType &&
        other.fieldTypeCode == fieldTypeCode &&
        other.description == description &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.competitorName == competitorName &&
        other.competitorActivity == competitorActivity &&
        other.competitorTasting == competitorTasting &&
        other.competitorProductName == competitorProductName &&
        other.competitorProductPrice == competitorProductPrice &&
        other.competitorSalesQuantity == competitorSalesQuantity &&
        _listEquals(other.photos, photos) &&
        other.createdAt == createdAt;
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
      id,
      category,
      storeName,
      storeId,
      themeName,
      themeId,
      inspectionDate,
      fieldType,
      fieldTypeCode,
      description,
      productCode,
      productName,
      competitorName,
      competitorActivity,
      competitorTasting,
      competitorProductName,
      competitorProductPrice,
      competitorSalesQuantity,
      Object.hashAll(photos),
      createdAt,
    );
  }

  @override
  String toString() {
    return 'InspectionDetailModel(id: $id, category: $category, '
        'storeName: $storeName, storeId: $storeId, '
        'themeName: $themeName, themeId: $themeId, '
        'inspectionDate: $inspectionDate, fieldType: $fieldType, '
        'fieldTypeCode: $fieldTypeCode, description: $description, '
        'productCode: $productCode, productName: $productName, '
        'competitorName: $competitorName, competitorActivity: $competitorActivity, '
        'competitorTasting: $competitorTasting, competitorProductName: $competitorProductName, '
        'competitorProductPrice: $competitorProductPrice, competitorSalesQuantity: $competitorSalesQuantity, '
        'photos: $photos, createdAt: $createdAt)';
  }
}

/// 현장 점검 사진 모델 (DTO)
class InspectionPhotoModel {
  final int id;
  final String url;

  const InspectionPhotoModel({
    required this.id,
    required this.url,
  });

  /// JSON에서 파싱
  factory InspectionPhotoModel.fromJson(Map<String, dynamic> json) {
    return InspectionPhotoModel(
      id: json['id'] as int,
      url: json['url'] as String,
    );
  }

  /// JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'url': url,
    };
  }

  /// Domain Entity로 변환
  InspectionPhoto toEntity() {
    return InspectionPhoto(
      id: id,
      url: url,
    );
  }

  /// Domain Entity에서 생성
  factory InspectionPhotoModel.fromEntity(InspectionPhoto entity) {
    return InspectionPhotoModel(
      id: entity.id,
      url: entity.url,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is InspectionPhotoModel &&
        other.id == id &&
        other.url == url;
  }

  @override
  int get hashCode => Object.hash(id, url);

  @override
  String toString() {
    return 'InspectionPhotoModel(id: $id, url: $url)';
  }
}
