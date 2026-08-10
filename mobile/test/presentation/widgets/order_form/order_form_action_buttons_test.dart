import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/presentation/widgets/order_form/order_form_action_buttons.dart';

Widget _host({
  bool isSubmitting = false,
  bool requiredFieldsFilled = true,
  bool loanExceeded = false,
  bool pastDeadline = false,
  int zeroQuantityLineCount = 0,
  VoidCallback? onSubmit,
}) {
  return MaterialApp(
    home: Scaffold(
      body: OrderFormActionButtons(
        onDelete: () {},
        onSaveDraft: () {},
        onSubmit: onSubmit ?? () {},
        isSubmitting: isSubmitting,
        requiredFieldsFilled: requiredFieldsFilled,
        loanExceeded: loanExceeded,
        pastDeadline: pastDeadline,
        zeroQuantityLineCount: zeroQuantityLineCount,
      ),
    ),
  );
}

/// 승인요청 세그먼트의 배경색 — 하단 바에서 마지막 Material 이 승인요청이다.
Color _submitBackground(WidgetTester tester) {
  final materials = tester
      .widgetList<Material>(
        find.descendant(
          of: find.byType(OrderFormActionButtons),
          matching: find.byType(Material),
        ),
      )
      .toList();
  return materials.last.color!;
}

Color _labelColor(WidgetTester tester, String label) {
  return tester.widget<Text>(find.text(label)).style!.color!;
}

void main() {
  group('OrderFormActionButtons 승인요청 상태', () {
    testWidgets('정상 → 승인요청 라벨 + 옐로 배경 + 탭 가능', (tester) async {
      var tapped = false;
      await tester.pumpWidget(_host(onSubmit: () => tapped = true));

      expect(find.text('승인요청'), findsOneWidget);
      expect(_submitBackground(tester), AppColors.legacyYellow);
      expect(_labelColor(tester, '승인요청'), AppColors.onPrimary);

      await tester.tap(find.text('승인요청'));
      expect(tapped, isTrue);
    });

    testWidgets('마감 경과 → 마감시간 지남 라벨 + 붉은 배경 + 탭 차단', (tester) async {
      var tapped = false;
      await tester.pumpWidget(
        _host(pastDeadline: true, onSubmit: () => tapped = true),
      );

      expect(find.text('마감시간 지남'), findsOneWidget);
      expect(find.text('승인요청'), findsNothing);
      expect(_submitBackground(tester), AppColors.errorLight);
      expect(_labelColor(tester, '마감시간 지남'), AppColors.blockedForeground);

      await tester.tap(find.text('마감시간 지남'));
      expect(tapped, isFalse, reason: '마감 후에는 승인요청이 눌리면 안 된다');
    });

    testWidgets('여신 초과 → 여신한도 초과 라벨 + 붉은 배경', (tester) async {
      await tester.pumpWidget(_host(loanExceeded: true));

      expect(find.text('여신한도 초과'), findsOneWidget);
      expect(_submitBackground(tester), AppColors.errorLight);
      expect(_labelColor(tester, '여신한도 초과'), AppColors.blockedForeground);
    });

    testWidgets('수량 0 라인 존재 → 건수 라벨 + 회색 배경 유지', (tester) async {
      await tester.pumpWidget(
        _host(requiredFieldsFilled: false, zeroQuantityLineCount: 1),
      );

      expect(find.text('수량 미입력 1건'), findsOneWidget);
      expect(
        _submitBackground(tester),
        AppColors.surfaceVariant,
        reason: '수정 가능한 상태는 경고색이 아니라 회색을 유지한다',
      );
      expect(
        _labelColor(tester, '수량 미입력 1건'),
        AppColors.legacyTextSub,
        reason: 'textTertiary(2.35:1) 대신 legacyTextSub(11.09:1) 로 가독성 확보',
      );
    });

    testWidgets('수량 0 라인 다건 → 건수를 그대로 노출', (tester) async {
      await tester.pumpWidget(
        _host(requiredFieldsFilled: false, zeroQuantityLineCount: 3),
      );

      expect(find.text('수량 미입력 3건'), findsOneWidget);
    });

    testWidgets('마감 + 수량 0 동시 → 마감이 우선 (해소 불가가 먼저)', (tester) async {
      await tester.pumpWidget(
        _host(
          pastDeadline: true,
          requiredFieldsFilled: false,
          zeroQuantityLineCount: 2,
        ),
      );

      expect(find.text('마감시간 지남'), findsOneWidget);
      expect(find.text('수량 미입력 2건'), findsNothing);
    });

    testWidgets('마감 + 여신 초과 동시 → 마감이 우선', (tester) async {
      await tester.pumpWidget(_host(pastDeadline: true, loanExceeded: true));

      expect(find.text('마감시간 지남'), findsOneWidget);
      expect(find.text('여신한도 초과'), findsNothing);
    });

    testWidgets('필수 항목 미입력(수량 0 아님) → 승인요청 라벨 유지 + 탭 차단', (tester) async {
      var tapped = false;
      await tester.pumpWidget(
        _host(
          requiredFieldsFilled: false,
          zeroQuantityLineCount: 0,
          onSubmit: () => tapped = true,
        ),
      );

      expect(find.text('승인요청'), findsOneWidget);
      expect(_submitBackground(tester), AppColors.surfaceVariant);

      await tester.tap(find.text('승인요청'));
      expect(tapped, isFalse);
    });

    testWidgets('제출 중 → 인디케이터로 대체되고 라벨은 사라진다', (tester) async {
      await tester.pumpWidget(_host(isSubmitting: true));

      expect(find.text('승인요청'), findsNothing);
      expect(find.byType(CircularProgressIndicator), findsWidgets);
    });
  });
}
