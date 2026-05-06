import 'package:flutter/material.dart';

/// 페이지 이탈 시 확인 다이얼로그 (Spec #598 P2-M §2.6 / Q9).
///
/// 워딩 정확히 보존:
/// - title: "작성 중인 내용 있음"
/// - content: "작성 중인 내용이 사라집니다.\n임시저장 하시겠습니까?"
/// - 버튼: [그냥 나가기] / [임시저장]
class ExitConfirmDialog extends StatelessWidget {
  static const String titleText = '작성 중인 내용 있음';
  static const String contentText = '작성 중인 내용이 사라집니다.\n임시저장 하시겠습니까?';

  final VoidCallback onDiscard;
  final VoidCallback onSaveDraft;

  const ExitConfirmDialog({
    super.key,
    required this.onDiscard,
    required this.onSaveDraft,
  });

  /// 다이얼로그 표시 + 사용자 선택 결과 반환 (true=임시저장 시도, false=그냥 나가기).
  static Future<void> show(
    BuildContext context, {
    required VoidCallback onDiscard,
    required VoidCallback onSaveDraft,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => ExitConfirmDialog(
        onDiscard: () {
          Navigator.of(ctx).pop();
          onDiscard();
        },
        onSaveDraft: () {
          Navigator.of(ctx).pop();
          onSaveDraft();
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
        TextButton(onPressed: onDiscard, child: const Text('그냥 나가기')),
        TextButton(onPressed: onSaveDraft, child: const Text('임시저장')),
      ],
    );
  }
}
