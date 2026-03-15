import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/safety_check_item.dart';

/// 안전점검 체크박스 항목 위젯 (섹션 2: 예방사항)
class SafetyCheckCheckboxTile extends StatelessWidget {
  final SafetyCheckItem item;
  final bool isChecked;
  final ValueChanged<int> onToggle;

  const SafetyCheckCheckboxTile({
    super.key,
    required this.item,
    required this.isChecked,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => onToggle(item.seqNum),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
        child: Row(
          children: [
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: isChecked,
                onChanged: (_) => onToggle(item.seqNum),
                activeColor: AppColors.primary,
                checkColor: AppColors.onPrimary,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                item.contents,
                style: TextStyle(
                  fontSize: 15,
                  color: isChecked
                      ? AppColors.textPrimary
                      : AppColors.textSecondary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 안전점검 라디오 항목 위젯 (섹션 1: 장비 착용)
class SafetyCheckRadioTile extends StatelessWidget {
  final SafetyCheckItem item;
  final String? selectedAnswer;
  final List<String> options;
  final void Function(int seqNum, String answer) onSelect;

  const SafetyCheckRadioTile({
    super.key,
    required this.item,
    required this.selectedAnswer,
    required this.options,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 6.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // 항목 번호 + 내용
          Expanded(
            flex: 3,
            child: Text(
              '${item.seqNum}. ${item.contents}',
              style: TextStyle(
                fontSize: 14,
                color: selectedAnswer != null
                    ? AppColors.textPrimary
                    : AppColors.textSecondary,
              ),
            ),
          ),
          // 라디오 버튼들
          ...options.map((option) {
            final isSelected = selectedAnswer == option;
            return SizedBox(
              width: 72,
              child: InkWell(
                onTap: () => onSelect(item.seqNum, option),
                borderRadius: BorderRadius.circular(20),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Radio<String>(
                      value: option,
                      groupValue: selectedAnswer,
                      onChanged: (value) {
                        if (value != null) onSelect(item.seqNum, value);
                      },
                      activeColor: AppColors.primary,
                      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      visualDensity: VisualDensity.compact,
                    ),
                  ],
                ),
              ),
            );
          }),
        ],
      ),
    );
  }
}
