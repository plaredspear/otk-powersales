import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_daily_status.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/error_view.dart';
import '../../widgets/common/loading_indicator.dart';

/// 조장 — 여사원 일별 현황 화면 (레거시 `employee/mngDaily.jsp` 결과 동등).
///
/// 선택 날짜의 팀 여사원 진열/행사/연차 근무 현황 + 거래처별 출근 등록 현황을 **조회 전용**으로 표시한다.
/// 진열은 진열 마스터(확정·안전점검 제출자) 기준이라 레거시와 동일하게 조회만 제공한다.
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
        // 진열/행사/연차 모두 조회 전용 (레거시 mngDaily 동등).
        _WorkerList(
          workers: displayWorkers,
          emptyText: isSearching ? '검색 결과가 없습니다' : '진열 근무자가 없습니다',
          onRefresh: onRefresh,
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

  const _WorkerList({
    required this.workers,
    required this.emptyText,
    required this.onRefresh,
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
        itemBuilder: (context, index) => _WorkerCard(worker: workers[index]),
      ),
    );
  }
}

class _WorkerCard extends StatelessWidget {
  final LeaderDailyWorker worker;

  const _WorkerCard({required this.worker});

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

