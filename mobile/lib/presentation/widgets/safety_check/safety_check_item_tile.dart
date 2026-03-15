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

/// 안전점검 아코디언 항목 위젯 (섹션 1: 장비 착용)
class SafetyCheckAccordionTile extends StatelessWidget {
  final SafetyCheckItem item;
  final String? selectedAnswer;
  final List<String> options;
  final bool isExpanded;
  final VoidCallback onTap;
  final void Function(int seqNum, String answer) onSelect;

  const SafetyCheckAccordionTile({
    super.key,
    required this.item,
    required this.selectedAnswer,
    required this.options,
    required this.isExpanded,
    required this.onTap,
    required this.onSelect,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 4.0),
      child: Container(
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isExpanded ? AppColors.primary : AppColors.border,
          ),
        ),
        child: Column(
          children: [
            // 헤더 (항상 표시)
            InkWell(
              onTap: onTap,
              borderRadius: BorderRadius.circular(8),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12.0,
                  vertical: 12.0,
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        '${item.seqNum}) ${item.contents}',
                        style: TextStyle(
                          fontSize: 14,
                          color: selectedAnswer != null
                              ? AppColors.textPrimary
                              : AppColors.textSecondary,
                        ),
                      ),
                    ),
                    if (selectedAnswer != null) ...[
                      const SizedBox(width: 8),
                      Text(
                        selectedAnswer!,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                    const SizedBox(width: 4),
                    Icon(
                      isExpanded
                          ? Icons.keyboard_arrow_up
                          : Icons.keyboard_arrow_down,
                      size: 20,
                      color: AppColors.textTertiary,
                    ),
                  ],
                ),
              ),
            ),
            // 라디오 버튼 (펼친 상태에서만)
            if (isExpanded)
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                child: Row(
                  children: options.map((option) {
                    final isSelected = selectedAnswer == option;
                    return Expanded(
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: InkWell(
                          onTap: () => onSelect(item.seqNum, option),
                          borderRadius: BorderRadius.circular(8),
                          child: Container(
                            padding: const EdgeInsets.symmetric(vertical: 10),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? AppColors.primary.withValues(alpha: 0.1)
                                  : AppColors.surfaceVariant
                                      .withValues(alpha: 0.3),
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: isSelected
                                    ? AppColors.primary
                                    : AppColors.border,
                              ),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Radio<String>(
                                  value: option,
                                  groupValue: selectedAnswer,
                                  onChanged: (value) {
                                    if (value != null) {
                                      onSelect(item.seqNum, value);
                                    }
                                  },
                                  activeColor: AppColors.primary,
                                  materialTapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                  visualDensity: VisualDensity.compact,
                                ),
                                Text(
                                  option,
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: isSelected
                                        ? FontWeight.w600
                                        : FontWeight.normal,
                                    color: isSelected
                                        ? AppColors.primary
                                        : AppColors.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
