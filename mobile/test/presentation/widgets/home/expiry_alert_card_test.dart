import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';
import 'package:mobile/presentation/widgets/home/expiry_alert_card.dart';

void main() {
  Widget buildTestWidget({
    ExpiryAlert? expiryAlert,
    VoidCallback? onTap,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: ExpiryAlertCard(
          expiryAlert: expiryAlert,
          onTap: onTap,
        ),
      ),
    );
  }

  group('ExpiryAlertCard', () {
    testWidgets('T1: expiryCount가 0이어도 카드가 표시되어야 한다', (tester) async {
      await tester.pumpWidget(buildTestWidget(
        expiryAlert: const ExpiryAlert(
          branchName: '강남53지점',
          employeeName: '홍길동',
          employeeId: '00000009',
          expiryCount: 0,
        ),
      ));

      expect(find.text('0건'), findsOneWidget);
      expect(find.text('유통기한 임박제품 : '), findsOneWidget);
    });

    testWidgets('T1: 0건일 때 건수 텍스트가 textSecondary 색상이어야 한다',
        (tester) async {
      await tester.pumpWidget(buildTestWidget(
        expiryAlert: const ExpiryAlert(
          branchName: '강남53지점',
          employeeName: '홍길동',
          employeeId: '00000009',
          expiryCount: 0,
        ),
      ));

      final countText = tester.widget<Text>(find.text('0건'));
      final style = countText.style!;
      expect(style.color, equals(AppColors.textSecondary));
    });

    testWidgets('T2: N건일 때 건수 텍스트가 otokiBlue 색상이어야 한다',
        (tester) async {
      await tester.pumpWidget(buildTestWidget(
        expiryAlert: const ExpiryAlert(
          branchName: '강남53지점',
          employeeName: '홍길동',
          employeeId: '00000009',
          expiryCount: 5,
        ),
      ));

      final countText = tester.widget<Text>(find.text('5건'));
      final style = countText.style!;
      expect(style.color, equals(AppColors.otokiBlue));
    });

    testWidgets('T3: expiryAlert가 null이면 카드가 숨겨져야 한다',
        (tester) async {
      await tester.pumpWidget(buildTestWidget(expiryAlert: null));

      expect(find.byType(SizedBox), findsOneWidget);
      expect(find.text('유통기한 임박제품 : '), findsNothing);
    });

    testWidgets('T4: 지점명+사원명(사번) 형식으로 표시되어야 한다', (tester) async {
      await tester.pumpWidget(buildTestWidget(
        expiryAlert: const ExpiryAlert(
          branchName: '강남53지점',
          employeeName: '홍길동',
          employeeId: '00000009',
          expiryCount: 3,
        ),
      ));

      expect(find.text('강남53지점, 홍길동(00000009)'), findsOneWidget);
    });

    testWidgets('카드 탭 시 onTap 콜백이 호출되어야 한다', (tester) async {
      var tapped = false;
      await tester.pumpWidget(buildTestWidget(
        expiryAlert: const ExpiryAlert(
          branchName: '강남53지점',
          employeeName: '홍길동',
          employeeId: '00000009',
          expiryCount: 3,
        ),
        onTap: () => tapped = true,
      ));

      await tester.tap(find.byType(InkWell));
      expect(tapped, isTrue);
    });
  });
}
