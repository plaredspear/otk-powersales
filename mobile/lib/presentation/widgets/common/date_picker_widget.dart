import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

/// 날짜 범위 선택 위젯
/// 시작일과 종료일을 선택할 수 있으며, 날짜 범위 validation을 제공합니다.
class DatePickerWidget extends StatefulWidget {
  /// 초기 시작일
  final DateTime? initialStartDate;

  /// 초기 종료일
  final DateTime? initialEndDate;

  /// 날짜 범위가 변경될 때 호출되는 콜백
  final Function(DateTime startDate, DateTime endDate)? onDateRangeChanged;

  /// 시작일이 변경될 때 호출되는 콜백
  final Function(DateTime date)? onStartDateChanged;

  /// 종료일이 변경될 때 호출되는 콜백
  final Function(DateTime date)? onEndDateChanged;

  /// 선택 가능한 최소 날짜
  final DateTime? minDate;

  /// 선택 가능한 최대 날짜
  final DateTime? maxDate;

  /// 시작일 레이블
  final String startDateLabel;

  /// 종료일 레이블
  final String endDateLabel;

  const DatePickerWidget({
    super.key,
    this.initialStartDate,
    this.initialEndDate,
    this.onDateRangeChanged,
    this.onStartDateChanged,
    this.onEndDateChanged,
    this.minDate,
    this.maxDate,
    this.startDateLabel = '시작일',
    this.endDateLabel = '종료일',
  });

  @override
  State<DatePickerWidget> createState() => _DatePickerWidgetState();
}

class _DatePickerWidgetState extends State<DatePickerWidget> {
  late DateTime _startDate;
  late DateTime _endDate;
  final DateFormat _dateFormat = DateFormat('yyyy-MM-dd');

  @override
  void initState() {
    super.initState();
    _startDate = widget.initialStartDate ?? DateTime.now();
    _endDate = widget.initialEndDate ?? DateTime.now();
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: _buildDateButton(
            label: widget.startDateLabel,
            date: _startDate,
            onTap: () => _selectStartDate(context),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _buildDateButton(
            label: widget.endDateLabel,
            date: _endDate,
            onTap: () => _selectEndDate(context),
          ),
        ),
      ],
    );
  }

  Widget _buildDateButton({
    required String label,
    required DateTime date,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.grey),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontSize: 12,
                    color: Colors.grey,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _dateFormat.format(date),
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
            const Icon(Icons.calendar_today, size: 20, color: Colors.grey),
          ],
        ),
      ),
    );
  }

  Future<void> _selectStartDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _startDate,
      firstDate: widget.minDate ?? DateTime(2000),
      lastDate: _endDate, // 시작일은 종료일보다 이전이어야 함
    );

    if (picked != null && picked != _startDate) {
      setState(() {
        _startDate = picked;
      });
      widget.onStartDateChanged?.call(picked);
      widget.onDateRangeChanged?.call(_startDate, _endDate);
    }
  }

  Future<void> _selectEndDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _endDate,
      firstDate: _startDate, // 종료일은 시작일보다 이후여야 함
      lastDate: widget.maxDate ?? DateTime(2100),
    );

    if (picked != null && picked != _endDate) {
      setState(() {
        _endDate = picked;
      });
      widget.onEndDateChanged?.call(picked);
      widget.onDateRangeChanged?.call(_startDate, _endDate);
    }
  }
}
