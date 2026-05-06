import 'package:flutter/material.dart';

/// 임시저장 발견 시 표시되는 다이얼로그 (Spec #598 P2-M §2.2).
///
/// 워딩 정확히 보존:
/// - title: "임시저장 발견"
/// - content: "이전에 작성 중인 내용이 있습니다.\n이어서 작성하시겠습니까?"
/// - 버튼: "아니오" / "예" (긍정 버튼은 우측)
class DraftRestoreDialog extends StatelessWidget {
  static const String titleText = '임시저장 발견';
  static const String contentText = '이전에 작성 중인 내용이 있습니다.\n이어서 작성하시겠습니까?';

  final VoidCallback onAccept;
  final VoidCallback onDecline;

  const DraftRestoreDialog({
    super.key,
    required this.onAccept,
    required this.onDecline,
  });

  /// 화면에 다이얼로그를 띄우고 사용자 선택을 처리한다.
  ///
  /// `barrierDismissible: false` — 사용자는 반드시 [예]/[아니오] 중 하나를 선택해야 한다.
  static Future<void> show(
    BuildContext context, {
    required VoidCallback onAccept,
    required VoidCallback onDecline,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => DraftRestoreDialog(
        onAccept: () {
          Navigator.of(ctx).pop();
          onAccept();
        },
        onDecline: () {
          Navigator.of(ctx).pop();
          onDecline();
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text(titleText),
      content: const Text(contentText),
      actions: [
        TextButton(onPressed: onDecline, child: const Text('아니오')),
        TextButton(onPressed: onAccept, child: const Text('예')),
      ],
    );
  }
}
