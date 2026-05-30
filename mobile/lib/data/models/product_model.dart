import '../../domain/entities/product.dart';
import '../../domain/repositories/product_repository.dart';

/// 제품 API 모델 (DTO)
///
/// 백엔드 ProductDto(`/api/v1/mobile/products/search`) 응답을 Product 엔티티로
/// 변환한다. 백엔드 필드명(logisticsBarcode/storageCondition/category1·2)을
/// 엔티티 필드명(barcode/storageType/categoryMid·Sub)으로 매핑한다.
///
/// 백엔드는 productId 를 별도로 제공하지 않으므로 productCode 를 식별자로 사용한다.
/// 모든 필드가 nullable 이므로 비-null 엔티티 필드는 빈 문자열로 보정한다.
class ProductModel {
  final String? productCode;
  final String? productName;
  final String? logisticsBarcode;
  final String? storageCondition;
  final String? shelfLife;
  final String? category1;
  final String? category2;

  const ProductModel({
    this.productCode,
    this.productName,
    this.logisticsBarcode,
    this.storageCondition,
    this.shelfLife,
    this.category1,
    this.category2,
  });

  factory ProductModel.fromJson(Map<String, dynamic> json) {
    return ProductModel(
      productCode: json['productCode'] as String?,
      productName: json['productName'] as String?,
      logisticsBarcode: json['logisticsBarcode'] as String?,
      storageCondition: json['storageCondition'] as String?,
      shelfLife: json['shelfLife'] as String?,
      category1: json['category1'] as String?,
      category2: json['category2'] as String?,
    );
  }

  Product toEntity() {
    final code = productCode ?? '';
    return Product(
      productId: code,
      productName: productName ?? '',
      productCode: code,
      barcode: logisticsBarcode ?? '',
      storageType: storageCondition ?? '',
      shelfLife: shelfLife ?? '',
      categoryMid: category1,
      categorySub: category2,
    );
  }
}

/// 제품검색 페이지 API 모델 (Spring Data `Page<ProductDto>` 래퍼)
///
/// 백엔드는 Spring `Page` 직렬화 형태(content/totalElements/totalPages/number/
/// size/first/last)로 응답한다.
class ProductPageModel {
  final List<ProductModel> content;
  final int totalElements;
  final int totalPages;
  final int number; // 0부터 시작
  final int size;
  final bool first;
  final bool last;

  const ProductPageModel({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.number,
    required this.size,
    required this.first,
    required this.last,
  });

  factory ProductPageModel.fromJson(Map<String, dynamic> json) {
    return ProductPageModel(
      content: (json['content'] as List<dynamic>)
          .map((item) => ProductModel.fromJson(item as Map<String, dynamic>))
          .toList(),
      totalElements: (json['totalElements'] as num).toInt(),
      totalPages: (json['totalPages'] as num).toInt(),
      number: (json['number'] as num).toInt(),
      size: (json['size'] as num).toInt(),
      first: json['first'] as bool? ?? true,
      last: json['last'] as bool? ?? true,
    );
  }

  ProductSearchResult toEntity() {
    return ProductSearchResult(
      products: content.map((model) => model.toEntity()).toList(),
      totalElements: totalElements,
      totalPages: totalPages,
      currentPage: number,
      pageSize: size,
      isFirst: first,
      isLast: last,
    );
  }
}
