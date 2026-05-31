import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/promotion.dart';
import 'promotion_amount_text.dart';

/// 행사 상세 - 배정 조원 목록 위젯
class PromotionEmployeeList extends StatelessWidget {
  final List<PromotionEmployee> employees;

  /// 본인 행에서 일매출 마감 진입 시 호출 (null 이면 진입점 미노출).
  final void Function(PromotionEmployee employee)? onDailySalesTap;

  const PromotionEmployeeList({
    super.key,
    required this.employees,
    this.onDailySalesTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '배정 조원 (${employees.length}명)',
          style: AppTypography.headlineSmall,
        ),
        const SizedBox(height: AppSpacing.md),
        Container(
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.border),
            borderRadius: AppSpacing.cardBorderRadius,
          ),
          child: ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: employees.length,
            separatorBuilder: (_, _) => const Divider(
              height: 1,
              color: AppColors.border,
            ),
            itemBuilder: (context, index) =>
                _buildEmployeeItem(employees[index]),
          ),
        ),
      ],
    );
  }

  Widget _buildEmployeeItem(PromotionEmployee emp) {
    return Padding(
      padding: AppSpacing.cardPadding,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                emp.employeeName ?? '-',
                style: AppTypography.bodyMedium.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(width: AppSpacing.md),
              if (emp.scheduleDate != null)
                Text(
                  emp.scheduleDate!,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              if (emp.workType3 != null) ...[
                const SizedBox(width: AppSpacing.sm),
                _buildWorkTypeBadge(emp.workType3!),
              ],
            ],
          ),
          const SizedBox(height: AppSpacing.xs),
          Row(
            children: [
              if (emp.professionalPromotionTeam != null) ...[
                Text(
                  emp.professionalPromotionTeam!,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
              ],
              Text(
                '목표 ',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              PromotionAmountText(amount: emp.targetAmount),
              const SizedBox(width: AppSpacing.md),
              Text(
                '실적 ',
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
              PromotionAmountText(amount: emp.actualAmount),
            ],
          ),
          if (emp.isMine && onDailySalesTap != null) ...[
            const SizedBox(height: AppSpacing.sm),
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton.icon(
                onPressed: () => onDailySalesTap!(emp),
                icon: Icon(
                  emp.isClosed ? Icons.visibility_outlined : Icons.edit_outlined,
                  size: 16,
                ),
                label: Text(emp.isClosed ? '일매출 보기' : '일매출 마감'),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildWorkTypeBadge(String workType) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: AppColors.secondary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        workType,
        style: AppTypography.labelSmall.copyWith(
          color: AppColors.secondary,
        ),
      ),
    );
  }
}
