import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/claim_code.dart';

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
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 필드 라벨
        const Text(
          '기한 *',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(height: 8),

        // 기한 종류 + 날짜 선택
        Row(
          children: [
            // 기한 종류 드롭다운
            Expanded(
              flex: 2,
              child: DropdownButtonFormField<ClaimDateType>(
                value: dateType,
                decoration: const InputDecoration(
                  contentPadding:
                      EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  border: OutlineInputBorder(),
                ),
                items: const [
                  DropdownMenuItem(
                    value: ClaimDateType.expiryDate,
                    child: Text('유통기한'),
                  ),
                  DropdownMenuItem(
                    value: ClaimDateType.manufactureDate,
                    child: Text('제조일자'),
                  ),
                ],
                onChanged: (value) {
                  if (value != null) {
                    onDateTypeChanged(value);
                  }
                },
              ),
            ),
            const SizedBox(width: 8),

            // 날짜 선택 버튼
            Expanded(
              flex: 3,
              child: _DatePickerButton(
                date: date,
                onDateSelected: onDateSelected,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

/// 날짜 선택 버튼
class _DatePickerButton extends StatelessWidget {
  const _DatePickerButton({
    required this.date,
    required this.onDateSelected,
  });

  final DateTime date;
  final ValueChanged<DateTime> onDateSelected;

  @override
  Widget build(BuildContext context) {
    final dateFormatter = DateFormat('yyyy-MM-dd');

    return OutlinedButton(
      onPressed: () => _showDatePicker(context),
      style: OutlinedButton.styleFrom(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            dateFormatter.format(date),
            style: const TextStyle(
              fontSize: 14,
              color: Colors.black87,
            ),
          ),
          const Text(
            '📅',
            style: TextStyle(fontSize: 18),
          ),
        ],
      ),
    );
  }

  Future<void> _showDatePicker(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: date,
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
      locale: const Locale('ko', 'KR'),
    );

    if (picked != null) {
      onDateSelected(picked);
    }
  }
}
