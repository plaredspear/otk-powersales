import 'package:flutter/material.dart';
import 'package:mobile/core/theme/app_colors.dart';
import 'package:mobile/core/theme/app_spacing.dart';
import 'package:mobile/core/theme/app_typography.dart';
import 'package:mobile/domain/entities/education_post.dart';

/// 교육 자료 게시물 목록 항목 위젯
///
/// 게시물 제목과 작성일을 표시하는 리스트 항목입니다.
/// 레거시 edu/list 디자인 정합: 제목(굵게) + "YYYY.MM.DD(요일)" 날짜.
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

  /// 한글 요일 (월~일)
  static const List<String> _weekdayNames = ['월', '화', '수', '목', '금', '토', '일'];

  /// 날짜를 "YYYY.MM.DD(요일)" 형식으로 포맷 (레거시 정합)
  String _formatDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    final w = _weekdayNames[date.weekday - 1]; // DateTime.weekday: 월=1 ~ 일=7
    return '$y.$m.$d($w)';
  }

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        width: double.infinity,
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
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 제목
            Text(
              post.title,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w700,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            const SizedBox(height: AppSpacing.xs),

            // 작성일 "YYYY.MM.DD(요일)"
            Text(
              _formatDate(post.createdAt),
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
