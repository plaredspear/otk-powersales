import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

/// 교육 자료 검색 바 위젯
///
/// 레거시 edu/list 정합: "타이틀, 내용 입력" 입력 필드 + 노란색 "검색" 버튼.
class EducationSearchBar extends StatelessWidget {
  /// 텍스트 컨트롤러
  final TextEditingController controller;

  /// 검색 실행 콜백
  final ValueChanged<String> onSearch;

  /// 검색어 변경 콜백
  final ValueChanged<String>? onChanged;

  const EducationSearchBar({
    super.key,
    required this.controller,
    required this.onSearch,
    this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        // 검색어 입력
        Expanded(
          child: TextField(
            controller: controller,
            onChanged: onChanged,
            onSubmitted: onSearch,
            textInputAction: TextInputAction.search,
            decoration: const InputDecoration(
              hintText: '타이틀, 내용 입력',
              hintStyle: TextStyle(
                fontSize: 16,
                color: AppColors.textTertiary,
              ),
              border: UnderlineInputBorder(
                borderSide: BorderSide(color: AppColors.border),
              ),
              enabledBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: AppColors.border),
              ),
              focusedBorder: UnderlineInputBorder(
                borderSide: BorderSide(color: AppColors.textPrimary),
              ),
              contentPadding: EdgeInsets.symmetric(vertical: 10),
            ),
            style: const TextStyle(
              fontSize: 16,
              color: AppColors.textPrimary,
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.md),

        // 노란색 검색 버튼
        SizedBox(
          height: 40,
          child: ElevatedButton(
            onPressed: () => onSearch(controller.text),
            style: ElevatedButton.styleFrom(
              // Row 안 무한폭 제약 크래시 방지 (전역 테마 덮어쓰기).
              minimumSize: Size.zero,
              backgroundColor: AppColors.otokiYellow,
              foregroundColor: AppColors.textPrimary,
              elevation: 0,
              padding: const EdgeInsets.symmetric(horizontal: 20),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusXl),
              ),
            ),
            child: Text(
              '검색',
              style: AppTypography.labelLarge.copyWith(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
