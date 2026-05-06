import 'package:flutter/material.dart';

/// 납기일 +10일 확인 다이얼로그 (Spec #598 P3-M §2.6 (I) / Q7-1).
///
/// 워딩 정확히 보존:
/// - title: "납기일 확인"
/// - content: "납기일자가 주문일자로부터 10일 이상입니다.\n그래도 주문을 진행하시겠습니까?"
class DeliveryDateWarningDialog extends StatelessWidget {
  static const String titleText = '납기일 확인';
  static const String contentText =
      '납기일자가 주문일자로부터 10일 이상입니다.\n그래도 주문을 진행하시겠습니까?';

  final VoidCallback onConfirm;
  final VoidCallback onCancel;

  const DeliveryDateWarningDialog({
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
      builder: (ctx) => DeliveryDateWarningDialog(
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
