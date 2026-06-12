import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/notice.dart';

/// 공지 가로 스크롤 위젯
///
/// 홈 화면의 #3 영역: 최근 공지사항을 가로 스크롤 카드로 표시한다.
/// - 카드 150×146 고정, 간격 7dp
/// - createdAt이 현재 시각으로부터 7일 이내인 공지에 우상단 NEW 배지 표시
class NoticeCarousel extends StatelessWidget {
  /// 공지사항 목록
  final List<Notice> notices;

  /// 공지 카드 탭 콜백 (공지 상세 화면으로 이동)
  final void Function(Notice notice)? onNoticeTap;

  /// 전체보기 카드 탭 콜백 (공지사항 목록 화면으로 이동)
  final VoidCallback? onViewAllTap;

  /// NEW 배지 판정 기준 시각 (테스트 주입용. 미지정 시 DateTime.now())
  final DateTime? now;

  const NoticeCarousel({
    super.key,
    required this.notices,
    this.onNoticeTap,
    this.onViewAllTap,
    this.now,
  });

  /// NEW 배지 표시 기준: createdAt이 기준 시각으로부터 7일 이내
  static const Duration _newThreshold = Duration(days: 7);

  bool _isNew(Notice notice) {
    final base = now ?? DateTime.now();
    return base.difference(notice.createdAt) < _newThreshold;
  }

  @override
  Widget build(BuildContext context) {
    if (notices.isEmpty) {
      return _buildEmptyNotice();
    }

    return SizedBox(
      height: AppSpacing.homeNoticeCardHeight,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        // 카드 높이가 뷰포트와 같아 기본 클리핑 시 하단 그림자가 잘린다.
        // 클리핑을 꺼 위/아래 간격으로 그림자가 그려지게 한다(가로 오버플로우는 화면 밖).
        clipBehavior: Clip.none,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.homeGutter),
        itemCount: notices.length + 1,
        separatorBuilder: (_, _) =>
            const SizedBox(width: AppSpacing.homeNoticeCardGap),
        itemBuilder: (context, index) {
          if (index == notices.length) {
            return _buildViewAllCard();
          }
          return _buildNoticeCard(notices[index]);
        },
      ),
    );
  }

  /// 공지 없음 UI
  Widget _buildEmptyNotice() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.homeGutter),
      child: Center(
        child: Text(
          '공지사항이 없습니다.',
          style: AppTypography.legacyBody.copyWith(
            color: AppColors.legacyTextMute,
          ),
        ),
      ),
    );
  }

  /// 공지 카드 UI (150×146, radius 10)
  Widget _buildNoticeCard(Notice notice) {
    final showNew = _isNew(notice);

    return InkWell(
      onTap: onNoticeTap != null ? () => onNoticeTap!(notice) : null,
      borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
      child: Container(
        width: AppSpacing.homeNoticeCardWidth,
        height: AppSpacing.homeNoticeCardHeight,
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
          boxShadow: AppSpacing.cardShadow,
        ),
        child: Stack(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: 15,
                vertical: 20,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 헤더: 노란 종 아이콘 + 카테고리명 (16/800)
                  Row(
                    children: [
                      Image.asset(
                        'assets/images/ico_notice.png',
                        width: 19,
                        height: 18,
                        fit: BoxFit.contain,
                      ),
                      const SizedBox(width: 3),
                      Expanded(
                        child: Text(
                          notice.categoryName,
                          style: AppTypography.legacyTitleMD,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: AppSpacing.sm),

                  // 본문 3줄 ellipsis
                  Expanded(
                    child: Text(
                      notice.title,
                      style: AppTypography.legacyBody.copyWith(
                        color: AppColors.legacyTextSub,
                        height: 16 / 15,
                      ),
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),

                  // 작성일 (yyyy.MM.dd(요일))
                  Text(
                    _formatDateWithDay(notice.createdAt),
                    style: AppTypography.legacyCaption,
                  ),
                ],
              ),
            ),
            // NEW 배지 (우상단 24×24)
            if (showNew)
              Positioned(
                top: 0,
                right: 0,
                child: Image.asset(
                  'assets/images/ico_new.png',
                  width: 24,
                  height: 24,
                  fit: BoxFit.contain,
                ),
              ),
          ],
        ),
      ),
    );
  }

  /// 전체보기 카드 UI (150×146)
  Widget _buildViewAllCard() {
    return InkWell(
      onTap: onViewAllTap,
      borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
      child: Container(
        width: AppSpacing.homeNoticeCardWidth,
        height: AppSpacing.homeNoticeCardHeight,
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
          boxShadow: AppSpacing.cardShadow,
        ),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.arrow_forward_ios,
                size: 24,
                color: AppColors.legacyTextMute,
              ),
              const SizedBox(height: AppSpacing.sm),
              Text(
                '전체보기',
                style: AppTypography.legacyBody.copyWith(
                  color: AppColors.legacyTextMute,
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
