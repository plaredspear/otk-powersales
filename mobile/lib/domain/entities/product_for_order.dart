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

  /// 소비기한 기간
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

  /// 제품 유형 (Spec #598 P3-M §2.1 — 차단 룰).
  /// `'EXCLUSIVE'` 면 전용상품으로 추가 차단. 그 외는 통과. 레거시 `producttype__c=='2'` 매핑.
  final String? productType;

  /// 시식·증정용 여부 (Spec #598 P3-M §2.1 — 차단 룰).
  /// `'TASTING_GIFT'` 면 시식·증정으로 추가 차단. 레거시 `tastegift__c='x'/'X'` 매핑.
  final String? tasteGiftType;

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
    this.productType,
    this.tasteGiftType,
  });

  /// 전용상품 차단 예외 제품코드 — 옛날_구수한끓여먹는누룽지 450g (레거시 poplayer.js 하드코딩 정합).
  /// 전용상품이지만 현업 요청으로 주문이 허용된 제품.
  static const String exclusiveBlockExemptCode = '20010042';

  /// 전용상품 여부 (`productType == 'EXCLUSIVE'`).
  bool get isExclusive => productType == 'EXCLUSIVE';

  /// 주문서 추가가 차단되는 전용상품 여부.
  /// 전용상품이면서 예외 코드([exclusiveBlockExemptCode])가 아닌 경우 true.
  bool get isExclusiveBlocked =>
      isExclusive && productCode != exclusiveBlockExemptCode;

  /// 시식·증정용 여부 (`tasteGiftType == 'TASTING_GIFT'`).
  bool get isTastingGift => tasteGiftType == 'TASTING_GIFT';

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
    String? productType,
    String? tasteGiftType,
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
      productType: productType ?? this.productType,
      tasteGiftType: tasteGiftType ?? this.tasteGiftType,
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
