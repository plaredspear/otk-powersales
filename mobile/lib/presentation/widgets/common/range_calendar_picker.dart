import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';

/// 날짜 범위 선택 달력 다이얼로그.
///
/// 레거시 jQuery daterangepicker(`singleDatePicker:false`, `maxSpan:7d`) 동작에 정합한다.
/// - 하나의 달력에서 시작일과 종료일을 한 번에 선택한다.
/// - 시작일을 선택하면 시작일 + [maxRangeDays]일까지만 종료일로 선택할 수 있고, 그 이후 날짜는 비활성화된다.
/// - 종료일까지 선택되면 선택한 범위를 반환하며 닫힌다.
///
/// 조회 가능 기간([firstDate]/[lastDate])과 최대 범위 일수([maxRangeDays])는
/// 화면별 조건에 맞게 호출부에서 지정한다. [maxRangeDays] 가 null 이면 범위 일수 제한 없이
/// [firstDate]~[lastDate] 안에서 자유롭게 선택할 수 있다.
///
/// [firstDate]/[lastDate] 를 생략하면 사실상 제한 없이(2000~2100) 달력을 탐색할 수 있다.
/// 레거시 daterangepicker 가 minDate/maxDate 제약이 없는 화면(클레임·현장점검·행사 등)에 정합한다.
Future<DateTimeRange?> showRangeCalendar(
  BuildContext context, {
  required DateTime initialStart,
  required DateTime initialEnd,
  DateTime? firstDate,
  DateTime? lastDate,
  int? maxRangeDays = 7,
}) {
  return showDialog<DateTimeRange>(
    context: context,
    builder: (_) => _RangeCalendarDialog(
      initialStart: initialStart,
      initialEnd: initialEnd,
      firstDate: firstDate ?? DateTime(2000),
      lastDate: lastDate ?? DateTime(2100),
      maxRangeDays: maxRangeDays,
    ),
  );
}

class _RangeCalendarDialog extends StatefulWidget {
  final DateTime initialStart;
  final DateTime initialEnd;
  final DateTime firstDate;
  final DateTime lastDate;
  final int? maxRangeDays;

  const _RangeCalendarDialog({
    required this.initialStart,
    required this.initialEnd,
    required this.firstDate,
    required this.lastDate,
    required this.maxRangeDays,
  });

  @override
  State<_RangeCalendarDialog> createState() => _RangeCalendarDialogState();
}

class _RangeCalendarDialogState extends State<_RangeCalendarDialog> {
  static const List<String> _weekdayLabels = ['일', '월', '화', '수', '목', '금', '토'];

  /// 선택된 시작일(시각 제거). 종료일을 고르는 중에는 end 가 null 이다.
  late DateTime _rangeStart;

  /// 선택된 종료일(시각 제거). null 이면 시작일만 고른 상태.
  DateTime? _rangeEnd;

  /// 현재 화면에 표시 중인 달(1일 기준).
  late DateTime _focusedMonth;

  @override
  void initState() {
    super.initState();
    _rangeStart = _dateOnly(widget.initialStart);
    _rangeEnd = _dateOnly(widget.initialEnd);
    _focusedMonth = DateTime(_rangeStart.year, _rangeStart.month);
  }

  static DateTime _dateOnly(DateTime d) => DateTime(d.year, d.month, d.day);

  bool _isSameDay(DateTime a, DateTime b) =>
      a.year == b.year && a.month == b.month && a.day == b.day;

  /// 종료일 선택 가능한 마지막 날짜(시작일 + maxRangeDays, lastDate 로 클램프).
  /// maxRangeDays 가 null 이면 lastDate 가 한계다.
  DateTime get _selectableEndLimit {
    final maxRangeDays = widget.maxRangeDays;
    if (maxRangeDays == null) return widget.lastDate;
    final limit = _rangeStart.add(Duration(days: maxRangeDays));
    return limit.isAfter(widget.lastDate) ? widget.lastDate : limit;
  }

  /// 현재 단계에서 해당 날짜가 선택 가능한지.
  bool _isSelectable(DateTime day) {
    if (day.isBefore(_dateOnly(widget.firstDate)) ||
        day.isAfter(_dateOnly(widget.lastDate))) {
      return false;
    }
    // 종료일을 고르는 중(시작일만 선택됨): 시작일 이전은 새 시작일로 허용, 시작일 이후는 +maxRangeDays 까지만.
    if (widget.maxRangeDays != null && _rangeEnd == null && day.isAfter(_rangeStart)) {
      return !day.isAfter(_selectableEndLimit);
    }
    return true;
  }

  bool _isInRange(DateTime day) {
    if (_rangeEnd == null) return false;
    return !day.isBefore(_rangeStart) && !day.isAfter(_rangeEnd!);
  }

  void _onDayTap(DateTime day) {
    setState(() {
      if (_rangeEnd != null) {
        // 범위가 이미 확정된 상태에서 다시 누르면 새 범위 시작.
        _rangeStart = day;
        _rangeEnd = null;
        return;
      }
      if (day.isBefore(_rangeStart)) {
        // 시작일 이전을 누르면 시작일을 다시 잡는다.
        _rangeStart = day;
        return;
      }
      // 시작일 이후(또는 동일)를 누르면 종료일 확정 → 범위 반환.
      _rangeEnd = day;
    });
    if (_rangeEnd != null) {
      Navigator.of(context).pop(DateTimeRange(start: _rangeStart, end: _rangeEnd!));
    }
  }

  void _changeMonth(int delta) {
    setState(() {
      _focusedMonth = DateTime(_focusedMonth.year, _focusedMonth.month + delta);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            _buildHeader(),
            const SizedBox(height: 12),
            _buildWeekdayRow(),
            const SizedBox(height: 4),
            _buildDayGrid(),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        IconButton(
          onPressed: () => _changeMonth(-1),
          icon: const Icon(Icons.chevron_left),
          color: AppColors.textPrimary,
        ),
        Text(
          '${_focusedMonth.year}년 ${_focusedMonth.month}월',
          style: AppTypography.bodyLarge.copyWith(fontWeight: FontWeight.w600),
        ),
        IconButton(
          onPressed: () => _changeMonth(1),
          icon: const Icon(Icons.chevron_right),
          color: AppColors.textPrimary,
        ),
      ],
    );
  }

  Widget _buildWeekdayRow() {
    return Row(
      children: List.generate(7, (i) {
        return Expanded(
          child: Center(
            child: Text(
              _weekdayLabels[i],
              style: AppTypography.bodySmall.copyWith(
                color: i == 0
                    ? AppColors.otokiRed
                    : (i == 6 ? AppColors.otokiBlue : AppColors.textSecondary),
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        );
      }),
    );
  }

  Widget _buildDayGrid() {
    final firstOfMonth = DateTime(_focusedMonth.year, _focusedMonth.month, 1);
    // 일요일(weekday 7)을 한 주의 시작으로 맞춰 앞쪽 빈칸 수를 구한다.
    final leadingBlanks = firstOfMonth.weekday % 7;
    final gridStart = firstOfMonth.subtract(Duration(days: leadingBlanks));

    return Column(
      children: List.generate(6, (week) {
        return Row(
          children: List.generate(7, (dow) {
            final day = gridStart.add(Duration(days: week * 7 + dow));
            return Expanded(child: _buildDayCell(day));
          }),
        );
      }),
    );
  }

  Widget _buildDayCell(DateTime day) {
    final inMonth = day.month == _focusedMonth.month;
    final selectable = inMonth && _isSelectable(day);
    final isStart = _isSameDay(day, _rangeStart);
    final isEnd = _rangeEnd != null && _isSameDay(day, _rangeEnd!);
    final isEndpoint = isStart || isEnd;
    final inRange = inMonth && _isInRange(day);

    Color? bgColor;
    if (isEndpoint && inMonth) {
      bgColor = AppColors.otokiBlue;
    } else if (inRange) {
      bgColor = AppColors.secondaryLight.withValues(alpha: 0.18);
    }

    Color textColor;
    if (isEndpoint && inMonth) {
      textColor = AppColors.onSecondary;
    } else if (!inMonth || !selectable) {
      textColor = AppColors.textTertiary;
    } else {
      textColor = AppColors.textPrimary;
    }

    // 현재 달이지만 선택 불가한 날짜(시작일 + maxRangeDays 초과 등)는 취소선으로 표시한다.
    final isDisabledInMonth = inMonth && !selectable;

    return GestureDetector(
      onTap: selectable ? () => _onDayTap(day) : null,
      behavior: HitTestBehavior.opaque,
      child: Container(
        height: 40,
        margin: const EdgeInsets.symmetric(vertical: 2),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(8),
        ),
        alignment: Alignment.center,
        child: Text(
          '${day.day}',
          style: AppTypography.bodyMedium.copyWith(
            color: textColor,
            fontWeight: isEndpoint ? FontWeight.w700 : FontWeight.w400,
            decoration: isDisabledInMonth ? TextDecoration.lineThrough : null,
            decorationColor: AppColors.textTertiary,
          ),
        ),
      ),
    );
  }
}
