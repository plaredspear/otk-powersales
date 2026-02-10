/// 주문용 제품 정보 엔티티
///
/// 주문서 작성 시 제품 추가 화면에서 사용되는 제품 정보입니다.
/// 기존 Product 엔티티에 단가, 박스 사이즈, 즐겨찾기 여부 등
/// 주문 관련 추가 필드를 포함합니다.
class ProductForOrder {
  /// 제품 코드
  final String productCode;

  /// 제품명
  final String productName;

  /// 바코드
  final String barcode;

  /// 보관 조건 (냉장/냉동/상온)
  final String storageType;

  /// 유통기한 기간
  final String shelfLife;

  /// 제품 단가 (원)
  final int unitPrice;

  /// 1박스당 개수
  final int boxSize;

  /// 즐겨찾기 여부
  final bool isFavorite;

  /// 중분류 카테고리
  final String? categoryMid;

  /// 소분류 카테고리
  final String? categorySub;

  const ProductForOrder({
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

  /// 보관 조건 아이콘 이모지
  String get storageTypeIcon {
    switch (storageType) {
      case '냉장':
        return '🧊';
      case '냉동':
        return '❄️';
      case '상온':
        return '🌡️';
      default:
        return '';
    }
  }

  ProductForOrder copyWith({
    String? productCode,
    String? productName,
    String? barcode,
    String? storageType,
    String? shelfLife,
    int? unitPrice,
    int? boxSize,
    bool? isFavorite,
    String? categoryMid,
    String? categorySub,
  }) {
    return ProductForOrder(
      productCode: productCode ?? this.productCode,
      productName: productName ?? this.productName,
      barcode: barcode ?? this.barcode,
      storageType: storageType ?? this.storageType,
      shelfLife: shelfLife ?? this.shelfLife,
      unitPrice: unitPrice ?? this.unitPrice,
      boxSize: boxSize ?? this.boxSize,
      isFavorite: isFavorite ?? this.isFavorite,
      categoryMid: categoryMid ?? this.categoryMid,
      categorySub: categorySub ?? this.categorySub,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'productCode': productCode,
      'productName': productName,
      'barcode': barcode,
      'storageType': storageType,
      'shelfLife': shelfLife,
      'unitPrice': unitPrice,
      'boxSize': boxSize,
      'isFavorite': isFavorite,
      'categoryMid': categoryMid,
      'categorySub': categorySub,
    };
  }

  factory ProductForOrder.fromJson(Map<String, dynamic> json) {
    return ProductForOrder(
      productCode: json['productCode'] as String,
      productName: json['productName'] as String,
      barcode: json['barcode'] as String,
      storageType: json['storageType'] as String,
      shelfLife: json['shelfLife'] as String,
      unitPrice: json['unitPrice'] as int,
      boxSize: json['boxSize'] as int,
      isFavorite: json['isFavorite'] as bool,
      categoryMid: json['categoryMid'] as String?,
      categorySub: json['categorySub'] as String?,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is ProductForOrder &&
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
    return 'ProductForOrder(productCode: $productCode, '
        'productName: $productName, barcode: $barcode, '
        'storageType: $storageType, shelfLife: $shelfLife, '
        'unitPrice: $unitPrice, boxSize: $boxSize, '
        'isFavorite: $isFavorite)';
  }
}
