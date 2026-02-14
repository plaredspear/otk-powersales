import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/monthly_schedule_day.dart';

/// 커스텀 캘린더 위젯
///
/// 월간 일정을 표시하며, 근무일에 "근무" 마커를 표시합니다.
class CalendarWidget extends StatelessWidget {
  /// 표시할 연도
  final int year;

  /// 표시할 월 (1-12)
  final int month;

  /// 근무일 목록
  final List<MonthlyScheduleDay> workDays;

  /// 날짜 선택 시 콜백
  final Function(DateTime) onDateTap;

  const CalendarWidget({
    super.key,
    required this.year,
    required this.month,
    required this.workDays,
    required this.onDateTap,
  });

  /// 해당 날짜가 근무일인지 확인
  bool _isWorkDay(DateTime date) {
    return workDays.any((workDay) =>
        workDay.hasWork &&
        workDay.date.year == date.year &&
        workDay.date.month == date.month &&
        workDay.date.day == date.day);
  }

  /// 해당 날짜가 오늘인지 확인
  bool _isToday(DateTime date) {
    final now = DateTime.now();
    return date.year == now.year &&
        date.month == now.month &&
        date.day == now.day;
  }

  /// 해당 월의 달력 날짜 목록 생성 (6주 고정)
  List<DateTime?> _generateCalendarDates() {
    final firstDay = DateTime(year, month, 1);
    final lastDay = DateTime(year, month + 1, 0);

    // 첫 주 시작일 (일요일 기준)
    final firstWeekday = firstDay.weekday % 7; // 일요일=0, 월요일=1, ..., 토요일=6
    final startDate = firstDay.subtract(Duration(days: firstWeekday));

    // 6주 = 42일 생성
    final dates = <DateTime?>[];
    for (int i = 0; i < 42; i++) {
      final date = startDate.add(Duration(days: i));
      // 현재 월의 날짜만 추가, 이전/다음 월은 null
      if (date.month == month) {
        dates.add(date);
      } else {
        dates.add(null);
      }
    }

    return dates;
  }

  @override
  Widget build(BuildContext context) {
    final dates = _generateCalendarDates();

    return Column(
      children: [
        // 요일 헤더
        Container(
          padding: const EdgeInsets.symmetric(
            vertical: AppSpacing.sm,
            horizontal: AppSpacing.md,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: ['일', '월', '화', '수', '목', '금', '토'].map((weekday) {
              return Expanded(
                child: Center(
                  child: Text(
                    weekday,
                    style: AppTypography.bodyMedium.copyWith(
                      fontWeight: FontWeight.w600,
                      color: weekday == '일'
                          ? AppColors.otokiRed
                          : weekday == '토'
                              ? AppColors.secondary
                              : AppColors.textPrimary,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),

        // 날짜 그리드
        Expanded(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
            child: GridView.builder(
              physics: const NeverScrollableScrollPhysics(),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 7,
                childAspectRatio: 0.9,
              ),
              itemCount: dates.length,
              itemBuilder: (context, index) {
                final date = dates[index];

                // 이전/다음 월 날짜는 빈 셀
                if (date == null) {
                  return const SizedBox.shrink();
                }

                final isWorkDay = _isWorkDay(date);
                final isToday = _isToday(date);
                final weekday = index % 7;
                final isSunday = weekday == 0;
                final isSaturday = weekday == 6;

                return GestureDetector(
                  onTap: isWorkDay ? () => onDateTap(date) : null,
                  child: Container(
                    margin: const EdgeInsets.all(2.0),
                    decoration: BoxDecoration(
                      color: isToday
                          ? AppColors.otokiYellow.withOpacity(0.3)
                          : Colors.transparent,
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // 날짜 숫자
                        Text(
                          '${date.day}',
                          style: AppTypography.bodyMedium.copyWith(
                            fontWeight:
                                isToday ? FontWeight.bold : FontWeight.normal,
                            color: isSunday
                                ? AppColors.otokiRed
                                : isSaturday
                                    ? AppColors.secondary
                                    : AppColors.textPrimary,
                          ),
                        ),

                        // 근무 표시
                        if (isWorkDay)
                          Padding(
                            padding: const EdgeInsets.only(top: 2.0),
                            child: Text(
                              '근무',
                              style: AppTypography.labelSmall.copyWith(
                                color: AppColors.otokiRed,
                                fontSize: 10,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ),
      ],
    );
  }
}
