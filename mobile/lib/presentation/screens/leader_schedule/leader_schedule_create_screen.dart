import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_account.dart';
import '../../../domain/entities/leader_team_member.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/primary_button.dart';

/// 조장 — 팀원 일정 대리 등록 화면 (Spec #554 P2-M §3.3).
class LeaderScheduleCreateScreen extends ConsumerStatefulWidget {
  final LeaderTeamMember targetMember;

  const LeaderScheduleCreateScreen({
    super.key,
    required this.targetMember,
  });

  @override
  ConsumerState<LeaderScheduleCreateScreen> createState() =>
      _LeaderScheduleCreateScreenState();
}

class _LeaderScheduleCreateScreenState
    extends ConsumerState<LeaderScheduleCreateScreen> with ThrottledTapMixin {
  static const List<String> _category3Options = ['고정', '격고', '순회'];
  static const List<String> _category1Options = ['진열'];

  bool _resultListenerAttached = false;

  @override
  Widget build(BuildContext context) {
    final providerArg = widget.targetMember.id;
    final state = ref.watch(leaderScheduleCreateProvider(providerArg));
    final notifier =
        ref.read(leaderScheduleCreateProvider(providerArg).notifier);

    if (!_resultListenerAttached) {
      _resultListenerAttached = true;
      ref.listen<LeaderScheduleCreateState>(
        leaderScheduleCreateProvider(providerArg),
        (prev, next) {
          if (next.errorMessage != null && prev?.errorMessage != next.errorMessage) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text(next.errorMessage!)),
            );
            notifier.clearError();
          }
          if (next.isSubmitted && prev?.isSubmitted != true) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('팀원 일정이 등록되었습니다.')),
            );
            Navigator.of(context).pop(true);
          }
        },
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('일정 등록 (대리)'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildLabel('대상 팀원'),
              _ReadOnlyField(
                value:
                    '${widget.targetMember.name} (${widget.targetMember.employeeCode})',
              ),
              const SizedBox(height: AppSpacing.lg),
              _buildLabel('일자'),
              _DateField(
                date: state.workingDate,
                onTap: () => throttledTap(() => _pickDate(notifier, state)),
              ),
              const SizedBox(height: AppSpacing.lg),
              _buildLabel('거래처 (필수)'),
              _AccountField(
                account: state.selectedAccount,
                onTap: () => throttledTap(() => _pickAccount(notifier, providerArg)),
              ),
              const SizedBox(height: AppSpacing.lg),
              _buildLabel('근무 분류 3 (필수)'),
              _CategoryDropdown(
                value: state.workingCategory3,
                options: _category3Options,
                hint: '고정 / 격고 / 순회 중 선택',
                onChanged: (value) => notifier.selectCategory3(value),
              ),
              const SizedBox(height: AppSpacing.lg),
              _buildLabel('근무 분류 1 (선택)'),
              _CategoryDropdown(
                value: state.workingCategory1,
                options: _category1Options,
                hint: '-',
                allowClear: true,
                onChanged: (value) => notifier.selectCategory1(value),
              ),
              const SizedBox(height: AppSpacing.xxl),
              PrimaryButton(
                text: '저장',
                isLoading: state.isLoading,
                onPressed: state.canSubmit ? notifier.submit : null,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLabel(String label) => Padding(
        padding: const EdgeInsets.only(bottom: AppSpacing.xs),
        child: Text(
          label,
          style: AppTypography.bodyMedium.copyWith(
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
      );

  Future<void> _pickDate(
    LeaderScheduleCreateNotifier notifier,
    LeaderScheduleCreateState state,
  ) async {
    final now = DateTime.now();
    final initial = state.workingDate ?? now;
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(now.year - 2, 1, 1),
      lastDate: DateTime(now.year + 2, 12, 31),
      helpText: '근무 일자 선택',
      cancelText: '취소',
      confirmText: '확인',
    );
    if (picked != null) {
      notifier.selectWorkingDate(picked);
    }
  }

  Future<void> _pickAccount(
    LeaderScheduleCreateNotifier notifier,
    int providerArg,
  ) async {
    await notifier.loadAccounts();
    if (!mounted) return;
    final selected = await showModalBottomSheet<LeaderAccount?>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusLg),
        ),
      ),
      builder: (_) => _AccountPickerSheet(providerArg: providerArg),
    );
    if (selected != null) {
      notifier.selectAccount(selected);
    }
  }
}

/// 읽기 전용 텍스트 필드 (대상 팀원 표시).
class _ReadOnlyField extends StatelessWidget {
  final String value;

  const _ReadOnlyField({required this.value});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.md,
      ),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Text(value, style: AppTypography.bodyMedium),
    );
  }
}

/// 일자 입력 필드.
class _DateField extends StatelessWidget {
  final DateTime? date;
  final VoidCallback onTap;

  const _DateField({required this.date, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final hasValue = date != null;
    return InkWell(
      onTap: onTap,
      borderRadius: AppSpacing.cardBorderRadius,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.md,
        ),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: AppSpacing.cardBorderRadius,
          border: Border.all(color: AppColors.border),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                hasValue ? _format(date!) : '일자를 선택하세요',
                style: AppTypography.bodyMedium.copyWith(
                  color:
                      hasValue ? AppColors.textPrimary : AppColors.textTertiary,
                ),
              ),
            ),
            const Icon(Icons.calendar_today, size: 18),
          ],
        ),
      ),
    );
  }

  String _format(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
}

/// 거래처 선택 필드.
class _AccountField extends StatelessWidget {
  final LeaderAccount? account;
  final VoidCallback onTap;

  const _AccountField({required this.account, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final hasValue = account != null;
    return InkWell(
      onTap: onTap,
      borderRadius: AppSpacing.cardBorderRadius,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.md,
        ),
        decoration: BoxDecoration(
          color: AppColors.white,
          borderRadius: AppSpacing.cardBorderRadius,
          border: Border.all(color: AppColors.border),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                hasValue ? (account!.name ?? '-') : '거래처를 선택하세요',
                style: AppTypography.bodyMedium.copyWith(
                  color:
                      hasValue ? AppColors.textPrimary : AppColors.textTertiary,
                ),
              ),
            ),
            const Icon(Icons.search, size: 18),
          ],
        ),
      ),
    );
  }
}

/// 카테고리 선택 드롭다운.
class _CategoryDropdown extends StatelessWidget {
  final String? value;
  final List<String> options;
  final String hint;
  final bool allowClear;
  final ValueChanged<String?> onChanged;

  const _CategoryDropdown({
    required this.value,
    required this.options,
    required this.hint,
    required this.onChanged,
    this.allowClear = false,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String?>(
          value: value,
          isExpanded: true,
          hint: Text(
            hint,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
          items: [
            if (allowClear)
              const DropdownMenuItem<String?>(
                value: null,
                child: Text('-'),
              ),
            ...options.map(
              (option) => DropdownMenuItem<String?>(
                value: option,
                child: Text(option),
              ),
            ),
          ],
          onChanged: onChanged,
        ),
      ),
    );
  }
}

/// 거래처 선택 BottomSheet.
class _AccountPickerSheet extends ConsumerStatefulWidget {
  final int providerArg;

  const _AccountPickerSheet({required this.providerArg});

  @override
  ConsumerState<_AccountPickerSheet> createState() =>
      _AccountPickerSheetState();
}

class _AccountPickerSheetState extends ConsumerState<_AccountPickerSheet> {
  final TextEditingController _keywordController = TextEditingController();

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(leaderScheduleCreateProvider(widget.providerArg));
    final notifier =
        ref.read(leaderScheduleCreateProvider(widget.providerArg).notifier);

    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.7,
      maxChildSize: 0.9,
      builder: (_, scrollController) {
        return Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          child: Column(
            children: [
              Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: AppSpacing.md),
                decoration: BoxDecoration(
                  color: AppColors.divider,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              TextField(
                controller: _keywordController,
                decoration: InputDecoration(
                  hintText: '거래처명/주소 검색',
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: IconButton(
                    icon: const Icon(Icons.refresh),
                    onPressed: () => notifier.loadAccounts(
                      keyword: _keywordController.text.trim(),
                    ),
                  ),
                  border: OutlineInputBorder(
                    borderRadius: AppSpacing.cardBorderRadius,
                  ),
                ),
                onSubmitted: (value) =>
                    notifier.loadAccounts(keyword: value.trim()),
              ),
              const SizedBox(height: AppSpacing.md),
              Expanded(child: _buildList(state, scrollController)),
            ],
          ),
        );
      },
    );
  }

  Widget _buildList(
    LeaderScheduleCreateState state,
    ScrollController scrollController,
  ) {
    if (state.isAccountsLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (state.accountsError != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Text(
            state.accountsError!,
            style: AppTypography.bodyMedium,
          ),
        ),
      );
    }
    if (state.accounts.isEmpty) {
      return const Center(child: Text('거래처가 없습니다.'));
    }
    return ListView.separated(
      controller: scrollController,
      itemCount: state.accounts.length,
      separatorBuilder: (context, index) => const Divider(height: 1),
      itemBuilder: (_, index) {
        final account = state.accounts[index];
        return ListTile(
          title: Text(account.name ?? '-'),
          subtitle:
              account.address1 != null ? Text(account.address1!) : null,
          onTap: () => Navigator.of(context).pop(account),
        );
      },
    );
  }
}
