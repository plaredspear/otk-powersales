import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';

import '../../../domain/entities/notice_category.dart';
import '../../../domain/entities/notice_post.dart';

/// 공지사항 목록 항목 위젯
///
/// 분류 태그, 제목, 등록일을 표시합니다.
class NoticePostItem extends StatelessWidget {
  /// 공지사항 게시물
  final NoticePost post;

  /// 탭 콜백
  final VoidCallback onTap;

  const NoticePostItem({
    super.key,
    required this.post,
    required this.onTap,
  });

  /// 날짜를 "yyyy.MM.dd(요일)" 형식으로 변환
  String _formatDate(DateTime date) {
    final formatter = DateFormat('yyyy.MM.dd(E)', 'ko_KR');
    return formatter.format(date);
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(AppSpacing.md),
        decoration: const BoxDecoration(
          color: AppColors.white,
          border: Border(
            bottom: BorderSide(
              color: AppColors.border,
              width: 1,
            ),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 분류 태그
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.sm,
                vertical: AppSpacing.xs,
              ),
              decoration: BoxDecoration(
                color: post.category == NoticeCategory.company
                    ? AppColors.primaryLight
                    : AppColors.secondaryLight,
                borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
              ),
              child: Text(
                post.categoryName,
                style: AppTypography.labelSmall.copyWith(
                  color: post.category == NoticeCategory.company
                      ? AppColors.primary
                      : AppColors.secondary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),

            const SizedBox(width: AppSpacing.md),

            // 제목 + 날짜
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

                  // 등록일
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
              color: AppColors.textTertiary,
              size: 20,
            ),
          ],
        ),
      ),
    );
  }
}
