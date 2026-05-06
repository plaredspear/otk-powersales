import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/widgets/order_form/draft_delete_dialog.dart';
import 'package:mobile/presentation/widgets/order_form/draft_restore_dialog.dart';
import 'package:mobile/presentation/widgets/order_form/exit_confirm_dialog.dart';

void main() {
  group('Spec #598 P2-M 다이얼로그 워딩 (D1~D3)', () {
    testWidgets('D1 — DraftRestoreDialog 워딩', (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: Builder(builder: (context) {
          return ElevatedButton(
            onPressed: () => DraftRestoreDialog.show(
              context,
              onAccept: () {},
              onDecline: () {},
            ),
            child: const Text('open'),
          );
        })),
      );
      await tester.tap(find.text('open'));
      await tester.pump();

      expect(find.text(DraftRestoreDialog.titleText), findsOneWidget);
      expect(find.text(DraftRestoreDialog.contentText), findsOneWidget);
      expect(find.text('아니오'), findsOneWidget);
      expect(find.text('예'), findsOneWidget);
      expect(DraftRestoreDialog.contentText, '이전에 작성 중인 내용이 있습니다.\n이어서 작성하시겠습니까?');
    });

    testWidgets('D2 — DraftDeleteDialog 워딩', (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: Builder(builder: (context) {
          return ElevatedButton(
            onPressed: () => DraftDeleteDialog.show(
              context,
              onConfirm: () {},
            ),
            child: const Text('open'),
          );
        })),
      );
      await tester.tap(find.text('open'));
      await tester.pump();

      expect(find.text(DraftDeleteDialog.titleText), findsOneWidget);
      expect(find.text(DraftDeleteDialog.contentText), findsOneWidget);
      expect(find.text('취소'), findsOneWidget);
      expect(find.text('삭제'), findsOneWidget);
      expect(DraftDeleteDialog.contentText, '임시저장 데이터를 삭제하시겠습니까?');
    });

    testWidgets('D3 — ExitConfirmDialog 워딩', (tester) async {
      await tester.pumpWidget(
        MaterialApp(home: Builder(builder: (context) {
          return ElevatedButton(
            onPressed: () => ExitConfirmDialog.show(
              context,
              onDiscard: () {},
              onSaveDraft: () {},
            ),
            child: const Text('open'),
          );
        })),
      );
      await tester.tap(find.text('open'));
      await tester.pump();

      expect(find.text(ExitConfirmDialog.titleText), findsOneWidget);
      expect(find.text(ExitConfirmDialog.contentText), findsOneWidget);
      expect(find.text('그냥 나가기'), findsOneWidget);
      expect(find.text('임시저장'), findsOneWidget);
      expect(ExitConfirmDialog.contentText, '작성 중인 내용이 사라집니다.\n임시저장 하시겠습니까?');
    });
  });
}
