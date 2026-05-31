/// 제품 상세 엔티티
///
/// 제품 상세 화면(레거시 `product/search/detail.jsp` 대응)에 표시되는
/// 단건 제품의 전체 정보를 담습니다. 검색 결과(Product)보다 많은 필드를 가집니다.
/// 백엔드 ProductDetailResponse 의 모든 필드가 nullable 이므로 동일하게 처리합니다.
class ProductDetail {
  final String? productCode;
  final String? productName;
  final String? category1;
  final String? category2;
  final String? category3;
  final String? unit;

  /// 출시일 (ISO 날짜 문자열, 예: "2024-01-15")
  final String? launchDate;
  final String? shelfLife;
  final String? shelfLifeUnit;
  final String? storageCondition;
  final String? barcode;

  /// 박스 입수 수량
  final double? boxReceivingQuantity;

  /// 출고가 (원)
  final double? standardUnitPrice;
  final String? sellingPoint;
  final String? purpose;
  final String? targetAccountType;
  final String? allergen;
  final String? crossContamination;
  final String? frontImageUrl;
  final String? backImageUrl;

  const ProductDetail({
    this.productCode,
    this.productName,
    this.category1,
    this.category2,
    this.category3,
    this.unit,
    this.launchDate,
    this.shelfLife,
    this.shelfLifeUnit,
    this.storageCondition,
    this.barcode,
    this.boxReceivingQuantity,
    this.standardUnitPrice,
    this.sellingPoint,
    this.purpose,
    this.targetAccountType,
    this.allergen,
    this.crossContamination,
    this.frontImageUrl,
    this.backImageUrl,
  });

  /// 이미지 보유 여부
  bool get hasImages => frontImageUrl != null || backImageUrl != null;

  /// 품목 그룹명 경로 (category1 / category2 / category3, null 항목은 제외)
  String get categoryPath =>
      [category1, category2, category3].whereType<String>().join(' / ');

  /// 유통기한 표시 문자열 (보관조건 + 기간 + 단위), 정보 없으면 빈 문자열
  String get shelfLifeDisplay {
    final parts = <String>[
      if (storageCondition != null && storageCondition!.isNotEmpty)
        storageCondition!,
      if (shelfLife != null && shelfLife!.isNotEmpty)
        '$shelfLife${shelfLifeUnit ?? ''}',
    ];
    return parts.join(' ');
  }

  ProductDetail copyWith({
    String? productCode,
    String? productName,
    String? category1,
    String? category2,
    String? category3,
    String? unit,
    String? launchDate,
    String? shelfLife,
    String? shelfLifeUnit,
    String? storageCondition,
    String? barcode,
    double? boxReceivingQuantity,
    double? standardUnitPrice,
    String? sellingPoint,
    String? purpose,
    String? targetAccountType,
    String? allergen,
    String? crossContamination,
    String? frontImageUrl,
    String? backImageUrl,
  }) {
    return ProductDetail(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      category1: category1 ?? this.category1,
      category2: category2 ?? this.category2,
      category3: category3 ?? this.category3,
      unit: unit ?? this.unit,
      launchDate: launchDate ?? this.launchDate,
      shelfLife: shelfLife ?? this.shelfLife,
      shelfLifeUnit: shelfLifeUnit ?? this.shelfLifeUnit,
      storageCondition: storageCondition ?? this.storageCondition,
      barcode: barcode ?? this.barcode,
      boxReceivingQuantity: boxReceivingQuantity ?? this.boxReceivingQuantity,
      standardUnitPrice: standardUnitPrice ?? this.standardUnitPrice,
      sellingPoint: sellingPoint ?? this.sellingPoint,
      purpose: purpose ?? this.purpose,
      targetAccountType: targetAccountType ?? this.targetAccountType,
      allergen: allergen ?? this.allergen,
      crossContamination: crossContamination ?? this.crossContamination,
      frontImageUrl: frontImageUrl ?? this.frontImageUrl,
      backImageUrl: backImageUrl ?? this.backImageUrl,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ProductDetail &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.category1 == category1 &&
        other.category2 == category2 &&
        other.category3 == category3 &&
        other.unit == unit &&
        other.launchDate == launchDate &&
        other.shelfLife == shelfLife &&
        other.shelfLifeUnit == shelfLifeUnit &&
        other.storageCondition == storageCondition &&
        other.barcode == barcode &&
        other.boxReceivingQuantity == boxReceivingQuantity &&
        other.standardUnitPrice == standardUnitPrice &&
        other.sellingPoint == sellingPoint &&
        other.purpose == purpose &&
        other.targetAccountType == targetAccountType &&
        other.allergen == allergen &&
        other.crossContamination == crossContamination &&
        other.frontImageUrl == frontImageUrl &&
        other.backImageUrl == backImageUrl;
  }

  @override
  int get hashCode => Object.hashAll([
        productCode,
        productName,
        category1,
        category2,
        category3,
        unit,
        launchDate,
        shelfLife,
        shelfLifeUnit,
        storageCondition,
        barcode,
        boxReceivingQuantity,
        standardUnitPrice,
        sellingPoint,
        purpose,
        targetAccountType,
        allergen,
        crossContamination,
        frontImageUrl,
        backImageUrl,
      ]);
}
