import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';

/// 공지사항 검색 바 위젯
///
/// 제목+내용 검색을 위한 입력 필드를 제공합니다.
/// 레거시 디자인: 전체 너비 회색 바 + 우측 노란 "검색" 버튼.
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
      color: AppColors.surfaceVariant,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          // 검색 입력
          Expanded(
            child: TextField(
              controller: controller,
              onChanged: onChanged,
              onSubmitted: onSearch,
              decoration: const InputDecoration(
                hintText: '타이틀, 내용 입력',
                hintStyle: TextStyle(
                  fontSize: 15,
                  color: AppColors.legacyPlaceholder,
                ),
                border: InputBorder.none,
                isDense: true,
                contentPadding: EdgeInsets.zero,
              ),
              style: const TextStyle(
                fontSize: 15,
                color: AppColors.legacyTextSub,
              ),
            ),
          ),

          const SizedBox(width: AppSpacing.sm),

          // 검색 버튼
          GestureDetector(
            onTap: () => onSearch(controller.text),
            child: Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg,
                vertical: AppSpacing.sm,
              ),
              decoration: BoxDecoration(
                color: AppColors.legacyYellow,
                borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
              ),
              child: const Text(
                '검색',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: AppColors.legacyTextSub,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
