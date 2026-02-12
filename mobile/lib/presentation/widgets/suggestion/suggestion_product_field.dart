import 'package:flutter/material.dart';

/// 제안하기 제품 선택 필드
class SuggestionProductField extends StatelessWidget {
  const SuggestionProductField({
    super.key,
    required this.enabled,
    this.productName,
    this.productCode,
    this.onBarcodePressed,
    this.onSelectPressed,
  });

  final bool enabled;
  final String? productName;
  final String? productCode;
  final VoidCallback? onBarcodePressed;
  final VoidCallback? onSelectPressed;

  @override
  Widget build(BuildContext context) {
    final hasProduct = productName != null && productCode != null;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 레이블과 버튼
        Row(
          children: [
            const Text(
              '제품',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
            const Spacer(),
            // 바코드 버튼
            OutlinedButton.icon(
              onPressed: enabled ? onBarcodePressed : null,
              icon: const Icon(Icons.qr_code_scanner, size: 18),
              label: const Text('바코드'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: const Size(0, 36),
                visualDensity: VisualDensity.compact,
              ),
            ),
            const SizedBox(width: 8),
            // 선택 버튼
            OutlinedButton.icon(
              onPressed: enabled ? onSelectPressed : null,
              icon: const Icon(Icons.add, size: 18),
              label: const Text('선택'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: const Size(0, 36),
                visualDensity: VisualDensity.compact,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),

        // 제품 표시 필드
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: enabled ? Colors.white : Colors.grey.shade100,
            border: Border.all(
              color: enabled ? Colors.grey.shade300 : Colors.grey.shade200,
            ),
            borderRadius: BorderRadius.circular(4),
          ),
          child: hasProduct
              ? Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      productName!,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      productCode!,
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey.shade600,
                      ),
                    ),
                  ],
                )
              : Text(
                  enabled ? '제품 선택' : '신제품 제안 시 선택 불필요',
                  style: TextStyle(
                    fontSize: 14,
                    color: Colors.grey.shade600,
                  ),
                ),
        ),
      ],
    );
  }
}
