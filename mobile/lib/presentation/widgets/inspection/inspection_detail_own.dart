import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/inspection_detail.dart';

/// 자사 현장점검 상세 정보 위젯
///
/// 제품명, 제품코드, 설명을 표시합니다.
class InspectionDetailOwnWidget extends StatelessWidget {
  final InspectionDetail detail;

  const InspectionDetailOwnWidget({
    super.key,
    required this.detail,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 섹션 제목
          Text(
            '자사 활동 정보',
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppSpacing.md),

          // 제품 정보
          if (detail.productName != null || detail.productCode != null) ...[
            _buildInfoRow(
              '제품',
              detail.productName != null && detail.productCode != null
                  ? '${detail.productName}(${detail.productCode})'
                  : detail.productName ?? detail.productCode ?? '-',
            ),
            const SizedBox(height: AppSpacing.sm),
          ],

          // 설명
          if (detail.description != null && detail.description!.isNotEmpty) ...[
            _buildInfoRow('설명', detail.description!),
          ],
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 80,
          child: Text(
            label,
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: Text(
            value,
            style: AppTypography.bodyMedium,
          ),
        ),
      ],
    );
  }
}
