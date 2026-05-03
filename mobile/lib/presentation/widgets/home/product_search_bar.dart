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
      borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
      child: Container(
        height: 40,
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
          boxShadow: AppSpacing.cardShadow,
        ),
        child: Row(
          children: [
            const SizedBox(width: AppSpacing.homeGutter),
            Image.asset(
              'assets/images/ico_search.png',
              width: 18,
              height: 18,
            ),
            const SizedBox(width: 9),
            Text(
              '제품 검색',
              style: AppTypography.legacyTitleMD.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.legacyPlaceholder,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
