import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

import '../../../domain/entities/notice_category.dart';
import '../common/single_select_sheet.dart';

/// 공지사항 분류 필터 위젯
///
/// 회사공지/지점공지 필터링을 위한 분류 선택 바를 제공합니다.
/// 탭하면 거래처 선택과 동일한 공용 바텀시트([SingleSelectSheet])를 띄웁니다.
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
    return Material(
      color: AppColors.white,
      child: InkWell(
        onTap: () => _selectCategory(context),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.sm,
          ),
          decoration: const BoxDecoration(
            border: Border(
              bottom: BorderSide(color: AppColors.divider, width: 1),
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  selectedCategory?.displayName ?? '분류 전체',
                  style: AppTypography.bodyLarge.copyWith(
                    color: AppColors.legacyTextSub,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const Icon(
                Icons.keyboard_arrow_down,
                color: AppColors.legacyTextSub,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _selectCategory(BuildContext context) async {
    final result = await SingleSelectSheet.show<NoticeCategory?>(
      context,
      title: '분류 선택',
      selectedValue: selectedCategory,
      options: [
        const SingleSelectOption(value: null, label: '분류 전체'),
        ...NoticeCategory.values.map(
          (c) => SingleSelectOption(value: c, label: c.displayName),
        ),
      ],
    );
    if (result == null) return;
    onCategoryChanged(result.value);
  }
}
