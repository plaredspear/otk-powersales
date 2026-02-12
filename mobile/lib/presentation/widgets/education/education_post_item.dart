import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';
import 'package:mobile/domain/entities/education_post.dart';

/// 교육 자료 게시물 목록 항목 위젯
///
/// 게시물 제목과 작성일을 표시하는 리스트 항목입니다.
/// 탭하면 게시물 상세 화면으로 이동합니다.
class EducationPostItem extends StatelessWidget {
  /// 게시물 정보
  final EducationPost post;

  /// 항목 탭 콜백
  final VoidCallback? onTap;

  const EducationPostItem({
    super.key,
    required this.post,
    this.onTap,
  });

  /// 날짜를 "YYYY-MM-DD" 형식으로 포맷
  String _formatDate(DateTime date) {
    return DateFormat('yyyy-MM-dd').format(date);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        decoration: const BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: AppColors.border,
              width: 1,
            ),
          ),
        ),
        child: Row(
          children: [
            // 문서 아이콘
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: AppColors.surface,
                borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              ),
              child: const Icon(
                Icons.description_outlined,
                size: AppSpacing.iconSize,
                color: AppColors.secondary,
              ),
            ),
            const SizedBox(width: AppSpacing.md),

            // 제목 및 날짜
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 제목
                  Text(
                    post.title,
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w500,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: AppSpacing.xs),

                  // 작성일
                  Text(
                    _formatDate(post.createdAt),
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                ],
              ),
            ),

            // 화살표 아이콘
            const Icon(
              Icons.chevron_right,
              size: AppSpacing.iconSize,
              color: AppColors.textTertiary,
            ),
          ],
        ),
      ),
    );
  }
}
