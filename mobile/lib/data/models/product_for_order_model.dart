import '../../domain/entities/product_for_order.dart';
import '../../domain/entities/product_search_result.dart';

/// 주문용 제품 정보 API 모델 (DTO)
///
/// Backend `OrderProductDto` 의 camelCase JSON을 파싱하여 ProductForOrder 엔티티로 변환합니다.
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

  /// 전용상품 차단 판정값 (`'EXCLUSIVE'` 이면 차단). 레거시 `producttype__c == '2'` 매핑.
  final String? productType;

  /// 시식·증정용 차단 판정값 (`'TASTING_GIFT'` 이면 차단). 레거시 `tastegift__c == 'x'/'X'` 매핑.
  final String? tasteGiftType;

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
    this.productType,
    this.tasteGiftType,
  });

  /// camelCase JSON에서 파싱 (null-safe — 누락 필드는 기본값으로 방어).
  factory ProductForOrderModel.fromJson(Map<String, dynamic> json) {
    return ProductForOrderModel(
      productCode: json['productCode'] as String? ?? '',
      productName: json['productName'] as String? ?? '',
      barcode: json['barcode'] as String? ?? '',
      storageType: json['storageType'] as String? ?? '',
      shelfLife: json['shelfLife'] as String? ?? '',
      unitPrice: (json['unitPrice'] as num?)?.toInt() ?? 0,
      boxSize: (json['boxSize'] as num?)?.toInt() ?? 0,
      isFavorite: json['isFavorite'] as bool? ?? false,
      categoryMid: json['categoryMid'] as String?,
      categorySub: json['categorySub'] as String?,
      productType: json['productType'] as String?,
      tasteGiftType: json['tasteGiftType'] as String?,
    );
  }

  /// camelCase JSON으로 직렬화
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
      'productType': productType,
      'tasteGiftType': tasteGiftType,
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
      productType: productType,
      tasteGiftType: tasteGiftType,
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
      productType: entity.productType,
      tasteGiftType: entity.tasteGiftType,
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
        other.categorySub == categorySub &&
        other.productType == productType &&
        other.tasteGiftType == tasteGiftType;
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
      productType,
      tasteGiftType,
    );
  }

  @override
  String toString() {
    return 'ProductForOrderModel(productCode: $productCode, '
        'productName: $productName, barcode: $barcode, '
        'storageType: $storageType, shelfLife: $shelfLife, '
        'unitPrice: $unitPrice, boxSize: $boxSize, '
        'isFavorite: $isFavorite, categoryMid: $categoryMid, '
        'categorySub: $categorySub, productType: $productType, '
        'tasteGiftType: $tasteGiftType)';
  }
}

/// 주문서 제품검색 1회 조회 상한.
///
/// 주문서 제품검색은 페이징 UI 가 없어 이 건수까지만 노출한다(backend
/// `ProductService.MAX_PAGE_SIZE` 와 동일 값 — 초과 시 서버가 400 을 반환하므로
/// 양쪽을 함께 올려야 한다). 초과분은 "검색어를 좁혀 달라" 안내로 유도한다.
const int orderProductSearchPageLimit = 200;

/// 주문용 제품 검색 결과 API 모델 — 목록 + 서버 집계 전체 건수.
class ProductSearchResultModel {
  final List<ProductForOrderModel> products;

  /// 검색 조건에 매칭되는 전체 건수 (목록 길이와 다를 수 있음)
  final int totalCount;

  const ProductSearchResultModel({
    required this.products,
    required this.totalCount,
  });

  ProductSearchResult toEntity() {
    return ProductSearchResult(
      products: products.map((m) => m.toEntity()).toList(),
      totalCount: totalCount,
      pageLimit: orderProductSearchPageLimit,
    );
  }
}
