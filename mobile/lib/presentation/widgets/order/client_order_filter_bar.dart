import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../account/account_selector_field.dart';
import '../common/range_calendar_picker.dart';
import 'order_filter_styles.dart';

/// 거래처별 주문 필터 바
///
/// 거래처 선택(바텀시트), 납기일 선택으로 구성됩니다.
/// 거래처를 선택하거나 납기일을 바꾸면 별도 검색 버튼 없이 즉시 조회합니다
/// (거래처 미선택 시에는 조회가 no-op 이므로 안전).
/// Heroku 레거시(order/list.jsp - 거래처별 주문 탭)의 플랫 검색 영역 디자인을 기반으로 하되,
/// 모바일 UX 상 검색 버튼을 제거하고 즉시 조회 방식으로 변경했습니다.
/// 내 주문 탭과 달리 거래처는 필수 선택(전체 옵션 없음)이며 상태/정렬이 없습니다.
class ClientOrderFilterBar extends StatelessWidget {
  final String? selectedAccountName;
  final int? selectedAccountId;
  final String selectedDeliveryDate; // YYYY-MM-DD
  final ValueChanged<MapEntry<int, String>?> onAccountChanged; // null = clear
  final ValueChanged<String> onDeliveryDateChanged;

  const ClientOrderFilterBar({
    super.key,
    this.selectedAccountName,
    this.selectedAccountId,
    required this.selectedDeliveryDate,
    required this.onAccountChanged,
    required this.onDeliveryDateChanged,
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
          // 2행: 납기일 (선택 즉시 조회 — 검색 버튼 없음)
          SizedBox(
            height: OrderFilterStyles.rowHeight,
            child: _buildDateSelector(context),
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
        // 확인 버튼 없이 날짜를 탭하면 즉시 선택 확정되는 커스텀 달력.
        final pickedDate = await showSingleDateCalendar(
          context,
          initialDate: DateTime.parse(selectedDeliveryDate),
          firstDate: DateTime(2020),
          lastDate: DateTime(2030),
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
