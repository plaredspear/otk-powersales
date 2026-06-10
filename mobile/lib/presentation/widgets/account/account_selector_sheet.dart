import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/my_account.dart';
import '../../../domain/repositories/my_account_repository.dart';
import '../../providers/my_accounts_provider.dart';

/// 거래처 선택 바텀시트
///
/// "내 거래처" 목록(`GET /accounts/my`)을 검색·선택하여 선택된 [MyAccount]를 반환한다.
/// 클레임 등록·월매출 등 폼/필터의 거래처 선택에서 공용으로 사용한다.
///
/// [includeAllOption] 이 true 면 목록 최상단에 "거래처 전체"(필터 해제) 항목을 노출한다.
/// 레거시에서 거래처 필터에 `<option value="">거래처 전체</option>` 가 있는 화면(목록 필터)에만
/// 사용하고, 거래처 필수 선택 화면(등록 폼·주문 작성·거래처별 주문)에는 사용하지 않는다.
/// "거래처 전체" 선택 시 [show] 는 [allOption] sentinel 을 반환한다([isAllOption] 으로 판별).
class AccountSelectorSheet extends ConsumerStatefulWidget {
  const AccountSelectorSheet({
    super.key,
    this.scope = MyAccountScope.field,
    this.includeAllOption = false,
  });

  /// 거래처 조회 범위 — 매출 계열(POS/전산/월매출)은 [MyAccountScope.sales] 전달.
  final MyAccountScope scope;

  /// 목록 최상단 "거래처 전체"(필터 해제) 항목 노출 여부.
  final bool includeAllOption;

  /// "거래처 전체" 선택 시 [show] 가 반환하는 sentinel (accountId == -1).
  static const MyAccount allOption = MyAccount(
    accountId: -1,
    accountName: '거래처 전체',
    accountCode: '',
  );

  /// [show] 결과가 "거래처 전체"(필터 해제) 인지 여부.
  static bool isAllOption(MyAccount? account) =>
      account != null && account.accountId == allOption.accountId;

  /// 바텀시트로 표시하고 선택된 거래처를 반환한다 (취소 시 null, 전체 선택 시 [allOption]).
  static Future<MyAccount?> show(
    BuildContext context, {
    MyAccountScope scope = MyAccountScope.field,
    bool includeAllOption = false,
  }) {
    return showModalBottomSheet<MyAccount>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (_) =>
          AccountSelectorSheet(scope: scope, includeAllOption: includeAllOption),
    );
  }

  @override
  ConsumerState<AccountSelectorSheet> createState() =>
      _AccountSelectorSheetState();
}

class _AccountSelectorSheetState extends ConsumerState<AccountSelectorSheet> {
  final TextEditingController _searchController = TextEditingController();
  List<MyAccount> _accounts = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _load({String? keyword}) async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final result = await ref
          .read(getMyAccountsUseCaseProvider)
          .call(keyword: keyword, scope: widget.scope);
      if (!mounted) return;
      setState(() {
        _accounts = result.accounts;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = '거래처를 불러오지 못했습니다';
        _loading = false;
      });
    }
  }

  void _onSearch(String value) {
    final keyword = value.trim();
    _load(keyword: keyword.isEmpty ? null : keyword);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.7,
        child: Column(
          children: [
            const SizedBox(height: AppSpacing.sm),
            // 핸들 바
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.divider,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            // 제목
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Text('거래처 선택', style: AppTypography.headlineSmall),
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            // 검색
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: TextField(
                controller: _searchController,
                textInputAction: TextInputAction.search,
                onSubmitted: _onSearch,
                decoration: InputDecoration(
                  hintText: '거래처명 / 코드 검색',
                  prefixIcon: const Icon(Icons.search),
                  isDense: true,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  ),
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            Expanded(child: _buildList()),
          ],
        ),
      ),
    );
  }

  Widget _buildList() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(child: Text(_error!));
    }
    final showAll = widget.includeAllOption;
    if (_accounts.isEmpty && !showAll) {
      return const Center(child: Text('거래처가 없습니다'));
    }
    final itemCount = _accounts.length + (showAll ? 1 : 0);
    return ListView.separated(
      itemCount: itemCount,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) {
        // 최상단 "거래처 전체"(필터 해제) 항목
        if (showAll && index == 0) {
          return ListTile(
            title: Text(
              '거래처 전체',
              style: AppTypography.bodyLarge
                  .copyWith(color: AppColors.textSecondary),
            ),
            onTap: () => Navigator.of(context)
                .pop(AccountSelectorSheet.allOption),
          );
        }
        final account = _accounts[index - (showAll ? 1 : 0)];
        return ListTile(
          title: Text(account.accountName, style: AppTypography.bodyLarge),
          subtitle: Text(
            account.accountCode,
            style: AppTypography.bodySmall
                .copyWith(color: AppColors.textSecondary),
          ),
          onTap: () => Navigator.of(context).pop(account),
        );
      },
    );
  }
}
