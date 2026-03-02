import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/attendance_summary.dart';
import '../../../domain/entities/schedule.dart';
import '../common/primary_button.dart';

/// 일정 카드 위젯
///
/// 홈 화면의 #1 영역: 오늘 일정을 표시한다.
/// - 일정 없음: "출근 후 등록을 누르세요." + "등록" 버튼
/// - 일정 있음: 오늘 스케줄 목록 (근무유형 배지, 매장명, 출근 상태)
/// - 출근 카운트 배지: "✓ X/N" 형태로 출근 현황 표시
class ScheduleCard extends StatelessWidget {
  /// 오늘 일정 목록
  final List<Schedule> schedules;

  /// 현재 날짜 문자열 (예: '2026-02-07')
  final String currentDate;

  /// 출근 현황 집계
  final AttendanceSummary attendanceSummary;

  /// "등록" 버튼 탭 콜백
  final VoidCallback? onRegisterTap;

  /// 일정 아이템 탭 콜백
  final void Function(Schedule schedule)? onScheduleTap;

  /// "내 일정 >" 헤더 링크 탭 콜백
  final VoidCallback? onHeaderTap;

  const ScheduleCard({
    super.key,
    required this.schedules,
    required this.currentDate,
    required this.attendanceSummary,
    this.onRegisterTap,
    this.onScheduleTap,
    this.onHeaderTap,
  });

  @override
  Widget build(BuildContext context) {
    final totalCount = attendanceSummary.totalCount;
    final registeredCount = attendanceSummary.registeredCount;

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
            // 날짜 헤더 + "내 일정 >" 링크 + 출근 카운트 배지
            Row(
              children: [
                Text(
                  _formatDate(currentDate),
                  style: AppTypography.headlineLarge.copyWith(
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const Spacer(),
                if (totalCount > 0) _buildAttendanceBadge(registeredCount, totalCount),
                if (totalCount > 0) const SizedBox(width: AppSpacing.sm),
                GestureDetector(
                  onTap: onHeaderTap,
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '내 일정',
                        style: AppTypography.labelMedium.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                      Icon(
                        Icons.chevron_right,
                        size: 16,
                        color: AppColors.textSecondary,
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: AppSpacing.md),

            // 본문 영역
            if (totalCount == 0)
              _buildEmptySchedule()
            else if (registeredCount == 0)
              _buildUnregisteredMessage()
            else
              _buildScheduleList(),

            // 등록 버튼
            const SizedBox(height: AppSpacing.md),
            PrimaryButton(
              text: _buttonText(totalCount, registeredCount),
              onPressed: _isButtonEnabled(totalCount, registeredCount)
                  ? onRegisterTap
                  : null,
              height: AppSpacing.buttonHeightSmall,
              fontSize: 14,
            ),
            const SizedBox(height: AppSpacing.sm),
          ],
        ),
      ),
    );
  }

  /// 출근 카운트 배지
  Widget _buildAttendanceBadge(int registeredCount, int totalCount) {
    final isComplete = registeredCount == totalCount;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: isComplete ? AppColors.success.withOpacity(0.1) : null,
        border: Border.all(color: isComplete ? AppColors.success : AppColors.border),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.check,
            size: 16,
            color: isComplete ? AppColors.success : AppColors.textSecondary,
          ),
          const SizedBox(width: 4),
          Text(
            '$registeredCount/$totalCount',
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.w700,
              color: isComplete ? AppColors.success : AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }

  /// 미출근 안내 텍스트
  Widget _buildUnregisteredMessage() {
    return Column(
      children: [
        const SizedBox(height: AppSpacing.xs),
        Text(
          '출근 후 등록을 누르세요.',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      ],
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
                schedule.workCategory,
                style: AppTypography.labelMedium.copyWith(
                  color: AppColors.onPrimary,
                ),
              ),
            ),
            const SizedBox(width: AppSpacing.sm),

            // 매장명
            Expanded(
              child: Text(
                schedule.storeName ?? '(미지정)',
                style: AppTypography.bodyMedium,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: AppSpacing.sm),

            // 출근 상태
            Text(
              schedule.isCommuteRegistered ? '출근완료' : '미등록',
              style: AppTypography.labelMedium.copyWith(
                color: schedule.isCommuteRegistered
                    ? AppColors.success
                    : AppColors.textSecondary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 버튼 텍스트 결정
  String _buttonText(int totalCount, int registeredCount) {
    if (totalCount == 0 || registeredCount == 0) return '등록';
    if (registeredCount == totalCount) return '등록 완료';
    return '다음 등록';
  }

  /// 버튼 활성 여부 결정
  bool _isButtonEnabled(int totalCount, int registeredCount) {
    if (totalCount == 0) return false;
    return registeredCount < totalCount;
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
