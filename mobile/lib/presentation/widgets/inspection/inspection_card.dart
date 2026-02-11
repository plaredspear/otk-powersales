import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/inspection_list_item.dart';

/// 현장점검 목록 카드 위젯
///
/// 분류 태그([자사]/[경쟁사]), 거래처명, 점검일, 현장유형 표시.
/// 탭 시 상세 화면으로 이동.
class InspectionCard extends StatelessWidget {
  final InspectionListItem item;
  final VoidCallback? onTap;

  const InspectionCard({
    super.key,
    required this.item,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy.MM.dd(E)', 'ko_KR');

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: AppSpacing.cardBorderRadius,
        child: Container(
          padding: AppSpacing.cardPadding,
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: AppSpacing.cardBorderRadius,
            border: Border.all(color: AppColors.border),
            boxShadow: AppSpacing.cardShadow,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 분류 태그 + 거래처명
              Row(
                children: [
                  _buildCategoryBadge(),
                  const SizedBox(width: AppSpacing.sm),
                  Expanded(
                    child: Text(
                      item.storeName,
                      style: AppTypography.headlineSmall,
                      overflow: TextOverflow.ellipsis,
                      maxLines: 1,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.xs),

              // 점검일 + 현장유형
              Row(
                children: [
                  Icon(
                    Icons.calendar_today,
                    size: 14,
                    color: AppColors.textTertiary,
                  ),
                  const SizedBox(width: AppSpacing.xs),
                  Text(
                    '점검일 ${dateFormat.format(item.inspectionDate)}',
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Container(
                    width: 1,
                    height: 12,
                    color: AppColors.border,
                  ),
                  const SizedBox(width: AppSpacing.sm),
                  Text(
                    item.fieldType,
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCategoryBadge() {
    final isOwn = item.category == InspectionCategory.OWN;
    final label = isOwn ? '자사' : '경쟁사';
    final backgroundColor = isOwn ? AppColors.primary : AppColors.secondary;
    final textColor = isOwn ? AppColors.onPrimary : AppColors.onSecondary;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        label,
        style: AppTypography.labelSmall.copyWith(
          color: textColor,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
