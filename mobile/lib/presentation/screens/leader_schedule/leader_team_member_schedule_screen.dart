import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/leader_team_member.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/error_view.dart';
import '../../widgets/common/loading_indicator.dart';
import '../../widgets/leader_schedule/leader_schedule_calendar_grid.dart';
import 'leader_daily_status_screen.dart';

/// 조장 — 여사원 일정관리(월간 캘린더) 화면 (레거시 `employee/mgnSchedule.jsp` 정합).
///
/// 여사원 명단의 조원 행 또는 "일정 / 등록 관리" 버튼으로 진입하는 **공유 페이지**.
/// 상단 드롭다운("여사원 전체" + 조원 목록)으로 대상을 전환하며, 진입 시 선택 상태는
/// [initialEmployeeId] 로 결정된다(null = 여사원 전체). 일정 있는 날짜는 `출근완료수 / 전체수`
/// 를 표시하고, 탭하면 해당 날짜의 일별 현황으로 이동한다.
class LeaderTeamMemberScheduleScreen extends ConsumerStatefulWidget {
  /// 진입 시 선택할 조원 id. null 이면 "여사원 전체".
  final int? initialEmployeeId;

  const LeaderTeamMemberScheduleScreen({super.key, this.initialEmployeeId});

  @override
  ConsumerState<LeaderTeamMemberScheduleScreen> createState() =>
      _LeaderTeamMemberScheduleScreenState();
}

class _LeaderTeamMemberScheduleScreenState
    extends ConsumerState<LeaderTeamMemberScheduleScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // 드롭다운용 조원 목록 보장.
      final membersState = ref.read(leaderTeamMembersProvider);
      if (!membersState.hasLoaded && !membersState.isLoading) {
        ref.read(leaderTeamMembersProvider.notifier).load();
      }
      ref
          .read(leaderScheduleCalendarProvider.notifier)
          .init(widget.initialEmployeeId);
    });
  }

  /// 선택된 조원명(검색어용). 전체 모드면 null.
  String? _selectedMemberName(int? employeeId, List<LeaderTeamMember> members) {
    if (employeeId == null) return null;
    for (final m in members) {
      if (m.id == employeeId) return m.name;
    }
    return null;
  }

  /// 선택 표시 라벨. 전체 모드면 "여사원 전체".
  String _selectedLabel(int? employeeId, List<LeaderTeamMember> members) {
    if (employeeId == null) return '여사원 전체';
    for (final m in members) {
      if (m.id == employeeId) return '${m.name}(${m.employeeCode})';
    }
    return '여사원 전체';
  }

  /// 거래처 선택과 동일한 BottomSheet 방식으로 조원 선택.
  Future<void> _showEmployeePicker(List<LeaderTeamMember> members) async {
    final current = ref.read(leaderScheduleCalendarProvider).selectedEmployeeId;
    final pick = await showModalBottomSheet<_EmployeePick>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius:
            BorderRadius.vertical(top: Radius.circular(AppSpacing.radiusLg)),
      ),
      builder: (_) => _EmployeePickerSheet(
        members: members,
        selectedEmployeeId: current,
      ),
    );
    if (pick != null) {
      ref
          .read(leaderScheduleCalendarProvider.notifier)
          .selectEmployee(pick.employeeId);
    }
  }

  void _onDateTap(DateTime date, int? selectedEmployeeId,
      List<LeaderTeamMember> members) {
    Navigator.of(context).push(
      MaterialPageRoute(
        // 조원 선택 상태면 단일 모드(레거시 mngDaily 개인 화면), 전체면 전체 모드.
        builder: (_) => LeaderDailyStatusScreen(
          initialDate: date,
          singleEmployeeId: selectedEmployeeId,
          singleEmployeeName:
              _selectedMemberName(selectedEmployeeId, members),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final notifier = ref.read(leaderScheduleCalendarProvider.notifier);
    final state = ref.watch(leaderScheduleCalendarProvider);
    final members = ref.watch(leaderTeamMembersProvider).members;

    ref.listen(
      leaderScheduleCalendarProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null && state.days.isNotEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(next)),
          );
        }
      },
    );

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('일정'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Column(
        children: [
          _EmployeeSelectorField(
            label: _selectedLabel(state.selectedEmployeeId, members),
            onTap:
                state.isLoading ? null : () => _showEmployeePicker(members),
          ),
          _MonthNavigator(
            year: state.year,
            month: state.month,
            onPrev: state.isLoading ? null : notifier.goToPreviousMonth,
            onNext: state.isLoading ? null : notifier.goToNextMonth,
          ),
          Expanded(
            child: _buildCalendar(state, notifier, members),
          ),
        ],
      ),
    );
  }

  Widget _buildCalendar(
    LeaderScheduleCalendarState state,
    LeaderScheduleCalendarNotifier notifier,
    List<LeaderTeamMember> members,
  ) {
    if (state.isLoading && state.days.isEmpty) {
      return const Center(child: LoadingIndicator(message: '월간 일정을 불러오는 중...'));
    }
    if (state.errorMessage != null && state.days.isEmpty) {
      return ErrorView(message: state.errorMessage!, onRetry: notifier.load);
    }
    return LeaderScheduleCalendarGrid(
      year: state.year,
      month: state.month,
      dayOf: state.dayOf,
      onDateTap: (date) =>
          _onDateTap(date, state.selectedEmployeeId, members),
    );
  }
}

/// 조원 선택 BottomSheet 결과 래퍼. null 반환(시트 닫힘)과 "여사원 전체"(employeeId=null)를 구분.
class _EmployeePick {
  final int? employeeId; // null = 여사원 전체
  const _EmployeePick(this.employeeId);
}

/// "여사원 전체" + 조원 선택 필드 (탭 → BottomSheet). 레거시 `<select id="sel-employee">`.
class _EmployeeSelectorField extends StatelessWidget {
  final String label;
  final VoidCallback? onTap;

  const _EmployeeSelectorField({required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.lg,
            vertical: AppSpacing.md,
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  label,
                  style: AppTypography.bodyLarge,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const Icon(Icons.arrow_drop_down),
            ],
          ),
        ),
      ),
    );
  }
}

/// 조원 선택 BottomSheet — 거래처 선택 시트와 동일 패턴(검색 + 리스트).
class _EmployeePickerSheet extends StatefulWidget {
  final List<LeaderTeamMember> members;
  final int? selectedEmployeeId;

  const _EmployeePickerSheet({
    required this.members,
    required this.selectedEmployeeId,
  });

  @override
  State<_EmployeePickerSheet> createState() => _EmployeePickerSheetState();
}

class _EmployeePickerSheetState extends State<_EmployeePickerSheet> {
  final TextEditingController _keywordController = TextEditingController();
  String _keyword = '';

  @override
  void dispose() {
    _keywordController.dispose();
    super.dispose();
  }

  List<LeaderTeamMember> get _filtered {
    final needle = _keyword.trim().toLowerCase();
    if (needle.isEmpty) return widget.members;
    return widget.members
        .where((m) =>
            m.name.toLowerCase().contains(needle) ||
            m.employeeCode.toLowerCase().contains(needle))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.7,
      maxChildSize: 0.9,
      builder: (_, scrollController) {
        final filtered = _filtered;
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
                onChanged: (v) => setState(() => _keyword = v),
                decoration: InputDecoration(
                  hintText: '이름, 사원번호 검색',
                  prefixIcon: const Icon(Icons.search),
                  border: OutlineInputBorder(
                    borderRadius: AppSpacing.cardBorderRadius,
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.md),
              Expanded(
                child: ListView.separated(
                  controller: scrollController,
                  itemCount: filtered.length + 1,
                  separatorBuilder: (_, index) => const Divider(height: 1),
                  itemBuilder: (_, index) {
                    if (index == 0) {
                      return ListTile(
                        title: const Text('여사원 전체'),
                        selected: widget.selectedEmployeeId == null,
                        onTap: () =>
                            Navigator.of(context).pop(const _EmployeePick(null)),
                      );
                    }
                    final m = filtered[index - 1];
                    return ListTile(
                      title: Text('${m.name}(${m.employeeCode})'),
                      selected: widget.selectedEmployeeId == m.id,
                      onTap: () =>
                          Navigator.of(context).pop(_EmployeePick(m.id)),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

/// 연/월 + 이전·다음 월 네비게이션.
class _MonthNavigator extends StatelessWidget {
  final int year;
  final int month;
  final VoidCallback? onPrev;
  final VoidCallback? onNext;

  const _MonthNavigator({
    required this.year,
    required this.month,
    required this.onPrev,
    required this.onNext,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      decoration: BoxDecoration(
        color: AppColors.surface,
        border: Border(bottom: BorderSide(color: AppColors.divider)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '$year년 $month월',
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          Row(
            children: [
              IconButton(
                icon: const Icon(Icons.chevron_left),
                color: AppColors.secondary,
                onPressed: onPrev,
              ),
              IconButton(
                icon: const Icon(Icons.chevron_right),
                color: AppColors.secondary,
                onPressed: onNext,
              ),
            ],
          ),
        ],
      ),
    );
  }
}
