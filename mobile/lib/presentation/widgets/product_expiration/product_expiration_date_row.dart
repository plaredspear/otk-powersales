import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../claim/claim_form_row.dart';

/// 유통기한 등록/수정 폼의 날짜 선택 행
///
/// 레거시 리스트형 폼 행(라벨 + 하단 날짜 + 우측 달력 아이콘)을 따른다.
/// 행을 탭하면 달력이 열린다.
class ProductExpirationDateRow extends StatelessWidget {
  const ProductExpirationDateRow({
    super.key,
    required this.label,
    required this.date,
    required this.onDateChanged,
  });

  final String label;
  final DateTime date;
  final ValueChanged<DateTime> onDateChanged;

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return ClaimFormRow(
      label: label,
      isRequired: true,
      onTap: () => _pickDate(context),
      trailing: const Icon(
        Icons.calendar_today,
        size: 16,
        color: ClaimFormColors.unit,
      ),
      below: Text(
        dateFormat.format(date),
        style: const TextStyle(fontSize: 14, color: ClaimFormColors.value),
      ),
    );
  }

  Future<void> _pickDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: date,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      locale: const Locale('ko', 'KR'),
    );
    if (picked != null) {
      onDateChanged(picked);
    }
  }
}
