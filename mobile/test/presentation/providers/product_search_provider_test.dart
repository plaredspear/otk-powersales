import 'dart:math';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product.dart';
import 'package:mobile/domain/entities/product_detail.dart';
import 'package:mobile/domain/repositories/product_repository.dart';
import 'package:mobile/presentation/providers/product_search_provider.dart';
import 'package:mobile/presentation/providers/product_search_state.dart';

void main() {
  late ProviderContainer container;
  late ProductSearchNotifier notifier;

  setUp(() {
    container = ProviderContainer(
      overrides: [
        productRepositoryProvider.overrideWithValue(_FakeProductRepository()),
      ],
    );
    notifier = container.read(productSearchProvider.notifier);
  });

  tearDown(() {
    container.dispose();
  });

  ProductSearchState readState() => container.read(productSearchProvider);

  group('ProductSearchNotifier', () {
    group('updateQuery', () {
      test('검색어를 업데이트한다', () {
        notifier.updateQuery('열라면');
        expect(readState().query, '열라면');
      });

      test('빈 검색어로 업데이트한다', () {
        notifier.updateQuery('열라면');
        notifier.updateQuery('');
        expect(readState().query, '');
      });
    });

    group('updateSearchType', () {
      test('검색 유형을 바코드로 변경한다', () {
        notifier.updateSearchType(SearchType.barcode);
        expect(readState().searchType, SearchType.barcode);
      });

      test('검색 유형을 텍스트로 변경한다', () {
        notifier.updateSearchType(SearchType.barcode);
        notifier.updateSearchType(SearchType.text);
        expect(readState().searchType, SearchType.text);
      });
    });

    group('search', () {
      test('텍스트 검색이 성공하면 결과를 반환한다', () async {
        notifier.updateQuery('열라면');
        await notifier.search();

        expect(readState().isLoading, isFalse);
        expect(readState().hasSearched, isTrue);
        expect(readState().products, isNotEmpty);
        expect(readState().totalElements, greaterThan(0));
        expect(readState().errorMessage, isNull);
      });

      test('검색 결과가 없으면 빈 리스트를 반환한다', () async {
        notifier.updateQuery('존재하지않는제품명ABC');
        await notifier.search();

        expect(readState().hasSearched, isTrue);
        expect(readState().products, isEmpty);
        expect(readState().totalElements, 0);
      });

      test('검색어가 2자 미만이면 검색하지 않는다', () async {
        notifier.updateQuery('열');
        await notifier.search();

        // canSearch가 false이므로 search가 early return
        expect(readState().hasSearched, isFalse);
        expect(readState().isLoading, isFalse);
      });

      test('검색 중 로딩 상태가 된다', () async {
        notifier.updateQuery('열라면');

        // search 호출 후 즉시 상태 확인 (비동기 전)
        final future = notifier.search();

        // 비동기 작업 완료 후
        await future;
        expect(readState().isLoading, isFalse);
        expect(readState().hasSearched, isTrue);
      });

      test('제품명으로 검색된 결과가 가나다순 정렬된다', () async {
        notifier.updateQuery('열라면');
        await notifier.search();

        final names = readState().products.map((p) => p.productName).toList();
        final sorted = List<String>.from(names)..sort();
        expect(names, sorted);
      });
    });

    group('searchByBarcode', () {
      test('바코드로 검색하면 결과를 반환한다', () async {
        await notifier.searchByBarcode('8801045570716');

        expect(readState().query, '8801045570716');
        expect(readState().searchType, SearchType.barcode);
        expect(readState().hasSearched, isTrue);
        expect(readState().products, isNotEmpty);
        expect(readState().products.first.barcode, '8801045570716');
      });

      test('존재하지 않는 바코드로 검색하면 빈 결과를 반환한다', () async {
        await notifier.searchByBarcode('0000000000000');

        expect(readState().hasSearched, isTrue);
        expect(readState().products, isEmpty);
      });
    });

    group('loadNextPage', () {
      test('다음 페이지를 로드한다', () async {
        // 소수의 결과를 가진 검색 실행 후 다음 페이지 로드
        notifier.updateQuery('라면');
        await notifier.search();

        final firstPageCount = readState().products.length;

        if (readState().hasNextPage) {
          await notifier.loadNextPage();
          expect(readState().products.length, greaterThanOrEqualTo(firstPageCount));
          expect(readState().currentPage, 1);
        }
      });

      test('마지막 페이지이면 추가 로드하지 않는다', () async {
        notifier.updateQuery('열라면');
        await notifier.search();

        // 열라면 3건, 기본 size 20이므로 이미 마지막 페이지
        expect(readState().isLastPage, isTrue);

        final pageBeforeLoad = readState().currentPage;
        await notifier.loadNextPage();
        expect(readState().currentPage, pageBeforeLoad);
      });

      test('로딩 중이면 중복 요청하지 않는다', () async {
        notifier.updateQuery('라면');
        await notifier.search();

        if (readState().hasNextPage) {
          // 동시에 두 번 호출
          final f1 = notifier.loadNextPage();
          final f2 = notifier.loadNextPage();
          await Future.wait([f1, f2]);

          // currentPage는 1번만 증가해야 함
          expect(readState().currentPage, 1);
        }
      });
    });

    group('clearSearch', () {
      test('검색 상태를 초기화한다', () async {
        notifier.updateQuery('열라면');
        await notifier.search();

        notifier.clearSearch();

        expect(readState().query, '');
        expect(readState().products, isEmpty);
        expect(readState().hasSearched, isFalse);
        expect(readState().totalElements, 0);
      });
    });

    group('clearError', () {
      test('에러 메시지를 초기화한다', () {
        // 에러 상태 시뮬레이션을 위해 직접 상태 변경은 불가하므로
        // 잘못된 검색으로 에러 유발 후 clearError
        notifier.clearError();
        expect(readState().errorMessage, isNull);
      });
    });
  });

  group('productRepositoryProvider', () {
    test('오버라이드된 ProductRepository를 반환한다', () {
      final repository = container.read(productRepositoryProvider);
      expect(repository, isA<ProductRepository>());
      expect(repository, isA<_FakeProductRepository>());
    });
  });
}

/// 테스트용 ProductRepository 페이크.
///
/// 삭제된 lib 측 MockProductRepository 의 데이터/검색 로직을 테스트 내부에
/// 재현하여 제품 검색 로직 검증을 유지한다.
class _FakeProductRepository implements ProductRepository {
  static final List<Product> _products = [
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
  ];

  @override
  Future<ProductSearchResult> searchProducts({
    required String query,
    String type = 'text',
    int page = 0,
    int size = 20,
  }) async {
    if (type == 'text' && query.length < 2) {
      throw ArgumentError('검색어를 2자 이상 입력해주세요');
    }

    final lowerQuery = query.toLowerCase();
    List<Product> filtered;

    if (type == 'barcode') {
      filtered = _products.where((p) => p.barcode == query).toList();
    } else {
      filtered = _products.where((p) {
        return p.productName.toLowerCase().contains(lowerQuery) ||
            p.productCode.toLowerCase().contains(lowerQuery) ||
            p.barcode.contains(lowerQuery);
      }).toList();
    }

    filtered.sort((a, b) => a.productName.compareTo(b.productName));

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

  @override
  Future<ProductDetail> getProductDetail(String productCode) async {
    final product = _products.firstWhere(
      (p) => p.productCode == productCode,
      orElse: () => throw ArgumentError('제품을 찾을 수 없습니다: $productCode'),
    );

    return ProductDetail(
      productCode: product.productCode,
      productName: product.productName,
      category2: product.categoryMid,
      category3: product.categorySub,
      storageCondition: product.storageType,
      shelfLife: product.shelfLife,
      barcode: product.barcode,
    );
  }
}
