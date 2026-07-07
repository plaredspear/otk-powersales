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

  /// 해당 월의 연차 건수
  final int annualLeaveCount;

  /// 해당 월의 대휴 건수
  final int substituteHolidayCount;

  const CalendarWidget({
    super.key,
    required this.year,
    required this.month,
    required this.workDays,
    required this.onDateTap,
    this.annualLeaveCount = 0,
    this.substituteHolidayCount = 0,
  });

  /// 해당 날짜가 근무일인지 확인
  bool _isWorkDay(DateTime date) {
    return workDays.any(
      (workDay) =>
          workDay.hasWork &&
          workDay.date.year == date.year &&
          workDay.date.month == date.month &&
          workDay.date.day == date.day,
    );
  }

  /// 해당 날짜의 근무유형 조회
  String? _getWorkingType(DateTime date) {
    final day = workDays.cast<MonthlyScheduleDay?>().firstWhere(
      (workDay) =>
          workDay!.date.year == date.year &&
          workDay.date.month == date.month &&
          workDay.date.day == date.day,
      orElse: () => null,
    );
    return day?.workingType;
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

  static const List<String> _weekdayLabels = [
    '일', '월', '화', '수', '목', '금', '토',
  ];

  /// 한 주(week 인덱스)의 7칸이 모두 이전/다음 월(null)인지.
  bool _isEmptyWeek(List<DateTime?> dates, int week) {
    for (var d = 0; d < 7; d++) {
      final i = week * 7 + d;
      if (i < dates.length && dates[i] != null) return false;
    }
    return true;
  }

  /// 날짜 한 칸을 그린다. 빈 칸(null)은 높이만 확보해 표 격자를 유지한다.
  Widget _buildCell(DateTime? date, int weekday, double height) {
    if (date == null) {
      return SizedBox(height: height);
    }
    final isWorkDay = _isWorkDay(date);
    final isToday = _isToday(date);
    final workingType = _getWorkingType(date);
    final isAnnualLeave = workingType == '연차';
    final isSubstituteHoliday = workingType == '대휴';
    final isSunday = weekday == 0;
    final isSaturday = weekday == 6;
    final isTappable = isWorkDay || isSubstituteHoliday;

    return GestureDetector(
      onTap: isTappable ? () => onDateTap(date) : null,
      behavior: HitTestBehavior.opaque,
      child: Container(
        height: height,
        color: isToday ? AppColors.otokiYellow.withValues(alpha: 0.3) : null,
        alignment: Alignment.center,
        padding: const EdgeInsets.symmetric(horizontal: 1, vertical: 2),
        // 글꼴 확대 시에도 셀 밖으로 넘치거나 잘리지 않도록 축소해 담는다.
        child: FittedBox(
          fit: BoxFit.scaleDown,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // 날짜 숫자
              Text(
                '${date.day}',
                style: AppTypography.bodyMedium.copyWith(
                  fontWeight: isToday ? FontWeight.bold : FontWeight.normal,
                  color: isSunday
                      ? AppColors.otokiRed
                      : isSaturday
                          ? AppColors.secondary
                          : AppColors.textPrimary,
                ),
              ),
              // 대휴 표시
              if (isSubstituteHoliday)
                Padding(
                  padding: const EdgeInsets.only(top: 2.0),
                  child: Text(
                    '대휴',
                    style: AppTypography.labelSmall.copyWith(
                      color: AppColors.otokiBlue,
                      fontSize: 10,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                )
              // 연차 표시
              else if (isAnnualLeave)
                Padding(
                  padding: const EdgeInsets.only(top: 2.0),
                  child: Text(
                    '연차',
                    style: AppTypography.labelSmall.copyWith(
                      color: AppColors.secondary,
                      fontSize: 10,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                )
              // 근무 표시
              else if (isWorkDay)
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
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final dates = _generateCalendarDates();

    // 시스템 글꼴 확대 시 셀 텍스트가 넘치지 않도록 스케일 상한을 제한한다.
    // (셀 내부는 추가로 FittedBox 로 축소되어 어떤 스케일에서도 숫자가 잘리지 않는다.)
    return MediaQuery.withClampedTextScaling(
      maxScaleFactor: 1.3,
      child: Column(
        children: [
          // Table + TableBorder 로 격자 선을 셀 경계에 정확히 그린다
          // (선이 셀 밖으로 삐져나오지 않는다).
          LayoutBuilder(
              builder: (context, constraints) {
                // 셀 높이는 레거시 비율(0.9)을 유지하도록 셀 너비에서 산출한다.
                final cellHeight = (constraints.maxWidth / 7) / 0.9;
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
                    // 주별 날짜 행 (완전히 빈 주는 표시하지 않음)
                    for (var week = 0; week * 7 < dates.length; week++)
                      if (!_isEmptyWeek(dates, week))
                        TableRow(
                          children: [
                            for (var dow = 0; dow < 7; dow++)
                              _buildCell(
                                dates[week * 7 + dow],
                                dow,
                                cellHeight,
                              ),
                          ],
                        ),
                  ],
                );
              },
            ),

          // 캘린더 아래 남는 공간을 채워 요약을 화면 하단에 붙인다.
          const Spacer(),

        // 연차/대휴 건수 요약 (iOS 홈 인디케이터에 가리지 않도록 하단 SafeArea 적용)
        SafeArea(
          top: false,
          child: Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.sm,
            ),
            decoration: BoxDecoration(
              border: Border(
                top: BorderSide(color: AppColors.divider, width: 1),
              ),
            ),
            child: Row(
              children: [
                Text(
                  '연차: ${annualLeaveCount}일',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.md,
                  ),
                  child: Text(
                    '|',
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textTertiary,
                    ),
                  ),
                ),
                Text(
                  '대휴: ${substituteHolidayCount}일',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.w700,
                    color: AppColors.textPrimary,
                  ),
                ),
              ],
            ),
          ),
        ),
        ],
      ),
    );
  }
}
