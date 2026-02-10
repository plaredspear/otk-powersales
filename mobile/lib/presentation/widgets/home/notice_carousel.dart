import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/notice.dart';

/// 공지 가로 스크롤 위젯
///
/// 홈 화면의 #3 영역: 최근 공지사항을 가로 스크롤 카드로 표시한다.
/// - 스피커 아이콘 + "전체 공지" 라벨
/// - 제목 + 날짜(요일) 형식
/// - 공지 없음: "공지사항이 없습니다." 메시지
class NoticeCarousel extends StatelessWidget {
  /// 공지사항 목록
  final List<Notice> notices;

  /// 공지 카드 탭 콜백 (공지 상세 화면으로 이동)
  final void Function(Notice notice)? onNoticeTap;

  const NoticeCarousel({
    super.key,
    required this.notices,
    this.onNoticeTap,
  });

  @override
  Widget build(BuildContext context) {
    if (notices.isEmpty) {
      return _buildEmptyNotice();
    }

    return SizedBox(
      height: 130,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
        itemCount: notices.length,
        separatorBuilder: (_, _) => const SizedBox(width: AppSpacing.md),
        itemBuilder: (context, index) {
          return _buildNoticeCard(notices[index]);
        },
      ),
    );
  }

  /// 공지 없음 UI
  Widget _buildEmptyNotice() {
    return Padding(
      padding: AppSpacing.screenHorizontal,
      child: Center(
        child: Text(
          '공지사항이 없습니다.',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ),
    );
  }

  /// 공지 카드 UI
  Widget _buildNoticeCard(Notice notice) {
    return InkWell(
      onTap: onNoticeTap != null ? () => onNoticeTap!(notice) : null,
      borderRadius: AppSpacing.cardBorderRadius,
      child: Container(
        width: 180,
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: AppSpacing.cardBorderRadius,
          border: Border.all(color: AppColors.border, width: 1),
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 스피커 아이콘 + "전체 공지" 라벨
              Row(
                children: [
                  const Icon(
                    Icons.campaign_outlined,
                    size: 18,
                    color: AppColors.textPrimary,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    notice.typeDisplayName,
                    style: AppTypography.labelLarge.copyWith(
                      fontWeight: FontWeight.w700,
                      fontSize: 14,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: AppSpacing.sm),

              // 제목
              Expanded(
                child: Text(
                  notice.title,
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                    height: 1.4,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),

              // 작성일 (yyyy.MM.dd(요일))
              Text(
                _formatDateWithDay(notice.createdAt),
                style: AppTypography.bodySmall.copyWith(
                  color: AppColors.textTertiary,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// DateTime을 "yyyy.MM.dd(요일)" 형식으로 변환
  String _formatDateWithDay(DateTime dateTime) {
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[dateTime.weekday - 1];
    final year = dateTime.year;
    final month = dateTime.month.toString().padLeft(2, '0');
    final day = dateTime.day.toString().padLeft(2, '0');
    return '$year.$month.$day($weekday)';
  }
}
