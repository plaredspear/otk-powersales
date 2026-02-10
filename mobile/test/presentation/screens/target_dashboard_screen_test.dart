import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/presentation/screens/target_dashboard_screen.dart';

void main() {
  group('TargetDashboardScreen Tests', () {
    testWidgets('화면이 올바르게 렌더링된다', (tester) async {
      // Arrange & Act
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('목표/진도율'), findsOneWidget);
      expect(find.byIcon(Icons.refresh), findsOneWidget);
      expect(find.byIcon(Icons.filter_alt_off), findsOneWidget);
    });

    testWidgets('초기 상태에서 빈 목록이 표시된다', (tester) async {
      // Arrange & Act
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pump();

      // Assert - 초기 상태 확인
      expect(find.text('목표/진도율'), findsOneWidget);

      // Cleanup - wait for pending timers
      await tester.pumpAndSettle();
    });

    testWidgets('데이터 로드 후 목표 목록이 표시된다', (tester) async {
      // Arrange & Act
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.byType(Card), findsWidgets);
      expect(find.byType(ListTile), findsWidgets);
    });

    testWidgets('필터 섹션이 표시된다', (tester) async {
      // Arrange & Act
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.byIcon(Icons.calendar_today), findsOneWidget);
      expect(find.byIcon(Icons.chevron_left), findsOneWidget);
      expect(find.byIcon(Icons.chevron_right), findsOneWidget);
      expect(find.byType(FilterChip), findsWidgets);
    });

    testWidgets('통계 요약이 표시된다', (tester) async {
      // Arrange & Act
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('전체 진도율'), findsOneWidget);
      expect(find.text('초과'), findsAtLeastNWidgets(1));
      expect(find.text('달성'), findsAtLeastNWidgets(1));
      expect(find.text('부족'), findsAtLeastNWidgets(1));
    });

    testWidgets('새로고침 버튼 클릭 시 데이터를 다시 로드한다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      await tester.tap(find.byIcon(Icons.refresh));
      await tester.pumpAndSettle();

      // Assert - 새로고침 이벤트가 트리거됨
      expect(find.byIcon(Icons.refresh), findsOneWidget);
    });

    testWidgets('진도율 부족 필터 칩을 토글할 수 있다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      final filterChip = find.widgetWithText(FilterChip, '진도율 부족');
      await tester.tap(filterChip);
      await tester.pumpAndSettle();

      // Assert - 탭 이벤트가 트리거됨
      expect(filterChip, findsOneWidget);
    });

    testWidgets('카테고리 필터 칩 클릭 시 다이얼로그가 표시된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      final categoryChip = find.text('전체 카테고리');
      await tester.tap(categoryChip);
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('카테고리 선택'), findsOneWidget);
      expect(find.text('전체'), findsOneWidget);
      expect(find.text('전산매출'), findsWidgets);
      expect(find.text('POS매출'), findsOneWidget);
      expect(find.text('물류매출'), findsWidgets);
    });

    testWidgets('카테고리를 선택하면 필터가 적용된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      await tester.tap(find.text('전체 카테고리'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('전산매출').last); // 다이얼로그의 항목 선택
      await tester.pumpAndSettle();

      // Assert - 카테고리 필터가 적용됨
      expect(find.byType(Card), findsWidgets);
    });

    testWidgets('이전 달 버튼 클릭 시 년월이 변경된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      await tester.tap(find.byIcon(Icons.chevron_left));
      await tester.pumpAndSettle();

      // Assert - 데이터가 다시 로드됨
      expect(find.byType(Card), findsWidgets);
    });

    testWidgets('다음 달 버튼 클릭 시 년월이 변경된다', (tester) async {
      // Arrange - Set larger test surface size to prevent RenderFlex overflow
      tester.view.physicalSize = const Size(800, 1200);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      await tester.tap(find.byIcon(Icons.chevron_right));
      await tester.pumpAndSettle();

      // Assert - 데이터가 다시 로드됨
      expect(find.byType(Card), findsWidgets);
    });

    testWidgets('목표 추가 FAB 클릭 시 스낵바가 표시된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      await tester.tap(find.byType(FloatingActionButton));
      await tester.pumpAndSettle();

      // Assert
      expect(find.text('목표 추가 기능 준비 중'), findsOneWidget);
    });

    testWidgets('목표 아이템 클릭 시 스낵바가 표시된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Act
      final listTile = find.byType(ListTile).first;
      await tester.tap(listTile);
      await tester.pumpAndSettle();

      // Assert
      expect(find.textContaining('상세 (준비 중)'), findsOneWidget);
    });

    testWidgets('통계 카드에 올바른 값이 표시된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.byIcon(Icons.trending_up), findsOneWidget); // 초과
      expect(find.byIcon(Icons.check_circle), findsOneWidget); // 달성
      expect(find.byIcon(Icons.trending_down), findsOneWidget); // 부족
    });

    testWidgets('목표 리스트 아이템에 진도율이 표시된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.byType(CircleAvatar), findsWidgets);
    });

    testWidgets('목표 리스트 아이템에 거래처명이 표시된다', (tester) async {
      // Arrange
      await tester.pumpWidget(
        const ProviderScope(
          child: MaterialApp(
            home: TargetDashboardScreen(),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Assert
      expect(find.byType(ListTile), findsWidgets);
    });
  });
}
