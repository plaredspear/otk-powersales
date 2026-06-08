import 'package:flutter/material.dart';

import 'claim_form_row.dart';

/// 클레임 제품 선택 필드
class ClaimProductField extends StatelessWidget {
  const ClaimProductField({
    super.key,
    required this.productName,
    required this.productCode,
    required this.onBarcodePressed,
    required this.onProductSelectPressed,
  });

  final String? productName;
  final String? productCode;
  final VoidCallback onBarcodePressed;
  final VoidCallback onProductSelectPressed;

  @override
  Widget build(BuildContext context) {
    final hasProduct = productName != null && productName!.isNotEmpty;

    return ClaimFormRow(
      label: '제품',
      isRequired: true,
      alignTrailingTop: true,
      trailing: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          ClaimPillButton(
            icon: Icons.qr_code_scanner,
            label: '바코드',
            onPressed: onBarcodePressed,
          ),
          const SizedBox(width: 8),
          ClaimPillButton(
            icon: Icons.add,
            label: '선택',
            onPressed: onProductSelectPressed,
          ),
        ],
      ),
      below: hasProduct
          ? _ProductInfo(
              productName: productName!,
              productCode: productCode ?? '',
            )
          : const ClaimValueText(value: null, placeholder: '제품 선택'),
    );
  }
}

/// 제품 정보 표시 (제품명 + 코드)
class _ProductInfo extends StatelessWidget {
  const _ProductInfo({
    required this.productName,
    required this.productCode,
  });

  final String productName;
  final String productCode;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          productName,
          style: const TextStyle(
            fontSize: 14,
            color: ClaimFormColors.value,
          ),
        ),
        if (productCode.isNotEmpty) ...[
          const SizedBox(height: 2),
          Text(
            productCode,
            style: const TextStyle(
              fontSize: 12,
              color: ClaimFormColors.placeholder,
            ),
          ),
        ],
      ],
    );
  }
}
