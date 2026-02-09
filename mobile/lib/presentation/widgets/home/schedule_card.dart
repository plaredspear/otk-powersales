import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/schedule.dart';
import '../common/primary_button.dart';

/// 일정 카드 위젯
///
/// 홈 화면의 #1 영역: 오늘 일정을 표시한다.
/// - 일정 없음: "오늘 등록된 스케줄이 없습니다." + "등록" 버튼
/// - 일정 있음: 오늘 스케줄 목록 (매장명, 시간, 근무유형)
class ScheduleCard extends StatelessWidget {
  /// 오늘 일정 목록
  final List<Schedule> schedules;

  /// 현재 날짜 문자열 (예: '2026-02-07')
  final String currentDate;

  /// "등록" 버튼 탭 콜백
  final VoidCallback? onRegisterTap;

  /// 일정 아이템 탭 콜백
  final void Function(Schedule schedule)? onScheduleTap;

  const ScheduleCard({
    super.key,
    required this.schedules,
    required this.currentDate,
    this.onRegisterTap,
    this.onScheduleTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border, width: 1),
      ),
      child: Padding(
        padding: AppSpacing.cardPadding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 날짜 헤더
            Text(
              _formatDate(currentDate),
              style: AppTypography.headlineLarge.copyWith(
                fontSize: 22,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: AppSpacing.md),

            // 일정 내용
            if (schedules.isEmpty)
              _buildEmptySchedule()
            else
              _buildScheduleList(),
          ],
        ),
      ),
    );
  }

  /// 일정 없음 UI
  Widget _buildEmptySchedule() {
    return Column(
      children: [
        const SizedBox(height: AppSpacing.xs),
        Text(
          '오늘 등록된 스케줄이 없습니다.',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
        PrimaryButton(
          text: '등록',
          onPressed: onRegisterTap,
          height: AppSpacing.buttonHeightSmall,
          fontSize: 14,
        ),
        const SizedBox(height: AppSpacing.lg),
      ],
    );
  }

  /// 일정 목록 UI
  Widget _buildScheduleList() {
    return Column(
      children: schedules.map((schedule) {
        return _buildScheduleItem(schedule);
      }).toList(),
    );
  }

  /// 일정 아이템 UI
  Widget _buildScheduleItem(Schedule schedule) {
    return InkWell(
      onTap: onScheduleTap != null ? () => onScheduleTap!(schedule) : null,
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          vertical: AppSpacing.sm,
        ),
        child: Row(
          children: [
            // 시간
            SizedBox(
              width: 100,
              child: Text(
                '${schedule.startTime}~${schedule.endTime}',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.secondary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.sm),

            // 매장명
            Expanded(
              child: Text(
                schedule.storeName,
                style: AppTypography.bodyMedium,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),

            // 근무유형 배지
            Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.sm,
                vertical: AppSpacing.xxs,
              ),
              decoration: BoxDecoration(
                color: AppColors.primaryLight,
                borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
              ),
              child: Text(
                schedule.type,
                style: AppTypography.labelMedium.copyWith(
                  color: AppColors.onPrimary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 날짜 문자열을 "MM월 dd일 (요일)" 형식으로 변환
  String _formatDate(String dateStr) {
    try {
      final date = DateTime.parse(dateStr);
      const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
      final weekday = weekdays[date.weekday - 1];
      final month = date.month.toString().padLeft(2, '0');
      final day = date.day.toString().padLeft(2, '0');
      return '$month월 $day일 ($weekday)';
    } catch (_) {
      return dateStr;
    }
  }
}
