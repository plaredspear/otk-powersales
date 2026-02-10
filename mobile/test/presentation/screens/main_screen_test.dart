import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/presentation/providers/auth_provider.dart';
import 'package:mobile/presentation/providers/auth_state.dart';
import 'package:mobile/presentation/providers/home_provider.dart';
import 'package:mobile/presentation/providers/home_state.dart';
import 'package:mobile/presentation/screens/main_screen.dart';
import 'package:mobile/presentation/pages/home_page.dart';
import 'package:mobile/presentation/widgets/menu/full_menu_drawer.dart';
import 'package:mobile/domain/entities/user.dart';
import 'package:mobile/domain/repositories/home_repository.dart';
import '../../test_helper.dart';

/// Mock HomeRepository for testing
class MockHomeRepository implements HomeRepository {
  @override
  Future<HomeData> getHomeData() async {
    return const HomeData(
      todaySchedules: [],
      expiryAlert: null,
      notices: [],
      currentDate: '2024-01-01',
    );
  }
}

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  // Mock user for authenticated state
  const mockUser = User(
    id: 1,
    employeeId: '12345678',
    name: '홍길동',
    department: '영업1팀',
    branchName: '서울지점',
    role: 'USER',
  );

  group('MainScreen - 새로운 구조 테스트', () {
    testWidgets('하단 네비게이션 바에 3개 버튼이 표시된다 (뒤로, 홈으로, 전체메뉴)',
        (WidgetTester tester) async {
      // Arrange: Provider overrides with mock repository
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            homeRepositoryProvider.overrideWithValue(MockHomeRepository()),
          ],
          child: const MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Assert: 3개의 버튼 레이블이 표시되는지 확인
      expect(find.text('뒤로'), findsOneWidget);
      expect(find.text('홈으로'), findsOneWidget);
      expect(find.text('전체메뉴'), findsOneWidget);

      // Assert: 각 버튼의 아이콘이 표시되는지 확인
      // Note: arrow_back, home, menu 아이콘이 각각 최소 1개씩 존재함
      expect(find.byIcon(Icons.arrow_back), findsWidgets);
      expect(find.byIcon(Icons.home), findsWidgets);
      expect(find.byIcon(Icons.menu), findsWidgets);
    });

    testWidgets('전체메뉴 버튼 탭 시 endDrawer가 열린다', (WidgetTester tester) async {
      // Arrange: Provider overrides with mock repository
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            homeRepositoryProvider.overrideWithValue(MockHomeRepository()),
          ],
          child: const MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Assert: endDrawer가 닫혀있는 상태
      expect(find.byType(FullMenuDrawer), findsNothing);

      // Act: 전체메뉴 버튼 탭
      await tester.tap(find.text('전체메뉴'));
      await tester.pumpAndSettle();

      // Assert: endDrawer가 열림
      expect(find.byType(FullMenuDrawer), findsOneWidget);
    });

    testWidgets('기존 5탭 BottomNavigationBar가 더 이상 존재하지 않는다',
        (WidgetTester tester) async {
      // Arrange: Provider overrides with mock repository
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            homeRepositoryProvider.overrideWithValue(MockHomeRepository()),
          ],
          child: const MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Assert: BottomNavigationBar가 존재하지 않음
      expect(find.byType(BottomNavigationBar), findsNothing);

      // Assert: 기존 5탭 레이블이 존재하지 않음
      expect(find.text('POS 매출'), findsNothing);
      expect(find.text('전산매출'), findsNothing);
      expect(find.text('물류매출'), findsNothing);
      expect(find.text('목표/진도율'), findsNothing);

      // Assert: IndexedStack도 더 이상 사용하지 않음
      expect(find.byType(IndexedStack), findsNothing);
    });

    testWidgets('MainScreen의 body는 HomePage이다', (WidgetTester tester) async {
      // Arrange: Provider overrides with mock repository
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            homeRepositoryProvider.overrideWithValue(MockHomeRepository()),
          ],
          child: const MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Assert: HomePage가 body로 표시됨
      expect(find.byType(HomePage), findsOneWidget);
    });

    testWidgets('MainScreen은 Scaffold를 사용한다', (WidgetTester tester) async {
      // Arrange: Provider overrides with mock repository
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            homeRepositoryProvider.overrideWithValue(MockHomeRepository()),
          ],
          child: const MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Assert: Scaffold가 존재함
      expect(find.byType(Scaffold), findsOneWidget);
    });

    testWidgets('하단 네비게이션 바는 Container 위젯을 사용한다',
        (WidgetTester tester) async {
      // Arrange: Provider overrides with mock repository
      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            homeRepositoryProvider.overrideWithValue(MockHomeRepository()),
          ],
          child: const MaterialApp(
            home: MainScreen(),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Assert: 하단 네비게이션 바는 Container 기반 (BottomNavigationBar 아님)
      final scaffold = tester.widget<Scaffold>(find.byType(Scaffold));
      expect(scaffold.bottomNavigationBar, isNotNull);
      expect(scaffold.bottomNavigationBar, isNot(isA<BottomNavigationBar>()));
    });
  });
}
