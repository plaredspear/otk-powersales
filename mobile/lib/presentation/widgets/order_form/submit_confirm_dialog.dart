import 'package:flutter/material.dart';

/// 승인요청 확인 다이얼로그.
///
/// 주문서 검증 통과 후, 실제 전송 전 사용자 확인을 요청한다.
class SubmitConfirmDialog extends StatelessWidget {
  static const String titleText = '승인요청';
  static const String contentText = '승인요청 하시겠습니까?';

  final VoidCallback onConfirm;
  final VoidCallback onCancel;

  const SubmitConfirmDialog({
    super.key,
    required this.onConfirm,
    required this.onCancel,
  });

  static Future<void> show(
    BuildContext context, {
    required VoidCallback onConfirm,
    required VoidCallback onCancel,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => SubmitConfirmDialog(
        onConfirm: () {
          Navigator.of(ctx).pop();
          onConfirm();
        },
        onCancel: () {
          Navigator.of(ctx).pop();
          onCancel();
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
        TextButton(onPressed: onCancel, child: const Text('아니오')),
        TextButton(onPressed: onConfirm, child: const Text('예')),
      ],
    );
  }
}
