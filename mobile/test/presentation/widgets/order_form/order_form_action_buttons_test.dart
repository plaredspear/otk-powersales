import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/presentation/providers/order_form_state.dart';
import 'package:mobile/presentation/widgets/order_form/order_form_action_buttons.dart';

Widget _host({
  bool isSubmitting = false,
  SubmitBlockKind? blockKind,
  int zeroQuantityLineCount = 0,
  bool hasItems = true,
  VoidCallback? onSubmit,
  VoidCallback? onSaveDraft,
  VoidCallback? onDisabledTap,
  VoidCallback? onSaveDraftDisabledTap,
}) {
  return MaterialApp(
    home: Scaffold(
      body: OrderFormActionButtons(
        onDelete: () {},
        onSaveDraft: onSaveDraft ?? () {},
        onSubmit: onSubmit ?? () {},
        isSubmitting: isSubmitting,
        blockKind: blockKind,
        zeroQuantityLineCount: zeroQuantityLineCount,
        hasItems: hasItems,
        onDisabledTap: onDisabledTap,
        onSaveDraftDisabledTap: onSaveDraftDisabledTap,
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
        _host(blockKind: SubmitBlockKind.deadline, onSubmit: () => tapped = true),
      );

      expect(find.text('마감시간 지남'), findsOneWidget);
      expect(find.text('승인요청'), findsNothing);
      expect(_submitBackground(tester), AppColors.errorLight);
      expect(_labelColor(tester, '마감시간 지남'), AppColors.blockedForeground);

      await tester.tap(find.text('마감시간 지남'));
      expect(tapped, isFalse, reason: '마감 후에는 승인요청이 눌리면 안 된다');
    });

    testWidgets('여신 초과 → 여신한도 초과 라벨 + 붉은 배경', (tester) async {
      await tester.pumpWidget(_host(blockKind: SubmitBlockKind.loanExceeded));

      expect(find.text('여신한도 초과'), findsOneWidget);
      expect(_submitBackground(tester), AppColors.errorLight);
      expect(_labelColor(tester, '여신한도 초과'), AppColors.blockedForeground);
    });

    testWidgets('제품 100개 초과 → 초과 라벨 + 회색 배경 + 탭 차단', (tester) async {
      var tapped = false;
      await tester.pumpWidget(
        _host(blockKind: SubmitBlockKind.lineLimit, onSubmit: () => tapped = true),
      );

      expect(find.text('제품 100개 초과'), findsOneWidget);
      expect(find.text('승인요청'), findsNothing);
      expect(
        _submitBackground(tester),
        AppColors.surfaceVariant,
        reason: '품목을 덜어내면 해소되는 상태라 회색을 유지한다',
      );

      await tester.tap(find.text('제품 100개 초과'));
      expect(tapped, isFalse, reason: '100개 초과 상태에서는 승인요청이 눌리면 안 된다');
    });

    testWidgets('수량 0 라인 존재 → 건수 라벨 + 회색 배경 유지', (tester) async {
      await tester.pumpWidget(
        _host(blockKind: SubmitBlockKind.zeroQuantity, zeroQuantityLineCount: 1),
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
        _host(blockKind: SubmitBlockKind.zeroQuantity, zeroQuantityLineCount: 3),
      );

      expect(find.text('수량 미입력 3건'), findsOneWidget);
    });

    // 차단 사유 사이의 우선순위는 위젯이 아니라 제출 검증(OrderFormNotifier.submitBlock)이
    // 정한다. 우선순위 회귀는 order_form_provider_test 의 "submitBlock 우선순위" 가 지킨다.

    testWidgets('거래처 미선택 등 요약 불가 사유 → 승인요청 라벨 유지 + 탭 차단', (tester) async {
      var tapped = false;
      await tester.pumpWidget(
        _host(
          blockKind: SubmitBlockKind.account,
          onSubmit: () => tapped = true,
        ),
      );

      expect(find.text('승인요청'), findsOneWidget);
      expect(_submitBackground(tester), AppColors.surfaceVariant);

      await tester.tap(find.text('승인요청'));
      expect(tapped, isFalse);
    });

    testWidgets('수량 미입력 → 탭하면 사유 콜백 호출 (제출은 되지 않음)', (tester) async {
      var notified = false;
      var submitted = false;
      await tester.pumpWidget(
        _host(
          blockKind: SubmitBlockKind.zeroQuantity,
          zeroQuantityLineCount: 1,
          onSubmit: () => submitted = true,
          onDisabledTap: () => notified = true,
        ),
      );

      await tester.tap(find.text('수량 미입력 1건'));
      expect(notified, isTrue, reason: '사유를 알리고 해당 줄로 이동시켜야 한다');
      expect(submitted, isFalse, reason: '수량이 0이므로 제출하면 안 된다');
    });

    testWidgets('차단(마감) → 탭하면 사유 콜백 호출 (제출은 되지 않음)', (tester) async {
      var notified = false;
      var submitted = false;
      await tester.pumpWidget(
        _host(
          blockKind: SubmitBlockKind.deadline,
          zeroQuantityLineCount: 1,
          onSubmit: () => submitted = true,
          onDisabledTap: () => notified = true,
        ),
      );

      await tester.tap(find.text('마감시간 지남'));
      expect(notified, isTrue, reason: '해소 불가 사유도 왜 막혔는지는 알려야 한다');
      expect(submitted, isFalse);
    });

    testWidgets('제품 100개 초과 → 탭하면 사유 콜백 호출', (tester) async {
      var notified = false;
      await tester.pumpWidget(
        _host(blockKind: SubmitBlockKind.lineLimit, onDisabledTap: () => notified = true),
      );

      await tester.tap(find.text('제품 100개 초과'));
      expect(notified, isTrue);
    });

    testWidgets('필수 항목 미입력(수량 0 아님) → 탭하면 사유 콜백 호출', (tester) async {
      var notified = false;
      await tester.pumpWidget(
        _host(
          blockKind: SubmitBlockKind.account,
          onDisabledTap: () => notified = true,
        ),
      );

      await tester.tap(find.text('승인요청'));
      expect(notified, isTrue, reason: '거래처/납기일 미선택도 사유를 알려야 한다');
    });

    testWidgets('활성 상태 → 사유 콜백이 아니라 제출이 호출된다', (tester) async {
      var notified = false;
      var submitted = false;
      await tester.pumpWidget(
        _host(
          onSubmit: () => submitted = true,
          onDisabledTap: () => notified = true,
        ),
      );

      await tester.tap(find.text('승인요청'));
      expect(submitted, isTrue);
      expect(notified, isFalse);
    });

    testWidgets('제품 0건 → 임시저장 비활성 + 탭하면 사유 콜백 호출 (레거시 정합)', (tester) async {
      var saved = false;
      var notified = false;
      await tester.pumpWidget(
        _host(
          hasItems: false,
          blockKind: SubmitBlockKind.noItems,
          onSaveDraft: () => saved = true,
          onSaveDraftDisabledTap: () => notified = true,
        ),
      );

      await tester.tap(find.text('임시저장'));
      expect(saved, isFalse, reason: '담긴 제품이 없으면 임시저장되면 안 된다');
      expect(notified, isTrue);
    });

    testWidgets('제품 1건 이상 → 임시저장 활성', (tester) async {
      var saved = false;
      await tester.pumpWidget(_host(onSaveDraft: () => saved = true));

      await tester.tap(find.text('임시저장'));
      expect(saved, isTrue);
    });

    testWidgets('제출 중 → 인디케이터로 대체되고 라벨은 사라진다', (tester) async {
      await tester.pumpWidget(_host(isSubmitting: true));

      expect(find.text('승인요청'), findsNothing);
      expect(find.byType(CircularProgressIndicator), findsWidgets);
    });

    testWidgets('제출 중 → 탭해도 사유 콜백이 호출되지 않는다', (tester) async {
      var notified = false;
      await tester.pumpWidget(
        _host(isSubmitting: true, onDisabledTap: () => notified = true),
      );

      await tester.tap(find.byType(CircularProgressIndicator).last);
      expect(notified, isFalse, reason: '전송 중은 차단 사유가 아니다');
    });
  });
}
