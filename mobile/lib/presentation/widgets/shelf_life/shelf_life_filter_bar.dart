import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 유통기한 검색 필터 바
///
/// 거래처 드롭다운 + 유통기한 날짜 범위 + 검색 버튼
class ShelfLifeFilterBar extends StatelessWidget {
  /// 거래처 목록 {storeId: storeName}
  final Map<int, String> stores;

  /// 선택된 거래처 ID
  final int? selectedStoreId;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 거래처 선택 콜백
  final void Function(int? storeId, String? storeName) onStoreChanged;

  /// 시작일 변경 콜백
  final void Function(DateTime date) onFromDateChanged;

  /// 종료일 변경 콜백
  final void Function(DateTime date) onToDateChanged;

  /// 검색 버튼 콜백
  final VoidCallback onSearch;

  const ShelfLifeFilterBar({
    super.key,
    required this.stores,
    this.selectedStoreId,
    required this.fromDate,
    required this.toDate,
    required this.onStoreChanged,
    required this.onFromDateChanged,
    required this.onToDateChanged,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.surface,
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 거래처 드롭다운
          _buildStoreDropdown(),
          const SizedBox(height: AppSpacing.sm),

          // 유통기한 날짜 범위
          _buildDateRange(context),
          const SizedBox(height: AppSpacing.md),

          // 검색 버튼
          SizedBox(
            height: AppSpacing.buttonHeight,
            child: ElevatedButton(
              onPressed: onSearch,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: AppColors.onPrimary,
                shape: RoundedRectangleBorder(
                  borderRadius: AppSpacing.buttonBorderRadius,
                ),
              ),
              child: const Text('검색'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStoreDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: AppSpacing.inputBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<int?>(
          value: selectedStoreId,
          isExpanded: true,
          hint: Text(
            '거래처 전체',
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary),
          ),
          items: [
            DropdownMenuItem<int?>(
              value: null,
              child: Text(
                '거래처 전체',
                style: AppTypography.bodyMedium,
              ),
            ),
            ...stores.entries.map((entry) {
              return DropdownMenuItem<int?>(
                value: entry.key,
                child: Text(
                  entry.value,
                  style: AppTypography.bodyMedium,
                  overflow: TextOverflow.ellipsis,
                ),
              );
            }),
          ],
          onChanged: (value) {
            onStoreChanged(value, value != null ? stores[value] : null);
          },
        ),
      ),
    );
  }

  Widget _buildDateRange(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return Row(
      children: [
        Text(
          '유통기한',
          style: AppTypography.labelLarge.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: GestureDetector(
            onTap: () => _selectDate(context, fromDate, onFromDateChanged),
            child: Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.sm,
                vertical: AppSpacing.sm,
              ),
              decoration: BoxDecoration(
                color: AppColors.background,
                borderRadius: AppSpacing.inputBorderRadius,
                border: Border.all(color: AppColors.border),
              ),
              child: Text(
                dateFormat.format(fromDate),
                style: AppTypography.bodySmall,
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ),
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: AppSpacing.xs),
          child: Text('~'),
        ),
        Expanded(
          child: GestureDetector(
            onTap: () => _selectDate(context, toDate, onToDateChanged),
            child: Container(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.sm,
                vertical: AppSpacing.sm,
              ),
              decoration: BoxDecoration(
                color: AppColors.background,
                borderRadius: AppSpacing.inputBorderRadius,
                border: Border.all(color: AppColors.border),
              ),
              child: Text(
                dateFormat.format(toDate),
                style: AppTypography.bodySmall,
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _selectDate(
    BuildContext context,
    DateTime initialDate,
    void Function(DateTime) onChanged,
  ) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );
    if (picked != null) {
      onChanged(picked);
    }
  }
}
