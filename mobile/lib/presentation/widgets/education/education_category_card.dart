import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';
import 'package:mobile/domain/entities/education_category.dart';

/// 교육 자료 카테고리 카드 위젯
///
/// 카테고리 아이콘과 이름을 표시하는 카드입니다.
/// 선택된 카테고리는 하이라이트 표시됩니다.
class EducationCategoryCard extends StatelessWidget {
  /// 카테고리
  final EducationCategory category;

  /// 선택 여부
  final bool isSelected;

  /// 카드 탭 콜백
  final VoidCallback? onTap;

  const EducationCategoryCard({
    super.key,
    required this.category,
    this.isSelected = false,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.lg,
        ),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.primaryLight : AppColors.white,
          borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
          border: Border.all(
            color: isSelected ? AppColors.primary : AppColors.border,
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // 카테고리 아이콘
            Image.asset(
              category.iconPath,
              width: AppSpacing.iconSizeMenu,
              height: AppSpacing.iconSizeMenu,
              errorBuilder: (context, error, stackTrace) {
                // 아이콘 로드 실패 시 기본 아이콘 표시
                return Icon(
                  Icons.folder_outlined,
                  size: AppSpacing.iconSizeMenu,
                  color: isSelected ? AppColors.primary : AppColors.textSecondary,
                );
              },
            ),
            const SizedBox(height: AppSpacing.sm),

            // 카테고리 이름
            Text(
              category.displayName,
              style: AppTypography.bodySmall.copyWith(
                color: isSelected ? AppColors.textPrimary : AppColors.textSecondary,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
              ),
              textAlign: TextAlign.center,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
      ),
    );
  }
}
