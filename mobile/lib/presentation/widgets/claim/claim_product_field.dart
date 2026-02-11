import 'package:flutter/material.dart';

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

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 필드 라벨 + 버튼들
        Row(
          children: [
            const Text(
              '제품 *',
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
            const Spacer(),
            // 바코드 스캔 버튼
            OutlinedButton.icon(
              onPressed: onBarcodePressed,
              icon: const Icon(Icons.qr_code_scanner, size: 18),
              label: const Text('바코드'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: const Size(0, 32),
              ),
            ),
            const SizedBox(width: 8),
            // 제품 선택 버튼
            OutlinedButton.icon(
              onPressed: onProductSelectPressed,
              icon: const Icon(Icons.add, size: 18),
              label: const Text('선택'),
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                minimumSize: const Size(0, 32),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),

        // 제품 정보 표시 영역
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey.shade300),
            borderRadius: BorderRadius.circular(4),
          ),
          child: hasProduct
              ? _ProductInfo(
                  productName: productName!,
                  productCode: productCode!,
                )
              : const _ProductPlaceholder(),
        ),
      ],
    );
  }
}

/// 제품 정보 표시
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
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          productCode,
          style: TextStyle(
            fontSize: 12,
            color: Colors.grey.shade600,
          ),
        ),
      ],
    );
  }
}

/// 제품 미선택 플레이스홀더
class _ProductPlaceholder extends StatelessWidget {
  const _ProductPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Text(
      '[제품 선택]',
      style: TextStyle(
        fontSize: 14,
        color: Colors.grey.shade600,
      ),
    );
  }
}
