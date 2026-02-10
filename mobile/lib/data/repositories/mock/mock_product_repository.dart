import 'dart:math';

import '../../../domain/entities/product.dart';
import '../../../domain/repositories/product_repository.dart';

/// 제품검색 Mock Repository
///
/// Backend API 개발 전 프론트엔드 개발을 위한 Mock 데이터 제공.
/// Flutter-First 전략에 따라 하드코딩된 데이터로 UI/UX 검증.
class MockProductRepository implements ProductRepository {
  /// 네트워크 지연 시뮬레이션 (300ms)
  Future<void> _simulateDelay() async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  /// Mock 제품 데이터
  static final List<Product> _mockProducts = [
    const Product(
      productId: '18110014',
      productName: '열라면_용기105G',
      productCode: '18110014',
      barcode: '8801045570716',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '용기면',
    ),
    const Product(
      productId: '18110021',
      productName: '열라면_봉지120G',
      productCode: '18110021',
      barcode: '8801045570723',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '봉지면',
    ),
    const Product(
      productId: '18110038',
      productName: '열라면_멀티5입',
      productCode: '18110038',
      barcode: '8801045570730',
      storageType: '상온',
      shelfLife: '7개월',
      categoryMid: '라면',
      categorySub: '멀티팩',
    ),
    const Product(
      productId: '18120001',
      productName: '진라면_순한맛_용기65G',
      productCode: '18120001',
      barcode: '8801045571001',
      storageType: '상온',
      shelfLife: '6개월',
      categoryMid: '라면',
      categorySub: '용기면',
    ),
    const Product(
      productId: '18120002',
      productName: '진라면_매운맛_용기65G',
      productCode: '18120002',
      barcode: '8801045571002',
      storageType: '상온',
      shelfLife: '6개월',
      categoryMid: '라면',
      categorySub: '용기면',
    ),
    const Product(
      productId: '18120003',
      productName: '진라면_순한맛_봉지120G',
      productCode: '18120003',
      barcode: '8801045571003',
      storageType: '상온',
      shelfLife: '6개월',
      categoryMid: '라면',
      categorySub: '봉지면',
    ),
    const Product(
      productId: '18130001',
      productName: '뿌셔뿌셔_불고기맛60G',
      productCode: '18130001',
      barcode: '8801045572001',
      storageType: '상온',
      shelfLife: '8개월',
      categoryMid: '스낵',
      categorySub: '스낵',
    ),
    const Product(
      productId: '18130002',
      productName: '뿌셔뿌셔_양념치킨맛60G',
      productCode: '18130002',
      barcode: '8801045572002',
      storageType: '상온',
      shelfLife: '8개월',
      categoryMid: '스낵',
      categorySub: '스낵',
    ),
    const Product(
      productId: '18140001',
      productName: '3분카레_순한맛200G',
      productCode: '18140001',
      barcode: '8801045573001',
      storageType: '상온',
      shelfLife: '24개월',
      categoryMid: '레토르트',
      categorySub: '카레',
    ),
    const Product(
      productId: '18140002',
      productName: '3분카레_매운맛200G',
      productCode: '18140002',
      barcode: '8801045573002',
      storageType: '상온',
      shelfLife: '24개월',
      categoryMid: '레토르트',
      categorySub: '카레',
    ),
    const Product(
      productId: '18140003',
      productName: '3분짜장200G',
      productCode: '18140003',
      barcode: '8801045573003',
      storageType: '상온',
      shelfLife: '24개월',
      categoryMid: '레토르트',
      categorySub: '짜장',
    ),
    const Product(
      productId: '18150001',
      productName: '오뚜기_참기름320ML',
      productCode: '18150001',
      barcode: '8801045574001',
      storageType: '상온',
      shelfLife: '18개월',
      categoryMid: '조미료',
      categorySub: '참기름',
    ),
    const Product(
      productId: '18150002',
      productName: '오뚜기_마요네즈500G',
      productCode: '18150002',
      barcode: '8801045574002',
      storageType: '냉장',
      shelfLife: '7개월',
      categoryMid: '소스',
      categorySub: '마요네즈',
    ),
    const Product(
      productId: '18150003',
      productName: '오뚜기_케첩300G',
      productCode: '18150003',
      barcode: '8801045574003',
      storageType: '냉장',
      shelfLife: '9개월',
      categoryMid: '소스',
      categorySub: '케첩',
    ),
    const Product(
      productId: '18160001',
      productName: '진짬뽕_용기115G',
      productCode: '18160001',
      barcode: '8801045575001',
      storageType: '상온',
      shelfLife: '6개월',
      categoryMid: '라면',
      categorySub: '용기면',
    ),
    const Product(
      productId: '18160002',
      productName: '진짬뽕_봉지130G',
      productCode: '18160002',
      barcode: '8801045575002',
      storageType: '상온',
      shelfLife: '6개월',
      categoryMid: '라면',
      categorySub: '봉지면',
    ),
    const Product(
      productId: '18170001',
      productName: '오뚜기밥_발아현미210G',
      productCode: '18170001',
      barcode: '8801045576001',
      storageType: '상온',
      shelfLife: '12개월',
      categoryMid: '즉석밥',
      categorySub: '즉석밥',
    ),
    const Product(
      productId: '18170002',
      productName: '오뚜기밥_오곡210G',
      productCode: '18170002',
      barcode: '8801045576002',
      storageType: '상온',
      shelfLife: '12개월',
      categoryMid: '즉석밥',
      categorySub: '즉석밥',
    ),
    const Product(
      productId: '18180001',
      productName: '컵누들_매콤한맛37.8G',
      productCode: '18180001',
      barcode: '8801045577001',
      storageType: '상온',
      shelfLife: '8개월',
      categoryMid: '라면',
      categorySub: '컵라면',
    ),
    const Product(
      productId: '18180002',
      productName: '컵누들_우동맛38.1G',
      productCode: '18180002',
      barcode: '8801045577002',
      storageType: '상온',
      shelfLife: '8개월',
      categoryMid: '라면',
      categorySub: '컵라면',
    ),
    const Product(
      productId: '18190001',
      productName: '오뚜기_냉동볶음밥_김치300G',
      productCode: '18190001',
      barcode: '8801045578001',
      storageType: '냉동',
      shelfLife: '12개월',
      categoryMid: '냉동식품',
      categorySub: '볶음밥',
    ),
    const Product(
      productId: '18190002',
      productName: '오뚜기_냉동볶음밥_새우300G',
      productCode: '18190002',
      barcode: '8801045578002',
      storageType: '냉동',
      shelfLife: '12개월',
      categoryMid: '냉동식품',
      categorySub: '볶음밥',
    ),
    const Product(
      productId: '18200001',
      productName: '오뚜기_XO교자만두_고기350G',
      productCode: '18200001',
      barcode: '8801045579001',
      storageType: '냉동',
      shelfLife: '9개월',
      categoryMid: '냉동식품',
      categorySub: '만두',
    ),
    const Product(
      productId: '18200002',
      productName: '오뚜기_XO교자만두_김치350G',
      productCode: '18200002',
      barcode: '8801045579002',
      storageType: '냉동',
      shelfLife: '9개월',
      categoryMid: '냉동식품',
      categorySub: '만두',
    ),
    const Product(
      productId: '18210001',
      productName: '오뚜기_황금카레_약간매운맛100G',
      productCode: '18210001',
      barcode: '8801045580001',
      storageType: '상온',
      shelfLife: '18개월',
      categoryMid: '조미료',
      categorySub: '카레가루',
    ),
  ];

  @override
  Future<ProductSearchResult> searchProducts({
    required String query,
    String type = 'text',
    int page = 0,
    int size = 20,
  }) async {
    await _simulateDelay();

    // 검색어 최소 길이 검증
    if (type == 'text' && query.length < 2) {
      throw ArgumentError('검색어를 2자 이상 입력해주세요');
    }

    // 검색 필터링
    final lowerQuery = query.toLowerCase();
    List<Product> filtered;

    if (type == 'barcode') {
      // 바코드 검색: 정확 일치
      filtered = _mockProducts
          .where((p) => p.barcode == query)
          .toList();
    } else {
      // 텍스트 검색: 제품명, 제품코드, 바코드 부분 일치
      filtered = _mockProducts.where((p) {
        return p.productName.toLowerCase().contains(lowerQuery) ||
            p.productCode.toLowerCase().contains(lowerQuery) ||
            p.barcode.contains(lowerQuery);
      }).toList();
    }

    // 제품명 기준 가나다순 정렬
    filtered.sort((a, b) => a.productName.compareTo(b.productName));

    // 페이지네이션 적용
    final totalElements = filtered.length;
    final totalPages = (totalElements / size).ceil();
    final startIndex = page * size;
    final endIndex = min(startIndex + size, totalElements);

    final pagedProducts = startIndex < totalElements
        ? filtered.sublist(startIndex, endIndex)
        : <Product>[];

    return ProductSearchResult(
      products: pagedProducts,
      totalElements: totalElements,
      totalPages: totalPages == 0 ? 1 : totalPages,
      currentPage: page,
      pageSize: size,
      isFirst: page == 0,
      isLast: page >= totalPages - 1 || totalPages == 0,
    );
  }
}
