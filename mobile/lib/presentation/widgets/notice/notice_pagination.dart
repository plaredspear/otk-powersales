import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

/// 공지사항 페이지네이션 위젯
///
/// 처음/이전/페이지 번호/다음/마지막 버튼을 표시합니다.
class NoticePagination extends StatelessWidget {
  /// 현재 페이지 (1부터 시작)
  final int currentPage;

  /// 전체 페이지 수
  final int totalPages;

  /// 전체 게시물 수
  final int totalCount;

  /// 처음 페이지 콜백
  final VoidCallback? onFirstPage;

  /// 이전 페이지 콜백
  final VoidCallback? onPreviousPage;

  /// 다음 페이지 콜백
  final VoidCallback? onNextPage;

  /// 마지막 페이지 콜백
  final VoidCallback? onLastPage;

  const NoticePagination({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.totalCount,
    this.onFirstPage,
    this.onPreviousPage,
    this.onNextPage,
    this.onLastPage,
  });

  @override
  Widget build(BuildContext context) {
    final isFirstPage = currentPage <= 1;
    final isLastPage = currentPage >= totalPages || totalPages == 0;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(
          top: BorderSide(
            color: AppColors.border,
            width: 1,
          ),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // 처음 페이지 버튼 (|<)
          IconButton(
            icon: const Icon(Icons.first_page),
            iconSize: AppSpacing.iconSize,
            color: isFirstPage ? AppColors.textTertiary : AppColors.textPrimary,
            onPressed: isFirstPage ? null : onFirstPage,
            tooltip: '처음',
          ),

          const SizedBox(width: AppSpacing.xs),

          // 이전 페이지 버튼 (<)
          IconButton(
            icon: const Icon(Icons.chevron_left),
            iconSize: AppSpacing.iconSize,
            color: isFirstPage ? AppColors.textTertiary : AppColors.textPrimary,
            onPressed: isFirstPage ? null : onPreviousPage,
            tooltip: '이전',
          ),

          const SizedBox(width: AppSpacing.md),

          // 페이지 정보
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.sm,
            ),
            decoration: BoxDecoration(
              color: AppColors.surface,
              borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            ),
            child: Text(
              '$currentPage',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),

          const SizedBox(width: AppSpacing.md),

          // 다음 페이지 버튼 (>)
          IconButton(
            icon: const Icon(Icons.chevron_right),
            iconSize: AppSpacing.iconSize,
            color: isLastPage ? AppColors.textTertiary : AppColors.textPrimary,
            onPressed: isLastPage ? null : onNextPage,
            tooltip: '다음',
          ),

          const SizedBox(width: AppSpacing.xs),

          // 마지막 페이지 버튼 (>|)
          IconButton(
            icon: const Icon(Icons.last_page),
            iconSize: AppSpacing.iconSize,
            color: isLastPage ? AppColors.textTertiary : AppColors.textPrimary,
            onPressed: isLastPage ? null : onLastPage,
            tooltip: '마지막',
          ),
        ],
      ),
    );
  }
}
