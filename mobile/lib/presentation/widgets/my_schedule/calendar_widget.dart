import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/monthly_schedule_day.dart';

/// 커스텀 캘린더 위젯
///
/// 월간 일정을 표시하며, 근무일 셀에 `보고완료 / 총건` 숫자를 표시합니다.
/// (레거시 mgnSchedule calSchedule 셀 = sum/cnt 정합)
class CalendarWidget extends StatelessWidget {
  /// 보고 미완료(과거·오늘) — 레거시 #dc2c34
  static const Color _incompleteColor = Color(0xFFDC2C34);

  /// 보고 완료(과거·오늘) — 레거시 #00b52a
  static const Color _completeColor = Color(0xFF00B52A);

  /// 미래 근무일 — 레거시 #cccccc
  static const Color _futureColor = Color(0xFFCCCCCC);

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

  /// 캘린더가 차지할 가로 폭 (셀 높이 산출용)
  ///
  /// 내부에서 LayoutBuilder 로 측정하지 않고 호출측이 넘긴다.
  /// (이 위젯은 IntrinsicHeight 아래에서 배치되는데, LayoutBuilder 는
  ///  intrinsic 높이를 계산할 수 없어 layout 예외가 발생하기 때문이다.)
  final double width;

  const CalendarWidget({
    super.key,
    required this.year,
    required this.month,
    required this.workDays,
    required this.onDateTap,
    required this.width,
    this.annualLeaveCount = 0,
  });

  /// 해당 날짜의 일정 항목 조회 (없으면 null)
  MonthlyScheduleDay? _getWorkDay(DateTime date) {
    return workDays.cast<MonthlyScheduleDay?>().firstWhere(
      (workDay) =>
          workDay!.date.year == date.year &&
          workDay.date.month == date.month &&
          workDay.date.day == date.day,
      orElse: () => null,
    );
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
    final workDay = _getWorkDay(date);
    final isWorkDay = workDay?.hasWork ?? false;
    final isToday = _isToday(date);
    final workingType = workDay?.workingType;
    final isAnnualLeave = workingType == '연차';
    final isSunday = weekday == 0;
    final isSaturday = weekday == 6;

    return GestureDetector(
      onTap: isWorkDay ? () => onDateTap(date) : null,
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
              // 연차 표시
              // (레거시 본인 캘린더에는 '대휴' 근무유형 자체가 없어 배지도 두지 않는다.)
              if (isAnnualLeave)
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
              // 근무 표시 — 보고완료 / 총건 (레거시 calSchedule 셀 sum/cnt)
              else if (isWorkDay && workDay != null)
                Padding(
                  padding: const EdgeInsets.only(top: 2.0),
                  child: _buildReportCount(date, workDay),
                ),
            ],
          ),
        ),
      ),
    );
  }

  /// 근무일 셀의 `보고완료 / 총건` 숫자쌍 (레거시 calSchedule 셀 정합).
  /// 보고완료 숫자 색: 과거·오늘 미완료=빨강, 과거·오늘 완료=초록, 미래=회색. 분모(/총건)는 검정.
  Widget _buildReportCount(DateTime date, MonthlyScheduleDay day) {
    final now = DateTime.now();
    final target = DateTime(date.year, date.month, date.day);
    final today = DateTime(now.year, now.month, now.day);
    final isFuture = target.isAfter(today);

    final Color completedColor = isFuture
        ? _futureColor
        : (day.completedCount < day.totalCount ? _incompleteColor : _completeColor);

    final baseStyle = AppTypography.labelSmall.copyWith(
      fontSize: 10,
      fontWeight: FontWeight.w600,
    );

    return Text.rich(
      TextSpan(
        children: [
          TextSpan(
            text: '${day.completedCount}',
            style: baseStyle.copyWith(color: completedColor),
          ),
          TextSpan(
            text: ' / ${day.totalCount}',
            style: baseStyle.copyWith(color: AppColors.textPrimary),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final dates = _generateCalendarDates();

    // 셀 높이는 레거시 비율(0.9)을 유지하도록 셀 너비에서 산출한다.
    final cellHeight = (width / 7) / 0.9;

    // 시스템 글꼴 확대 시 셀 텍스트가 넘치지 않도록 스케일 상한을 제한한다.
    // (셀 내부는 추가로 FittedBox 로 축소되어 어떤 스케일에서도 숫자가 잘리지 않는다.)
    return MediaQuery.withClampedTextScaling(
      maxScaleFactor: 1.3,
      child: Column(
        children: [
          // Table + TableBorder 로 격자 선을 셀 경계에 정확히 그린다
          // (선이 셀 밖으로 삐져나오지 않는다).
          Table(
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
                  '연차: $annualLeaveCount일',
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
