import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/claim_code.dart';
import '../common/single_date_picker_sheet.dart';
import 'claim_form_row.dart';

/// 클레임 기한 입력 필드
class ClaimDateField extends StatelessWidget {
  const ClaimDateField({
    super.key,
    required this.dateType,
    required this.date,
    required this.onDateTypeChanged,
    required this.onDateSelected,
  });

  final ClaimDateType dateType;
  final DateTime date;
  final ValueChanged<ClaimDateType> onDateTypeChanged;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    return ClaimFormRow(
      label: '기한',
      isRequired: true,
      below: Row(
        children: [
          // 기한 종류 드롭다운 (보더리스)
          _DateTypeDropdown(
            dateType: dateType,
            onChanged: onDateTypeChanged,
          ),
          const Spacer(),
          // 날짜 선택 (인라인)
          _DatePickerInline(
            date: date,
            onDateSelected: onDateSelected,
          ),
        ],
      ),
    );
  }
}

/// 기한 종류 드롭다운 (소비기한/제조일자)
class _DateTypeDropdown extends StatelessWidget {
  const _DateTypeDropdown({
    required this.dateType,
    required this.onChanged,
  });

  final ClaimDateType dateType;
  final ValueChanged<ClaimDateType> onChanged;

  @override
  Widget build(BuildContext context) {
    return DropdownButton<ClaimDateType>(
      value: dateType,
      isDense: true,
      underline: const SizedBox.shrink(),
      icon: const Icon(
        Icons.arrow_drop_down,
        color: ClaimFormColors.unit,
      ),
      style: const TextStyle(fontSize: 14, color: ClaimFormColors.value),
      items: const [
        DropdownMenuItem(
          value: ClaimDateType.expiryDate,
          child: Text('소비기한'),
        ),
        DropdownMenuItem(
          value: ClaimDateType.manufactureDate,
          child: Text('제조일자'),
        ),
      ],
      onChanged: (value) {
        if (value != null) {
          onChanged(value);
        }
      },
    );
  }
}

/// 날짜 선택 (인라인 텍스트 + 달력 아이콘)
class _DatePickerInline extends StatelessWidget {
  const _DatePickerInline({
    required this.date,
    required this.onDateSelected,
  });

  final DateTime date;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    final dateFormatter = DateFormat('yyyy-MM-dd');

    return InkWell(
      onTap: () => _showDatePicker(context),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              dateFormatter.format(date),
              style: const TextStyle(
                fontSize: 14,
                color: ClaimFormColors.value,
              ),
            ),
            const SizedBox(width: 6),
            const Icon(
              Icons.calendar_today,
              size: 16,
              color: ClaimFormColors.unit,
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showDatePicker(BuildContext context) async {
    // 날짜를 탭하면 즉시 확정하고 모달이 닫힌다(확인 버튼 불필요). 취소 대신 "닫기" 제공.
    final picked = await SingleDatePickerSheet.show(
      context,
      initialDate: date,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );

    if (picked != null) {
      onDateSelected(picked);
    }
  }
}
