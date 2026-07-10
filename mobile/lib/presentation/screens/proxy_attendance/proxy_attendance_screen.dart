import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/branch_option.dart';
import '../../../domain/entities/leader_daily_status.dart';
import '../../providers/proxy_attendance_provider.dart';
import '../../widgets/common/error_view.dart';
import '../../widgets/common/loading_indicator.dart';
import '../../widgets/common/single_select_sheet.dart';
import '../../widgets/proxy_attendance/proxy_register_sheet.dart';

/// AccountViewAll 대리출근 화면 — 지점 선택형.
///
/// 조장 대리출근을 대체 — AccountViewAll(부서장) 사용자가 지점을 선택하면 그 지점 여사원의
/// 일별 근무 현황을 조회하고, 미등록 거래처를 골라 대리출근을 등록한다.
/// 지점 미선택 시(초기)에는 여사원 목록 없이 "지점을 선택하세요" 안내만 표시한다.
class ProxyAttendanceScreen extends ConsumerStatefulWidget {
  const ProxyAttendanceScreen({super.key});

  @override
  ConsumerState<ProxyAttendanceScreen> createState() =>
      _ProxyAttendanceScreenState();
}

class _ProxyAttendanceScreenState extends ConsumerState<ProxyAttendanceScreen>
    with ThrottledTapMixin {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(proxyBranchesProvider.notifier).load();
    });
  }

  /// 지점 선택 바텀시트 열기 → 선택 시 일별현황 조회.
  void _openBranchPicker() {
    throttledTapAsync(() async {
      final branchesState = ref.read(proxyBranchesProvider);
      if (branchesState.branches.isEmpty) return;
      final current = ref.read(proxyAttendanceProvider).selectedBranch;

      final result = await SingleSelectSheet.show<String>(
        context,
        title: '지점 선택',
        options: branchesState.branches
            .map((b) => SingleSelectOption(
                  value: b.branchCode,
                  label: b.branchName,
                ))
            .toList(),
        selectedValue: current?.branchCode ?? '',
        searchHint: '지점명 검색',
      );
      if (result == null || !mounted) return;
      final selected = branchesState.branches
          .firstWhere((b) => b.branchCode == result.value);
      ref.read(proxyAttendanceProvider.notifier).selectBranch(selected);
    });
  }

  void _shiftDate(int days) {
    throttledTap(() {
      final current = ref.read(proxyAttendanceProvider).selectedDate;
      ref
          .read(proxyAttendanceProvider.notifier)
          .changeDate(current.add(Duration(days: days)));
    });
  }

  void _pickDate() {
    throttledTapAsync(() async {
      final current = ref.read(proxyAttendanceProvider).selectedDate;
      final picked = await showDatePicker(
        context: context,
        initialDate: current,
        firstDate: DateTime(2020),
        lastDate: DateTime(2100),
      );
      if (picked == null || !mounted) return;
      ref.read(proxyAttendanceProvider.notifier).changeDate(
            DateTime(picked.year, picked.month, picked.day),
          );
    });
  }

  /// 대리출근 등록 시트 — 여사원의 미등록 거래처를 선택해 일괄 등록.
  void _openRegisterSheet(_GroupedWorker group) {
    throttledTapAsync(() async {
      final unregistered = group.rows.where((r) => !r.attended).toList();
      if (unregistered.isEmpty) return;
      final employeeId = group.rows.first.employeeId;
      if (employeeId == null) return;

      final selected = await showModalBottomSheet<List<LeaderDailyWorker>>(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius:
              BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
        ),
        builder: (_) => ProxyRegisterSheet(
          employeeName: group.employeeName,
          accounts: unregistered,
        ),
      );
      if (selected == null || selected.isEmpty || !mounted) return;

      final notifier = ref.read(proxyAttendanceProvider.notifier);
      // 한 건 실패해도 나머지 건은 계속 등록 (레거시 mngDaily addScheduleProc 정합).
      String? firstError;
      var successCount = 0;
      var failCount = 0;
      for (final r in selected) {
        final err = await notifier.registerProxyAttendance(
          targetEmployeeId: employeeId,
          scheduleId: r.displayWorkScheduleId == null ? r.scheduleId : null,
          displayWorkScheduleId: r.displayWorkScheduleId,
        );
        if (err != null) {
          firstError ??= err;
          failCount++;
          continue;
        }
        successCount++;
      }
      if (!mounted) return;
      final messenger = ScaffoldMessenger.of(context);
      final message = failCount == 0
          ? '$successCount건 대리출근 등록이 완료되었습니다.'
          : successCount == 0
              ? '대리출근 등록에 실패했습니다: $firstError'
              : '$successCount건 등록 완료, $failCount건 실패했습니다: $firstError';
      messenger.showSnackBar(SnackBar(content: Text(message)));
    });
  }

  void _showBlockedReason(ProxyEligibility eligibility) {
    final reason = eligibility.reason;
    if (reason == null) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(reason)));
  }

  @override
  Widget build(BuildContext context) {
    final branchesState = ref.watch(proxyBranchesProvider);
    final state = ref.watch(proxyAttendanceProvider);

    // 재조회 실패(데이터 보유 시)는 SnackBar 로 알리고 상태 유지.
    ref.listen<ProxyAttendanceState>(proxyAttendanceProvider, (prev, next) {
      if (next.errorMessage != null && next.data != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
        ref.read(proxyAttendanceProvider.notifier).clearError();
      }
    });

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        title: const Text('대리출근'),
        backgroundColor: AppColors.secondary,
        foregroundColor: AppColors.white,
      ),
      body: Column(
        children: [
          _BranchSelector(
            branch: state.selectedBranch,
            loading: branchesState.isLoading,
            onTap: _openBranchPicker,
          ),
          if (state.selectedBranch != null)
            _DateSelector(
              date: state.selectedDate,
              onPrev: () => _shiftDate(-1),
              onNext: () => _shiftDate(1),
              onTap: _pickDate,
            ),
          Expanded(child: _buildBody(branchesState, state)),
        ],
      ),
    );
  }

  Widget _buildBody(ProxyBranchesState branchesState, ProxyAttendanceState state) {
    // 지점 목록 로드 실패
    if (branchesState.errorMessage != null && branchesState.branches.isEmpty) {
      return ErrorView(
        message: branchesState.errorMessage!,
        onRetry: () => ref.read(proxyBranchesProvider.notifier).load(),
      );
    }
    // 지점 미선택 → 빈 화면(안내)
    if (state.selectedBranch == null) {
      return const _EmptyHint(
        icon: Icons.store_mall_directory_outlined,
        text: '대리출근할 지점을 선택하세요.',
      );
    }
    // 일별현황 로딩(최초)
    if (state.isLoading && state.data == null) {
      return const Center(child: LoadingIndicator());
    }
    // 일별현황 로드 실패(최초)
    if (state.errorMessage != null && state.data == null) {
      return ErrorView(
        message: state.errorMessage!,
        onRetry: () => ref.read(proxyAttendanceProvider.notifier).load(),
      );
    }

    final data = state.data;
    if (data == null) {
      return const _EmptyHint(
        icon: Icons.people_outline,
        text: '해당 지점의 근무 현황이 없습니다.',
      );
    }

    final now = DateTime.now();
    final eligibility = proxyEligibility(state.selectedDate, now);
    final displayGroups = _groupByEmployee(data.displayWorkers);
    final eventGroups = _groupByEmployee(data.eventWorkers);

    if (displayGroups.isEmpty && eventGroups.isEmpty) {
      return RefreshIndicator(
        onRefresh: () => ref.read(proxyAttendanceProvider.notifier).load(),
        child: ListView(
          children: const [
            SizedBox(height: 120),
            _EmptyHint(
              icon: Icons.event_available_outlined,
              text: '해당 날짜에 대리출근할 근무가 없습니다.',
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => ref.read(proxyAttendanceProvider.notifier).load(),
      child: ListView(
        padding: const EdgeInsets.only(bottom: AppSpacing.xl),
        children: [
          _EligibilityBanner(eligibility: eligibility, now: now),
          if (displayGroups.isNotEmpty)
            _SectionHeader(title: '진열 (${displayGroups.length}명)'),
          ...displayGroups.map((g) => _buildGroupBand(g, '진열', eligibility)),
          if (eventGroups.isNotEmpty)
            _SectionHeader(title: '행사 (${eventGroups.length}명)'),
          ...eventGroups.map((g) => _buildGroupBand(g, '행사', eligibility)),
        ],
      ),
    );
  }

  Widget _buildGroupBand(
    _GroupedWorker group,
    String category,
    ProxyEligibility eligibility,
  ) {
    final hasUnregistered = group.rows.any((r) => !r.attended);
    final canRegister = eligibility.canRegister;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _RegisterPersonBand(
          name: group.employeeName,
          category: category,
          enabled: canRegister,
          onRegister: !hasUnregistered
              ? null
              : (canRegister
                  ? () => _openRegisterSheet(group)
                  : () => _showBlockedReason(eligibility)),
        ),
        ...group.rows.map((r) => _RegisterAccountRow(worker: r)),
      ],
    );
  }
}

/// 백엔드의 (여사원×거래처) 행을 여사원 단위로 묶는다. 등장 순서(백엔드 정렬) 유지.
List<_GroupedWorker> _groupByEmployee(List<LeaderDailyWorker> workers) {
  final order = <String>[];
  final map = <String, List<LeaderDailyWorker>>{};
  for (final w in workers) {
    final key = w.employeeId?.toString() ?? 'code:${w.employeeCode}';
    if (!map.containsKey(key)) {
      map[key] = <LeaderDailyWorker>[];
      order.add(key);
    }
    map[key]!.add(w);
  }
  return order.map((k) {
    final rows = map[k]!;
    return _GroupedWorker(
      employeeName: rows.first.employeeName,
      employeeCode: rows.first.employeeCode,
      rows: rows,
    );
  }).toList();
}

class _GroupedWorker {
  final String employeeName;
  final String employeeCode;
  final List<LeaderDailyWorker> rows;

  const _GroupedWorker({
    required this.employeeName,
    required this.employeeCode,
    required this.rows,
  });
}

class _BranchSelector extends StatelessWidget {
  final BranchOption? branch;
  final bool loading;
  final VoidCallback onTap;

  const _BranchSelector({
    required this.branch,
    required this.loading,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: loading ? null : onTap,
      child: Container(
        width: double.infinity,
        color: AppColors.card,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            const Icon(Icons.store_mall_directory_outlined,
                size: 18, color: AppColors.secondary),
            const SizedBox(width: AppSpacing.sm),
            Expanded(
              child: Text(
                branch?.branchName ?? '지점을 선택하세요',
                style: AppTypography.bodyMedium.copyWith(
                  color: branch == null
                      ? AppColors.textSecondary
                      : AppColors.textPrimary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            if (loading)
              const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            else
              const Icon(Icons.expand_more, color: AppColors.textSecondary),
          ],
        ),
      ),
    );
  }
}

class _DateSelector extends StatelessWidget {
  final DateTime date;
  final VoidCallback onPrev;
  final VoidCallback onNext;
  final VoidCallback onTap;

  const _DateSelector({
    required this.date,
    required this.onPrev,
    required this.onNext,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final label = DateFormat('yyyy.MM.dd(E)', 'ko_KR').format(date);
    return Container(
      color: AppColors.card,
      padding: const EdgeInsets.only(bottom: AppSpacing.sm),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            onPressed: onPrev,
            icon: const Icon(Icons.chevron_left),
            color: AppColors.textSecondary,
          ),
          InkWell(
            onTap: onTap,
            child: Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          IconButton(
            onPressed: onNext,
            icon: const Icon(Icons.chevron_right),
            color: AppColors.textSecondary,
          ),
        ],
      ),
    );
  }
}

/// 등록 가능 여부/사유를 한 줄로 표시하는 배너.
class _EligibilityBanner extends StatelessWidget {
  final ProxyEligibility eligibility;
  final DateTime now;

  const _EligibilityBanner({required this.eligibility, required this.now});

  @override
  Widget build(BuildContext context) {
    final ok = eligibility.canRegister;
    final color = ok ? AppColors.secondary : AppColors.warning;
    final icon = ok ? Icons.check_circle_outline : Icons.info_outline;
    final text = ok
        ? '오늘 ${DateFormat('HH:mm').format(now)} · 17:00까지 대리출근 등록 가능'
        : (eligibility.reason ?? '');

    return Container(
      width: double.infinity,
      color: color.withValues(alpha: 0.08),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          Icon(icon, size: 16, color: color),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              text,
              style: AppTypography.bodySmall.copyWith(
                color: color,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionHeader extends StatelessWidget {
  final String title;
  const _SectionHeader({required this.title});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.md,
        AppSpacing.lg,
        AppSpacing.xs,
      ),
      child: Text(
        title,
        style: AppTypography.bodyMedium.copyWith(
          fontWeight: FontWeight.w700,
          color: AppColors.textSecondary,
        ),
      ),
    );
  }
}

/// 여사원별 밴드 — "이름 (진열/행사)" + (조건부) 등록 버튼.
class _RegisterPersonBand extends StatelessWidget {
  final String name;
  final String category;
  final bool enabled;
  final VoidCallback? onRegister;

  const _RegisterPersonBand({
    required this.name,
    required this.category,
    this.enabled = true,
    this.onRegister,
  });

  @override
  Widget build(BuildContext context) {
    final textColor = enabled ? AppColors.secondary : AppColors.textSecondary;
    return Container(
      width: double.infinity,
      color: AppColors.surface,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '$name ($category)',
              style: AppTypography.bodyMedium.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
          ),
          if (onRegister != null)
            InkWell(
              onTap: onRegister,
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.xs,
                ),
                decoration: BoxDecoration(
                  border: Border.all(color: textColor),
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                ),
                child: Text(
                  '등록',
                  style: AppTypography.bodySmall.copyWith(
                    color: textColor,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// 여사원별 밴드 아래 거래처 1줄 — 거래처명 + 근무유형 + 출근 여부.
class _RegisterAccountRow extends StatelessWidget {
  final LeaderDailyWorker worker;
  const _RegisterAccountRow({required this.worker});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.card,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xl,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  worker.accountName,
                  style: AppTypography.bodyMedium
                      .copyWith(color: AppColors.textPrimary),
                ),
                if (worker.workCategoryLabel.isNotEmpty)
                  Text(
                    worker.workCategoryLabel,
                    style: AppTypography.bodySmall
                        .copyWith(color: AppColors.textSecondary),
                  ),
              ],
            ),
          ),
          Text(
            worker.attended ? '등록완료' : '미등록',
            style: AppTypography.bodySmall.copyWith(
              color: worker.attended ? AppColors.secondary : AppColors.warning,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyHint extends StatelessWidget {
  final IconData icon;
  final String text;

  const _EmptyHint({required this.icon, required this.text});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 48, color: AppColors.textSecondary),
          const SizedBox(height: AppSpacing.md),
          Text(
            text,
            style:
                AppTypography.bodyMedium.copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }
}
