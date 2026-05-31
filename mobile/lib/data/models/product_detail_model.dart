import '../../domain/entities/product_detail.dart';

/// 제품 상세 API 모델 (DTO)
///
/// 백엔드 ProductDetailResponse JSON 을 Domain Entity 로 변환한다.
class ProductDetailModel {
  final String? productCode;
  final String? productName;
  final String? category1;
  final String? category2;
  final String? category3;
  final String? unit;
  final String? launchDate;
  final String? shelfLife;
  final String? shelfLifeUnit;
  final String? storageCondition;
  final String? barcode;
  final double? boxReceivingQuantity;
  final double? standardUnitPrice;
  final String? sellingPoint;
  final String? purpose;
  final String? targetAccountType;
  final String? allergen;
  final String? crossContamination;
  final String? frontImageUrl;
  final String? backImageUrl;

  const ProductDetailModel({
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

  /// 백엔드 공용 ProductDetail DTO 와 정합.
  /// - 제품명 키는 `name`
  /// - 이미지 URL 은 결합 완료된 `imgRefPathFront`/`imgRefPathBack`
  /// - 바코드는 검색 결과 카드와의 일관성을 위해 `logisticsBarcode` 사용
  factory ProductDetailModel.fromJson(Map<String, dynamic> json) {
    return ProductDetailModel(
      productCode: json['productCode'] as String?,
      productName: json['name'] as String?,
      category1: json['category1'] as String?,
      category2: json['category2'] as String?,
      category3: json['category3'] as String?,
      unit: json['unit'] as String?,
      launchDate: json['launchDate'] as String?,
      shelfLife: json['shelfLife'] as String?,
      shelfLifeUnit: json['shelfLifeUnit'] as String?,
      storageCondition: json['storageCondition'] as String?,
      barcode: json['logisticsBarcode'] as String?,
      boxReceivingQuantity: (json['boxReceivingQuantity'] as num?)?.toDouble(),
      standardUnitPrice: (json['standardUnitPrice'] as num?)?.toDouble(),
      sellingPoint: json['sellingPoint'] as String?,
      purpose: json['purpose'] as String?,
      targetAccountType: json['targetAccountType'] as String?,
      allergen: json['allergen'] as String?,
      crossContamination: json['crossContamination'] as String?,
      frontImageUrl: json['imgRefPathFront'] as String?,
      backImageUrl: json['imgRefPathBack'] as String?,
    );
  }

  ProductDetail toEntity() {
    return ProductDetail(
      productCode: productCode,
      productName: productName,
      category1: category1,
      category2: category2,
      category3: category3,
      unit: unit,
      launchDate: launchDate,
      shelfLife: shelfLife,
      shelfLifeUnit: shelfLifeUnit,
      storageCondition: storageCondition,
      barcode: barcode,
      boxReceivingQuantity: boxReceivingQuantity,
      standardUnitPrice: standardUnitPrice,
      sellingPoint: sellingPoint,
      purpose: purpose,
      targetAccountType: targetAccountType,
      allergen: allergen,
      crossContamination: crossContamination,
      frontImageUrl: frontImageUrl,
      backImageUrl: backImageUrl,
    );
  }
}
