import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../providers/claim_list_provider.dart';
import '../widgets/account/account_selector_sheet.dart';
import '../widgets/claim/claim_list_item_card.dart';
import '../widgets/common/loading_indicator.dart';

/// 클레임 현황 목록 페이지
class ClaimListPage extends ConsumerStatefulWidget {
  const ClaimListPage({super.key});

  @override
  ConsumerState<ClaimListPage> createState() => _ClaimListPageState();
}

class _ClaimListPageState extends ConsumerState<ClaimListPage>
    with ThrottledTapMixin {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(claimListProvider.notifier).loadClaims();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(claimListProvider);

    ref.listen<String?>(
      claimListProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next)),
          );
          ref.read(claimListProvider.notifier).clearError();
        }
      },
    );

    return Scaffold(
      appBar: AppBar(title: const Text('클레임 현황')),
      body: Column(
        children: [
          _buildAccountFilter(state),
          _buildDateFilter(state),
          Expanded(child: _buildBody(state)),
        ],
      ),
    );
  }

  Widget _buildAccountFilter(dynamic state) {
    final hasAccount = state.selectedAccountId != null;
    return Padding(
      padding: const EdgeInsets.fromLTRB(
          AppSpacing.lg, AppSpacing.lg, AppSpacing.lg, 0),
      child: InkWell(
        onTap: () => throttledTap(_selectAccount),
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.sm,
            vertical: AppSpacing.sm,
          ),
          decoration: BoxDecoration(
            border: Border.all(color: AppColors.border),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Row(
            children: [
              const Icon(Icons.store_outlined,
                  size: 18, color: AppColors.textSecondary),
              const SizedBox(width: AppSpacing.sm),
              Expanded(
                child: Text(
                  hasAccount ? state.selectedAccountName! : '거래처 전체',
                  style: AppTypography.bodySmall.copyWith(
                    color: hasAccount
                        ? AppColors.textPrimary
                        : AppColors.textSecondary,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              if (hasAccount)
                GestureDetector(
                  onTap: () =>
                      ref.read(claimListProvider.notifier).clearAccount(),
                  child: const Icon(Icons.close,
                      size: 18, color: AppColors.textSecondary),
                )
              else
                const Icon(Icons.arrow_drop_down,
                    color: AppColors.textSecondary),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _selectAccount() async {
    final account = await AccountSelectorSheet.show(context);
    if (account != null) {
      ref
          .read(claimListProvider.notifier)
          .selectAccount(account.accountId, account.accountName);
    }
  }

  Widget _buildDateFilter(dynamic state) {
    final dateFormat = DateFormat('yyyy-MM-dd');
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Row(
        children: [
          Expanded(
            child: InkWell(
              onTap: () => _selectDate(isStart: true),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                  vertical: AppSpacing.sm,
                ),
                decoration: BoxDecoration(
                  border: Border.all(color: AppColors.border),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  dateFormat.format(state.startDate),
                  style: AppTypography.bodySmall,
                  textAlign: TextAlign.center,
                ),
              ),
            ),
          ),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: AppSpacing.sm),
            child: Text('~'),
          ),
          Expanded(
            child: InkWell(
              onTap: () => _selectDate(isStart: false),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.sm,
                  vertical: AppSpacing.sm,
                ),
                decoration: BoxDecoration(
                  border: Border.all(color: AppColors.border),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  dateFormat.format(state.endDate),
                  style: AppTypography.bodySmall,
                  textAlign: TextAlign.center,
                ),
              ),
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          SizedBox(
            height: 40,
            child: ElevatedButton(
              onPressed: () => throttledTapAsync(
                () => ref.read(claimListProvider.notifier).loadClaims(),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
                minimumSize: const Size(48, 40),
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: const Icon(Icons.search, size: 20),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(dynamic state) {
    if (state.isLoading) {
      return const LoadingIndicator(message: '클레임 목록을 불러오는 중...');
    }

    if (state.isEmpty) {
      return Center(
        child: Text(
          '결과가 없습니다',
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      );
    }

    if (!state.hasSearched) {
      return const SizedBox.shrink();
    }

    return ListView.builder(
      itemCount: state.items.length,
      itemBuilder: (context, index) {
        final item = state.items[index];
        return ClaimListItemCard(
          item: item,
          onTap: () => throttledTap(
            () => AppRouter.navigateTo(
              context,
              AppRouter.claimDetail,
              arguments: item.claimId,
            ),
          ),
        );
      },
    );
  }

  Future<void> _selectDate({required bool isStart}) async {
    final state = ref.read(claimListProvider);
    final initialDate = isStart ? state.startDate : state.endDate;
    final picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(2020),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked != null) {
      if (isStart) {
        ref.read(claimListProvider.notifier).updateStartDate(picked);
      } else {
        ref.read(claimListProvider.notifier).updateEndDate(picked);
      }
    }
  }
}
