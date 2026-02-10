import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';

/// 출근등록 현황 카운터 위젯 (✓ N/M)
class AttendanceStatusCounter extends StatelessWidget {
  final int registeredCount;
  final int totalCount;
  final VoidCallback onTap;

  const AttendanceStatusCounter({
    super.key,
    required this.registeredCount,
    required this.totalCount,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isComplete = registeredCount >= totalCount && totalCount > 0;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isComplete
              ? AppColors.success.withOpacity(0.1)
              : AppColors.otokiBlue.withOpacity(0.08),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isComplete
                ? AppColors.success.withOpacity(0.3)
                : AppColors.otokiBlue.withOpacity(0.2),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              isComplete ? Icons.check_circle : Icons.check_circle_outline,
              size: 16,
              color: isComplete ? AppColors.success : AppColors.otokiBlue,
            ),
            const SizedBox(width: 4),
            Text(
              '$registeredCount/$totalCount',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: isComplete ? AppColors.success : AppColors.otokiBlue,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
