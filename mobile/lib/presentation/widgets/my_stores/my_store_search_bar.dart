import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 내 거래처 검색 바 위젯
///
/// 거래처명 또는 거래처 코드로 검색할 수 있는 입력 필드와 검색 버튼.
/// 검색 버튼을 탭해야 검색이 실행됩니다 (실시간 검색 아님).
class MyStoreSearchBar extends StatelessWidget {
  /// 텍스트 컨트롤러
  final TextEditingController controller;

  /// 검색 버튼 탭 콜백
  final VoidCallback onSearch;

  /// 텍스트 변경 콜백
  final ValueChanged<String>? onChanged;

  const MyStoreSearchBar({
    super.key,
    required this.controller,
    required this.onSearch,
    this.onChanged,
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
          // 검색 입력 필드
          Expanded(
            child: TextField(
              controller: controller,
              onChanged: onChanged,
              onSubmitted: (_) => onSearch(),
              decoration: InputDecoration(
                hintText: '거래처명, 거래처 코드 입력',
                hintStyle: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textTertiary,
                ),
                contentPadding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.sm,
                ),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  borderSide: const BorderSide(color: AppColors.border),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  borderSide: const BorderSide(color: AppColors.border),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  borderSide: const BorderSide(
                    color: AppColors.otokiBlue,
                    width: 1.5,
                  ),
                ),
                isDense: true,
              ),
              style: AppTypography.bodyMedium,
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          // 검색 버튼
          SizedBox(
            height: 40,
            child: ElevatedButton(
              onPressed: onSearch,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.otokiBlue,
                foregroundColor: AppColors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                ),
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.lg,
                ),
              ),
              child: const Text('검색'),
            ),
          ),
        ],
      ),
    );
  }
}
