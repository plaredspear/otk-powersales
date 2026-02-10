/// 제품 엔티티
///
/// 제품검색 결과에 표시되는 제품 정보를 담는 도메인 엔티티입니다.
class Product {
  /// 제품 ID (고유 식별자)
  final String productId;

  /// 제품명
  final String productName;

  /// 제품코드
  final String productCode;

  /// 바코드
  final String barcode;

  /// 보관 조건 (냉장/냉동/상온)
  final String storageType;

  /// 유통기한 기간
  final String shelfLife;

  /// 중분류 카테고리
  final String? categoryMid;

  /// 소분류 카테고리
  final String? categorySub;

  const Product({
    required this.productId,
    required this.productName,
    required this.productCode,
    required this.barcode,
    required this.storageType,
    required this.shelfLife,
    this.categoryMid,
    this.categorySub,
  });

  Product copyWith({
    String? productId,
    String? productName,
    String? productCode,
    String? barcode,
    String? storageType,
    String? shelfLife,
    String? categoryMid,
    String? categorySub,
  }) {
    return Product(
      productId: productId ?? this.productId,
      productName: productName ?? this.productName,
      productCode: productCode ?? this.productCode,
      barcode: barcode ?? this.barcode,
      storageType: storageType ?? this.storageType,
      shelfLife: shelfLife ?? this.shelfLife,
      categoryMid: categoryMid ?? this.categoryMid,
      categorySub: categorySub ?? this.categorySub,
    );
  }

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

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
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

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is Product &&
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
    return 'Product(productId: $productId, productName: $productName, '
        'productCode: $productCode, barcode: $barcode, '
        'storageType: $storageType, shelfLife: $shelfLife, '
        'categoryMid: $categoryMid, categorySub: $categorySub)';
  }
}
