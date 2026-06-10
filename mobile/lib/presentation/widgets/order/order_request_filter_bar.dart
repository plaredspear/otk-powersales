import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_request.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../account/account_selector_field.dart';
import '../common/range_calendar_picker.dart';
import 'order_filter_styles.dart';

/// 주문 필터 바 위젯
///
/// 거래처 선택(바텀시트), 상태 드롭다운, 납기일 범위, 검색 버튼을 포함합니다.
/// Heroku 레거시(order/list.jsp - 내 주문 탭)의 플랫 검색 영역 디자인에 정합합니다.
class OrderRequestFilterBar extends StatelessWidget {
  /// 선택된 거래처명 (미선택 시 전체)
  final String? selectedClientName;

  /// 선택된 거래처 ID
  final int? selectedClientId;

  /// 선택된 승인상태 코드
  final String? selectedStatus;

  /// 납기일 시작
  final String? deliveryDateFrom;

  /// 납기일 종료
  final String? deliveryDateTo;

  /// 거래처 선택 콜백
  final void Function(int? clientId, String? clientName) onClientChanged;

  /// 상태 선택 콜백
  final ValueChanged<String?> onStatusChanged;

  /// 납기일 범위 선택 콜백
  final void Function(String? from, String? to) onDateRangeChanged;

  /// 검색 버튼 콜백
  final VoidCallback onSearch;

  const OrderRequestFilterBar({
    super.key,
    this.selectedClientName,
    this.selectedClientId,
    this.selectedStatus,
    this.deliveryDateFrom,
    this.deliveryDateTo,
    required this.onClientChanged,
    required this.onStatusChanged,
    required this.onDateRangeChanged,
    required this.onSearch,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      child: Column(
        children: [
          // 1행: 거래처 + 상태 드롭다운 (세로 구분선으로 2분할)
          SizedBox(
            height: OrderFilterStyles.rowHeight,
            child: Row(
              children: [
                Expanded(child: _buildClientDropdown(context)),
                const OrderFilterVerticalDivider(),
                Expanded(child: _buildStatusDropdown(context)),
              ],
            ),
          ),
          const OrderFilterRowDivider(),
          // 2행: 납기일 범위 + 검색 버튼
          SizedBox(
            height: OrderFilterStyles.rowHeight,
            child: Row(
              children: [
                Expanded(child: _buildDateRangeSelector(context)),
                Padding(
                  padding: const EdgeInsets.only(right: AppSpacing.lg),
                  child: OrderSearchButton(onPressed: onSearch),
                ),
              ],
            ),
          ),
          // 필터 영역과 목록 사이의 굵은 회색 밴드 (레거시 .bline)
          const OrderFilterBand(),
        ],
      ),
    );
  }

  Widget _buildClientDropdown(BuildContext context) {
    return AccountSelectorField(
      selectedName: selectedClientName,
      placeholder: '거래처 전체',
      scope: MyAccountScope.order,
      leadingIcon: Icons.store_outlined,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      onSelected: (account) =>
          onClientChanged(account.accountId, account.accountName),
      onCleared: () => onClientChanged(null, null),
    );
  }

  Widget _buildStatusDropdown(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String?>(
          value: selectedStatus,
          isExpanded: true,
          icon: const Icon(
            Icons.arrow_drop_down,
            size: 22,
            color: AppColors.textSecondary,
          ),
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textPrimary,
          ),
          hint: Text('상태 전체', style: OrderFilterStyles.valueText),
          items: [
            DropdownMenuItem<String?>(
              value: null,
              child: Text('상태 전체', style: OrderFilterStyles.valueText),
            ),
            ...OrderRequestStatus.values.map((status) {
              return DropdownMenuItem<String?>(
                value: status.code,
                child: Text(
                  status.displayName,
                  style: OrderFilterStyles.valueText,
                ),
              );
            }),
          ],
          onChanged: onStatusChanged,
        ),
      ),
    );
  }

  Widget _buildDateRangeSelector(BuildContext context) {
    final from = deliveryDateFrom ?? '';
    final to = deliveryDateTo ?? '';
    final hasRange = from.isNotEmpty || to.isNotEmpty;

    return InkWell(
      onTap: () => _showDateRangePicker(context),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
        child: Row(
          children: [
            Text('납기일', style: OrderFilterStyles.labelText),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Text(
                hasRange ? '$from ~ $to' : '선택',
                style: OrderFilterStyles.valueText.copyWith(
                  color: hasRange
                      ? AppColors.textPrimary
                      : AppColors.textTertiary,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 납기일 시작일~종료일을 클레임 현황과 동일한 달력 UI 로 선택한다.
  /// 조회 가능 기간은 주문 조건(2020 ~ 2030)에 맞추고,
  /// 레거시 daterangepicker maxSpan 정합으로 최대 7일까지만 선택할 수 있다.
  Future<void> _showDateRangePicker(BuildContext context) async {
    // 현재 선택된 범위 기본값: 오늘 ~ 오늘+7일
    final now = DateTime.now();
    final initialFrom = deliveryDateFrom != null
        ? DateTime.parse(deliveryDateFrom!)
        : now;
    final initialTo = deliveryDateTo != null
        ? DateTime.parse(deliveryDateTo!)
        : now.add(const Duration(days: 7));

    final picked = await showRangeCalendar(
      context,
      initialStart: initialFrom,
      initialEnd: initialTo,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
      maxRangeDays: 7,
    );

    if (picked != null) {
      final start = picked.start;
      final end = picked.end;
      final fromStr =
          '${start.year}-${start.month.toString().padLeft(2, '0')}-${start.day.toString().padLeft(2, '0')}';
      final toStr =
          '${end.year}-${end.month.toString().padLeft(2, '0')}-${end.day.toString().padLeft(2, '0')}';
      onDateRangeChanged(fromStr, toStr);
    }
  }
}
