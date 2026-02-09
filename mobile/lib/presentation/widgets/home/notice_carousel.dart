import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/notice.dart';

/// 공지 가로 스크롤 위젯
///
/// 홈 화면의 #3 영역: 최근 공지사항을 가로 스크롤 카드로 표시한다.
/// - 지점공지/전체공지 구분 라벨
/// - 최근 1주일 공지 최대 5개
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
      height: 120,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
        itemCount: notices.length,
        separatorBuilder: (_, __) => const SizedBox(width: AppSpacing.md),
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
        width: 160,
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: AppSpacing.cardBorderRadius,
          boxShadow: AppSpacing.cardShadow,
        ),
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.md),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 유형 라벨
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                  vertical: AppSpacing.xxs,
                ),
                decoration: BoxDecoration(
                  color: _getTypeBadgeColor(notice.type),
                  borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
                ),
                child: Text(
                  notice.typeDisplayName,
                  style: AppTypography.labelSmall.copyWith(
                    color: _getTypeTextColor(notice.type),
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.sm),

              // 제목
              Expanded(
                child: Text(
                  notice.title,
                  style: AppTypography.bodyMedium,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ),

              // 작성일
              Text(
                _formatDate(notice.createdAt),
                style: AppTypography.bodySmall,
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 유형별 배지 배경색
  Color _getTypeBadgeColor(String type) {
    switch (type) {
      case 'BRANCH':
        return AppColors.secondary.withOpacity(0.1);
      case 'ALL':
        return AppColors.success.withOpacity(0.1);
      default:
        return AppColors.surfaceVariant;
    }
  }

  /// 유형별 텍스트 색상
  Color _getTypeTextColor(String type) {
    switch (type) {
      case 'BRANCH':
        return AppColors.secondary;
      case 'ALL':
        return AppColors.successDark;
      default:
        return AppColors.textSecondary;
    }
  }

  /// DateTime을 "MM.dd" 형식으로 변환
  String _formatDate(DateTime dateTime) {
    final month = dateTime.month.toString().padLeft(2, '0');
    final day = dateTime.day.toString().padLeft(2, '0');
    return '$month.$day';
  }
}
