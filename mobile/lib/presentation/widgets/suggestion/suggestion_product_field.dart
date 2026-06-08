import 'package:flutter/material.dart';

import 'suggestion_logistics_claim_fields.dart';

/// 제안하기 제품 선택 필드
///
/// - 신제품 제안: [enabled] = false → "신제품 제안 시 선택 불필요" 안내, 버튼 비활성
/// - 기존제품 / 물류 클레임: 바코드·선택 버튼 활성, [required] = true ("대표 제품 *")
class SuggestionProductField extends StatelessWidget {
  const SuggestionProductField({
    super.key,
    required this.enabled,
    this.label = '제품',
    this.required = false,
    this.guideText,
    this.productName,
    this.productCode,
    this.onBarcodePressed,
    this.onSelectPressed,
  });

  final bool enabled;
  final String label;
  final bool required;

  /// 미선택 시 노출할 안내 문구 (빨간색 — 레거시 정합)
  final String? guideText;

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
            SuggestionFieldLabel(text: label, required: required),
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

        // 미선택 안내 문구 (레거시 빨간 가이드)
        if (guideText != null && !hasProduct) ...[
          const SizedBox(height: 6),
          Text(
            guideText!,
            style: const TextStyle(fontSize: 12, color: Colors.red),
          ),
        ],
      ],
    );
  }
}
