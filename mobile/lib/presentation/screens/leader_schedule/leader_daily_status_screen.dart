import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/error_utils.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_account.dart';
import '../../../domain/entities/leader_daily_status.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/error_view.dart';
import '../../widgets/common/loading_indicator.dart';

/// 조장 — 여사원 일별 현황 화면 (레거시 `employee/mngDaily.jsp` 대응).
///
/// 선택 날짜의 팀 여사원 진열/행사/연차 근무 현황 + 거래처별 출근 등록 현황을 조회한다(P6).
/// P7: **진열 일정**에 한해 거래처 변경/삭제 가능(출근 미등록 건). 행사 일정변경은
/// admin promotion 도메인(spec #679/#571)이 소유하므로 본 화면 범위 밖.
class LeaderDailyStatusScreen extends ConsumerStatefulWidget {
  const LeaderDailyStatusScreen({super.key});

  @override
  ConsumerState<LeaderDailyStatusScreen> createState() =>
      _LeaderDailyStatusScreenState();
}

class _LeaderDailyStatusScreenState
    extends ConsumerState<LeaderDailyStatusScreen> with ThrottledTapMixin {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(leaderDailyStatusProvider.notifier).load();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _shiftDate(int days) {
    throttledTap(() {
      final current = ref.read(leaderDailyStatusProvider).selectedDate;
      ref
          .read(leaderDailyStatusProvider.notifier)
          .changeDate(current.add(Duration(days: days)));
    });
  }

  void _pickDate() {
    throttledTapAsync(() async {
      final current = ref.read(leaderDailyStatusProvider).selectedDate;
      final picked = await showDatePicker(
        context: context,
        initialDate: current,
        firstDate: DateTime(current.year - 2, 1, 1),
        lastDate: DateTime(current.year + 2, 12, 31),
        helpText: '조회 일자 선택',
        cancelText: '취소',
        confirmText: '확인',
      );
      if (picked != null) {
        ref.read(leaderDailyStatusProvider.notifier).changeDate(picked);
      }
    });
  }

  /// 진열 일정 거래처 변경(P7) — 거래처 선택 시트 → 수정 → 성공 토스트.
  void _onEditAccount(LeaderDailyWorker worker) {
    throttledTapAsync(() async {
      final accountId = await showModalBottomSheet<int>(
        context: context,
        isScrollControlled: true,
        builder: (_) => const _AccountPickerSheet(),
      );
      if (accountId == null) return;
      final ok = await ref
          .read(leaderDailyStatusProvider.notifier)
          .updateScheduleAccount(worker.scheduleId, accountId);
      if (ok && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('거래처가 변경되었습니다')),
        );
      }
    });
  }

  /// 진열 일정 삭제(P7) — 확인 다이얼로그 → 삭제 → 성공 토스트.
  void _onDelete(LeaderDailyWorker worker) {
    throttledTapAsync(() async {
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('일정 삭제'),
          content: Text(
            '${worker.employeeName}님의 "${worker.accountName}" 진열 일정을 삭제할까요?',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('취소'),
            ),
            TextButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('삭제'),
            ),
          ],
        ),
      );
      if (confirmed != true) return;
      final ok = await ref
          .read(leaderDailyStatusProvider.notifier)
          .deleteSchedule(worker.scheduleId);
      if (ok && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('일정이 삭제되었습니다')),
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    // 재조회 실패(데이터 보유 상태)는 SnackBar 로 1회성 노출 — 최초 로드 실패는 ErrorView 가 담당.
    ref.listen<String?>(
      leaderDailyStatusProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null &&
            ref.read(leaderDailyStatusProvider).data != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next)),
          );
          ref.read(leaderDailyStatusProvider.notifier).clearError();
        }
      },
    );

    final state = ref.watch(leaderDailyStatusProvider);
    final displayWorkers = state.filteredDisplayWorkers;
    final eventWorkers = state.filteredEventWorkers;
    final annualLeaveWorkers = state.filteredAnnualLeaveWorkers;
    final isSearching = state.searchKeyword.isNotEmpty;

    return DefaultTabController(
      length: 3,
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          title: const Text('여사원 일별 현황'),
          backgroundColor: AppColors.white,
          foregroundColor: AppColors.textPrimary,
          elevation: 0,
        ),
        body: Column(
          children: [
            _DateSelector(
              date: state.selectedDate,
              onPrev: () => _shiftDate(-1),
              onNext: () => _shiftDate(1),
              onTap: _pickDate,
            ),
            _SummaryHeader(summary: state.data?.summary),
            _SearchField(
              controller: _searchController,
              onChanged: (v) =>
                  ref.read(leaderDailyStatusProvider.notifier).setSearchKeyword(v),
            ),
            TabBar(
              labelColor: AppColors.primary,
              unselectedLabelColor: AppColors.textSecondary,
              indicatorColor: AppColors.primary,
              tabs: [
                Tab(text: '진열 ${displayWorkers.length}'),
                Tab(text: '행사 ${eventWorkers.length}'),
                Tab(text: '연차 ${annualLeaveWorkers.length}'),
              ],
            ),
            Expanded(
              child: _buildBody(
                state,
                displayWorkers: displayWorkers,
                eventWorkers: eventWorkers,
                annualLeaveWorkers: annualLeaveWorkers,
                isSearching: isSearching,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBody(
    LeaderDailyStatusState state, {
    required List<LeaderDailyWorker> displayWorkers,
    required List<LeaderDailyWorker> eventWorkers,
    required List<LeaderDailyEmployee> annualLeaveWorkers,
    required bool isSearching,
  }) {
    if (state.isLoading && state.data == null) {
      return const LoadingIndicator(message: '일별 현황을 불러오는 중...');
    }
    if (state.errorMessage != null && state.data == null) {
      return ErrorView(
        message: '일별 현황을 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () => ref.read(leaderDailyStatusProvider.notifier).load(),
      );
    }

    Future<void> onRefresh() =>
        ref.read(leaderDailyStatusProvider.notifier).load();
    return TabBarView(
      children: [
        // 진열 탭 — 거래처 변경/삭제 가능(P7). 행사/연차는 조회 전용.
        _WorkerList(
          workers: displayWorkers,
          emptyText: isSearching ? '검색 결과가 없습니다' : '진열 근무자가 없습니다',
          onRefresh: onRefresh,
          onEditAccount: _onEditAccount,
          onDelete: _onDelete,
        ),
        _WorkerList(
          workers: eventWorkers,
          emptyText: isSearching ? '검색 결과가 없습니다' : '행사 근무자가 없습니다',
          onRefresh: onRefresh,
        ),
        _AnnualLeaveList(
          employees: annualLeaveWorkers,
          emptyText: isSearching ? '검색 결과가 없습니다' : '연차 여사원이 없습니다',
          onRefresh: onRefresh,
        ),
      ],
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
      color: AppColors.white,
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: onPrev,
            color: AppColors.textSecondary,
          ),
          InkWell(
            onTap: onTap,
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.md,
                vertical: AppSpacing.xs,
              ),
              child: Row(
                children: [
                  Text(
                    label,
                    style: AppTypography.bodyLarge.copyWith(
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.xs),
                  const Icon(Icons.calendar_today, size: 16),
                ],
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed: onNext,
            color: AppColors.textSecondary,
          ),
        ],
      ),
    );
  }
}

class _SummaryHeader extends StatelessWidget {
  final LeaderDailyStatusSummary? summary;

  const _SummaryHeader({this.summary});

  @override
  Widget build(BuildContext context) {
    final s = summary ?? const LeaderDailyStatusSummary();
    return Container(
      color: AppColors.white,
      padding: const EdgeInsets.only(
        left: AppSpacing.lg,
        right: AppSpacing.lg,
        bottom: AppSpacing.md,
      ),
      child: Row(
        children: [
          _SummaryChip(label: '진열', value: '${s.displayAttended}/${s.displayTotal}'),
          const SizedBox(width: AppSpacing.sm),
          _SummaryChip(label: '행사', value: '${s.eventAttended}/${s.eventTotal}'),
          const SizedBox(width: AppSpacing.sm),
          _SummaryChip(label: '연차', value: '${s.annualLeaveCount}'),
        ],
      ),
    );
  }
}

class _SummaryChip extends StatelessWidget {
  final String label;
  final String value;

  const _SummaryChip({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        decoration: BoxDecoration(
          color: AppColors.surface,
          borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        ),
        child: Column(
          children: [
            Text(
              label,
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.xxs),
            Text(
              value,
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SearchField extends StatelessWidget {
  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  const _SearchField({required this.controller, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        0,
        AppSpacing.lg,
        AppSpacing.md,
      ),
      child: TextField(
        controller: controller,
        onChanged: onChanged,
        decoration: InputDecoration(
          hintText: '여사원명 / 사번 / 점포명 검색',
          prefixIcon: const Icon(Icons.search, size: 20),
          isDense: true,
          contentPadding: const EdgeInsets.symmetric(
            vertical: AppSpacing.sm,
            horizontal: AppSpacing.md,
          ),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            borderSide: BorderSide(color: AppColors.border),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            borderSide: BorderSide(color: AppColors.border),
          ),
        ),
      ),
    );
  }
}

class _WorkerList extends StatelessWidget {
  final List<LeaderDailyWorker> workers;
  final String emptyText;
  final Future<void> Function() onRefresh;

  /// 진열 탭에서만 제공(행사 탭은 null) — 거래처 변경 / 삭제 액션.
  final void Function(LeaderDailyWorker)? onEditAccount;
  final void Function(LeaderDailyWorker)? onDelete;

  const _WorkerList({
    required this.workers,
    required this.emptyText,
    required this.onRefresh,
    this.onEditAccount,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    if (workers.isEmpty) {
      return _EmptyList(text: emptyText, onRefresh: onRefresh);
    }
    return RefreshIndicator(
      onRefresh: onRefresh,
      color: AppColors.primary,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        itemCount: workers.length,
        itemBuilder: (context, index) => _WorkerCard(
          worker: workers[index],
          onEditAccount: onEditAccount,
          onDelete: onDelete,
        ),
      ),
    );
  }
}

class _WorkerCard extends StatelessWidget {
  final LeaderDailyWorker worker;
  final void Function(LeaderDailyWorker)? onEditAccount;
  final void Function(LeaderDailyWorker)? onDelete;

  const _WorkerCard({
    required this.worker,
    this.onEditAccount,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    // 출근 등록 전 + 진열 탭(콜백 제공)일 때만 편집/삭제 노출.
    final canManage =
        !worker.attended && (onEditAccount != null || onDelete != null);

    return Container(
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${worker.employeeName} (${worker.employeeCode})',
                  style: AppTypography.bodyLarge.copyWith(
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: AppSpacing.xxs),
                Text(
                  worker.accountName,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textPrimary,
                  ),
                ),
                if (worker.workCategoryLabel.isNotEmpty) ...[
                  const SizedBox(height: AppSpacing.xxs),
                  Text(
                    worker.workCategoryLabel,
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          _AttendanceBadge(attended: worker.attended),
          if (canManage)
            PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, size: 20),
              onSelected: (value) {
                if (value == 'edit') onEditAccount?.call(worker);
                if (value == 'delete') onDelete?.call(worker);
              },
              itemBuilder: (context) => [
                if (onEditAccount != null)
                  const PopupMenuItem(value: 'edit', child: Text('거래처 변경')),
                if (onDelete != null)
                  const PopupMenuItem(value: 'delete', child: Text('삭제')),
              ],
            ),
        ],
      ),
    );
  }
}

class _AttendanceBadge extends StatelessWidget {
  final bool attended;

  const _AttendanceBadge({required this.attended});

  @override
  Widget build(BuildContext context) {
    final color = attended ? AppColors.success : AppColors.error;
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        attended ? '등록완료' : '미등록',
        style: AppTypography.labelSmall.copyWith(
          color: color,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _AnnualLeaveList extends StatelessWidget {
  final List<LeaderDailyEmployee> employees;
  final String emptyText;
  final Future<void> Function() onRefresh;

  const _AnnualLeaveList({
    required this.employees,
    required this.emptyText,
    required this.onRefresh,
  });

  @override
  Widget build(BuildContext context) {
    if (employees.isEmpty) {
      return _EmptyList(text: emptyText, onRefresh: onRefresh);
    }
    return RefreshIndicator(
      onRefresh: onRefresh,
      color: AppColors.primary,
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        itemCount: employees.length,
        itemBuilder: (context, index) =>
            _AnnualLeaveCard(employee: employees[index]),
      ),
    );
  }
}

class _AnnualLeaveCard extends StatelessWidget {
  final LeaderDailyEmployee employee;

  const _AnnualLeaveCard({required this.employee});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      padding: const EdgeInsets.all(AppSpacing.md),
      decoration: BoxDecoration(
        color: AppColors.white,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(color: AppColors.border),
      ),
      child: Text(
        '${employee.employeeName} (${employee.employeeCode})',
        style: AppTypography.bodyLarge.copyWith(
          fontWeight: FontWeight.w600,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}

class _EmptyList extends StatelessWidget {
  final String text;
  final Future<void> Function() onRefresh;

  const _EmptyList({required this.text, required this.onRefresh});

  @override
  Widget build(BuildContext context) {
    return RefreshIndicator(
      onRefresh: onRefresh,
      child: ListView(
        children: [
          SizedBox(
            height: MediaQuery.of(context).size.height * 0.4,
            child: Center(
              child: Text(
                text,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 거래처 변경 시 사용하는 본인 담당 거래처 선택 시트 (P7).
/// 선택 시 `Navigator.pop(accountId)` 로 반환한다.
class _AccountPickerSheet extends ConsumerStatefulWidget {
  const _AccountPickerSheet();

  @override
  ConsumerState<_AccountPickerSheet> createState() =>
      _AccountPickerSheetState();
}

class _AccountPickerSheetState extends ConsumerState<_AccountPickerSheet> {
  bool _isLoading = true;
  String? _error;
  List<LeaderAccount> _accounts = const [];
  Timer? _debounce;

  /// 검색 응답 순서 역전(out-of-order) 방지용 요청 시퀀스 토큰.
  int _requestSeq = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  /// 입력마다 즉시 호출하지 않고 300ms 디바운스 후 검색.
  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () {
      final keyword = value.trim().isEmpty ? null : value.trim();
      _load(keyword: keyword);
    });
  }

  Future<void> _load({String? keyword}) async {
    final seq = ++_requestSeq;
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final accounts = await ref
          .read(leaderScheduleRepositoryProvider)
          .getAccounts(keyword: keyword);
      if (!mounted || seq != _requestSeq) return; // 최신 요청만 반영
      setState(() {
        _accounts = accounts;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted || seq != _requestSeq) return;
      setState(() {
        _error = extractErrorMessage(e);
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    return Padding(
      padding: EdgeInsets.only(bottom: bottomInset),
      child: SizedBox(
        height: MediaQuery.of(context).size.height * 0.6,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: Row(
                children: [
                  Text(
                    '거래처 선택',
                    style: AppTypography.bodyLarge.copyWith(
                      fontWeight: FontWeight.w600,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const Spacer(),
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
              child: TextField(
                onChanged: _onSearchChanged,
                decoration: InputDecoration(
                  hintText: '거래처명 / 주소 검색',
                  prefixIcon: const Icon(Icons.search, size: 20),
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
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: AppColors.primary),
      );
    }
    if (_error != null) {
      return Center(
        child: Text(
          _error!,
          style: AppTypography.bodyMedium.copyWith(color: AppColors.error),
        ),
      );
    }
    if (_accounts.isEmpty) {
      return Center(
        child: Text(
          '거래처가 없습니다',
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.textSecondary,
          ),
        ),
      );
    }
    return ListView.separated(
      itemCount: _accounts.length,
      separatorBuilder: (_, __) => Divider(height: 1, color: AppColors.border),
      itemBuilder: (context, index) {
        final account = _accounts[index];
        return ListTile(
          title: Text(account.name ?? '-'),
          subtitle: account.address1 != null ? Text(account.address1!) : null,
          onTap: () => Navigator.of(context).pop(account.id),
        );
      },
    );
  }
}
