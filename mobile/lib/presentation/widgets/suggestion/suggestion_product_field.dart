import 'package:flutter/material.dart';

import 'suggestion_logistics_claim_fields.dart';

/// 제안하기 제품 선택 필드 (레거시 suggestWrite.jsp 정합)
///
/// - 신제품 제안: [enabled] = false → "신제품 제안 시 선택 불필요" 안내, 버튼 비활성
/// - 기존제품 / 물류 클레임: 바코드·선택 pill 버튼 활성, [required] = true ("대표 제품 *")
///
/// 레거시는 라벨 우측에 바코드/+선택 pill 버튼을 두고, 미선택 시 빨간 안내만
/// 노출한다(별도 입력 박스 없음). 선택 시 제품명/코드를 평면 텍스트로 보여준다.
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

    return SuggestionFieldRow(
      label: label,
      required: required,
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          SuggestionPillButton(
            icon: Icons.qr_code_scanner,
            label: '바코드',
            onPressed: enabled ? onBarcodePressed : null,
          ),
          const SizedBox(width: 8),
          SuggestionPillButton(
            icon: Icons.add,
            label: '선택',
            onPressed: enabled ? onSelectPressed : null,
          ),
        ],
      ),
      guideText: (!hasProduct && guideText != null) ? guideText : null,
      child: _buildValue(hasProduct),
    );
  }

  Widget? _buildValue(bool hasProduct) {
    if (hasProduct) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            productName!,
            style: const TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w600,
              color: kSuggestionValueColor,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            productCode!,
            style: const TextStyle(
              fontSize: 13,
              color: kSuggestionPlaceholderColor,
            ),
          ),
        ],
      );
    }
    // 신제품 제안 — 선택 불필요 안내
    if (!enabled) {
      return const Text(
        '신제품 제안 시 선택 불필요',
        style: TextStyle(fontSize: 15, color: kSuggestionPlaceholderColor),
      );
    }
    // 선택 가능하나 미선택 — 레거시처럼 빨간 안내(guideText)만 노출, 별도 값 없음
    return null;
  }
}
