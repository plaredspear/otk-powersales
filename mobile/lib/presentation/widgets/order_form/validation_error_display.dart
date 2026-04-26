import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/validation_error.dart';

/// 유효성 검증 에러 표시 위젯
///
/// 주문서 제품의 유효성 에러 정보를 일관된 형식으로 표시합니다.
class ValidationErrorDisplay extends StatelessWidget {
  final ValidationError error;

  const ValidationErrorDisplay({
    super.key,
    required this.error,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: AppSpacing.cardPadding,
      decoration: BoxDecoration(
        color: AppColors.errorLight,
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (error.minOrderQuantity != null)
            _buildInfoRow('최소 주문수량', '${error.minOrderQuantity}박스'),
          if (error.supplyQuantity != null)
            _buildInfoRow('공급수량', '${error.supplyQuantity}개'),
          if (error.dcQuantity != null)
            _buildInfoRow('DC수량', '${error.dcQuantity}개'),
          const SizedBox(height: AppSpacing.xs),
          Text(
            error.message,
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.error,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: AppSpacing.xs),
      child: Text(
        '$label: $value',
        style: AppTypography.bodySmall.copyWith(
          color: AppColors.error,
        ),
      ),
    );
  }
}
