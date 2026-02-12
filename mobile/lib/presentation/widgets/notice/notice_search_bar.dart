import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';

/// 공지사항 검색 바 위젯
///
/// 제목+내용 검색을 위한 입력 필드를 제공합니다.
class NoticeSearchBar extends StatelessWidget {
  /// 텍스트 컨트롤러
  final TextEditingController controller;

  /// 검색 실행 콜백
  final ValueChanged<String> onSearch;

  /// 검색어 변경 콜백
  final ValueChanged<String>? onChanged;

  const NoticeSearchBar({
    super.key,
    required this.controller,
    required this.onSearch,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.white,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
      ),
      child: TextField(
        controller: controller,
        onChanged: onChanged,
        onSubmitted: onSearch,
        decoration: InputDecoration(
          hintText: '타이틀, 내용 입력',
          hintStyle: const TextStyle(
            fontSize: 14,
            color: AppColors.textTertiary,
          ),
          prefixIcon: const Icon(
            Icons.search,
            color: AppColors.secondary,
          ),
          suffixIcon: controller.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(
                    Icons.clear,
                    size: 20,
                    color: AppColors.textTertiary,
                  ),
                  onPressed: () {
                    controller.clear();
                    onSearch('');
                  },
                )
              : null,
          border: InputBorder.none,
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        ),
        style: const TextStyle(
          fontSize: 14,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}
