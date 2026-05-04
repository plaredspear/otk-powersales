import '../../domain/entities/product.dart';

/// 제품 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 Product 엔티티로 변환합니다.
class ProductModel {
  final String productId;
  final String productName;
  final String productCode;
  final String barcode;
  final String storageType;
  final String shelfLife;
  final String? categoryMid;
  final String? categorySub;

  const ProductModel({
    required this.productId,
    required this.productName,
    required this.productCode,
    required this.barcode,
    required this.storageType,
    required this.shelfLife,
    this.categoryMid,
    this.categorySub,
  });

  /// snake_case JSON에서 파싱
  factory ProductModel.fromJson(Map<String, dynamic> json) {
    return ProductModel(
      productId: json['productId'] as String,
      productName: json['productName'] as String,
      productCode: json['productCode'] as String,
      barcode: json['barcode'] as String,
      storageType: json['storageType'] as String,
      shelfLife: json['shelfLife'] as String,
      categoryMid: json['categoryMid'] as String?,
      categorySub: json['categorySub'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'productId': productId,
      'productName': productName,
      'productCode': productCode,
      'barcode': barcode,
      'storageType': storageType,
      'shelfLife': shelfLife,
      'categoryMid': categoryMid,
      'categorySub': categorySub,
    };
  }

  /// Domain Entity로 변환
  Product toEntity() {
    return Product(
      productId: productId,
      productName: productName,
      productCode: productCode,
      barcode: barcode,
      storageType: storageType,
      shelfLife: shelfLife,
      categoryMid: categoryMid,
      categorySub: categorySub,
    );
  }

  /// Domain Entity에서 생성
  factory ProductModel.fromEntity(Product entity) {
    return ProductModel(
      productId: entity.productId,
      productName: entity.productName,
      productCode: entity.productCode,
      barcode: entity.barcode,
      storageType: entity.storageType,
      shelfLife: entity.shelfLife,
      categoryMid: entity.categoryMid,
      categorySub: entity.categorySub,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ProductModel &&
        other.productId == productId &&
        other.productName == productName &&
        other.productCode == productCode &&
        other.barcode == barcode &&
        other.storageType == storageType &&
        other.shelfLife == shelfLife &&
        other.categoryMid == categoryMid &&
        other.categorySub == categorySub;
  }

  @override
  int get hashCode {
    return Object.hash(
      productId,
      productName,
      productCode,
      barcode,
      storageType,
      shelfLife,
      categoryMid,
      categorySub,
    );
  }

  @override
  String toString() {
    return 'ProductModel(productId: $productId, productName: $productName, '
        'productCode: $productCode, barcode: $barcode, '
        'storageType: $storageType, shelfLife: $shelfLife, '
        'categoryMid: $categoryMid, categorySub: $categorySub)';
  }
}
