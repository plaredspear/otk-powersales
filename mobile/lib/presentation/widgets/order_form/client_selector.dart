import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처 선택 드롭다운
class ClientSelector extends StatelessWidget {
  final Map<int, String> clients;
  final int? selectedClientId;
  final ValueChanged<int> onClientSelected;

  const ClientSelector({
    super.key,
    required this.clients,
    required this.selectedClientId,
    required this.onClientSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        RichText(
          text: TextSpan(
            text: '거래처 ',
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
        DropdownButtonFormField<int>(
          // ignore: deprecated_member_use
          value: selectedClientId,
          hint: Text(
            '선택하세요',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          decoration: InputDecoration(
            contentPadding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.md,
            ),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              borderSide: BorderSide(color: AppColors.border),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              borderSide: BorderSide(color: AppColors.border),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              borderSide: BorderSide(color: AppColors.primary, width: 2),
            ),
          ),
          items: clients.entries.map((entry) {
            return DropdownMenuItem<int>(
              value: entry.key,
              child: Text(
                '[${entry.key}] ${entry.value}',
                style: AppTypography.bodyMedium,
              ),
            );
          }).toList(),
          onChanged: (value) {
            if (value != null) {
              onClientSelected(value);
            }
          },
        ),
      ],
    );
  }
}
