import '../../domain/entities/product_for_order.dart';

/// 주문용 제품 정보 API 모델 (DTO)
///
/// Backend API의 snake_case JSON을 파싱하여 ProductForOrder 엔티티로 변환합니다.
class ProductForOrderModel {
  final String productCode;
  final String productName;
  final String barcode;
  final String storageType;
  final String shelfLife;
  final int unitPrice;
  final int boxSize;
  final bool isFavorite;
  final String? categoryMid;
  final String? categorySub;

  const ProductForOrderModel({
    required this.productCode,
    required this.productName,
    required this.barcode,
    required this.storageType,
    required this.shelfLife,
    required this.unitPrice,
    required this.boxSize,
    required this.isFavorite,
    this.categoryMid,
    this.categorySub,
  });

  /// snake_case JSON에서 파싱
  factory ProductForOrderModel.fromJson(Map<String, dynamic> json) {
    return ProductForOrderModel(
      productCode: json['product_code'] as String,
      productName: json['product_name'] as String,
      barcode: json['barcode'] as String,
      storageType: json['storage_type'] as String,
      shelfLife: json['shelf_life'] as String,
      unitPrice: json['unit_price'] as int,
      boxSize: json['box_size'] as int,
      isFavorite: json['is_favorite'] as bool,
      categoryMid: json['category_mid'] as String?,
      categorySub: json['category_sub'] as String?,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'product_code': productCode,
      'product_name': productName,
      'barcode': barcode,
      'storage_type': storageType,
      'shelf_life': shelfLife,
      'unit_price': unitPrice,
      'box_size': boxSize,
      'is_favorite': isFavorite,
      'category_mid': categoryMid,
      'category_sub': categorySub,
    };
  }

  /// Domain Entity로 변환
  ProductForOrder toEntity() {
    return ProductForOrder(
      productCode: productCode,
      productName: productName,
      barcode: barcode,
      storageType: storageType,
      shelfLife: shelfLife,
      unitPrice: unitPrice,
      boxSize: boxSize,
      isFavorite: isFavorite,
      categoryMid: categoryMid,
      categorySub: categorySub,
    );
  }

  /// Domain Entity에서 생성
  factory ProductForOrderModel.fromEntity(ProductForOrder entity) {
    return ProductForOrderModel(
      productCode: entity.productCode,
      productName: entity.productName,
      barcode: entity.barcode,
      storageType: entity.storageType,
      shelfLife: entity.shelfLife,
      unitPrice: entity.unitPrice,
      boxSize: entity.boxSize,
      isFavorite: entity.isFavorite,
      categoryMid: entity.categoryMid,
      categorySub: entity.categorySub,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ProductForOrderModel &&
        other.productCode == productCode &&
        other.productName == productName &&
        other.barcode == barcode &&
        other.storageType == storageType &&
        other.shelfLife == shelfLife &&
        other.unitPrice == unitPrice &&
        other.boxSize == boxSize &&
        other.isFavorite == isFavorite &&
        other.categoryMid == categoryMid &&
        other.categorySub == categorySub;
  }

  @override
  int get hashCode {
    return Object.hash(
      productCode,
      productName,
      barcode,
      storageType,
      shelfLife,
      unitPrice,
      boxSize,
      isFavorite,
      categoryMid,
      categorySub,
    );
  }

  @override
  String toString() {
    return 'ProductForOrderModel(productCode: $productCode, '
        'productName: $productName, barcode: $barcode, '
        'storageType: $storageType, shelfLife: $shelfLife, '
        'unitPrice: $unitPrice, boxSize: $boxSize, '
        'isFavorite: $isFavorite, categoryMid: $categoryMid, '
        'categorySub: $categorySub)';
  }
}
