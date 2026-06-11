import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/inspection_list_item.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../account/account_selector_field.dart';
import '../common/date_range_filter_field.dart';

/// 현장점검 검색 필터 바
///
/// 거래처 선택(공용 [AccountSelectorField] 바텀시트) + 분류 드롭다운
/// + 점검일 날짜 범위 + 검색 버튼
class InspectionFilterBar extends StatelessWidget {
  /// 선택된 거래처명 (null이면 전체)
  final String? selectedAccountName;

  /// 선택된 분류
  final InspectionCategory? selectedCategory;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 거래처 선택 콜백
  final void Function(int? accountId, String? accountName) onAccountChanged;

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
    this.selectedAccountName,
    this.selectedCategory,
    required this.fromDate,
    required this.toDate,
    required this.onAccountChanged,
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
          // 거래처 선택 + 분류 드롭다운 (한 줄에 배치)
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(child: _buildAccountField()),
                const SizedBox(width: AppSpacing.sm),
                Expanded(child: _buildCategoryDropdown()),
              ],
            ),
          ),
          const SizedBox(height: AppSpacing.sm),

          // 점검일 날짜 범위 + 검색 버튼 (한 줄에 배치, 레거시 정렬)
          _buildDateRange(context),
        ],
      ),
    );
  }

  /// 거래처 선택 — 공용 [AccountSelectorField] 바텀시트 재사용.
  /// 목록 필터형이므로 "거래처 전체"(필터 해제) 항목/버튼을 노출한다(scope=field).
  Widget _buildAccountField() {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: AppSpacing.inputBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: AccountSelectorField(
        selectedName: selectedAccountName,
        placeholder: '거래처 전체',
        scope: MyAccountScope.field,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.sm,
        ),
        onSelected: (account) =>
            onAccountChanged(account.accountId, account.accountName),
        onCleared: () => onAccountChanged(null, null),
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
    return Row(
      children: [
        Expanded(
          // 주문 현황 납기일과 동일한 인라인 기간 UI.
          // 레거시(fieldChk/list.jsp): minDate/maxDate 없음, maxSpan 7일.
          child: DateRangeFilterField(
            label: '점검일',
            startDate: fromDate,
            endDate: toDate,
            maxRangeDays: 7,
            onChanged: (start, end) {
              onFromDateChanged(start);
              onToDateChanged(end);
            },
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        // 검색 버튼 (노란 pill - 레거시 .type_btn button 정렬). 기간 필드와 높이 통일.
        SizedBox(
          height: DateRangeFilterField.fieldHeight,
          child: ElevatedButton(
            onPressed: onSearch,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.primary,
              foregroundColor: AppColors.onPrimary,
              elevation: 0,
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
              minimumSize:
                  const Size(57, DateRangeFilterField.fieldHeight),
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(AppSpacing.homePillRadius),
              ),
            ),
            child: const Text(
              '검색',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ),
      ],
    );
  }

}
