import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product.dart';
import 'package:mobile/presentation/providers/product_search_state.dart';

void main() {
  group('ProductSearchState', () {
    test('초기 상태가 올바르게 생성된다', () {
      final state = ProductSearchState.initial();

      expect(state.isLoading, isFalse);
      expect(state.isLoadingMore, isFalse);
      expect(state.errorMessage, isNull);
      expect(state.query, '');
      expect(state.searchType, SearchType.text);
      expect(state.products, isEmpty);
      expect(state.totalElements, 0);
      expect(state.currentPage, 0);
      expect(state.isLastPage, isFalse);
      expect(state.hasSearched, isFalse);
    });

    group('상태 전환 메서드', () {
      test('toLoading이 로딩 상태로 전환한다', () {
        final state = ProductSearchState.initial().toLoading();

        expect(state.isLoading, isTrue);
        expect(state.errorMessage, isNull);
      });

      test('toLoadingMore가 추가 로딩 상태로 전환한다', () {
        final state = ProductSearchState.initial().toLoadingMore();

        expect(state.isLoadingMore, isTrue);
        expect(state.errorMessage, isNull);
      });

      test('toError가 에러 상태로 전환한다', () {
        final state = ProductSearchState.initial()
            .toLoading()
            .toError('검색 오류 발생');

        expect(state.isLoading, isFalse);
        expect(state.isLoadingMore, isFalse);
        expect(state.errorMessage, '검색 오류 발생');
      });

      test('toLoading이 기존 에러 메시지를 초기화한다', () {
        final state = ProductSearchState.initial()
            .toError('이전 에러')
            .toLoading();

        expect(state.errorMessage, isNull);
      });
    });

    group('computed getters', () {
      test('hasResults는 제품 목록이 비어있지 않으면 true이다', () {
        final state = const ProductSearchState(
          products: [
            Product(
              productId: '1',
              productName: '테스트',
              productCode: '1',
              barcode: '1',
              storageType: '상온',
              shelfLife: '7개월',
            ),
          ],
        );

        expect(state.hasResults, isTrue);
      });

      test('hasResults는 제품 목록이 비어있으면 false이다', () {
        final state = ProductSearchState.initial();
        expect(state.hasResults, isFalse);
      });

      test('isEmpty는 검색 후 결과가 없으면 true이다', () {
        final state = const ProductSearchState(
          hasSearched: true,
          products: [],
        );

        expect(state.isEmpty, isTrue);
      });

      test('isEmpty는 검색 전이면 false이다', () {
        final state = ProductSearchState.initial();
        expect(state.isEmpty, isFalse);
      });

      test('canSearch는 텍스트 검색 시 2자 이상이면 true이다', () {
        const state = ProductSearchState(
          query: '열라면',
          searchType: SearchType.text,
        );

        expect(state.canSearch, isTrue);
      });

      test('canSearch는 텍스트 검색 시 1자 이하이면 false이다', () {
        const state = ProductSearchState(
          query: '열',
          searchType: SearchType.text,
        );

        expect(state.canSearch, isFalse);
      });

      test('canSearch는 바코드 검색 시 항상 true이다', () {
        const state = ProductSearchState(
          query: '0',
          searchType: SearchType.barcode,
        );

        expect(state.canSearch, isTrue);
      });

      test('hasNextPage는 마지막 페이지가 아니면 true이다', () {
        const state = ProductSearchState(isLastPage: false);
        expect(state.hasNextPage, isTrue);
      });

      test('hasNextPage는 마지막 페이지이면 false이다', () {
        const state = ProductSearchState(isLastPage: true);
        expect(state.hasNextPage, isFalse);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경한 복사본을 생성한다', () {
        final state = ProductSearchState.initial().copyWith(
          query: '열라면',
          isLoading: true,
        );

        expect(state.query, '열라면');
        expect(state.isLoading, isTrue);
        expect(state.searchType, SearchType.text);
        expect(state.products, isEmpty);
      });

      test('인자 없이 호출하면 errorMessage가 null로 초기화된다', () {
        final state = ProductSearchState.initial()
            .toError('에러')
            .copyWith();

        // errorMessage는 copyWith에서 명시적으로 전달하지 않으면 null
        expect(state.errorMessage, isNull);
      });
    });
  });

  group('SearchType', () {
    test('text와 barcode 두 가지 유형이 있다', () {
      expect(SearchType.values.length, 2);
      expect(SearchType.values.contains(SearchType.text), isTrue);
      expect(SearchType.values.contains(SearchType.barcode), isTrue);
    });
  });
}
