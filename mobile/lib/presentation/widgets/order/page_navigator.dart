import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 페이지 네비게이터 위젯
///
/// 페이지 번호 버튼으로 페이지를 이동합니다.
/// F28 거래처별 주문의 페이지네이션에 사용됩니다.
class PageNavigator extends StatelessWidget {
  final int currentPage; // 0-indexed
  final int totalPages;
  final ValueChanged<int> onPageChanged;

  const PageNavigator({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.onPageChanged,
  });

  List<int> _getVisiblePages() {
    const maxVisible = 5;
    if (totalPages <= maxVisible) {
      return List.generate(totalPages, (i) => i);
    }

    int start = currentPage - 2;
    int end = currentPage + 2;

    if (start < 0) {
      start = 0;
      end = maxVisible - 1;
    }

    if (end >= totalPages) {
      end = totalPages - 1;
      start = totalPages - maxVisible;
    }

    return List.generate(end - start + 1, (i) => start + i);
  }

  @override
  Widget build(BuildContext context) {
    if (totalPages == 0) {
      return const SizedBox.shrink();
    }

    final visiblePages = _getVisiblePages();

    return Padding(
      padding: const EdgeInsets.all(AppSpacing.md),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // First page button
          IconButton(
            icon: const Icon(Icons.first_page),
            onPressed: currentPage == 0 ? null : () => onPageChanged(0),
            tooltip: '첫 페이지',
          ),
          // Previous page button
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: currentPage == 0 ? null : () => onPageChanged(currentPage - 1),
            tooltip: '이전 페이지',
          ),
          const SizedBox(width: AppSpacing.sm),
          // Page number buttons
          ...visiblePages.map((pageIndex) {
            final isActive = pageIndex == currentPage;
            return Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.xs),
              child: isActive
                  ? ElevatedButton(
                      onPressed: null,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: AppColors.white,
                        minimumSize: const Size(40, 40),
                        padding: EdgeInsets.zero,
                      ),
                      child: Text(
                        '${pageIndex + 1}',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.white,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  : OutlinedButton(
                      onPressed: () => onPageChanged(pageIndex),
                      style: OutlinedButton.styleFrom(
                        minimumSize: const Size(40, 40),
                        padding: EdgeInsets.zero,
                      ),
                      child: Text(
                        '${pageIndex + 1}',
                        style: AppTypography.bodyMedium,
                      ),
                    ),
            );
          }),
          const SizedBox(width: AppSpacing.sm),
          // Next page button
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed: currentPage == totalPages - 1
                ? null
                : () => onPageChanged(currentPage + 1),
            tooltip: '다음 페이지',
          ),
          // Last page button
          IconButton(
            icon: const Icon(Icons.last_page),
            onPressed: currentPage == totalPages - 1
                ? null
                : () => onPageChanged(totalPages - 1),
            tooltip: '마지막 페이지',
          ),
        ],
      ),
    );
  }
}
