import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/safety_check_item.dart';

/// 안전점검 체크리스트 항목 위젯
///
/// 개별 체크리스트 항목을 체크박스와 라벨 텍스트로 표시합니다.
class SafetyCheckItemTile extends StatelessWidget {
  /// 안전점검 항목
  final SafetyCheckItem item;

  /// 체크 상태
  final bool isChecked;

  /// 체크 상태 변경 콜백
  final ValueChanged<int> onToggle;

  const SafetyCheckItemTile({
    super.key,
    required this.item,
    required this.isChecked,
    required this.onToggle,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: () => onToggle(item.id),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
        child: Row(
          children: [
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: isChecked,
                onChanged: (_) => onToggle(item.id),
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
                item.label,
                style: TextStyle(
                  fontSize: 15,
                  color: isChecked
                      ? AppColors.textPrimary
                      : AppColors.textSecondary,
                  decoration:
                      isChecked ? TextDecoration.lineThrough : null,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
