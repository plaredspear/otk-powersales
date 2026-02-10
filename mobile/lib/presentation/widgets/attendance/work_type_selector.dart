import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 근무유형 라디오 선택 위젯
///
/// "상온" / "냉장/냉동" 택 1 라디오 버튼 그룹
class WorkTypeSelector extends StatelessWidget {
  final String selectedWorkType;
  final ValueChanged<String> onChanged;

  const WorkTypeSelector({
    super.key,
    required this.selectedWorkType,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.white,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            '근무유형',
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: _WorkTypeRadio(
                  label: '상온',
                  value: 'ROOM_TEMP',
                  groupValue: selectedWorkType,
                  onChanged: onChanged,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _WorkTypeRadio(
                  label: '냉장/냉동',
                  value: 'REFRIGERATED',
                  groupValue: selectedWorkType,
                  onChanged: onChanged,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _WorkTypeRadio extends StatelessWidget {
  final String label;
  final String value;
  final String groupValue;
  final ValueChanged<String> onChanged;

  const _WorkTypeRadio({
    required this.label,
    required this.value,
    required this.groupValue,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    final isSelected = value == groupValue;

    return InkWell(
      onTap: () => onChanged(value),
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.otokiBlue.withOpacity(0.08)
              : AppColors.background,
          border: Border.all(
            color: isSelected ? AppColors.otokiBlue : AppColors.border,
            width: isSelected ? 1.5 : 1,
          ),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              isSelected
                  ? Icons.radio_button_checked
                  : Icons.radio_button_unchecked,
              size: 20,
              color: isSelected ? AppColors.otokiBlue : AppColors.textTertiary,
            ),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                fontSize: 14,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.w400,
                color:
                    isSelected ? AppColors.otokiBlue : AppColors.textSecondary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
