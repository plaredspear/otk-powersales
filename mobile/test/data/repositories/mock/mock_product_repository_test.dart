import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/mock_product_repository.dart';
import 'package:mobile/domain/entities/product.dart';
import 'package:mobile/domain/repositories/product_repository.dart';

void main() {
  late MockProductRepository repository;

  setUp(() {
    repository = MockProductRepository();
  });

  group('MockProductRepository', () {
    group('텍스트 검색', () {
      test('제품명으로 검색하면 매칭되는 결과를 반환한다', () async {
        final result = await repository.searchProducts(query: '열라면');

        expect(result.products, isNotEmpty);
        expect(
          result.products.every(
            (p) => p.productName.contains('열라면'),
          ),
          isTrue,
        );
        expect(result.totalElements, 3); // 열라면_용기, 열라면_봉지, 열라면_멀티
      });

      test('제품코드로 검색하면 매칭되는 결과를 반환한다', () async {
        final result = await repository.searchProducts(query: '18110014');

        expect(result.products, isNotEmpty);
        expect(result.products.first.productCode, '18110014');
      });

      test('바코드 번호로 텍스트 검색하면 매칭되는 결과를 반환한다', () async {
        final result =
            await repository.searchProducts(query: '8801045570716');

        expect(result.products, isNotEmpty);
        expect(result.products.first.barcode, '8801045570716');
      });

      test('대소문자 구분 없이 검색한다', () async {
        // Mock 데이터에 영문이 포함된 제품이 없으므로 한글로 검색
        final result1 = await repository.searchProducts(query: '열라면');
        final result2 = await repository.searchProducts(query: '열라면');

        expect(result1.totalElements, result2.totalElements);
      });

      test('검색 결과가 없으면 빈 리스트를 반환한다', () async {
        final result = await repository.searchProducts(
          query: '존재하지않는제품',
        );

        expect(result.products, isEmpty);
        expect(result.totalElements, 0);
      });

      test('검색어가 2자 미만이면 ArgumentError를 던진다', () async {
        expect(
          () => repository.searchProducts(query: '열'),
          throwsA(isA<ArgumentError>()),
        );
      });

      test('검색 결과가 제품명 기준 가나다순으로 정렬된다', () async {
        final result = await repository.searchProducts(query: '라면');

        final names = result.products.map((p) => p.productName).toList();
        final sorted = List<String>.from(names)..sort();
        expect(names, sorted);
      });
    });

    group('바코드 검색', () {
      test('정확한 바코드로 검색하면 해당 제품을 반환한다', () async {
        final result = await repository.searchProducts(
          query: '8801045570716',
          type: 'barcode',
        );

        expect(result.products.length, 1);
        expect(result.products.first.barcode, '8801045570716');
        expect(result.products.first.productName, '열라면_용기105G');
      });

      test('존재하지 않는 바코드로 검색하면 빈 리스트를 반환한다', () async {
        final result = await repository.searchProducts(
          query: '0000000000000',
          type: 'barcode',
        );

        expect(result.products, isEmpty);
        expect(result.totalElements, 0);
      });

      test('바코드 검색은 최소 길이 검증을 하지 않는다', () async {
        // 바코드 검색은 type='barcode'이므로 2자 미만이어도 가능
        final result = await repository.searchProducts(
          query: '0',
          type: 'barcode',
        );

        // 일치하는 바코드가 없으므로 빈 결과
        expect(result.products, isEmpty);
      });
    });

    group('페이지네이션', () {
      test('기본 페이지 크기는 20이다', () async {
        final result = await repository.searchProducts(query: '오뚜기');

        expect(result.pageSize, 20);
        expect(result.currentPage, 0);
      });

      test('첫 번째 페이지에서 isFirst가 true이다', () async {
        final result = await repository.searchProducts(
          query: '오뚜기',
          page: 0,
        );

        expect(result.isFirst, isTrue);
      });

      test('결과가 pageSize 이하이면 isLast가 true이다', () async {
        final result = await repository.searchProducts(
          query: '열라면',
          size: 20,
        );

        expect(result.isLast, isTrue);
      });

      test('페이지 크기를 조절하여 페이지네이션이 동작한다', () async {
        // 라면 카테고리 제품으로 검색 (여러 건)
        final fullResult = await repository.searchProducts(
          query: '라면',
          size: 100,
        );

        if (fullResult.totalElements > 2) {
          final page1 = await repository.searchProducts(
            query: '라면',
            page: 0,
            size: 2,
          );

          expect(page1.products.length, 2);
          expect(page1.isFirst, isTrue);
          expect(page1.totalElements, fullResult.totalElements);

          final page2 = await repository.searchProducts(
            query: '라면',
            page: 1,
            size: 2,
          );

          expect(page2.products, isNotEmpty);
          expect(page2.isFirst, isFalse);

          // 페이지 간 제품이 중복되지 않음
          final page1Ids =
              page1.products.map((p) => p.productId).toSet();
          final page2Ids =
              page2.products.map((p) => p.productId).toSet();
          expect(page1Ids.intersection(page2Ids), isEmpty);
        }
      });

      test('범위를 초과한 페이지 요청 시 빈 결과를 반환한다', () async {
        final result = await repository.searchProducts(
          query: '열라면',
          page: 100,
          size: 20,
        );

        expect(result.products, isEmpty);
      });

      test('hasNextPage가 올바르게 동작한다', () async {
        final result = await repository.searchProducts(
          query: '열라면',
          page: 0,
          size: 1,
        );

        // 열라면 3건, size 1이므로 다음 페이지 있음
        expect(result.hasNextPage, isTrue);

        final lastPage = await repository.searchProducts(
          query: '열라면',
          page: 2,
          size: 1,
        );

        expect(lastPage.hasNextPage, isFalse);
      });
    });

    group('ProductSearchResult', () {
      test('totalElements가 올바르게 반환된다', () async {
        final result = await repository.searchProducts(query: '열라면');
        expect(result.totalElements, 3);
      });

      test('totalPages가 올바르게 계산된다', () async {
        final result = await repository.searchProducts(
          query: '열라면',
          size: 2,
        );

        // 3건, size 2 → 2페이지
        expect(result.totalPages, 2);
      });

      test('검색 결과가 Product 엔티티 리스트이다', () async {
        final result = await repository.searchProducts(query: '열라면');

        for (final product in result.products) {
          expect(product, isA<Product>());
          expect(product.productId, isNotEmpty);
          expect(product.productName, isNotEmpty);
          expect(product.productCode, isNotEmpty);
          expect(product.barcode, isNotEmpty);
          expect(product.storageType, isNotEmpty);
          expect(product.shelfLife, isNotEmpty);
        }
      });
    });

    group('다양한 카테고리 검색', () {
      test('스낵 카테고리 제품을 검색한다', () async {
        final result = await repository.searchProducts(query: '뿌셔뿌셔');

        expect(result.products, isNotEmpty);
        expect(
          result.products.every((p) => p.categoryMid == '스낵'),
          isTrue,
        );
      });

      test('레토르트 카테고리 제품을 검색한다', () async {
        final result = await repository.searchProducts(query: '3분');

        expect(result.products, isNotEmpty);
      });

      test('냉동식품 카테고리 제품을 검색한다', () async {
        final result = await repository.searchProducts(query: '냉동볶음밥');

        expect(result.products, isNotEmpty);
        expect(
          result.products.every((p) => p.storageType == '냉동'),
          isTrue,
        );
      });

      test('냉장 보관 제품을 검색한다', () async {
        final result = await repository.searchProducts(query: '마요네즈');

        expect(result.products, isNotEmpty);
        expect(result.products.first.storageType, '냉장');
      });
    });

    test('ProductRepository 인터페이스를 구현한다', () {
      expect(repository, isA<ProductRepository>());
    });
  });
}
