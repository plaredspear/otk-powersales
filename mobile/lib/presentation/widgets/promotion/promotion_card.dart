import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/promotion.dart';
import 'promotion_amount_text.dart';

/// 행사 목록 카드 위젯
class PromotionCard extends StatelessWidget {
  final PromotionItem item;
  final VoidCallback? onTap;

  const PromotionCard({super.key, required this.item, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: AppSpacing.cardBorderRadius,
        child: Container(
          decoration: BoxDecoration(
            color: item.isClosed ? const Color(0xFFF5F5F5) : AppColors.card,
            borderRadius: AppSpacing.cardBorderRadius,
            border: Border.all(color: AppColors.border),
          ),
          padding: AppSpacing.cardPadding,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeader(),
              const SizedBox(height: AppSpacing.sm),
              if (item.promotionName != null)
                Text(
                  item.promotionName!,
                  style: AppTypography.headlineSmall,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              if (item.accountName != null) ...[
                const SizedBox(height: AppSpacing.xs),
                Text(
                  item.accountName!,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
              const SizedBox(height: AppSpacing.xs),
              Text(
                '${item.startDate} ~ ${_shortEndDate()}',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              if (item.standLocation != null) ...[
                const SizedBox(height: AppSpacing.xs),
                Text(
                  '매대: ${item.standLocation}',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
              const SizedBox(height: AppSpacing.sm),
              _buildAmountRow(),
              if (item.myScheduleDate != null) ...[
                const SizedBox(height: AppSpacing.xs),
                Text(
                  '내 투입일: ${item.myScheduleDate}',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.info,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      children: [
        Text(
          item.promotionNumber,
          style: AppTypography.labelMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        const Spacer(),
        if (item.category != null) _buildBadge(item.category!),
        if (item.promotionType != null) ...[
          const SizedBox(width: AppSpacing.xs),
          _buildBadge(item.promotionType!),
        ],
        if (item.isClosed) ...[
          const SizedBox(width: AppSpacing.xs),
          _buildBadge('마감', color: AppColors.textTertiary),
        ],
        const SizedBox(width: AppSpacing.xs),
        const Icon(Icons.chevron_right,
            size: AppSpacing.iconSize, color: AppColors.textTertiary),
      ],
    );
  }

  Widget _buildBadge(String text, {Color? color}) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: (color ?? AppColors.secondary).withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        text,
        style: AppTypography.labelSmall.copyWith(
          color: color ?? AppColors.secondary,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }

  Widget _buildAmountRow() {
    return Row(
      children: [
        Text(
          '실적 ',
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        PromotionAmountText(amount: item.actualAmount),
        Text(
          ' / 목표 ',
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        PromotionAmountText(amount: item.targetAmount),
      ],
    );
  }

  String _shortEndDate() {
    if (item.startDate.length >= 7 &&
        item.endDate.length >= 7 &&
        item.startDate.substring(0, 7) == item.endDate.substring(0, 7)) {
      // 같은 연-월이면 일만 표시
      return item.endDate.substring(5);
    }
    return item.endDate;
  }
}
