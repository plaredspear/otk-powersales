import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/leader_monthly_schedule.dart';

/// 조장 여사원 월간 일정 캘린더 그리드 (레거시 `employee/mgnSchedule.jsp` FullCalendar 정합).
///
/// 일정 있는 날짜마다 `출근완료수 / 전체수`(sum/cnt)를 표시한다. 색상 규칙(레거시 `calSchedule`):
/// - 과거·오늘: 출근완료<전체 → 빨강, 완료 → 초록
/// - 미래: 회색
/// total=0(일정 없음)인 날짜는 숫자 미표시·탭 불가.
class LeaderScheduleCalendarGrid extends StatelessWidget {
  final int year;
  final int month;

  /// 날짜(YYYY-MM-DD) → 집계 조회. 없으면 null.
  final LeaderCalendarDay? Function(String date) dayOf;

  /// 일정 있는 날짜 탭 콜백.
  final void Function(DateTime date) onDateTap;

  const LeaderScheduleCalendarGrid({
    super.key,
    required this.year,
    required this.month,
    required this.dayOf,
    required this.onDateTap,
  });

  static String _fmt(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-'
      '${d.month.toString().padLeft(2, '0')}-'
      '${d.day.toString().padLeft(2, '0')}';

  List<DateTime?> _calendarDates() {
    final firstDay = DateTime(year, month, 1);
    final lastDay = DateTime(year, month + 1, 0);
    final firstWeekday = firstDay.weekday % 7; // 일=0 ... 토=6
    final dates = <DateTime?>[];
    for (var i = 0; i < firstWeekday; i++) {
      dates.add(null);
    }
    for (var day = 1; day <= lastDay.day; day++) {
      dates.add(DateTime(year, month, day));
    }
    while (dates.length % 7 != 0) {
      dates.add(null);
    }
    return dates;
  }

  @override
  Widget build(BuildContext context) {
    final dates = _calendarDates();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    return Column(
      children: [
        // 요일 헤더
        Padding(
          padding: const EdgeInsets.symmetric(
            vertical: AppSpacing.sm,
            horizontal: AppSpacing.md,
          ),
          child: Row(
            children: ['일', '월', '화', '수', '목', '금', '토'].asMap().entries.map(
              (e) {
                final color = e.key == 0
                    ? AppColors.otokiRed
                    : e.key == 6
                        ? AppColors.secondary
                        : AppColors.textPrimary;
                return Expanded(
                  child: Center(
                    child: Text(
                      e.value,
                      style: AppTypography.bodyMedium.copyWith(
                        fontWeight: FontWeight.w600,
                        color: color,
                      ),
                    ),
                  ),
                );
              },
            ).toList(),
          ),
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
            child: GridView.builder(
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate:
                  const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 7,
                childAspectRatio: 0.78,
              ),
              itemCount: dates.length,
              itemBuilder: (context, index) {
                final date = dates[index];
                if (date == null) return const SizedBox.shrink();
                return _DayCell(
                  date: date,
                  weekday: index % 7,
                  isToday: date == today,
                  isFuture: date.isAfter(today),
                  day: dayOf(_fmt(date)),
                  onTap: onDateTap,
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}

class _DayCell extends StatelessWidget {
  final DateTime date;
  final int weekday;
  final bool isToday;
  final bool isFuture;
  final LeaderCalendarDay? day;
  final void Function(DateTime date) onTap;

  const _DayCell({
    required this.date,
    required this.weekday,
    required this.isToday,
    required this.isFuture,
    required this.day,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final hasWork = day != null && day!.total > 0;

    final dayNumColor = weekday == 0
        ? AppColors.otokiRed
        : weekday == 6
            ? AppColors.secondary
            : AppColors.textPrimary;

    return GestureDetector(
      onTap: hasWork ? () => onTap(date) : null,
      child: Container(
        margin: const EdgeInsets.all(2),
        decoration: BoxDecoration(
          color: isToday
              ? AppColors.otokiYellow.withValues(alpha: 0.35)
              : Colors.transparent,
          borderRadius: BorderRadius.circular(4),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              '${date.day}',
              style: AppTypography.bodyMedium.copyWith(
                fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                color: dayNumColor,
              ),
            ),
            if (hasWork)
              Padding(
                padding: const EdgeInsets.only(top: 2),
                child: _SumCount(
                  attended: day!.attended,
                  total: day!.total,
                  isFuture: isFuture,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// `출근완료수 / 전체수` 렌더 — 레거시 색상 규칙.
class _SumCount extends StatelessWidget {
  final int attended;
  final int total;
  final bool isFuture;

  const _SumCount({
    required this.attended,
    required this.total,
    required this.isFuture,
  });

  @override
  Widget build(BuildContext context) {
    // 과거·오늘: 미완=빨강 / 완료=초록, 미래: 회색
    final Color sumColor = isFuture
        ? AppColors.textTertiary
        : (attended < total ? AppColors.error : AppColors.success);
    final Color totalColor =
        isFuture ? AppColors.textTertiary : AppColors.textPrimary;

    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          '$attended',
          style: AppTypography.labelSmall.copyWith(
            fontSize: 11,
            fontWeight: FontWeight.w700,
            color: sumColor,
          ),
        ),
        Text(
          ' / $total',
          style: AppTypography.labelSmall.copyWith(
            fontSize: 11,
            color: totalColor,
          ),
        ),
      ],
    );
  }
}
