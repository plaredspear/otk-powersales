import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../domain/entities/my_account.dart';
import '../../../domain/repositories/my_account_repository.dart';
import 'account_selector_sheet.dart';

/// 거래처 선택 필드 (월매출 화면의 거래처 선택 헤더를 공용화).
///
/// 탭하면 [AccountSelectorSheet] 바텀시트를 띄워 거래처를 검색·선택하고,
/// 선택된 거래처를 [onSelected] 로 돌려준다. 매출/현장/주문 등 화면별 [scope] 를 받는다.
///
/// - [onCleared] 가 주어지면 선택 상태에서 X(전체 보기) 버튼을 노출한다(필터형).
///   null 이면 X 없이 항상 chevron 만 표시한다(필수 선택형).
class AccountSelectorField extends StatelessWidget {
  const AccountSelectorField({
    super.key,
    required this.selectedName,
    required this.scope,
    required this.onSelected,
    this.onCleared,
    this.placeholder = '거래처 선택',
    this.leadingIcon = Icons.store_outlined,
    this.padding = const EdgeInsets.all(16),
  });

  /// 선택된 거래처명 (null 이면 [placeholder] 표시).
  final String? selectedName;

  /// 거래처 조회 범위.
  final MyAccountScope scope;

  /// 거래처 선택 콜백.
  final ValueChanged<MyAccount> onSelected;

  /// 선택 해제(전체) 콜백 — 지정 시 X 버튼 노출. null 이면 필수 선택형.
  final VoidCallback? onCleared;

  /// 미선택 시 표시 문구.
  final String placeholder;

  /// 좌측 아이콘.
  final IconData leadingIcon;

  /// 내부 패딩.
  final EdgeInsets padding;

  Future<void> _select(BuildContext context) async {
    final account = await AccountSelectorSheet.show(context, scope: scope);
    if (account != null) onSelected(account);
  }

  @override
  Widget build(BuildContext context) {
    final hasSelection = selectedName != null;
    return InkWell(
      onTap: () => _select(context),
      child: Padding(
        padding: padding,
        child: Row(
          children: [
            Icon(leadingIcon, size: 20),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                selectedName ?? placeholder,
                style: TextStyle(
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                  color: hasSelection
                      ? AppColors.textPrimary
                      : AppColors.textSecondary,
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
            if (onCleared != null && hasSelection)
              IconButton(
                onPressed: onCleared,
                icon: const Icon(Icons.close, size: 18),
                visualDensity: VisualDensity.compact,
                tooltip: '전체 보기',
              )
            else
              const Icon(Icons.chevron_right, size: 20),
          ],
        ),
      ),
    );
  }
}
