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
    // 레거시 box_chklist: 행 배경 #f4f4f4, 체크 시 행 전체 #006DB2 + 흰 글씨
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 5, 16, 5),
      child: InkWell(
        onTap: () => onToggle(item.seqNum),
        child: Container(
          width: double.infinity,
          constraints: const BoxConstraints(minHeight: 44),
          color: isChecked ? AppColors.legacyCheckBlue : AppColors.legacyCheckRow,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Container(
                width: 20,
                height: 20,
                decoration: BoxDecoration(
                  color: isChecked ? AppColors.legacyCheckActive : AppColors.white,
                  border: Border.all(
                    color: isChecked
                        ? AppColors.legacyCheckActive
                        : AppColors.legacyTextMute,
                  ),
                  borderRadius: BorderRadius.circular(2),
                ),
                child: isChecked
                    ? const Icon(Icons.check, size: 15, color: AppColors.white)
                    : null,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  item.contents,
                  style: TextStyle(
                    fontSize: 15,
                    height: 1.3,
                    color: isChecked ? AppColors.white : AppColors.textPrimary,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 안전점검 아코디언 항목 위젯 (섹션 1: 장비 착용)
class SafetyCheckAccordionTile extends StatelessWidget {
  final SafetyCheckItem item;

  /// 화면 표시용 1-based 순번 (DB seqNum 아님, 레거시 정합)
  final int displayNumber;
  final String? selectedAnswer;
  final List<String> options;
  final bool isExpanded;
  final VoidCallback onTap;
  final void Function(int seqNum, String answer) onSelect;

  const SafetyCheckAccordionTile({
    super.key,
    required this.item,
    required this.displayNumber,
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
            color: isExpanded ? AppColors.legacyCheckBlue : AppColors.border,
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
                        '$displayNumber) ${item.contents}',
                        style: const TextStyle(
                          fontSize: 14,
                          color: AppColors.textPrimary,
                        ),
                      ),
                    ),
                    if (selectedAnswer != null) ...[
                      const SizedBox(width: 8),
                      Text(
                        selectedAnswer!,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: AppColors.legacyCheckBlue,
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
                                  ? AppColors.legacyCheckBlue
                                  : AppColors.surfaceVariant
                                      .withValues(alpha: 0.3),
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: isSelected
                                    ? AppColors.legacyCheckBlue
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
                                  activeColor: AppColors.white,
                                  materialTapTargetSize:
                                      MaterialTapTargetSize.shrinkWrap,
                                  visualDensity: VisualDensity.compact,
                                ),
                                Text(
                                  option,
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: isSelected
                                        ? FontWeight.w700
                                        : FontWeight.normal,
                                    color: isSelected
                                        ? AppColors.white
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
