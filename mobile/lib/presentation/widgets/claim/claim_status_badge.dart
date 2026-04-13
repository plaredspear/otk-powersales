import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 클레임 상태 뱃지 위젯
class ClaimStatusBadge extends StatelessWidget {
  final String status;
  final String statusLabel;

  const ClaimStatusBadge({
    super.key,
    required this.status,
    required this.statusLabel,
  });

  @override
  Widget build(BuildContext context) {
    final color = _getStatusColor();
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: 2,
      ),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Text(
        statusLabel,
        style: AppTypography.labelSmall.copyWith(
          color: color,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }

  Color _getStatusColor() {
    switch (status) {
      case 'SUBMITTED':
        return Colors.blue;
      case 'IN_PROGRESS':
        return Colors.orange;
      case 'RESOLVED':
        return Colors.green;
      case 'REJECTED':
        return AppColors.error;
      default:
        return AppColors.textSecondary;
    }
  }
}
