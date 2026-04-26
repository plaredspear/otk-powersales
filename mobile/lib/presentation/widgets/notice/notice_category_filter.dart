import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

import '../../../domain/entities/notice_category.dart';

/// 공지사항 분류 필터 드롭다운 위젯
///
/// 회사공지/지점공지 필터링을 위한 드롭다운을 제공합니다.
class NoticeCategoryFilter extends StatelessWidget {
  /// 선택된 분류 (null이면 "분류 전체")
  final NoticeCategory? selectedCategory;

  /// 분류 선택 콜백
  final ValueChanged<NoticeCategory?> onCategoryChanged;

  const NoticeCategoryFilter({
    super.key,
    required this.selectedCategory,
    required this.onCategoryChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: AppColors.white,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<NoticeCategory?>(
          value: selectedCategory,
          icon: const Icon(
            Icons.keyboard_arrow_down,
            color: AppColors.textSecondary,
          ),
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textPrimary,
          ),
          dropdownColor: AppColors.white,
          items: [
            // 전체
            DropdownMenuItem<NoticeCategory?>(
              value: null,
              child: Text(
                '분류 전체',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),
            // 회사공지
            DropdownMenuItem<NoticeCategory?>(
              value: NoticeCategory.company,
              child: Text(
                NoticeCategory.company.displayName,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),
            // 지점공지
            DropdownMenuItem<NoticeCategory?>(
              value: NoticeCategory.branch,
              child: Text(
                NoticeCategory.branch.displayName,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ],
          onChanged: onCategoryChanged,
        ),
      ),
    );
  }
}
