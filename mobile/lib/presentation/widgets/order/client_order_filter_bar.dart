import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../account/account_selector_field.dart';
import 'order_filter_styles.dart';

/// 거래처별 주문 필터 바
///
/// 거래처 선택(바텀시트), 납기일 선택, 검색 버튼으로 구성됩니다.
/// Heroku 레거시(order/list.jsp - 거래처별 주문 탭)의 플랫 검색 영역 디자인에 정합합니다.
/// 내 주문 탭과 달리 거래처는 필수 선택(전체 옵션 없음)이며 상태/정렬이 없습니다.
class ClientOrderFilterBar extends StatelessWidget {
  final String? selectedAccountName;
  final int? selectedAccountId;
  final String selectedDeliveryDate; // YYYY-MM-DD
  final ValueChanged<MapEntry<int, String>?> onAccountChanged; // null = clear
  final ValueChanged<String> onDeliveryDateChanged;
  final VoidCallback onSearch;
  final bool canSearch; // false if no account selected

  const ClientOrderFilterBar({
    super.key,
    this.selectedAccountName,
    this.selectedAccountId,
    required this.selectedDeliveryDate,
    required this.onAccountChanged,
    required this.onDeliveryDateChanged,
    required this.onSearch,
    this.canSearch = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      child: Column(
        children: [
          // 1행: 거래처 선택 (필수 — 전체 옵션 없음)
          SizedBox(
            height: OrderFilterStyles.rowHeight,
            child: _buildAccountDropdown(),
          ),
          const OrderFilterRowDivider(),
          // 2행: 납기일 + 검색 버튼
          SizedBox(
            height: OrderFilterStyles.rowHeight,
            child: Row(
              children: [
                Expanded(child: _buildDateSelector(context)),
                Padding(
                  padding: const EdgeInsets.only(right: AppSpacing.lg),
                  child: OrderSearchButton(
                    onPressed: canSearch ? onSearch : null,
                  ),
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

  Widget _buildAccountDropdown() {
    return AccountSelectorField(
      selectedName: selectedAccountName,
      placeholder: '거래처 선택',
      scope: MyAccountScope.order,
      leadingIcon: Icons.store_outlined,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      onSelected: (account) =>
          onAccountChanged(MapEntry(account.accountId, account.accountName)),
    );
  }

  Widget _buildDateSelector(BuildContext context) {
    return InkWell(
      onTap: () async {
        final pickedDate = await showDatePicker(
          context: context,
          initialDate: DateTime.parse(selectedDeliveryDate),
          firstDate: DateTime(2020),
          lastDate: DateTime(2030),
          locale: const Locale('ko', 'KR'),
        );
        if (pickedDate != null) {
          onDeliveryDateChanged(DateFormat('yyyy-MM-dd').format(pickedDate));
        }
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
        child: Row(
          children: [
            Text('납기일', style: OrderFilterStyles.labelText),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Text(
                selectedDeliveryDate,
                style: OrderFilterStyles.valueText,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
