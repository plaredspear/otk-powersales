import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';

/// 임시저장 삭제 확인 다이얼로그 (Spec #598 P2-M §2.5).
///
/// 워딩 정확히 보존:
/// - title: "임시저장 삭제"
/// - content: "임시저장 데이터를 삭제하시겠습니까?"
class DraftDeleteDialog extends StatelessWidget {
  static const String titleText = '임시저장 삭제';
  static const String contentText = '임시저장 데이터를 삭제하시겠습니까?';

  final VoidCallback onConfirm;

  const DraftDeleteDialog({super.key, required this.onConfirm});

  static Future<void> show(
    BuildContext context, {
    required VoidCallback onConfirm,
  }) {
    return showDialog<void>(
      context: context,
      builder: (ctx) => DraftDeleteDialog(
        onConfirm: () {
          Navigator.of(ctx).pop();
          onConfirm();
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
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('취소'),
        ),
        TextButton(
          onPressed: onConfirm,
          child: Text('삭제', style: TextStyle(color: AppColors.error)),
        ),
      ],
    );
  }
}
