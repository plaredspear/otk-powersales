import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product_for_order.dart';
import 'package:mobile/domain/entities/product_search_result.dart';
import 'package:mobile/domain/repositories/order_request_repository.dart';
import 'package:mobile/domain/usecases/search_products_for_order_usecase.dart';
import 'package:mobile/presentation/providers/add_product_provider.dart';
import 'package:mobile/presentation/widgets/order_form/search_products_tab.dart';

import '../../../helpers/fake_order_request_repository.dart';

/// 무한스크롤 하단 감지 경로 검증.
///
/// provider 단위 테스트(add_product_provider_test)는 loadMoreSearchResults 를
/// 직접 호출하므로, "스크롤이 실제로 그것을 호출하는가"는 여기서만 검증된다.
/// 바텀시트가 넘겨주던 공유 ScrollController 를 쓰면 여러 ScrollView 가 한
/// controller 에 붙어 position 접근이 실패했다 — 그 회귀를 막는 것이 목적.
void main() {
  late _PagingFakeRepository fakeRepo;

  setUp(() {
    fakeRepo = _PagingFakeRepository();
  });

  Widget host() {
    return ProviderScope(
      overrides: [
        searchProductsForOrderUseCaseProvider
            .overrideWithValue(SearchProductsForOrder(fakeRepo)),
      ],
      child: const MaterialApp(
        home: Scaffold(
          // 실제 화면과 같이 높이가 제한된 영역에 배치한다.
          body: SizedBox(height: 600, child: SearchProductsTab()),
        ),
      ),
    );
  }

  testWidgets('목록 하단까지 스크롤하면 다음 페이지가 이어붙는다', (tester) async {
    await tester.pumpWidget(host());

    // 검색 실행 (debounce 500ms 경과 후 요청)
    await tester.enterText(find.byType(TextField), '라면');
    await tester.pump(const Duration(milliseconds: 600));
    await tester.pumpAndSettle();

    expect(fakeRepo.requestedPages, [0]);

    // 하단까지 스크롤 — 무한스크롤 트리거.
    // 로딩 스피너가 회전 중이면 pumpAndSettle 이 정착하지 않으므로 고정 횟수로 편다.
    await tester.drag(find.byType(ListView), const Offset(0, -4000));
    for (var i = 0; i < 5; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }

    // 다음 페이지가 요청되어야 한다. 공유 controller 회귀 시 position 접근이
    // 실패해 이 기대가 깨진다.
    expect(fakeRepo.requestedPages, contains(1));
  });

  testWidgets('스크롤해도 ScrollController 예외가 발생하지 않는다', (tester) async {
    await tester.pumpWidget(host());

    await tester.enterText(find.byType(TextField), '라면');
    await tester.pump(const Duration(milliseconds: 600));
    await tester.pumpAndSettle();

    // 로딩 스피너가 계속 회전해 pumpAndSettle 이 정착하지 않으므로 고정 횟수로 편다.
    await tester.drag(find.byType(ListView), const Offset(0, -2000));
    for (var i = 0; i < 5; i++) {
      await tester.pump(const Duration(milliseconds: 100));
    }

    expect(tester.takeException(), isNull);
  });
}

/// 페이지당 20건씩 총 60건을 돌려주는 fake — 무한스크롤 페이징만 재현한다.
class _PagingFakeRepository extends FakeOrderRequestRepository {
  static const _pageSize = 20;
  static const _totalCount = 60;

  final List<int> requestedPages = [];

  @override
  Future<ProductSearchResult> searchProductsForOrder({
    required String query,
    String? categoryMid,
    String? categorySub,
    int page = 0,
    int loadedCount = 0,
  }) async {
    requestedPages.add(page);

    final start = page * _pageSize;
    final end = (start + _pageSize).clamp(0, _totalCount);
    final products = <ProductForOrder>[
      for (var i = start; i < end; i++)
        ProductForOrder(
          productCode: 'P${i.toString().padLeft(3, '0')}',
          productName: '제품 $i',
          barcode: '880${i.toString().padLeft(10, '0')}',
          storageType: '실온',
          shelfLife: '12개월',
          unitPrice: 1000,
          boxSize: 10,
          isFavorite: false,
        ),
    ];

    return ProductSearchResult(
      products: products,
      totalCount: _totalCount,
      page: page,
      loadedCount: loadedCount + products.length,
    );
  }
}
