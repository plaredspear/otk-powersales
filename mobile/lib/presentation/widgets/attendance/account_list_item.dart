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
        children: [
          const Icon(Icons.check_circle, color: AppColors.success, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _buildNameWithBadges(
                  nameColor: AppColors.textTertiary,
                  nameWeight: FontWeight.w500,
                ),
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
                  _buildNameWithBadges(
                    nameColor: AppColors.textPrimary,
                    nameWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                  ),
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

  /// 거래처명 + 근무유형 배지. 이름 확인이 중요하므로 이름은 절대 잘리지 않게
  /// 카드 전폭까지 자동 줄바꿈(ellipsis 미적용)하고, 배지는 Wrap 으로 이름 뒤에
  /// 흐르다 공간이 부족하면 다음 줄로 밀려 내려간다.
  Widget _buildNameWithBadges({
    required Color nameColor,
    required FontWeight nameWeight,
  }) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return Wrap(
          spacing: 6,
          runSpacing: 4,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            ConstrainedBox(
              constraints: BoxConstraints(maxWidth: constraints.maxWidth),
              child: Text(
                account.accountName,
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: nameWeight,
                  color: nameColor,
                ),
              ),
            ),
            _buildWorkCategoryBadge(),
            ..._buildWorkCategory3Badge(),
          ],
        );
      },
    );
  }

  Widget _buildWorkCategoryBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: AppColors.otokiYellow.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        account.workCategory,
        style: const TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w500,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }

  /// 근무유형3(고정/격고/순회) 배지. 진열 거래처에만 값이 있으므로 없으면 미표시.
  List<Widget> _buildWorkCategory3Badge() {
    final category3 = account.workCategory3;
    if (category3 == null || category3.isEmpty) return const [];

    return [
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
        decoration: BoxDecoration(
          color: AppColors.otokiBlue.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(4),
        ),
        child: Text(
          category3,
          style: const TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w500,
            color: AppColors.otokiBlue,
          ),
        ),
      ),
    ];
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
