import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 유통기한 수정 폼 위젯
///
/// 거래처/제품은 읽기 전용, 유통기한/알림일/설명만 수정 가능합니다.
class ShelfLifeEditForm extends StatelessWidget {
  /// 거래처명 (읽기 전용)
  final String storeName;

  /// 제품명 (읽기 전용)
  final String productName;

  /// 제품 코드 (읽기 전용)
  final String productCode;

  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  /// 설명
  final String description;

  /// 유통기한 변경 콜백
  final void Function(DateTime date) onExpiryDateChanged;

  /// 알림일 변경 콜백
  final void Function(DateTime date) onAlertDateChanged;

  /// 설명 변경 콜백
  final void Function(String text) onDescriptionChanged;

  const ShelfLifeEditForm({
    super.key,
    required this.storeName,
    required this.productName,
    required this.productCode,
    required this.expiryDate,
    required this.alertDate,
    required this.description,
    required this.onExpiryDateChanged,
    required this.onAlertDateChanged,
    required this.onDescriptionChanged,
  });

  @override
  Widget build(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return ListView(
      padding: const EdgeInsets.all(AppSpacing.lg),
      children: [
        // 거래처 (읽기 전용)
        _buildLabel('거래처'),
        const SizedBox(height: AppSpacing.xs),
        _buildReadOnlyField(storeName),

        const SizedBox(height: AppSpacing.lg),

        // 제품 (읽기 전용)
        _buildLabel('제품'),
        const SizedBox(height: AppSpacing.xs),
        _buildReadOnlyField('$productName ($productCode)'),

        const SizedBox(height: AppSpacing.lg),

        // 유통기한 (수정 가능)
        _buildLabel('유통기한', required: true),
        const SizedBox(height: AppSpacing.xs),
        _buildDateField(
          context: context,
          date: expiryDate,
          dateFormat: dateFormat,
          onChanged: onExpiryDateChanged,
        ),

        const SizedBox(height: AppSpacing.lg),

        // 마감 전 알림 (수정 가능)
        _buildLabel('마감 전 알림', required: true),
        const SizedBox(height: AppSpacing.xs),
        _buildDateField(
          context: context,
          date: alertDate,
          dateFormat: dateFormat,
          onChanged: onAlertDateChanged,
        ),

        const SizedBox(height: AppSpacing.lg),

        // 설명 (수정 가능)
        _buildLabel('설명'),
        const SizedBox(height: AppSpacing.xs),
        _buildDescriptionField(),

        const SizedBox(height: AppSpacing.xl),
      ],
    );
  }

  Widget _buildLabel(String text, {bool required = false}) {
    return Row(
      children: [
        Text(text, style: AppTypography.headlineSmall),
        if (required) ...[
          const SizedBox(width: 4),
          Text('*', style: AppTypography.bodyMedium.copyWith(color: AppColors.error)),
        ],
      ],
    );
  }

  Widget _buildReadOnlyField(String value) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.md,
      ),
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: Border.all(color: AppColors.divider),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        value,
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textSecondary,
        ),
      ),
    );
  }

  Widget _buildDateField({
    required BuildContext context,
    required DateTime date,
    required DateFormat dateFormat,
    required void Function(DateTime) onChanged,
  }) {
    return InkWell(
      onTap: () async {
        final picked = await showDatePicker(
          context: context,
          initialDate: date,
          firstDate: DateTime(2020),
          lastDate: DateTime(2030),
        );
        if (picked != null) {
          onChanged(picked);
        }
      },
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.md,
        ),
        decoration: BoxDecoration(
          border: Border.all(color: AppColors.divider),
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(dateFormat.format(date), style: AppTypography.bodyMedium),
            const Icon(Icons.calendar_today, size: 18, color: AppColors.textSecondary),
          ],
        ),
      ),
    );
  }

  Widget _buildDescriptionField() {
    return TextFormField(
      initialValue: description,
      maxLines: 3,
      decoration: InputDecoration(
        hintText: '설명을 입력해주세요 (선택)',
        hintStyle: AppTypography.bodyMedium.copyWith(color: AppColors.textTertiary),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: AppColors.divider),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
          borderSide: const BorderSide(color: AppColors.divider),
        ),
        contentPadding: const EdgeInsets.all(AppSpacing.md),
      ),
      style: AppTypography.bodyMedium,
      onChanged: onDescriptionChanged,
    );
  }
}
