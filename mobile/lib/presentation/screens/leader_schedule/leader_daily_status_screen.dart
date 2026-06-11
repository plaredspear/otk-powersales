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
///
/// 레거시 정합: 탭이 아니라 한 화면에 진열/행사/연차 근무자 섹션을 세로로 나열하고,
/// 상단 "바로가기"(진열/행사/연차) 로 각 섹션으로 스크롤 이동한다. 카드는 여사원 1명 단위로
/// 묶어 담당 거래처를 여러 줄로 표시한다(레거시 mngDaily 카드 = 여사원 1명).
class LeaderDailyStatusScreen extends ConsumerStatefulWidget {
  /// 진입 시 조회할 초기 날짜. null 이면 오늘. (조원 월간 캘린더 날짜 탭 진입용)
  final DateTime? initialDate;

  /// 단일 여사원 모드 대상 ID. null 이면 "여사원 전체" 모드(섹션/바로가기/검색 노출).
  /// 지정 시 레거시 mngDaily 개인 화면 — 타이틀=이름, 헤더="X/Y 보고 완료", 해당 여사원 카드만.
  final int? singleEmployeeId;

  /// 단일 여사원 모드 타이틀(여사원 이름).
  final String? singleEmployeeName;

  const LeaderDailyStatusScreen({
    super.key,
    this.initialDate,
    this.singleEmployeeId,
    this.singleEmployeeName,
  });

  @override
  ConsumerState<LeaderDailyStatusScreen> createState() =>
      _LeaderDailyStatusScreenState();
}

class _LeaderDailyStatusScreenState
    extends ConsumerState<LeaderDailyStatusScreen> with ThrottledTapMixin {
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  // 바로가기 스크롤 앵커 (레거시 #move1/#move2/#move3).
  final GlobalKey _displaySectionKey = GlobalKey();
  final GlobalKey _eventSectionKey = GlobalKey();
  final GlobalKey _annualSectionKey = GlobalKey();

  /// 등록 탭 "등록 전" 필터 — 체크 시 미등록 거래처만 노출(레거시 #before-comm-btn).
  bool _registerUnregisteredOnly = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final notifier = ref.read(leaderDailyStatusProvider.notifier);
      if (widget.initialDate != null) {
        // changeDate 가 내부에서 load() 까지 수행.
        notifier.changeDate(widget.initialDate!);
      } else {
        notifier.load();
      }
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    _scrollController.dispose();
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

  void _jumpTo(GlobalKey key) {
    final ctx = key.currentContext;
    if (ctx == null) return;
    Scrollable.ensureVisible(
      ctx,
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      alignment: 0.0,
    );
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
    final isSingle = widget.singleEmployeeId != null;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        // 단일 모드: 타이틀=여사원 이름 (레거시 mngDaily 개인 화면 h1).
        title: Text(isSingle
            ? (widget.singleEmployeeName ?? '여사원 일정')
            : '여사원 일별 현황'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: isSingle ? _buildSingleMode(state) : _buildAllMode(state),
    );
  }

  /// "여사원 전체" 모드 — 요약칩 + 검색 + 일정/등록 탭.
  Widget _buildAllMode(LeaderDailyStatusState state) {
    // 레거시 mngDaily 카드 1개 = 여사원 1명 — 백엔드의 (여사원×거래처) 행을 여사원 단위로 그룹핑.
    final displayGroups = _groupByEmployee(state.filteredDisplayWorkers);
    final eventGroups = _groupByEmployee(state.filteredEventWorkers);
    final annualLeaveWorkers = state.filteredAnnualLeaveWorkers;
    final isSearching = state.searchKeyword.isNotEmpty;

    return DefaultTabController(
      length: 2,
      child: Column(
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
            onChanged: (v) => ref
                .read(leaderDailyStatusProvider.notifier)
                .setSearchKeyword(v),
          ),
          const _ScheduleRegisterTabBar(),
          Expanded(
            child: TabBarView(
              children: [
                // 일정 탭: 바로가기 + 진열/행사/연차 섹션.
                Column(
                  children: [
                    _QuickJumpBar(
                      onDisplay: () => _jumpTo(_displaySectionKey),
                      onEvent: () => _jumpTo(_eventSectionKey),
                      onAnnual: () => _jumpTo(_annualSectionKey),
                    ),
                    Expanded(
                      child: _buildBody(
                        state,
                        displayGroups: displayGroups,
                        eventGroups: eventGroups,
                        annualLeaveWorkers: annualLeaveWorkers,
                        isSearching: isSearching,
                      ),
                    ),
                  ],
                ),
                // 등록 탭: 등록 전 필터 + 사람별 밴드 + 거래처별 등록 상태.
                _buildRegisterTab(state, displayGroups, eventGroups),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// 단일 여사원 모드 — 날짜 + "X/Y 보고 완료(카테고리)" 헤더 + 해당 여사원 카드만.
  /// 레거시 mngDaily `employeeid != ''` 분기 동등 (검색/바로가기/섹션헤더 없음).
  Widget _buildSingleMode(LeaderDailyStatusState state) {
    final empId = widget.singleEmployeeId!;
    final data = state.data;
    final displayRows =
        (data?.displayWorkers ?? const []).where((w) => w.employeeId == empId).toList();
    final eventRows =
        (data?.eventWorkers ?? const []).where((w) => w.employeeId == empId).toList();

    final displayGroups = _groupByEmployee(displayRows);
    final eventGroups = _groupByEmployee(eventRows);

    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          // 단일 모드: 레거시처럼 날짜는 텍스트만 (이동 화살표/피커 없음).
          _DateText(date: state.selectedDate),
          _SingleEmployeeHeader(displayRows: displayRows, eventRows: eventRows),
          const _ScheduleRegisterTabBar(),
          Expanded(
            child: TabBarView(
              children: [
                // 일정 탭.
                _buildSingleBody(state,
                    displayRows: displayRows, eventRows: eventRows),
                // 등록 탭.
                _buildRegisterTab(state, displayGroups, eventGroups),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// 등록 탭 공통 빌더 (전체/단일 공유) — 등록 전 필터 + 사람별 밴드 + 거래처별 등록 상태.
  /// 레거시 mngDaily `#commute` 영역 동등. 대리출근 등록 버튼은 레거시에서 정지(주석)라 미노출.
  Widget _buildRegisterTab(
    LeaderDailyStatusState state,
    List<_GroupedWorker> displayGroups,
    List<_GroupedWorker> eventGroups,
  ) {
    final loadingOrError = _loadingOrError(state);

    final sections = <Widget>[];
    void addGroups(List<_GroupedWorker> groups, String category) {
      for (final g in groups) {
        final rows = _registerUnregisteredOnly
            ? g.rows.where((r) => !r.attended).toList()
            : g.rows;
        if (rows.isEmpty) continue;
        sections.add(_RegisterPersonBand(name: g.employeeName, category: category));
        for (final r in rows) {
          sections.add(_RegisterAccountRow(row: r));
        }
      }
    }

    addGroups(displayGroups, '진열');
    addGroups(eventGroups, '행사');

    Future<void> onRefresh() =>
        ref.read(leaderDailyStatusProvider.notifier).load();

    return Column(
      children: [
        _UnregisteredFilterBar(
          value: _registerUnregisteredOnly,
          onChanged: (v) => setState(() => _registerUnregisteredOnly = v),
        ),
        Expanded(
          child: loadingOrError ??
              RefreshIndicator(
                onRefresh: onRefresh,
                color: AppColors.primary,
                child: sections.isEmpty
                    ? ListView(
                        children: [
                          SizedBox(
                            height: MediaQuery.of(context).size.height * 0.35,
                            child: Center(
                              child: Text(
                                _registerUnregisteredOnly
                                    ? '미등록 거래처가 없습니다'
                                    : '등록 대상 거래처가 없습니다',
                                style: AppTypography.bodyMedium
                                    .copyWith(color: AppColors.textSecondary),
                              ),
                            ),
                          ),
                        ],
                      )
                    : ListView(
                        padding: const EdgeInsets.only(bottom: AppSpacing.xl),
                        children: sections,
                      ),
              ),
        ),
      ],
    );
  }

  /// 최초 로드 중/실패 시 표시 위젯, 데이터 보유 시 null.
  Widget? _loadingOrError(LeaderDailyStatusState state) {
    if (state.isLoading && state.data == null) {
      return const LoadingIndicator(message: '불러오는 중...');
    }
    if (state.errorMessage != null && state.data == null) {
      return ErrorView(
        message: '불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () => ref.read(leaderDailyStatusProvider.notifier).load(),
      );
    }
    return null;
  }

  Widget _buildSingleBody(
    LeaderDailyStatusState state, {
    required List<LeaderDailyWorker> displayRows,
    required List<LeaderDailyWorker> eventRows,
  }) {
    if (state.isLoading && state.data == null) {
      return const LoadingIndicator(message: '일정을 불러오는 중...');
    }
    if (state.errorMessage != null && state.data == null) {
      return ErrorView(
        message: '일정을 불러올 수 없습니다',
        description: state.errorMessage,
        onRetry: () => ref.read(leaderDailyStatusProvider.notifier).load(),
      );
    }

    Future<void> onRefresh() =>
        ref.read(leaderDailyStatusProvider.notifier).load();

    final displayGroups = _groupByEmployee(displayRows);
    final eventGroups = _groupByEmployee(eventRows);

    if (displayGroups.isEmpty && eventGroups.isEmpty) {
      return RefreshIndicator(
        onRefresh: onRefresh,
        color: AppColors.primary,
        child: ListView(
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.4,
              child: Center(
                child: Text(
                  '해당 일자 근무 일정이 없습니다',
                  style: AppTypography.bodyMedium
                      .copyWith(color: AppColors.textSecondary),
                ),
              ),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: onRefresh,
      color: AppColors.primary,
      child: ListView(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        children: [
          for (final g in displayGroups) _WorkerCard(worker: g, isEvent: false),
          for (final g in eventGroups) _WorkerCard(worker: g, isEvent: true),
        ],
      ),
    );
  }

  Widget _buildBody(
    LeaderDailyStatusState state, {
    required List<_GroupedWorker> displayGroups,
    required List<_GroupedWorker> eventGroups,
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

    final emptyHint = isSearching ? '검색 결과가 없습니다' : null;

    return RefreshIndicator(
      onRefresh: onRefresh,
      color: AppColors.primary,
      child: ListView(
        controller: _scrollController,
        padding: const EdgeInsets.only(bottom: AppSpacing.xl),
        children: [
          // ── 진열 근무자 섹션 ──
          _SectionHeader(
            key: _displaySectionKey,
            title: '진열 근무자',
            count: displayGroups.length,
          ),
          ..._buildWorkerCards(
            displayGroups,
            emptyText: emptyHint ?? '진열 근무자가 없습니다',
            isEvent: false,
          ),
          // ── 행사 근무자 섹션 ──
          _SectionHeader(
            key: _eventSectionKey,
            title: '행사 근무자',
            count: eventGroups.length,
          ),
          ..._buildWorkerCards(
            eventGroups,
            emptyText: emptyHint ?? '행사 근무자가 없습니다',
            isEvent: true,
          ),
          // ── 연차 근무자 섹션 ──
          _SectionHeader(
            key: _annualSectionKey,
            title: '연차 근무자',
            count: annualLeaveWorkers.length,
          ),
          ..._buildAnnualCards(
            annualLeaveWorkers,
            emptyText: emptyHint ?? '연차 여사원이 없습니다',
          ),
        ],
      ),
    );
  }

  List<Widget> _buildWorkerCards(
    List<_GroupedWorker> groups, {
    required String emptyText,
    required bool isEvent,
  }) {
    if (groups.isEmpty) {
      return [_SectionEmpty(text: emptyText)];
    }
    return groups.map((g) => _WorkerCard(worker: g, isEvent: isEvent)).toList();
  }

  List<Widget> _buildAnnualCards(
    List<LeaderDailyEmployee> employees, {
    required String emptyText,
  }) {
    if (employees.isEmpty) {
      return [_SectionEmpty(text: emptyText)];
    }
    return employees.map((e) => _AnnualLeaveCard(employee: e)).toList();
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

/// 여사원 1명 단위 일별 근무 묶음 (레거시 mngDaily 카드 = 여사원 1명, 담당 거래처 N줄).
class _GroupedWorker {
  final String employeeName;
  final String employeeCode;
  final List<LeaderDailyWorker> rows;

  const _GroupedWorker({
    required this.employeeName,
    required this.employeeCode,
    required this.rows,
  });

  /// 담당 거래처 중 하나라도 출근(출근등록)했으면 등록완료 (레거시 commuteNullChk 동등).
  bool get attended => rows.any((r) => r.attended);
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

/// 단일 모드 날짜 텍스트 — 레거시 "2023년 09월 07일(목)" 표기 (이동 없음).
class _DateText extends StatelessWidget {
  final DateTime date;

  const _DateText({required this.date});

  @override
  Widget build(BuildContext context) {
    final label = DateFormat('yyyy년 MM월 dd일(E)', 'ko_KR').format(date);
    return Container(
      width: double.infinity,
      color: AppColors.white,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Text(
        label,
        style: AppTypography.bodyLarge.copyWith(
          fontWeight: FontWeight.w700,
          color: AppColors.textPrimary,
        ),
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

/// 단일 여사원 모드 헤더 — "X / Y 보고 완료 (카테고리)".
/// Y=담당 거래처 수, X=출근등록 완료 거래처 수, 카테고리=진열/행사 (레거시 emp-acc-cnt/comm-cnt).
class _SingleEmployeeHeader extends StatelessWidget {
  final List<LeaderDailyWorker> displayRows;
  final List<LeaderDailyWorker> eventRows;

  const _SingleEmployeeHeader({
    required this.displayRows,
    required this.eventRows,
  });

  @override
  Widget build(BuildContext context) {
    final rows = [...displayRows, ...eventRows];
    final total = rows.length;
    final done = rows.where((r) => r.attended).length;
    // 레거시는 첫 .workingcategory — 진열 우선, 없으면 행사.
    final category = displayRows.isNotEmpty
        ? '진열'
        : (eventRows.isNotEmpty ? '행사' : '');

    return Container(
      width: double.infinity,
      color: AppColors.surface,
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
      child: Center(
        child: RichText(
          text: TextSpan(
            style: AppTypography.bodyLarge.copyWith(color: AppColors.textPrimary),
            children: [
              TextSpan(
                text: '$done',
                style: const TextStyle(color: AppColors.error, fontWeight: FontWeight.bold),
              ),
              const TextSpan(text: ' / '),
              TextSpan(
                text: '$total',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
              const TextSpan(text: ' 보고 완료'),
              if (category.isNotEmpty) TextSpan(text: ' ($category)'),
            ],
          ),
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

/// 상단 바로가기 (레거시 "바로가기 | 진열 행사 연차" 앵커).
class _QuickJumpBar extends StatelessWidget {
  final VoidCallback onDisplay;
  final VoidCallback onEvent;
  final VoidCallback onAnnual;

  const _QuickJumpBar({
    required this.onDisplay,
    required this.onEvent,
    required this.onAnnual,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.white,
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        0,
        AppSpacing.lg,
        AppSpacing.sm,
      ),
      child: Row(
        children: [
          Text(
            '바로가기',
            style: AppTypography.bodySmall.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          Container(width: 1, height: 12, color: AppColors.border),
          const SizedBox(width: AppSpacing.sm),
          _JumpLink(label: '진열', onTap: onDisplay),
          const SizedBox(width: AppSpacing.md),
          _JumpLink(label: '행사', onTap: onEvent),
          const SizedBox(width: AppSpacing.md),
          _JumpLink(label: '연차', onTap: onAnnual),
        ],
      ),
    );
  }
}

class _JumpLink extends StatelessWidget {
  final String label;
  final VoidCallback onTap;

  const _JumpLink({required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.xs,
          vertical: AppSpacing.xxs,
        ),
        child: Text(
          label,
          style: AppTypography.bodyMedium.copyWith(
            color: AppColors.secondary,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}

/// 일정 / 등록 탭바 (레거시 mngDaily 상단 탭). 선택색은 네이비(가독성).
class _ScheduleRegisterTabBar extends StatelessWidget {
  const _ScheduleRegisterTabBar();

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      child: const TabBar(
        labelColor: AppColors.secondary,
        unselectedLabelColor: AppColors.textSecondary,
        indicatorColor: AppColors.secondary,
        tabs: [
          Tab(text: '일정'),
          Tab(text: '등록'),
        ],
      ),
    );
  }
}

/// 등록 탭 "등록 전" 체크박스 필터 바 (레거시 #before-comm-btn, 우측 정렬).
class _UnregisteredFilterBar extends StatelessWidget {
  final bool value;
  final ValueChanged<bool> onChanged;

  const _UnregisteredFilterBar({required this.value, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      padding: const EdgeInsets.only(right: AppSpacing.md),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          InkWell(
            onTap: () => onChanged(!value),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
              child: Row(
                children: [
                  SizedBox(
                    width: 24,
                    height: 24,
                    child: Checkbox(
                      value: value,
                      onChanged: (v) => onChanged(v ?? false),
                      activeColor: AppColors.secondary,
                      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                  ),
                  const SizedBox(width: AppSpacing.xs),
                  Text(
                    '등록 전',
                    style: AppTypography.bodyMedium
                        .copyWith(color: AppColors.textPrimary),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 등록 탭 사람별 밴드 — "이름 (진열/행사)" (레거시 list_title).
class _RegisterPersonBand extends StatelessWidget {
  final String name;
  final String category;

  const _RegisterPersonBand({required this.name, required this.category});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.surface,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Text(
        '$name ($category)',
        style: AppTypography.bodyMedium.copyWith(
          fontWeight: FontWeight.w700,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}

/// 등록 탭 거래처별 행 — 거래처명 / 근무유형 + 등록완료·미등록 (레거시 comm-list li).
class _RegisterAccountRow extends StatelessWidget {
  final LeaderDailyWorker row;

  const _RegisterAccountRow({required this.row});

  @override
  Widget build(BuildContext context) {
    final done = row.attended;
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(bottom: BorderSide(color: AppColors.border)),
      ),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  row.accountName,
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  ),
                ),
                if (row.workCategoryLabel.isNotEmpty) ...[
                  const SizedBox(height: AppSpacing.xxs),
                  Text(
                    row.workCategoryLabel,
                    style: AppTypography.bodySmall
                        .copyWith(color: AppColors.textSecondary),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          Text(
            done ? '등록 완료' : '미등록',
            style: AppTypography.bodySmall.copyWith(
              color: done ? AppColors.success : AppColors.textSecondary,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}

/// 섹션 헤더 — "진열 근무자 (N)" (N 은 레거시처럼 빨강 강조).
class _SectionHeader extends StatelessWidget {
  final String title;
  final int count;

  const _SectionHeader({super.key, required this.title, required this.count});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.md,
        AppSpacing.lg,
        AppSpacing.xs,
      ),
      child: Row(
        children: [
          Text(
            title,
            style: AppTypography.bodyLarge.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(width: AppSpacing.xs),
          Text(
            '($count)',
            style: AppTypography.bodyLarge.copyWith(
              fontWeight: FontWeight.bold,
              color: AppColors.error,
            ),
          ),
        ],
      ),
    );
  }
}

/// 섹션 내 빈 상태 안내 (레거시 (0) 헤더와 함께 빈 화면 방지).
class _SectionEmpty extends StatelessWidget {
  final String text;

  const _SectionEmpty({required this.text});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.xs,
        AppSpacing.lg,
        AppSpacing.md,
      ),
      child: Text(
        text,
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textSecondary,
        ),
      ),
    );
  }
}

/// 여사원 1명 카드 — 이름(사번) + 담당 거래처 N줄(거래처명 | 진열/상시/순회).
///
/// 우측 표시는 레거시 mngDaily 정합:
/// - 진열([isEvent]=false): 출근 상태 배지(등록완료/미등록).
/// - 행사([isEvent]=true): 변경/변경불가 버튼. 행사 일정변경(mutation)은 보류라 `변경`은
///   안내만 노출(레거시 promotionScheduleChange 이동 자리).
class _WorkerCard extends StatelessWidget {
  final _GroupedWorker worker;
  final bool isEvent;

  const _WorkerCard({required this.worker, required this.isEvent});

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
                const SizedBox(height: AppSpacing.xs),
                // 담당 거래처별 1줄: "거래처명 | 진열/상시/순회"
                ...worker.rows.map((r) => Padding(
                      padding: const EdgeInsets.only(bottom: AppSpacing.xxs),
                      child: Text(
                        r.workCategoryLabel.isEmpty
                            ? r.accountName
                            : '${r.accountName} | ${r.workCategoryLabel}',
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.textPrimary,
                        ),
                      ),
                    )),
              ],
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          // 행사: 변경/변경불가 버튼, 진열: 등록완료/미등록 배지.
          if (isEvent)
            _ChangeButton(attended: worker.attended)
          else
            _AttendanceBadge(attended: worker.attended),
        ],
      ),
    );
  }
}

/// 행사 근무자 변경/변경불가 버튼 (레거시 mngDaily commuteNullChk 정합).
///
/// - 출근등록 없음([attended]=false) → "변경"(활성). 탭 시 행사 일정변경은 보류 안내.
/// - 출근등록 있음([attended]=true) → "변경불가"(비활성 라벨).
class _ChangeButton extends StatelessWidget {
  final bool attended;

  const _ChangeButton({required this.attended});

  @override
  Widget build(BuildContext context) {
    if (attended) {
      // 변경불가 — 비활성 라벨.
      return Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.xs,
        ),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
          border: Border.all(color: AppColors.border),
        ),
        child: Text(
          '변경불가',
          style: AppTypography.labelSmall.copyWith(
            color: AppColors.textSecondary,
            fontWeight: FontWeight.w600,
          ),
        ),
      );
    }
    // 변경 — 활성. mutation 보류라 안내만.
    return InkWell(
      borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
      onTap: () {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('행사 일정 변경 기능은 추후 제공됩니다.')),
        );
      },
      child: Container(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.xs,
        ),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
          border: Border.all(color: AppColors.secondary),
        ),
        child: Text(
          '변경',
          style: AppTypography.labelSmall.copyWith(
            color: AppColors.secondary,
            fontWeight: FontWeight.w600,
          ),
        ),
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
