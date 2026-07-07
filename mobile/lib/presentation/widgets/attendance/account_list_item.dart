import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/account_schedule_item.dart';

/// 거래처 리스트 아이템 위젯
class AccountListItem extends StatelessWidget {
  final AccountScheduleItem account;
  final bool isSelected;
  final VoidCallback onTap;

  const AccountListItem({
    super.key,
    required this.account,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    if (account.isRegistered) {
      return _buildRegisteredItem();
    }

    return _buildSelectableItem();
  }

  Widget _buildRegisteredItem() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.background,
        border: Border.all(color: AppColors.border),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.check_circle, color: AppColors.success, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  account.accountName,
                  style: const TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: AppColors.textTertiary,
                  ),
                ),
                _buildCategoryLine(),
                const SizedBox(height: 4),
                Text(
                  account.address,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.textTertiary,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          _buildRegisteredWorkTypeBadge(),
        ],
      ),
    );
  }

  Widget _buildSelectableItem() {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.otokiBlue.withValues(alpha: 0.06)
              : AppColors.white,
          border: Border.all(
            color: isSelected ? AppColors.otokiBlue : AppColors.border,
            width: isSelected ? 1.5 : 1,
          ),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              isSelected
                  ? Icons.radio_button_checked
                  : Icons.radio_button_unchecked,
              color: isSelected ? AppColors.otokiBlue : AppColors.textTertiary,
              size: 24,
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    account.accountName,
                    style: TextStyle(
                      fontSize: 15,
                      fontWeight: isSelected
                          ? FontWeight.w700
                          : FontWeight.w500,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  _buildCategoryLine(),
                  const SizedBox(height: 4),
                  Text(
                    account.address,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSecondary,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (account.accountTypeCode != null) ...[
                    const SizedBox(height: 2),
                    Text(
                      account.accountTypeCode!,
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 근무유형을 레거시처럼 `진열/상시/순회` 형태로 이름 아래 별도 줄에 슬래시로
  /// 표시(근무구분/근무형태/순회유형). 이름 확인이 최우선이므로 배지 대신 텍스트
  /// 줄로 분리해 이름이 절대 잘리지 않게 한다. 값이 없는 토큰은 건너뛰고, 전부
  /// 비면 줄 자체를 렌더링하지 않는다.
  Widget _buildCategoryLine() {
    final parts = <String>[
      if (account.workCategory.isNotEmpty) account.workCategory,
      if (account.workCategory2 != null && account.workCategory2!.isNotEmpty)
        account.workCategory2!,
      if (account.workCategory3 != null && account.workCategory3!.isNotEmpty)
        account.workCategory3!,
    ];
    if (parts.isEmpty) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.only(top: 2),
      child: Text(
        parts.join('/'),
        style: const TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w500,
          color: AppColors.textSecondary,
        ),
      ),
    );
  }

  Widget _buildRegisteredWorkTypeBadge() {
    if (account.registeredWorkType == null) return const SizedBox.shrink();

    final label = account.registeredWorkType == 'ROOM_TEMP' ? '상온' : '냉장/냉동';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.success.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        label,
        style: const TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w500,
          color: AppColors.success,
        ),
      ),
    );
  }
}
