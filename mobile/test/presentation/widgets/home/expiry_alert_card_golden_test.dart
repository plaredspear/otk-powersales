import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';
import 'package:mobile/presentation/widgets/home/expiry_alert_card.dart';

/// ExpiryAlertCard 골든 테스트
void main() {
  Widget wrap(Widget child) {
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

  testWidgets('zero_count (0건)', (tester) async {
    await tester.pumpWidget(wrap(const ExpiryAlertCard(
      expiryAlert: ExpiryAlert(
        branchName: '강서 1지점',
        employeeName: '김정임',
        employeeCode: '00000',
        expiryCount: 0,
      ),
    )));
    await expectLater(
      find.byType(ExpiryAlertCard),
      matchesGoldenFile('../../../goldens/home/expiry_alert_zero.png'),
    );
  });

  testWidgets('thirteen_count (13건)', (tester) async {
    await tester.pumpWidget(wrap(const ExpiryAlertCard(
      expiryAlert: ExpiryAlert(
        branchName: '강서 1지점',
        employeeName: '김정임',
        employeeCode: '00000',
        expiryCount: 13,
      ),
    )));
    await expectLater(
      find.byType(ExpiryAlertCard),
      matchesGoldenFile('../../../goldens/home/expiry_alert_thirteen.png'),
    );
  });
}
