import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../account/account_selector_field.dart';
import '../common/range_calendar_picker.dart';

/// 소비기한 검색 필터 바
///
/// 레거시(otg_PowerSales `product/expiration/list.jsp`)의 `search_top` 을 기반으로 하되,
/// 모바일 UX 상 검색 버튼을 제거하고 즉시 조회 방식으로 변경했다:
/// - 거래처 전체 선택 (공용 [AccountSelectorField] 바텀시트, flat full-width 행)
/// - "소비기한 [기간]" 한 줄 (탭 → 달력 → 범위 선택 즉시 조회)
class ProductExpirationFilterBar extends StatelessWidget {
  /// 선택된 거래처명 (미선택 시 전체)
  final String? selectedAccountName;

  /// 검색 시작일
  final DateTime fromDate;

  /// 검색 종료일
  final DateTime toDate;

  /// 거래처 선택 콜백
  final void Function(String? accountCode, String? accountName) onAccountChanged;

  /// 소비기한 범위 선택 콜백 (시작일/종료일 동시 확정)
  final void Function(DateTime from, DateTime to) onDateRangeChanged;

  const ProductExpirationFilterBar({
    super.key,
    this.selectedAccountName,
    required this.fromDate,
    required this.toDate,
    required this.onAccountChanged,
    required this.onDateRangeChanged,
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
          // 소비기한 기간 + 검색 (한 줄, 상단 구분선)
          _buildDateSearchRow(context),
        ],
      ),
    );
  }

  Widget _buildAccountRow() {
    // 레거시 search_top 행 높이(50)에 맞춰 세로 패딩으로 중앙 정렬한다.
    return AccountSelectorField(
      selectedName: selectedAccountName,
      placeholder: '거래처 전체',
      scope: MyAccountScope.field,
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 15),
      onSelected: (account) =>
          onAccountChanged(account.accountCode, account.accountName),
      onCleared: () => onAccountChanged(null, null),
    );
  }

  Widget _buildDateSearchRow(BuildContext context) {
    final dateFormat = DateFormat('yyyy-MM-dd');

    return Container(
      height: _rowHeight,
      padding: const EdgeInsets.symmetric(horizontal: 20),
      decoration: const BoxDecoration(
        border: Border(top: BorderSide(color: _rowBorder)),
      ),
      child: Row(
        children: [
          Text(
            '소비기한',
            style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
          ),
          const SizedBox(width: 12),
          // 기간 (단일 행, 탭 시 달력에서 범위 선택 즉시 조회 — 검색 버튼 없음)
          Expanded(
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onTap: () => _selectDateRange(context),
              child: Text(
                '${dateFormat.format(fromDate)} ~ ${dateFormat.format(toDate)}',
                style: AppTypography.bodyMedium
                    .copyWith(color: AppColors.textPrimary),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 소비기한 시작일~종료일을 클레임 현황과 동일한 달력 UI 로 선택한다.
  /// 조회 가능 기간은 소비기한 조건(2020 ~ 2030)에 맞춘다. 범위 일수 제한은 없다.
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
      onDateRangeChanged(picked.start, picked.end);
    }
  }
}
