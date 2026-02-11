import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/inspection_list_item.dart';

/// 현장점검 검색 필터 바
///
/// 거래처 드롭다운 + 분류 드롭다운 + 점검일 날짜 범위 + 검색 버튼
class InspectionFilterBar extends StatelessWidget {
  /// 거래처 목록 {storeId: storeName}
  final Map<int, String> stores;

  /// 선택된 거래처 ID
  final int? selectedStoreId;

  /// 선택된 분류
  final InspectionCategory? selectedCategory;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 거래처 선택 콜백
  final void Function(int? storeId, String? storeName) onStoreChanged;

  /// 분류 선택 콜백
  final void Function(InspectionCategory? category) onCategoryChanged;

  /// 시작일 변경 콜백
  final void Function(DateTime date) onFromDateChanged;

  /// 종료일 변경 콜백
  final void Function(DateTime date) onToDateChanged;

  /// 검색 버튼 콜백
  final VoidCallback onSearch;

  const InspectionFilterBar({
    super.key,
    required this.stores,
    this.selectedStoreId,
    this.selectedCategory,
    required this.fromDate,
    required this.toDate,
    required this.onStoreChanged,
    required this.onCategoryChanged,
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
          // 거래처 + 분류 드롭다운 (한 줄에 배치)
          Row(
            children: [
              Expanded(child: _buildStoreDropdown()),
              const SizedBox(width: AppSpacing.sm),
              Expanded(child: _buildCategoryDropdown()),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),

          // 점검일 날짜 범위
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

  Widget _buildCategoryDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: AppSpacing.inputBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<InspectionCategory?>(
          value: selectedCategory,
          isExpanded: true,
          hint: Text(
            '분류 전체',
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary),
          ),
          items: [
            DropdownMenuItem<InspectionCategory?>(
              value: null,
              child: Text(
                '분류 전체',
                style: AppTypography.bodyMedium,
              ),
            ),
            DropdownMenuItem<InspectionCategory?>(
              value: InspectionCategory.OWN,
              child: Text(
                '자사',
                style: AppTypography.bodyMedium,
              ),
            ),
            DropdownMenuItem<InspectionCategory?>(
              value: InspectionCategory.COMPETITOR,
              child: Text(
                '경쟁사',
                style: AppTypography.bodyMedium,
              ),
            ),
          ],
          onChanged: (value) {
            onCategoryChanged(value);
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
          '점검일',
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
