import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 제품 검색 바 위젯
///
/// 홈 화면의 #4 영역: 제품 검색 바 UI.
/// 입력 불가 (읽기 전용). 탭 시 제품 검색 화면으로 이동.
class ProductSearchBar extends StatelessWidget {
  /// 탭 콜백 (제품 검색 화면으로 이동)
  final VoidCallback? onTap;

  const ProductSearchBar({
    super.key,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: Container(
        height: AppSpacing.searchBarHeight,
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          border: Border.all(color: AppColors.border, width: 1),
        ),
        child: Row(
          children: [
            const SizedBox(width: AppSpacing.md),
            const Icon(
              Icons.search,
              color: AppColors.otokiYellow,
              size: AppSpacing.iconSize,
            ),
            const SizedBox(width: AppSpacing.sm),
            Text(
              '제품 검색',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textTertiary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
