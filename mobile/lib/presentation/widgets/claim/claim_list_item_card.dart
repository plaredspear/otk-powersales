import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/claim_list_item.dart';
import 'claim_status_badge.dart';

/// 클레임 목록 카드 위젯
class ClaimListItemCard extends StatelessWidget {
  final ClaimListItem item;
  final VoidCallback? onTap;

  const ClaimListItemCard({
    super.key,
    required this.item,
    this.onTap,
  });

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
          padding: AppSpacing.cardPadding,
          decoration: BoxDecoration(
            color: AppColors.card,
            borderRadius: AppSpacing.cardBorderRadius,
            border: Border.all(color: AppColors.border),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeader(),
              const SizedBox(height: AppSpacing.xs),
              _buildAccountName(),
              const SizedBox(height: AppSpacing.xs),
              _buildCategoryRow(),
              if (item.defectDescription != null &&
                  item.defectDescription!.isNotEmpty) ...[
                const SizedBox(height: AppSpacing.xs),
                _buildDescription(),
              ],
              const SizedBox(height: AppSpacing.xs),
              _buildDate(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    final productName = item.productName ?? '-';
    final title = item.claimNo != null && item.claimNo!.isNotEmpty
        ? '[${item.claimNo}] $productName'
        : productName;
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: AppTypography.bodySmall.copyWith(
              fontWeight: FontWeight.w700,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        ClaimStatusBadge(label: item.actionStatusLabel),
      ],
    );
  }

  Widget _buildAccountName() {
    return Text(
      item.accountName ?? '-',
      style: AppTypography.bodySmall.copyWith(
        color: AppColors.textSecondary,
      ),
    );
  }

  Widget _buildCategoryRow() {
    final category = [
      item.categoryLabel,
      item.subcategoryLabel,
    ].where((e) => e != null).join(' > ');
    final quantity =
        item.defectQuantity != null ? '${item.defectQuantity}개' : '';

    return Row(
      children: [
        Expanded(
          child: Text(
            category.isNotEmpty ? category : '-',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        if (quantity.isNotEmpty)
          Text(
            quantity,
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
      ],
    );
  }

  Widget _buildDescription() {
    return Text(
      item.defectDescription!,
      style: AppTypography.bodySmall.copyWith(
        color: AppColors.textSecondary,
      ),
      maxLines: 2,
      overflow: TextOverflow.ellipsis,
    );
  }

  Widget _buildDate() {
    // 등록일 표시 — 레거시 SF `IF_REST_MOBILE_ClaimSearch` 가 목록 조회를 CreatedDate 로 필터하고
    // 응답의 ClaimDate 자리에도 CreatedDate 를 실어 보낸다. 목록의 기간 필터 축과 표시 축을 일치시킨다.
    return Text(
      DateFormat('yyyy-MM-dd').format(item.createdAt),
      style: AppTypography.labelSmall.copyWith(
        color: AppColors.textSecondary,
      ),
    );
  }
}
