import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/domain/entities/pos_sales.dart';
import 'package:mobile/domain/entities/favorite_product.dart';
import 'package:mobile/domain/repositories/favorite_product_repository.dart';
import 'package:mobile/presentation/widgets/pos/pos_sales_item.dart';
import 'package:mobile/presentation/providers/favorite_product_provider.dart';
import '../../../test_helper.dart';

/// Mock Repository (메모리 기반)
class MockFavoriteProductRepository implements FavoriteProductRepository {
  final Map<String, FavoriteProduct> _storage = {};

  @override
  Future<List<FavoriteProduct>> getAllFavorites() async {
    final list = _storage.values.toList();
    list.sort((a, b) => b.addedAt.compareTo(a.addedAt));
    return list;
  }

  @override
  Future<bool> isFavorite(String productId) async {
    return _storage.containsKey(productId);
  }

  @override
  Future<void> addFavorite(FavoriteProduct product) async {
    _storage[product.id] = product;
  }

  @override
  Future<void> removeFavorite(String productId) async {
    _storage.remove(productId);
  }

  @override
  Future<List<FavoriteProduct>> searchFavorites(String query) async {
    if (query.trim().isEmpty) {
      return getAllFavorites();
    }

    final allFavorites = await getAllFavorites();
    final lowerQuery = query.toLowerCase();

    return allFavorites
        .where((product) => product.productName.toLowerCase().contains(lowerQuery))
        .toList();
  }

  @override
  Future<void> clearAllFavorites() async {
    _storage.clear();
  }
}

void main() {
  // 테스트 환경 초기화
  setUpAll(() async {
    await TestHelper.initialize();
  });

  group('PosSalesItem Widget Tests', () {
    late PosSales testSales;
    late MockFavoriteProductRepository mockRepository;

    setUp(() {
      testSales = PosSales(
        storeName: '이마트 강남점',
        productName: '진라면 매운맛',
        salesDate: DateTime(2026, 1, 15),
        quantity: 120,
        amount: 156000,
        productCode: 'P001',
        category: '라면',
      );
      mockRepository = MockFavoriteProductRepository();
    });

    testWidgets('매출 아이템이 올바르게 렌더링된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 매장명 확인
      expect(find.text('이마트 강남점'), findsOneWidget);

      // 제품명 확인
      expect(find.text('진라면 매운맛'), findsOneWidget);

      // 카테고리 확인
      expect(find.text('라면'), findsOneWidget);

      // 아이콘 확인
      expect(find.byIcon(Icons.store), findsOneWidget);
      expect(find.byIcon(Icons.shopping_bag), findsOneWidget);
    });

    testWidgets('날짜가 올바른 형식으로 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 날짜 형식: 'yyyy-MM-dd (E)' - 요일은 언어 설정에 따라 다를 수 있음
      // 최소한 날짜 부분만 확인
      expect(find.textContaining('2026-01-15'), findsOneWidget);
    });

    testWidgets('수량이 올바른 형식으로 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      // 수량 포맷: '###,###개'
      expect(find.text('120개'), findsOneWidget);
    });

    testWidgets('금액이 올바른 형식으로 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      // 금액 포맷: '###,###원'
      expect(find.text('156,000원'), findsOneWidget);
    });

    testWidgets('큰 수량이 올바르게 포맷팅된다', (tester) async {
      final largeSales = testSales.copyWith(quantity: 1234567);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: largeSales),
            ),
          ),
        ),
      );

      expect(find.text('1,234,567개'), findsOneWidget);
    });

    testWidgets('큰 금액이 올바르게 포맷팅된다', (tester) async {
      final largeSales = testSales.copyWith(amount: 12345678);

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: largeSales),
            ),
          ),
        ),
      );

      expect(find.text('12,345,678원'), findsOneWidget);
    });

    testWidgets('카테고리가 없을 때 표시되지 않는다', (tester) async {
      final salesWithoutCategory = PosSales(
        storeName: '홈플러스',
        productName: '참깨라면',
        salesDate: DateTime(2026, 1, 20),
        quantity: 50,
        amount: 65000,
      );

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: salesWithoutCategory),
            ),
          ),
        ),
      );

      // 카테고리가 null이면 카테고리 컨테이너가 표시되지 않음
      // 카테고리 텍스트가 없는지 확인 (더 안정적인 테스트)
      expect(find.text('라면'), findsNothing);
      expect(find.text('상온'), findsNothing);
    });

    testWidgets('onTap 콜백이 올바르게 호출된다', (tester) async {
      bool tapped = false;

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(
                sales: testSales,
                onTap: () {
                  tapped = true;
                },
              ),
            ),
          ),
        ),
      );

      // 카드 탭
      await tester.tap(find.byType(InkWell));
      await tester.pump();

      expect(tapped, true);
    });

    testWidgets('Card와 InkWell이 올바르게 구성되어 있다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      // Card 위젯 확인
      expect(find.byType(Card), findsOneWidget);

      // InkWell 위젯 확인 (터치 피드백용)
      expect(find.byType(InkWell), findsOneWidget);
    });

    testWidgets('정보 칩이 올바른 색상으로 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      // 수량 아이콘 (녹색)
      final quantityIcon = tester.widget<Icon>(
        find.byIcon(Icons.inventory_2).first,
      );
      expect(quantityIcon.color, Colors.green);

      // 금액 아이콘 (주황색)
      final amountIcon = tester.widget<Icon>(
        find.byIcon(Icons.payments).first,
      );
      expect(amountIcon.color, Colors.orange);
    });

    testWidgets('매장명 아이콘이 파란색으로 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      final storeIcon = tester.widget<Icon>(find.byIcon(Icons.store).first);
      expect(storeIcon.color, Colors.blue[700]);
    });

    testWidgets('여러 매출 아이템이 리스트에 올바르게 표시된다', (tester) async {
      final salesList = [
        testSales,
        testSales.copyWith(
          storeName: '롯데마트',
          productName: '참깨라면',
        ),
        testSales.copyWith(
          storeName: '홈플러스',
          productName: '육개장',
        ),
      ];

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: ListView.builder(
                itemCount: salesList.length,
                itemBuilder: (context, index) {
                  return PosSalesItem(sales: salesList[index]);
                },
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 3개의 매출 아이템 확인
      expect(find.byType(PosSalesItem), findsNWidgets(3));
      expect(find.text('이마트 강남점'), findsOneWidget);
      expect(find.text('롯데마트'), findsOneWidget);
      expect(find.text('홈플러스'), findsOneWidget);
    });

    testWidgets('즐겨찾기 버튼이 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 즐겨찾기 버튼 확인 (초기: 빈 별)
      expect(find.byIcon(Icons.star_border), findsOneWidget);
    });

    testWidgets('즐겨찾기를 추가하면 별 아이콘이 채워진다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 초기 상태: 빈 별
      expect(find.byIcon(Icons.star_border), findsOneWidget);
      expect(find.byIcon(Icons.star), findsNothing);

      // 즐겨찾기 버튼 클릭
      await tester.tap(find.byIcon(Icons.star_border));
      await tester.pumpAndSettle();

      // 즐겨찾기 추가 후: 채워진 별
      expect(find.byIcon(Icons.star), findsOneWidget);
      expect(find.byIcon(Icons.star_border), findsNothing);
    });

    testWidgets('즐겨찾기를 제거하면 별 아이콘이 비워진다', (tester) async {
      // 미리 즐겨찾기 추가
      await mockRepository.addFavorite(FavoriteProduct(
        id: '진라면 매운맛',
        productName: '진라면 매운맛',
        addedAt: DateTime.now(),
      ));

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 초기 상태: 채워진 별
      expect(find.byIcon(Icons.star), findsOneWidget);

      // 즐겨찾기 버튼 클릭 (제거)
      await tester.tap(find.byIcon(Icons.star));
      await tester.pumpAndSettle();

      // 즐겨찾기 제거 후: 빈 별
      expect(find.byIcon(Icons.star_border), findsOneWidget);
      expect(find.byIcon(Icons.star), findsNothing);
    });

    testWidgets('즐겨찾기 버튼에 툴팁이 표시된다', (tester) async {
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            favoriteProductRepositoryProvider.overrideWithValue(mockRepository),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: PosSalesItem(sales: testSales),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // 툴팁 확인 (즐겨찾기 추가 상태)
      final tooltip = find.byTooltip('즐겨찾기 추가');
      expect(tooltip, findsOneWidget);
    });
  });
}
