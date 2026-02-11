import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/shelf_life_item.dart';

/// 유통기한 삭제용 제품 카드 위젯
///
/// 체크박스 + 제품 정보 표시. 삭제 화면에서 사용.
class ShelfLifeDeleteCard extends StatelessWidget {
  final ShelfLifeItem item;
  final bool isSelected;
  final ValueChanged<bool?> onChanged;

  const ShelfLifeDeleteCard({
    super.key,
    required this.item,
    required this.isSelected,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy.MM.dd(E)', 'ko_KR');

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.sm,
          vertical: AppSpacing.md,
        ),
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: AppSpacing.cardBorderRadius,
          border: Border.all(
            color: isSelected ? AppColors.error.withValues(alpha: 0.3) : AppColors.border,
          ),
          boxShadow: AppSpacing.cardShadow,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 체크박스
            Checkbox(
              value: isSelected,
              onChanged: onChanged,
              activeColor: AppColors.error,
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              visualDensity: VisualDensity.compact,
            ),
            const SizedBox(width: AppSpacing.xs),

            // 제품 정보
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 제품명 + 제품코드
                  Text(
                    '${item.productName}(${item.productCode})',
                    style: AppTypography.headlineSmall,
                    overflow: TextOverflow.ellipsis,
                    maxLines: 2,
                  ),
                  const SizedBox(height: AppSpacing.xs),

                  // 거래처명 | 유통기한 날짜
                  Text(
                    '${item.storeName} | ${dateFormat.format(item.expiryDate)}까지',
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: AppSpacing.xs),

                  // 알림일 + D-DAY
                  Row(
                    children: [
                      Icon(
                        Icons.notifications_outlined,
                        size: 14,
                        color: AppColors.textTertiary,
                      ),
                      const SizedBox(width: AppSpacing.xs),
                      Expanded(
                        child: Text(
                          dateFormat.format(item.alertDate),
                          style: AppTypography.bodySmall.copyWith(
                            color: AppColors.textTertiary,
                          ),
                        ),
                      ),
                      _buildDDayBadge(),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDDayBadge() {
    final dDayText = item.dDay == 0
        ? 'D-DAY'
        : item.dDay > 0
            ? '${item.dDay}일'
            : '${item.dDay.abs()}일 지남';

    final color = item.isExpired ? AppColors.error : AppColors.warning;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        dDayText,
        style: AppTypography.labelSmall.copyWith(
          color: color,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
