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
class AccountSelectorSheet extends ConsumerStatefulWidget {
  const AccountSelectorSheet({super.key, this.scope = MyAccountScope.field});

  /// 거래처 조회 범위 — 매출 계열(POS/전산/월매출)은 [MyAccountScope.sales] 전달.
  final MyAccountScope scope;

  /// 바텀시트로 표시하고 선택된 거래처를 반환한다 (취소 시 null).
  static Future<MyAccount?> show(
    BuildContext context, {
    MyAccountScope scope = MyAccountScope.field,
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
      builder: (_) => AccountSelectorSheet(scope: scope),
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
    if (_accounts.isEmpty) {
      return const Center(child: Text('거래처가 없습니다'));
    }
    return ListView.separated(
      itemCount: _accounts.length,
      separatorBuilder: (_, _) =>
          const Divider(height: 1, color: AppColors.divider),
      itemBuilder: (context, index) {
        final account = _accounts[index];
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
