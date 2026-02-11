import 'inspection_list_item.dart';

/// 현장 점검 상세 엔티티
///
/// 현장 점검 상세 화면에서 표시되는 상세 정보를 담는 도메인 엔티티입니다.
class InspectionDetail {
  /// 점검 ID
  final int id;

  /// 분류 (자사/경쟁사)
  final InspectionCategory category;

  /// 거래처명
  final String storeName;

  /// 거래처 ID
  final int storeId;

  /// 테마명
  final String themeName;

  /// 테마 ID
  final int themeId;

  /// 점검일
  final DateTime inspectionDate;

  /// 현장 유형명
  final String fieldType;

  /// 현장 유형 코드
  final String fieldTypeCode;

  /// 설명 (자사)
  final String? description;

  /// 제품 코드 (자사)
  final String? productCode;

  /// 제품명 (자사)
  final String? productName;

  /// 경쟁사명 (경쟁사)
  final String? competitorName;

  /// 경쟁사 활동 내용 (경쟁사)
  final String? competitorActivity;

  /// 시식 여부 (경쟁사)
  final bool? competitorTasting;

  /// 경쟁사 상품명 (시식=예)
  final String? competitorProductName;

  /// 제품 가격 (시식=예)
  final int? competitorProductPrice;

  /// 판매 수량 (시식=예)
  final int? competitorSalesQuantity;

  /// 사진 목록
  final List<InspectionPhoto> photos;

  /// 등록 일시
  final DateTime createdAt;

  const InspectionDetail({
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

  /// 자사 점검 여부
  bool get isOwn => category == InspectionCategory.OWN;

  /// 경쟁사 점검 여부
  bool get isCompetitor => category == InspectionCategory.COMPETITOR;

  InspectionDetail copyWith({
    int? id,
    InspectionCategory? category,
    String? storeName,
    int? storeId,
    String? themeName,
    int? themeId,
    DateTime? inspectionDate,
    String? fieldType,
    String? fieldTypeCode,
    String? description,
    String? productCode,
    String? productName,
    String? competitorName,
    String? competitorActivity,
    bool? competitorTasting,
    String? competitorProductName,
    int? competitorProductPrice,
    int? competitorSalesQuantity,
    List<InspectionPhoto>? photos,
    DateTime? createdAt,
  }) {
    return InspectionDetail(
      id: id ?? this.id,
      category: category ?? this.category,
      storeName: storeName ?? this.storeName,
      storeId: storeId ?? this.storeId,
      themeName: themeName ?? this.themeName,
      themeId: themeId ?? this.themeId,
      inspectionDate: inspectionDate ?? this.inspectionDate,
      fieldType: fieldType ?? this.fieldType,
      fieldTypeCode: fieldTypeCode ?? this.fieldTypeCode,
      description: description ?? this.description,
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      competitorName: competitorName ?? this.competitorName,
      competitorActivity: competitorActivity ?? this.competitorActivity,
      competitorTasting: competitorTasting ?? this.competitorTasting,
      competitorProductName:
          competitorProductName ?? this.competitorProductName,
      competitorProductPrice:
          competitorProductPrice ?? this.competitorProductPrice,
      competitorSalesQuantity:
          competitorSalesQuantity ?? this.competitorSalesQuantity,
      photos: photos ?? this.photos,
      createdAt: createdAt ?? this.createdAt,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'category': category.toJson(),
      'storeName': storeName,
      'storeId': storeId,
      'themeName': themeName,
      'themeId': themeId,
      'inspectionDate': inspectionDate.toIso8601String().substring(0, 10),
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
      'createdAt': createdAt.toIso8601String(),
    };
  }

  factory InspectionDetail.fromJson(Map<String, dynamic> json) {
    return InspectionDetail(
      id: json['id'] as int,
      category: InspectionCategoryExtension.fromJson(json['category'] as String),
      storeName: json['storeName'] as String,
      storeId: json['storeId'] as int,
      themeName: json['themeName'] as String,
      themeId: json['themeId'] as int,
      inspectionDate: DateTime.parse(json['inspectionDate'] as String),
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
          .map((photo) => InspectionPhoto.fromJson(photo as Map<String, dynamic>))
          .toList(),
      createdAt: DateTime.parse(json['createdAt'] as String),
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionDetail) return false;
    return other.id == id &&
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
    return 'InspectionDetail(id: $id, category: $category, '
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

/// 현장 점검 사진 엔티티
class InspectionPhoto {
  /// 사진 ID
  final int id;

  /// 사진 URL
  final String url;

  const InspectionPhoto({
    required this.id,
    required this.url,
  });

  InspectionPhoto copyWith({
    int? id,
    String? url,
  }) {
    return InspectionPhoto(
      id: id ?? this.id,
      url: url ?? this.url,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'url': url,
    };
  }

  factory InspectionPhoto.fromJson(Map<String, dynamic> json) {
    return InspectionPhoto(
      id: json['id'] as int,
      url: json['url'] as String,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! InspectionPhoto) return false;
    return other.id == id && other.url == url;
  }

  @override
  int get hashCode => Object.hash(id, url);

  @override
  String toString() {
    return 'InspectionPhoto(id: $id, url: $url)';
  }
}
