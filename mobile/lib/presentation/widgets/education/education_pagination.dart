import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

/// 교육 자료 페이지네이션 위젯
///
/// 이전/다음 페이지 버튼과 현재 페이지 정보를 표시합니다.
class EducationPagination extends StatelessWidget {
  /// 현재 페이지 (1부터 시작)
  final int currentPage;

  /// 전체 페이지 수
  final int totalPages;

  /// 전체 게시물 수
  final int totalCount;

  /// 이전 페이지 콜백
  final VoidCallback? onPreviousPage;

  /// 다음 페이지 콜백
  final VoidCallback? onNextPage;

  const EducationPagination({
    super.key,
    required this.currentPage,
    required this.totalPages,
    required this.totalCount,
    this.onPreviousPage,
    this.onNextPage,
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
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // 전체 게시물 수
          Text(
            '전체 $totalCount건',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),

          // 페이지 컨트롤
          Row(
            children: [
              // 이전 페이지 버튼
              IconButton(
                icon: const Icon(Icons.chevron_left),
                iconSize: AppSpacing.iconSize,
                color: isFirstPage ? AppColors.textTertiary : AppColors.textPrimary,
                onPressed: isFirstPage ? null : onPreviousPage,
              ),

              // 페이지 정보
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.xs,
                ),
                decoration: BoxDecoration(
                  color: AppColors.surface,
                  borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
                ),
                child: Text(
                  '$currentPage / $totalPages',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),

              // 다음 페이지 버튼
              IconButton(
                icon: const Icon(Icons.chevron_right),
                iconSize: AppSpacing.iconSize,
                color: isLastPage ? AppColors.textTertiary : AppColors.textPrimary,
                onPressed: isLastPage ? null : onNextPage,
              ),
            ],
          ),
        ],
      ),
    );
  }
}
