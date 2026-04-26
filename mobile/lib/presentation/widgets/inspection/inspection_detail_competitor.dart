import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/inspection_detail.dart';

/// 경쟁사 현장점검 상세 정보 위젯
///
/// 경쟁사명, 활동 내용, 시식 정보를 표시합니다.
class InspectionDetailCompetitorWidget extends StatelessWidget {
  final InspectionDetail detail;

  const InspectionDetailCompetitorWidget({
    super.key,
    required this.detail,
  });

  @override
  Widget build(BuildContext context) {
    final numberFormat = NumberFormat('#,###');

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
            '경쟁사 활동 정보',
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: AppSpacing.md),

          // 경쟁사명
          if (detail.competitorName != null &&
              detail.competitorName!.isNotEmpty) ...[
            _buildInfoRow('경쟁사', detail.competitorName!),
            const SizedBox(height: AppSpacing.sm),
          ],

          // 활동 내용
          if (detail.competitorActivity != null &&
              detail.competitorActivity!.isNotEmpty) ...[
            _buildInfoRow('활동 내용', detail.competitorActivity!),
            const SizedBox(height: AppSpacing.sm),
          ],

          // 시식 여부
          if (detail.competitorTasting != null) ...[
            _buildInfoRow(
              '시식 여부',
              detail.competitorTasting! ? '예' : '아니요',
            ),
            const SizedBox(height: AppSpacing.sm),
          ],

          // 시식 정보 (시식=예인 경우만)
          if (detail.competitorTasting == true) ...[
            // 경쟁사 상품명
            if (detail.competitorProductName != null &&
                detail.competitorProductName!.isNotEmpty) ...[
              _buildInfoRow('경쟁사 상품', detail.competitorProductName!),
              const SizedBox(height: AppSpacing.sm),
            ],

            // 제품 가격
            if (detail.competitorProductPrice != null) ...[
              _buildInfoRow(
                '제품 가격',
                '${numberFormat.format(detail.competitorProductPrice)}원',
              ),
              const SizedBox(height: AppSpacing.sm),
            ],

            // 판매 수량
            if (detail.competitorSalesQuantity != null) ...[
              _buildInfoRow(
                '판매 수량',
                '${numberFormat.format(detail.competitorSalesQuantity)}개',
              ),
            ],
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
