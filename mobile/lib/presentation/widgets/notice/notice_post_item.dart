import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';

import '../../../domain/entities/notice_post.dart';

/// 공지사항 목록 항목 위젯
///
/// 레거시 디자인: "[분류] 제목" 한 줄 + 등록일.
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
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        decoration: const BoxDecoration(
          color: AppColors.white,
          border: Border(
            bottom: BorderSide(
              color: AppColors.divider,
              width: 1,
            ),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 제목 ([분류] 제목)
            Text(
              '[${post.categoryName}] ${post.title}',
              style: const TextStyle(
                fontSize: 15,
                color: AppColors.legacyTextSub,
                fontWeight: FontWeight.w500,
                height: 1.4,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),

            const SizedBox(height: AppSpacing.sm),

            // 등록일
            Text(
              _formatDate(post.createdAt),
              style: const TextStyle(
                fontSize: 13,
                color: AppColors.legacyTextMute,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
