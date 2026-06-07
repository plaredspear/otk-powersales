import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

import '../../../domain/entities/notice_category.dart';

/// 공지사항 분류 필터 드롭다운 위젯
///
/// 회사공지/지점공지 필터링을 위한 드롭다운을 제공합니다.
/// 레거시 디자인: 전체 너비 바 + 하단 구분선 + 우측 끝 화살표.
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
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(
          bottom: BorderSide(color: AppColors.divider, width: 1),
        ),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<NoticeCategory?>(
          value: selectedCategory,
          isExpanded: true,
          icon: const Icon(
            Icons.keyboard_arrow_down,
            color: AppColors.legacyTextSub,
          ),
          style: AppTypography.bodyLarge.copyWith(
            color: AppColors.legacyTextSub,
          ),
          dropdownColor: AppColors.white,
          items: [
            // 전체
            DropdownMenuItem<NoticeCategory?>(
              value: null,
              child: Text(
                '분류 전체',
                style: AppTypography.bodyLarge.copyWith(
                  color: AppColors.legacyTextSub,
                ),
              ),
            ),
            // 회사공지
            DropdownMenuItem<NoticeCategory?>(
              value: NoticeCategory.company,
              child: Text(
                NoticeCategory.company.displayName,
                style: AppTypography.bodyLarge.copyWith(
                  color: AppColors.legacyTextSub,
                ),
              ),
            ),
            // 지점공지
            DropdownMenuItem<NoticeCategory?>(
              value: NoticeCategory.branch,
              child: Text(
                NoticeCategory.branch.displayName,
                style: AppTypography.bodyLarge.copyWith(
                  color: AppColors.legacyTextSub,
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
