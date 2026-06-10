import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../common/range_calendar_picker.dart';

/// 유통기한 검색 필터 바
///
/// 레거시(otg_PowerSales `product/expiration/list.jsp`)의 `search_top` 정합:
/// - 거래처 전체 선택 (flat full-width 행 + 우측 chevron)
/// - "유통기한 [기간]" 한 줄 + 우측 노란 pill `검색` 버튼
class ProductExpirationFilterBar extends StatelessWidget {
  /// 거래처 목록 {accountCode: accountName}
  final Map<String, String> accounts;

  /// 선택된 거래처 코드
  final String? selectedAccountCode;

  /// 거래처 목록 로딩 중 여부
  final bool isAccountsLoading;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 거래처 선택 콜백
  final void Function(String? accountCode, String? accountName) onAccountChanged;

  /// 시작일 변경 콜백
  final void Function(DateTime date) onFromDateChanged;

  /// 종료일 변경 콜백
  final void Function(DateTime date) onToDateChanged;

  /// 검색 버튼 콜백
  final VoidCallback onSearch;

  const ProductExpirationFilterBar({
    super.key,
    required this.accounts,
    this.selectedAccountCode,
    this.isAccountsLoading = false,
    required this.fromDate,
    required this.toDate,
    required this.onAccountChanged,
    required this.onFromDateChanged,
    required this.onToDateChanged,
    required this.onSearch,
  });

  // 레거시 search_top 의 행 구분선/높이
  static const Color _rowBorder = AppColors.surfaceVariant; // #F0F0F0
  static const double _rowHeight = 50;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 거래처 전체 (flat 행 + 우측 chevron)
          _buildAccountRow(),
          // 유통기한 기간 + 검색 (한 줄, 상단 구분선)
          _buildDateSearchRow(context),
        ],
      ),
    );
  }

  Widget _buildAccountRow() {
    if (isAccountsLoading) {
      return Container(
        height: _rowHeight,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        alignment: Alignment.centerLeft,
        child: Row(
          children: [
            const SizedBox(
              width: 16,
              height: 16,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
            const SizedBox(width: 8),
            Text(
              '거래처 로딩 중...',
              style: AppTypography.bodyLarge
                  .copyWith(color: AppColors.textSecondary),
            ),
          ],
        ),
      );
    }

    return Container(
      height: _rowHeight,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      alignment: Alignment.center,
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String?>(
          value: selectedAccountCode,
          isExpanded: true,
          icon: const Icon(Icons.keyboard_arrow_down,
              color: AppColors.textPrimary),
          style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
          hint: Text(
            '거래처 전체',
            style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
          ),
          items: [
            DropdownMenuItem<String?>(
              value: null,
              child: Text('거래처 전체', style: AppTypography.bodyLarge),
            ),
            ...accounts.entries.map((entry) {
              return DropdownMenuItem<String?>(
                value: entry.key,
                child: Text(
                  entry.value,
                  style: AppTypography.bodyLarge,
                  overflow: TextOverflow.ellipsis,
                ),
              );
            }),
          ],
          onChanged: (value) {
            onAccountChanged(value, value != null ? accounts[value] : null);
          },
        ),
      ),
    );
  }

  Widget _buildDateSearchRow(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return Container(
      height: _rowHeight,
      padding: const EdgeInsets.only(left: 20, right: 12),
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: _rowBorder)),
      ),
      child: Row(
        children: [
          Text(
            '유통기한',
            style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
          ),
          const SizedBox(width: 12),
          // 기간 (단일 행, 탭 시 기간 선택)
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => _selectDateRange(context),
              child: Text(
                '${dateFormat.format(fromDate)} - ${dateFormat.format(toDate)}',
                style: AppTypography.bodyMedium
                    .copyWith(color: AppColors.textPrimary),
              ),
            ),
          ),
          // 검색 pill (레거시 #FFE40C, 57x32, radius 50)
          SizedBox(
            width: 57,
            height: 32,
            child: ElevatedButton(
              onPressed: onSearch,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.legacyYellow,
                foregroundColor: AppColors.black,
                elevation: 0,
                padding: EdgeInsets.zero,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(50),
                ),
                textStyle:
                    const TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
              ),
              child: const Text('검색'),
            ),
          ),
        ],
      ),
    );
  }

  /// 유통기한 시작일~종료일을 클레임 현황과 동일한 달력 UI 로 선택한다.
  /// 조회 가능 기간은 유통기한 조건(2020 ~ 2030)에 맞춘다. 범위 일수 제한은 없다.
  Future<void> _selectDateRange(BuildContext context) async {
    final picked = await showRangeCalendar(
      context,
      initialStart: fromDate,
      initialEnd: toDate,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      maxRangeDays: null,
    );
    if (picked != null) {
      onFromDateChanged(picked.start);
      onToDateChanged(picked.end);
    }
  }
}
