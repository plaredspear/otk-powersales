import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 유통기한 등록 폼 위젯
///
/// 거래처 선택, 제품 선택, 유통기한, 알림일, 설명 입력을 제공합니다.
class ShelfLifeRegisterForm extends StatelessWidget {
  /// 거래처 목록
  final Map<int, String> stores;

  /// 선택된 거래처 ID
  final int? selectedStoreId;

  /// 선택된 제품 코드
  final String? selectedProductCode;

  /// 선택된 제품명
  final String? selectedProductName;

  /// 유통기한
  final DateTime expiryDate;

  /// 마감 전 알림 날짜
  final DateTime alertDate;

  /// 설명
  final String description;

  /// 거래처 선택 콜백
  final void Function(int storeId, String storeName) onStoreChanged;

  /// 제품 선택 버튼 콜백
  final VoidCallback onSelectProduct;

  /// 바코드 스캔 버튼 콜백
  final VoidCallback onScanBarcode;

  /// 유통기한 변경 콜백
  final void Function(DateTime date) onExpiryDateChanged;

  /// 알림일 변경 콜백
  final void Function(DateTime date) onAlertDateChanged;

  /// 설명 변경 콜백
  final void Function(String text) onDescriptionChanged;

  const ShelfLifeRegisterForm({
    super.key,
    required this.stores,
    this.selectedStoreId,
    this.selectedProductCode,
    this.selectedProductName,
    required this.expiryDate,
    required this.alertDate,
    required this.description,
    required this.onStoreChanged,
    required this.onSelectProduct,
    required this.onScanBarcode,
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
        // 거래처 선택
        _buildLabel('거래처', required: true),
        const SizedBox(height: AppSpacing.xs),
        _buildStoreDropdown(),

        const SizedBox(height: AppSpacing.lg),

        // 제품 선택
        _buildLabel('제품', required: true),
        const SizedBox(height: AppSpacing.xs),
        _buildProductSelector(),

        const SizedBox(height: AppSpacing.lg),

        // 유통기한
        _buildLabel('유통기한', required: true),
        const SizedBox(height: AppSpacing.xs),
        _buildDateField(
          context: context,
          date: expiryDate,
          dateFormat: dateFormat,
          onChanged: onExpiryDateChanged,
        ),

        const SizedBox(height: AppSpacing.lg),

        // 마감 전 알림
        _buildLabel('마감 전 알림', required: true),
        const SizedBox(height: AppSpacing.xs),
        _buildDateField(
          context: context,
          date: alertDate,
          dateFormat: dateFormat,
          onChanged: onAlertDateChanged,
        ),

        const SizedBox(height: AppSpacing.lg),

        // 설명
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

  Widget _buildStoreDropdown() {
    final sortedEntries = stores.entries.toList()
      ..sort((a, b) => a.value.compareTo(b.value));

    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: AppColors.divider),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<int>(
          value: selectedStoreId,
          hint: Text('거래처 선택', style: AppTypography.bodyMedium.copyWith(color: AppColors.textTertiary)),
          isExpanded: true,
          items: sortedEntries.map((entry) {
            return DropdownMenuItem<int>(
              value: entry.key,
              child: Text(entry.value, style: AppTypography.bodyMedium),
            );
          }).toList(),
          onChanged: (storeId) {
            if (storeId != null) {
              onStoreChanged(storeId, stores[storeId]!);
            }
          },
        ),
      ),
    );
  }

  Widget _buildProductSelector() {
    return Row(
      children: [
        Expanded(
          child: Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.md,
            ),
            decoration: BoxDecoration(
              border: Border.all(color: AppColors.divider),
              borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
              color: AppColors.background,
            ),
            child: Text(
              selectedProductName ?? '제품을 선택해주세요',
              style: AppTypography.bodyMedium.copyWith(
                color: selectedProductName != null
                    ? AppColors.textPrimary
                    : AppColors.textTertiary,
              ),
            ),
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        SizedBox(
          height: AppSpacing.buttonHeight,
          child: OutlinedButton(
            onPressed: onSelectProduct,
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: AppColors.primary),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
              ),
            ),
            child: Text('선택', style: AppTypography.bodyMedium.copyWith(color: AppColors.primary)),
          ),
        ),
        const SizedBox(width: AppSpacing.xs),
        SizedBox(
          height: AppSpacing.buttonHeight,
          child: OutlinedButton.icon(
            onPressed: onScanBarcode,
            icon: const Icon(Icons.qr_code_scanner, size: 18),
            label: Text('바코드', style: AppTypography.bodySmall.copyWith(color: AppColors.primary)),
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: AppColors.primary),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
              ),
            ),
          ),
        ),
      ],
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
