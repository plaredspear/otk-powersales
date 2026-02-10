import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

/// 주문 취소 화면의 개별 제품 항목 위젯
///
/// 체크박스 + 제품명 + 제품코드 + 수량 정보를 표시합니다.
/// 체크박스 선택/해제를 통해 취소할 제품을 선택합니다.
class CancelProductItem extends StatelessWidget {
  /// 주문한 제품 정보
  final OrderedItem item;

  /// 선택 여부
  final bool isSelected;

  /// 선택/해제 콜백
  final VoidCallback onToggle;

  const CancelProductItem({
    super.key,
    required this.item,
    required this.isSelected,
    required this.onToggle,
  });

  String _formatBoxes(double boxes) {
    if (boxes == boxes.toInt()) {
      return boxes.toInt().toString();
    }
    return boxes.toStringAsFixed(1);
  }

  String _formatPieces(int pieces) {
    return NumberFormat('#,###').format(pieces);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onToggle,
      child: Padding(
        padding: EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 체크박스
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: isSelected,
                onChanged: (_) => onToggle(),
                activeColor: AppColors.error,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
            SizedBox(width: AppSpacing.md),
            // 제품 정보
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 제품명
                  Text(
                    item.productName,
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textPrimary,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  SizedBox(height: AppSpacing.xs),
                  // 제품코드 | 총 주문수량
                  Text(
                    '제품코드 ${item.productCode} | '
                    '${_formatBoxes(item.totalQuantityBoxes)}박스 '
                    '(${_formatPieces(item.totalQuantityPieces)}개)',
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
