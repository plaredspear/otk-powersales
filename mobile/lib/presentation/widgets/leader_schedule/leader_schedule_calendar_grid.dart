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

  static const List<String> _weekdayLabels = [
    '일', '월', '화', '수', '목', '금', '토',
  ];

  @override
  Widget build(BuildContext context) {
    final dates = _calendarDates();
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    // 시스템 글꼴 확대 시 셀 텍스트가 넘치지 않도록 스케일 상한을 제한한다.
    // (셀 내부는 추가로 FittedBox 로 축소되어 어떤 스케일에서도 숫자가 잘리지 않는다.)
    return MediaQuery.withClampedTextScaling(
      maxScaleFactor: 1.3,
      // Table + TableBorder 로 격자 선을 셀 경계에 정확히 그린다(선이 셀 밖으로
      // 삐져나오지 않는다). 상단 정렬하여 표 아래 남는 공간에는 선을 그리지 않는다.
      child: Align(
        alignment: Alignment.topCenter,
        child: LayoutBuilder(
            builder: (context, constraints) {
              // 셀 높이는 레거시 비율(0.78)을 유지하도록 셀 너비에서 산출한다.
              final cellHeight = (constraints.maxWidth / 7) / 0.78;
              return Table(
                border: TableBorder.all(color: AppColors.divider, width: 1),
                defaultVerticalAlignment: TableCellVerticalAlignment.middle,
                children: [
                  // 요일 헤더
                  TableRow(
                    children: [
                      for (var i = 0; i < 7; i++)
                        Padding(
                          padding: const EdgeInsets.symmetric(
                            vertical: AppSpacing.sm,
                          ),
                          child: Center(
                            child: Text(
                              _weekdayLabels[i],
                              style: AppTypography.bodyMedium.copyWith(
                                fontWeight: FontWeight.w700,
                                color: i == 0
                                    ? AppColors.otokiRed
                                    : i == 6
                                        ? AppColors.secondary
                                        : AppColors.textPrimary,
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                  // 주별 날짜 행 (빈 셀도 표 격자 유지를 위해 높이만 확보)
                  for (var week = 0; week * 7 < dates.length; week++)
                    TableRow(
                      children: [
                        for (var dow = 0; dow < 7; dow++)
                          _buildCell(
                            dates[week * 7 + dow],
                            (week * 7 + dow) % 7,
                            today,
                            cellHeight,
                          ),
                      ],
                    ),
                ],
              );
            },
          ),
      ),
    );
  }

  Widget _buildCell(DateTime? date, int weekday, DateTime today, double height) {
    if (date == null) {
      return SizedBox(height: height);
    }
    return _DayCell(
      date: date,
      weekday: weekday,
      isToday: date == today,
      isFuture: date.isAfter(today),
      day: dayOf(_fmt(date)),
      onTap: onDateTap,
      height: height,
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

  /// 표 행 높이(모든 셀 동일).
  final double height;

  const _DayCell({
    required this.date,
    required this.weekday,
    required this.isToday,
    required this.isFuture,
    required this.day,
    required this.onTap,
    required this.height,
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
      behavior: HitTestBehavior.opaque,
      child: Container(
        height: height,
        color: isToday
            ? AppColors.otokiYellow.withValues(alpha: 0.35)
            : null,
        // 글꼴 확대 시에도 셀 밖으로 넘치지 않도록 내용 전체를 축소해 담는다.
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 1, vertical: 2),
        child: FittedBox(
          fit: BoxFit.scaleDown,
          child: Column(
            mainAxisSize: MainAxisSize.min,
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
