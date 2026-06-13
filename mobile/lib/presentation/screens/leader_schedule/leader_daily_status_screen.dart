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

  /// 등록 탭 공통 빌더 (전체/단일 공유) — 등록 가능 배너 + 등록 전 필터 + 사람별 밴드 + 거래처별 등록 상태.
  /// 레거시 mngDaily `#commute` 영역 동등. 등록 버튼은 항상 노출하되 조건(당일·17시 전) 미충족 시
  /// 비활성 + 탭하면 사유 안내(A안 — 레거시 alert 동등), 탭 상단에 등록 가능 여부 배너 노출(B안).
  Widget _buildRegisterTab(
    LeaderDailyStatusState state,
    List<_GroupedWorker> displayGroups,
    List<_GroupedWorker> eventGroups,
  ) {
    final loadingOrError = _loadingOrError(state);

    // 대리등록 가능 조건: 조회 날짜가 오늘 + 17시 이전 (레거시 mngDaily 동일).
    final now = DateTime.now();
    final eligibility = _proxyEligibility(state.selectedDate, now);
    final canRegister = eligibility.canRegister;

    final sections = <Widget>[];
    void addGroups(List<_GroupedWorker> groups, String category) {
      for (final g in groups) {
        final rows = _registerUnregisteredOnly
            ? g.rows.where((r) => !r.attended).toList()
            : g.rows;
        if (rows.isEmpty) continue;
        // 미등록 1곳 이상이면 등록 버튼 노출. 조건(당일·17시 전) 미충족 시에도
        // 버튼은 노출하되 비활성 + 탭 시 사유 안내(A안 — 레거시 alert 동등).
        final hasUnregistered = g.rows.any((r) => !r.attended);
        sections.add(_RegisterPersonBand(
          name: g.employeeName,
          category: category,
          enabled: canRegister,
          onRegister: !hasUnregistered
              ? null
              : (canRegister
                  ? () => _openProxyRegisterSheet(g)
                  : () => _showProxyBlockedReason(eligibility)),
        ));
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
        // B안: 현재 대리출근 등록 가능 여부/사유 안내 배너.
        _RegisterEligibilityBanner(eligibility: eligibility, now: now),
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

  /// 대리출근 불가 사유 안내 (A안 — 레거시 mngDaily alert 동등).
  /// 날짜≠오늘 / 17시 이후일 때 비활성 등록 버튼 탭 시 SnackBar 로 사유 노출.
  void _showProxyBlockedReason(_ProxyEligibility eligibility) {
    final reason = eligibility.reason;
    if (reason == null) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(reason)),
    );
  }

  /// 대리출근 등록 시트 — 해당 여사원의 미등록 거래처를 선택해 일괄 등록.
  /// 레거시 mngDaily addSchedule(체크박스 목록) 동등.
  void _openProxyRegisterSheet(_GroupedWorker group) {
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
        builder: (_) => _ProxyRegisterSheet(
          employeeName: group.employeeName,
          accounts: unregistered,
        ),
      );
      if (selected == null || selected.isEmpty || !mounted) return;

      final notifier = ref.read(leaderDailyStatusProvider.notifier);
      String? firstError;
      var successCount = 0;
      for (final r in selected) {
        final err = await notifier.registerProxyAttendance(
          targetEmployeeId: employeeId,
          scheduleId: r.displayWorkScheduleId == null ? r.scheduleId : null,
          displayWorkScheduleId: r.displayWorkScheduleId,
        );
        if (err != null) {
          firstError = err;
          break;
        }
        successCount++;
      }
      if (!mounted) return;
      final messenger = ScaffoldMessenger.of(context);
      messenger.showSnackBar(SnackBar(
        content: Text(firstError ??
            '$successCount건 대리출근 등록이 완료되었습니다.'),
      ));
    });
  }

  /// 행사 일정 변경/삭제 흐름 (레거시 scheduleChangePromo).
  ///
  /// 그룹의 행사 배정이 여러 건이면 대상 선택 → 변경 시트(담당 여사원/날짜 재배정 + 삭제) → 처리 후 재조회.
  void _openEventChangeFlow(_GroupedWorker group) {
    throttledTapAsync(() async {
      final rows = group.rows;
      if (rows.isEmpty) return;

      // 변경 대상 행사 배정 선택 (단건이면 바로).
      LeaderDailyWorker target;
      if (rows.length == 1) {
        target = rows.first;
      } else {
        final picked = await showModalBottomSheet<LeaderDailyWorker>(
          context: context,
          shape: const RoundedRectangleBorder(
            borderRadius:
                BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
          ),
          builder: (_) => _EventTargetPicker(
            employeeName: group.employeeName,
            rows: rows,
          ),
        );
        if (picked == null || !mounted) return;
        target = picked;
      }

      final result = await showModalBottomSheet<_EventChangeResult>(
        context: context,
        isScrollControlled: true,
        shape: const RoundedRectangleBorder(
          borderRadius:
              BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
        ),
        builder: (_) => _EventChangeSheet(
          employeeName: group.employeeName,
          row: target,
          initialDate: ref.read(leaderDailyStatusProvider).selectedDate,
        ),
      );
      if (result == null || !mounted) return;

      final notifier = ref.read(leaderDailyStatusProvider.notifier);
      final messenger = ScaffoldMessenger.of(context);
      String? err;
      String okMessage;
      if (result.delete) {
        err = await notifier.deleteEventAssignment(target.scheduleId);
        okMessage = '행사 일정이 삭제되었습니다.';
      } else {
        err = await notifier.changeEventAssignment(
          scheduleId: target.scheduleId,
          targetEmployeeId: result.targetEmployeeId!,
          workingDate: result.workingDate!,
        );
        okMessage = '행사 일정이 변경되었습니다.';
      }
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(err ?? okMessage)));
    });
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
          for (final g in displayGroups)
            _WorkerCard(
              worker: g,
              isEvent: false,
            ),
          for (final g in eventGroups)
            _WorkerCard(
              worker: g,
              isEvent: true,
              onChange: g.attended ? null : () => _openEventChangeFlow(g),
            ),
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
    return groups
        .map((g) => _WorkerCard(
              worker: g,
              isEvent: isEvent,
              // 행사 + 미출근(변경 가능)일 때만 변경 흐름 진입. 진열은 조회 전용(레거시 정합).
              onChange: (isEvent && !g.attended) ? () => _openEventChangeFlow(g) : null,
            ))
        .toList();
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
                      fontWeight: FontWeight.w700,
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
            fontWeight: FontWeight.w700,
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

/// 대리출근 등록 가능 여부 (레거시 mngDaily 가드: 당일 + 17시 이전).
enum _ProxyEligibility { ok, notToday, afterCutoff }

/// 선택 날짜·현재 시각으로 대리출근 가능 여부 판정 (레거시 mngDaily `btn-add-sch` 동등).
_ProxyEligibility _proxyEligibility(DateTime selectedDate, DateTime now) {
  final isToday = selectedDate.year == now.year &&
      selectedDate.month == now.month &&
      selectedDate.day == now.day;
  if (!isToday) return _ProxyEligibility.notToday;
  if (now.hour >= 17) return _ProxyEligibility.afterCutoff;
  return _ProxyEligibility.ok;
}

extension _ProxyEligibilityX on _ProxyEligibility {
  bool get canRegister => this == _ProxyEligibility.ok;

  /// 불가 사유 (레거시 alert 문구 정합). ok 면 null.
  String? get reason {
    switch (this) {
      case _ProxyEligibility.ok:
        return null;
      case _ProxyEligibility.notToday:
        return '당일 일정만 대리출근 등록할 수 있습니다.';
      case _ProxyEligibility.afterCutoff:
        return '오후 5시 이후에는 대리출근 등록할 수 없습니다.';
    }
  }
}

/// 등록 탭 상단 안내 배너 (B안) — 현재 대리출근 등록 가능 여부/사유를 한 줄로 표시.
/// 가능: 네이비 + "오늘 HH:mm · 17:00까지 등록 가능", 불가: 주황 + 사유.
class _RegisterEligibilityBanner extends StatelessWidget {
  final _ProxyEligibility eligibility;
  final DateTime now;

  const _RegisterEligibilityBanner({
    required this.eligibility,
    required this.now,
  });

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

/// 등록 탭 사람별 밴드 — "이름 (진열/행사)" + (조건부) 등록 버튼 (레거시 list_title + 등록).
class _RegisterPersonBand extends StatelessWidget {
  final String name;
  final String category;

  /// 등록 가능(활성) 여부. false 면 회색 비활성 스타일(탭 시 사유 안내).
  final bool enabled;

  /// 등록 버튼 탭 콜백. null 이면 버튼 미노출(미등록 0곳).
  /// 비활성(enabled=false)이어도 탭은 가능 — 사유 안내용(A안).
  final VoidCallback? onRegister;

  const _RegisterPersonBand({
    required this.name,
    required this.category,
    this.enabled = true,
    this.onRegister,
  });

  @override
  Widget build(BuildContext context) {
    final borderColor = enabled ? AppColors.secondary : AppColors.border;
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
              borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.xxs,
                ),
                decoration: BoxDecoration(
                  color: AppColors.white,
                  borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
                  border: Border.all(color: borderColor),
                ),
                child: Text(
                  '등록',
                  style: AppTypography.labelSmall.copyWith(
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

/// 대리출근 등록 바텀시트 — 여사원의 미등록 거래처 다중 선택(기본 전체 선택) 후 등록.
class _ProxyRegisterSheet extends StatefulWidget {
  final String employeeName;
  final List<LeaderDailyWorker> accounts;

  const _ProxyRegisterSheet({
    required this.employeeName,
    required this.accounts,
  });

  @override
  State<_ProxyRegisterSheet> createState() => _ProxyRegisterSheetState();
}

class _ProxyRegisterSheetState extends State<_ProxyRegisterSheet> {
  late final Set<int> _selected; // accounts 인덱스 집합

  @override
  void initState() {
    super.initState();
    // 기본 전체 선택 (레거시 addSchedule 미등록 항목 일괄).
    _selected = {for (var i = 0; i < widget.accounts.length; i++) i};
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${widget.employeeName} 대리출근 등록',
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            Text(
              '등록할 거래처를 선택하세요. 안전점검 미완료 거래처는 등록되지 않습니다.',
              style: AppTypography.bodySmall
                  .copyWith(color: AppColors.textSecondary),
            ),
            const SizedBox(height: AppSpacing.sm),
            Flexible(
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: widget.accounts.length,
                itemBuilder: (context, i) {
                  final a = widget.accounts[i];
                  final checked = _selected.contains(i);
                  return CheckboxListTile(
                    value: checked,
                    activeColor: AppColors.secondary,
                    contentPadding: EdgeInsets.zero,
                    controlAffinity: ListTileControlAffinity.leading,
                    title: Text(
                      a.accountName,
                      style: AppTypography.bodyMedium
                          .copyWith(color: AppColors.textPrimary),
                    ),
                    subtitle: a.workCategoryLabel.isEmpty
                        ? null
                        : Text(
                            a.workCategoryLabel,
                            style: AppTypography.bodySmall
                                .copyWith(color: AppColors.textSecondary),
                          ),
                    onChanged: (v) => setState(() {
                      if (v == true) {
                        _selected.add(i);
                      } else {
                        _selected.remove(i);
                      }
                    }),
                  );
                },
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.secondary,
                  foregroundColor: AppColors.white,
                  padding:
                      const EdgeInsets.symmetric(vertical: AppSpacing.md),
                ),
                onPressed: _selected.isEmpty
                    ? null
                    : () => Navigator.of(context).pop(
                          [for (final i in _selected) widget.accounts[i]],
                        ),
                child: Text('등록 (${_selected.length})'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 행사 변경 시트 결과 — 변경(담당자/날짜) 또는 삭제.
class _EventChangeResult {
  final bool delete;
  final int? targetEmployeeId;
  final DateTime? workingDate;

  const _EventChangeResult.change(this.targetEmployeeId, this.workingDate)
      : delete = false;
  const _EventChangeResult.delete()
      : delete = true,
        targetEmployeeId = null,
        workingDate = null;
}

/// 변경 대상 행사 배정 선택 시트 (그룹에 행사 배정 2건 이상일 때).
class _EventTargetPicker extends StatelessWidget {
  final String employeeName;
  final List<LeaderDailyWorker> rows;

  const _EventTargetPicker({required this.employeeName, required this.rows});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '$employeeName 변경할 행사 선택',
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.sm),
            ...rows.map((r) => ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: Text(
                    r.accountName,
                    style: AppTypography.bodyMedium
                        .copyWith(color: AppColors.textPrimary),
                  ),
                  subtitle: r.workCategoryLabel.isEmpty
                      ? null
                      : Text(
                          r.workCategoryLabel,
                          style: AppTypography.bodySmall
                              .copyWith(color: AppColors.textSecondary),
                        ),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.of(context).pop(r),
                )),
          ],
        ),
      ),
    );
  }
}

/// 행사 일정 변경 시트 — 거래처/행사 readonly + 담당 여사원 드롭다운 + 날짜 피커 + 삭제.
/// 레거시 scheduleChangePromo: 담당 여사원·투입일만 변경 가능, 거래처/근무유형은 표시만.
class _EventChangeSheet extends ConsumerStatefulWidget {
  final String employeeName;
  final LeaderDailyWorker row;
  final DateTime initialDate;

  const _EventChangeSheet({
    required this.employeeName,
    required this.row,
    required this.initialDate,
  });

  @override
  ConsumerState<_EventChangeSheet> createState() => _EventChangeSheetState();
}

class _EventChangeSheetState extends ConsumerState<_EventChangeSheet> {
  int? _selectedEmployeeId;
  late DateTime _date;

  @override
  void initState() {
    super.initState();
    _selectedEmployeeId = widget.row.employeeId;
    _date = widget.initialDate;
    // 담당 여사원 드롭다운 옵션 로드 (팀원 목록).
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final notifier = ref.read(leaderTeamMembersProvider.notifier);
      if (!ref.read(leaderTeamMembersProvider).hasLoaded) {
        notifier.load();
      }
    });
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(_date.year - 2, 1, 1),
      lastDate: DateTime(_date.year + 2, 12, 31),
      helpText: '투입일 선택',
      cancelText: '취소',
      confirmText: '확인',
    );
    if (picked != null) setState(() => _date = picked);
  }

  Future<void> _confirmDelete() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('행사 일정 삭제'),
        content: const Text('이 행사 배정을 삭제하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('삭제', style: TextStyle(color: AppColors.error)),
          ),
        ],
      ),
    );
    if (ok == true && mounted) {
      Navigator.of(context).pop(const _EventChangeResult.delete());
    }
  }

  @override
  Widget build(BuildContext context) {
    final membersState = ref.watch(leaderTeamMembersProvider);
    final members = membersState.members;
    final label =
        DateFormat('yyyy년 MM월 dd일(E)', 'ko_KR').format(_date);

    // 드롭다운 값이 목록에 없으면(예: 로드 전) null 로 보정.
    final dropdownValue =
        members.any((m) => m.id == _selectedEmployeeId) ? _selectedEmployeeId : null;

    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(
          left: AppSpacing.lg,
          right: AppSpacing.lg,
          top: AppSpacing.lg,
          bottom: MediaQuery.of(context).viewInsets.bottom + AppSpacing.lg,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '행사 일정 변경',
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: FontWeight.w700,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: AppSpacing.md),
            // 거래처/행사 — readonly (행사 마스터 파생).
            _ReadonlyField(
              label: '거래처',
              value: widget.row.workCategoryLabel.isEmpty
                  ? widget.row.accountName
                  : '${widget.row.accountName} | ${widget.row.workCategoryLabel}',
            ),
            const SizedBox(height: AppSpacing.md),
            // 담당 여사원 — 변경 가능.
            Text('담당 여사원',
                style: AppTypography.bodySmall
                    .copyWith(color: AppColors.textSecondary)),
            const SizedBox(height: AppSpacing.xxs),
            if (membersState.isLoading && members.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: AppSpacing.sm),
                child: LoadingIndicator(message: '팀원 불러오는 중...'),
              )
            else
              DropdownButtonFormField<int>(
                initialValue: dropdownValue,
                isExpanded: true,
                decoration: InputDecoration(
                  isDense: true,
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: AppSpacing.md,
                    vertical: AppSpacing.sm,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                    borderSide: BorderSide(color: AppColors.border),
                  ),
                ),
                items: members
                    .map((m) => DropdownMenuItem<int>(
                          value: m.id,
                          child: Text('${m.name} (${m.employeeCode})'),
                        ))
                    .toList(),
                onChanged: (v) => setState(() => _selectedEmployeeId = v),
              ),
            const SizedBox(height: AppSpacing.md),
            // 투입일 — 변경 가능.
            Text('투입일',
                style: AppTypography.bodySmall
                    .copyWith(color: AppColors.textSecondary)),
            const SizedBox(height: AppSpacing.xxs),
            InkWell(
              onTap: _pickDate,
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.md,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
                  border: Border.all(color: AppColors.border),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Text(
                        label,
                        style: AppTypography.bodyMedium
                            .copyWith(color: AppColors.textPrimary),
                      ),
                    ),
                    const Icon(Icons.calendar_today, size: 18),
                  ],
                ),
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.error,
                      side: const BorderSide(color: AppColors.error),
                      padding:
                          const EdgeInsets.symmetric(vertical: AppSpacing.md),
                    ),
                    onPressed: _confirmDelete,
                    child: const Text('삭제'),
                  ),
                ),
                const SizedBox(width: AppSpacing.sm),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.secondary,
                      foregroundColor: AppColors.white,
                      padding:
                          const EdgeInsets.symmetric(vertical: AppSpacing.md),
                    ),
                    onPressed: dropdownValue == null
                        ? null
                        : () => Navigator.of(context).pop(
                              _EventChangeResult.change(_selectedEmployeeId, _date),
                            ),
                    child: const Text('저장'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// 시트 내 readonly 표시 필드 (라벨 + 값).
class _ReadonlyField extends StatelessWidget {
  final String label;
  final String value;

  const _ReadonlyField({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: AppTypography.bodySmall
                .copyWith(color: AppColors.textSecondary)),
        const SizedBox(height: AppSpacing.xxs),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.md,
          ),
          decoration: BoxDecoration(
            color: AppColors.surface,
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
          ),
          child: Text(
            value,
            style: AppTypography.bodyMedium.copyWith(color: AppColors.textPrimary),
          ),
        ),
      ],
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
                    fontWeight: FontWeight.w700,
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
              fontWeight: FontWeight.w700,
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
/// - 진열([isEvent]=false): 출근 상태 배지(등록완료/미등록). 레거시처럼 변경/추가 진입점 없음(조회 전용).
/// - 행사([isEvent]=true): 변경/변경불가 버튼(출근 후 변경불가).
class _WorkerCard extends StatelessWidget {
  final _GroupedWorker worker;
  final bool isEvent;

  /// 행사 변경 흐름 진입 콜백 (행사 카드·미출근일 때만 전달).
  final VoidCallback? onChange;

  const _WorkerCard({
    required this.worker,
    required this.isEvent,
    this.onChange,
  });

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
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${worker.employeeName} (${worker.employeeCode})',
                      style: AppTypography.bodyLarge.copyWith(
                        fontWeight: FontWeight.w700,
                        color: AppColors.textPrimary,
                      ),
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    // 담당 거래처별 1줄: "거래처명 | 진열/상시/순회" (조회 전용).
                    ...worker.rows.map((r) => _RowLine(row: r)),
                  ],
                ),
              ),
              const SizedBox(width: AppSpacing.sm),
              // 행사: 변경/변경불가 버튼, 진열: 등록완료/미등록 배지.
              if (isEvent)
                _ChangeButton(attended: worker.attended, onChange: onChange)
              else
                _AttendanceBadge(attended: worker.attended),
            ],
          ),
        ],
      ),
    );
  }
}

/// 카드 내 거래처 1줄 — "거래처명 | 진열/상시/순회" 텍스트 (조회 전용).
class _RowLine extends StatelessWidget {
  final LeaderDailyWorker row;

  const _RowLine({required this.row});

  @override
  Widget build(BuildContext context) {
    final text = row.workCategoryLabel.isEmpty
        ? row.accountName
        : '${row.accountName} | ${row.workCategoryLabel}';
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xxs),
      child: Text(
        text,
        style: AppTypography.bodyMedium.copyWith(color: AppColors.textPrimary),
      ),
    );
  }
}

/// 행사 근무자 변경/변경불가 버튼 (레거시 mngDaily commuteNullChk 정합).
///
/// - 출근등록 없음([attended]=false) → "변경"(활성). 탭 시 행사 일정변경 시트 진입.
/// - 출근등록 있음([attended]=true) → "변경불가"(비활성 라벨).
class _ChangeButton extends StatelessWidget {
  final bool attended;
  final VoidCallback? onChange;

  const _ChangeButton({required this.attended, this.onChange});

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
            fontWeight: FontWeight.w700,
          ),
        ),
      );
    }
    // 변경 — 활성. 탭 시 행사 일정변경 시트 진입.
    return InkWell(
      borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
      onTap: onChange,
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
            fontWeight: FontWeight.w700,
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
          fontWeight: FontWeight.w700,
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
          fontWeight: FontWeight.w700,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}
