import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/home/activity_registration_popup.dart';

void main() {
  group('ActivityMenuItem', () {
    test('필수 파라미터로 생성된다', () {
      const item = ActivityMenuItem(
        icon: Icons.access_time,
        label: '유통기한 관리',
      );

      expect(item.icon, Icons.access_time);
      expect(item.label, '유통기한 관리');
      expect(item.route, isNull);
    });

    test('route 파라미터를 포함하여 생성된다', () {
      const item = ActivityMenuItem(
        icon: Icons.access_time,
        label: '유통기한 관리',
        route: '/expiry-management',
      );

      expect(item.route, '/expiry-management');
    });
  });

  group('ActivityRegistrationPopup 위젯 테스트', () {
    testWidgets('4개 메뉴 아이템이 모두 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      // 팝업 열기
      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      // 4개 메뉴 확인
      expect(find.text('유통기한 관리'), findsOneWidget);
      expect(find.text('현장점검'), findsOneWidget);
      expect(find.text('클레임 등록'), findsOneWidget);
      expect(find.text('제안하기'), findsOneWidget);
    });

    testWidgets('타이틀 "활동등록 하기"가 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      expect(find.text('활동등록 하기'), findsOneWidget);
    });

    testWidgets('닫기 버튼을 탭하면 팝업이 닫힌다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      // 팝업 열기
      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();
      expect(find.text('활동등록 하기'), findsOneWidget);

      // 닫기 버튼 탭
      await tester.tap(find.text('닫기'));
      await tester.pumpAndSettle();

      // 팝업이 닫혔는지 확인
      expect(find.text('활동등록 하기'), findsNothing);
    });

    testWidgets('X 버튼을 탭하면 팝업이 닫힌다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      // 팝업 열기
      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();
      expect(find.text('활동등록 하기'), findsOneWidget);

      // X 아이콘 탭
      await tester.tap(find.byIcon(Icons.close));
      await tester.pumpAndSettle();

      // 팝업이 닫혔는지 확인
      expect(find.text('활동등록 하기'), findsNothing);
    });

    testWidgets('메뉴 아이템 탭 시 onMenuTap 콜백이 호출된다', (tester) async {
      ActivityMenuItem? tappedItem;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(
                    context,
                    onMenuTap: (item) {
                      tappedItem = item;
                    },
                  );
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      // 팝업 열기
      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      // 유통기한 관리 탭
      await tester.tap(find.text('유통기한 관리'));
      await tester.pumpAndSettle();

      expect(tappedItem, isNotNull);
      expect(tappedItem!.label, '유통기한 관리');
    });

    testWidgets('메뉴 아이템 탭 시 팝업이 닫힌다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(
                    context,
                    onMenuTap: (item) {},
                  );
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      // 팝업 열기
      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();
      expect(find.text('활동등록 하기'), findsOneWidget);

      // 현장점검 탭
      await tester.tap(find.text('현장점검'));
      await tester.pumpAndSettle();

      // 팝업이 닫혔는지 확인
      expect(find.text('활동등록 하기'), findsNothing);
    });

    testWidgets('각 메뉴 아이템에 chevron_right 아이콘이 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      // 4개 메뉴 아이템의 chevron_right 아이콘
      expect(find.byIcon(Icons.chevron_right), findsNWidgets(4));
    });

    testWidgets('메뉴별 아이콘이 올바르게 표시된다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.access_time), findsOneWidget);
      expect(find.byIcon(Icons.fact_check_outlined), findsOneWidget);
      expect(find.byIcon(Icons.report_problem_outlined), findsOneWidget);
      expect(find.byIcon(Icons.lightbulb_outline), findsOneWidget);
    });

    testWidgets('defaultMenuItems의 기본값이 올바르다', (tester) async {
      final items = ActivityRegistrationPopup.defaultMenuItems;

      expect(items.length, 4);
      expect(items[0].label, '유통기한 관리');
      expect(items[1].label, '현장점검');
      expect(items[2].label, '클레임 등록');
      expect(items[3].label, '제안하기');

      // 모든 메뉴의 route가 null (미구현 상태)
      for (final item in items) {
        expect(item.route, isNull);
      }
    });

    testWidgets('클레임 등록 메뉴 탭 시 올바른 아이템이 콜백으로 전달된다', (tester) async {
      ActivityMenuItem? tappedItem;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(
                    context,
                    onMenuTap: (item) {
                      tappedItem = item;
                    },
                  );
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('클레임 등록'));
      await tester.pumpAndSettle();

      expect(tappedItem, isNotNull);
      expect(tappedItem!.label, '클레임 등록');
      expect(tappedItem!.icon, Icons.report_problem_outlined);
    });

    testWidgets('제안하기 메뉴 탭 시 올바른 아이템이 콜백으로 전달된다', (tester) async {
      ActivityMenuItem? tappedItem;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(
                    context,
                    onMenuTap: (item) {
                      tappedItem = item;
                    },
                  );
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('제안하기'));
      await tester.pumpAndSettle();

      expect(tappedItem, isNotNull);
      expect(tappedItem!.label, '제안하기');
      expect(tappedItem!.icon, Icons.lightbulb_outline);
    });

    testWidgets('onMenuTap이 null이어도 메뉴 탭 시 팝업이 닫힌다', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () {
                  ActivityRegistrationPopup.show(context);
                },
                child: const Text('팝업 열기'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('팝업 열기'));
      await tester.pumpAndSettle();
      expect(find.text('활동등록 하기'), findsOneWidget);

      await tester.tap(find.text('현장점검'));
      await tester.pumpAndSettle();

      expect(find.text('활동등록 하기'), findsNothing);
    });
  });
}
