import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 유통기한 그룹 헤더 위젯
///
/// "유통기한 지남" (빨간 점) / "유통기한 전" (주황 점) 그룹 구분 표시
class ShelfLifeGroupHeader extends StatelessWidget {
  /// 만료 그룹인지 여부
  final bool isExpired;

  /// 그룹 내 항목 수
  final int count;

  const ShelfLifeGroupHeader({
    super.key,
    required this.isExpired,
    required this.count,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          // 상태 점
          Container(
            width: 10,
            height: 10,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: isExpired ? AppColors.error : AppColors.warning,
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          // 그룹명 + 개수
          Text(
            isExpired
                ? '유통기한 지남 ($count)'
                : '유통기한 전 ($count)',
            style: AppTypography.headlineSmall.copyWith(
              color: isExpired ? AppColors.error : AppColors.warning,
            ),
          ),
        ],
      ),
    );
  }
}
