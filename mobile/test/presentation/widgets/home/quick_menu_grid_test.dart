import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/home/quick_menu_grid.dart';

void main() {
  group('QuickMenuItem', () {
    test('assetPath, label, route 필드가 올바르게 생성되어야 한다', () {
      const item = QuickMenuItem(
        assetPath: 'assets/images/ico_quick1.png',
        label: '내 일정',
        route: '/my-schedule',
      );

      expect(item.assetPath, 'assets/images/ico_quick1.png');
      expect(item.label, '내 일정');
      expect(item.route, '/my-schedule');
    });

    test('route는 선택 필드여야 한다', () {
      const item = QuickMenuItem(
        assetPath: 'assets/images/ico_quick1.png',
        label: '내 일정',
      );

      expect(item.route, isNull);
    });
  });

  group('QuickMenuGrid', () {
    test('defaultMenuItems는 6개여야 한다', () {
      expect(QuickMenuGrid.defaultMenuItems.length, 6);
    });

    test('각 메뉴의 assetPath가 올바른 경로여야 한다', () {
      final items = QuickMenuGrid.defaultMenuItems;

      expect(items[0].assetPath, 'assets/images/ico_quick1.png');
      expect(items[1].assetPath, 'assets/images/ico_quick2.png');
      expect(items[2].assetPath, 'assets/images/ico_quick3.png');
      expect(items[3].assetPath, 'assets/images/ico_quick4.png');
      expect(items[4].assetPath, 'assets/images/ico_quick5.png');
      expect(items[5].assetPath, 'assets/images/ico_quick6.png');
    });

    test('각 메뉴의 label이 올바르게 지정되어야 한다', () {
      final items = QuickMenuGrid.defaultMenuItems;

      expect(items[0].label, '내 일정');
      expect(items[1].label, '매출 현황');
      expect(items[2].label, '주문 관리');
      expect(items[3].label, '활동 등록');
      expect(items[4].label, '교육');
      expect(items[5].label, '행사매출\n등록');
    });

    testWidgets('6개 Image 위젯이 렌더링되어야 한다', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: QuickMenuGrid(),
          ),
        ),
      );

      expect(find.byType(Image), findsNWidgets(6));
    });

    testWidgets('메뉴 탭 시 onMenuTap 콜백이 호출되어야 한다', (tester) async {
      QuickMenuItem? tappedItem;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: QuickMenuGrid(
              onMenuTap: (item) {
                tappedItem = item;
              },
            ),
          ),
        ),
      );

      await tester.tap(find.text('내 일정'));
      await tester.pumpAndSettle();

      expect(tappedItem, isNotNull);
      expect(tappedItem!.label, '내 일정');
      expect(tappedItem!.assetPath, 'assets/images/ico_quick1.png');
    });
  });
}
