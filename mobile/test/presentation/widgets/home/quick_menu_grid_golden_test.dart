import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/home/quick_menu_grid.dart';

/// QuickMenuGrid 골든 테스트
void main() {
  Widget wrap(QuickMenuGrid child) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        backgroundColor: const Color(0xFFFFFFFF),
        body: SizedBox(
          width: 393,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: child,
          ),
        ),
      ),
    );
  }

  testWidgets('user (6 items)', (tester) async {
    await tester.pumpWidget(wrap(QuickMenuGrid(
      userRole: 'USER',
      onMenuTap: (_) {},
    )));
    await expectLater(
      find.byType(QuickMenuGrid),
      matchesGoldenFile('../../../goldens/home/quick_menu_user.png'),
    );
  });

  testWidgets('leader (7 items)', (tester) async {
    await tester.pumpWidget(wrap(QuickMenuGrid(
      userRole: 'LEADER',
      onMenuTap: (_) {},
    )));
    await expectLater(
      find.byType(QuickMenuGrid),
      matchesGoldenFile('../../../goldens/home/quick_menu_leader.png'),
    );
  });
}
