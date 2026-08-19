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

  /// 담긴 제품이 없어 임시저장할 수 없을 때의 문구 (레거시 정합 — 제품 1건 이상만 임시저장).
  static const String discardOnlyContentText = '작성 중인 내용이 사라집니다.\n나가시겠습니까?';

  final VoidCallback onDiscard;
  final VoidCallback onSaveDraft;

  /// 임시저장 가능 여부. false 면 임시저장 버튼을 아예 노출하지 않는다 —
  /// 눌러도 실패할 선택지를 권하지 않기 위함.
  final bool canSaveDraft;

  const ExitConfirmDialog({
    super.key,
    required this.onDiscard,
    required this.onSaveDraft,
    this.canSaveDraft = true,
  });

  /// 다이얼로그 표시 + 사용자 선택 결과 반환 (true=임시저장 시도, false=그냥 나가기).
  static Future<void> show(
    BuildContext context, {
    required VoidCallback onDiscard,
    required VoidCallback onSaveDraft,
    bool canSaveDraft = true,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => ExitConfirmDialog(
        canSaveDraft: canSaveDraft,
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
      content: Text(canSaveDraft ? contentText : discardOnlyContentText),
      actions: [
        TextButton(onPressed: onDiscard, child: const Text('그냥 나가기')),
        if (canSaveDraft)
          TextButton(onPressed: onSaveDraft, child: const Text('임시저장')),
      ],
    );
  }
}
