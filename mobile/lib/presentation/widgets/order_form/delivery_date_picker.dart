import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 납기일 선택 필드
class DeliveryDatePicker extends StatelessWidget {
  final DateTime? selectedDate;
  final VoidCallback onTap;

  const DeliveryDatePicker({
    super.key,
    required this.selectedDate,
    required this.onTap,
  });

  String _formatDate(DateTime date) {
    const dayNames = ['월', '화', '수', '목', '금', '토', '일'];
    final formatter = DateFormat('yyyy-MM-dd');
    final dayOfWeek = dayNames[date.weekday - 1];
    return '${formatter.format(date)} ($dayOfWeek)';
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        RichText(
          text: TextSpan(
            text: '납기일 ',
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textPrimary,
            ),
            children: [
              TextSpan(
                text: '*',
                style: TextStyle(
                  color: AppColors.error,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        OutlinedButton(
          onPressed: onTap,
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(double.infinity, 48),
            side: BorderSide(color: AppColors.border),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            ),
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.md,
            ),
          ),
          child: Align(
            alignment: Alignment.centerLeft,
            child: Text(
              selectedDate != null ? _formatDate(selectedDate!) : '선택하세요',
              style: AppTypography.bodyMedium.copyWith(
                color: selectedDate != null
                    ? AppColors.textPrimary
                    : AppColors.textSecondary,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
