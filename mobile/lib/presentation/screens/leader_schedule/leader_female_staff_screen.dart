import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_team_member.dart';
import '../../providers/auth_provider.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/error_view.dart';
import '../../widgets/common/loading_indicator.dart';
import 'leader_female_staff_detail_screen.dart';
import 'leader_team_member_schedule_screen.dart';

/// 조장 — 여사원(조원) 명단 화면.
///
/// 레거시 Heroku `/employee/main` (employee/main.jsp) parity:
/// - 지점명 + 인원수 헤더 + "일정 / 등록 관리" 버튼
/// - 이름/사원번호 검색 (로드된 목록 클라이언트 필터)
/// - 조원 목록: `이름(사번)` + (전화번호 있을 때) 전화 버튼
/// - 행 탭 / "일정 / 등록 관리" → 조원별 월간 일정 관리 화면
///
/// 레거시 실제 코드 정합: 출생년월/만 나이는 표시하지 않는다(JSP에서 주석 처리됨).
class LeaderFemaleStaffScreen extends ConsumerStatefulWidget {
  const LeaderFemaleStaffScreen({super.key});

  @override
  ConsumerState<LeaderFemaleStaffScreen> createState() =>
      _LeaderFemaleStaffScreenState();
}

class _LeaderFemaleStaffScreenState
    extends ConsumerState<LeaderFemaleStaffScreen> with ThrottledTapMixin {
  final _searchController = TextEditingController();
  String _keyword = '';

  /// 현재 펼쳐진(상세/일정 버튼 노출) 여사원 행의 id. 한 번에 하나만 펼쳐진다.
  int? _expandedId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(leaderTeamMembersProvider.notifier).load();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  /// 이름/사원번호 클라이언트 필터 (레거시 schTxt: name OR empcode LIKE).
  List<LeaderTeamMember> _filter(List<LeaderTeamMember> members) {
    final needle = _keyword.trim().toLowerCase();
    if (needle.isEmpty) return members;
    return members
        .where((m) =>
            m.name.toLowerCase().contains(needle) ||
            m.employeeCode.toLowerCase().contains(needle))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(leaderTeamMembersProvider);
    final orgName = ref.watch(authProvider).user?.orgName ?? '';

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('여사원'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      // 상단은 AppBar가 처리하므로 하단(홈 인디케이터) 안전영역만 적용.
      body: SafeArea(
        top: false,
        child: _buildBody(state, orgName),
      ),
    );
  }

  Widget _buildBody(LeaderTeamMembersState state, String orgName) {
    if (state.isLoading && state.members.isEmpty) {
      return const LoadingIndicator(message: '여사원 목록을 불러오는 중...');
    }
    if (state.errorMessage != null && state.members.isEmpty) {
      return ErrorView(
        message: state.errorMessage!,
        onRetry: () => ref.read(leaderTeamMembersProvider.notifier).load(),
      );
    }

    final filtered = _filter(state.members);

    return Column(
      children: [
        _Header(
          orgName: orgName,
          totalCount: state.members.length,
          onScheduleTap: () => throttledTap(() => _openCalendar(null)),
        ),
        _SearchBar(
          controller: _searchController,
          onChanged: (v) => setState(() => _keyword = v),
        ),
        Expanded(child: _buildList(state, filtered)),
      ],
    );
  }

  Widget _buildList(
    LeaderTeamMembersState state,
    List<LeaderTeamMember> filtered,
  ) {
    if (filtered.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(AppSpacing.xl),
          child: Text(
            state.members.isEmpty ? '여사원이 없습니다.' : '검색 결과가 없습니다.',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => ref.read(leaderTeamMembersProvider.notifier).load(),
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        itemCount: filtered.length + 1,
        separatorBuilder: (_, index) =>
            index == 0 ? const SizedBox.shrink() : const Divider(height: 1),
        itemBuilder: (context, index) {
          if (index == 0) {
            return _ListTitle(count: filtered.length);
          }
          final member = filtered[index - 1];
          return _FemaleStaffRow(
            member: member,
            isExpanded: _expandedId == member.id,
            onHeaderTap: () => setState(() {
              _expandedId = _expandedId == member.id ? null : member.id;
            }),
            onDetailTap: () => throttledTap(() => _openDetail(member)),
            onScheduleTap: () => throttledTap(() => _openCalendar(member.id)),
            onCallTap: () => _callMember(member),
          );
        },
      ),
    );
  }

  /// 여사원 일정관리(월간 캘린더) 페이지로 이동 (레거시 mgnSchedule).
  /// [employeeId] null = "여사원 전체"(일정/등록 관리 버튼), 지정 = 해당 조원 선택 상태.
  void _openCalendar(int? employeeId) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) =>
            LeaderTeamMemberScheduleScreen(initialEmployeeId: employeeId),
      ),
    );
  }

  /// 여사원 상세 화면으로 이동 (비밀번호/단말 초기화). 초기화 발생 시 목록 갱신.
  Future<void> _openDetail(LeaderTeamMember member) async {
    final changed = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => LeaderFemaleStaffDetailScreen(member: member),
      ),
    );
    if (changed == true && mounted) {
      ref.read(leaderTeamMembersProvider.notifier).load();
    }
  }

  Future<void> _callMember(LeaderTeamMember member) async {
    if (!member.hasPhone) return;
    final uri = Uri(scheme: 'tel', path: member.phone);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri);
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('전화 앱을 실행할 수 없습니다'),
          duration: Duration(seconds: 2),
        ),
      );
    }
  }
}

/// 지점명 + 인원수 + "일정 / 등록 관리" 헤더 (레거시 .top_info).
class _Header extends StatelessWidget {
  final String orgName;
  final int totalCount;
  final VoidCallback onScheduleTap;

  const _Header({
    required this.orgName,
    required this.totalCount,
    required this.onScheduleTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: AppColors.otokiBlue,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  orgName.isEmpty ? '-' : orgName,
                  style: AppTypography.headlineSmall.copyWith(
                    color: AppColors.white,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: AppSpacing.xxs),
                Row(
                  children: [
                    const Icon(Icons.person,
                        size: 16, color: AppColors.white),
                    const SizedBox(width: AppSpacing.xxs),
                    Text(
                      '$totalCount명',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.white,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          OutlinedButton(
            onPressed: onScheduleTap,
            style: OutlinedButton.styleFrom(
              backgroundColor: AppColors.white,
              foregroundColor: AppColors.otokiBlue,
              side: BorderSide.none,
              minimumSize: const Size(0, AppSpacing.buttonHeightSmall),
              padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
            ),
            child: const Text('일정 / 등록 관리'),
          ),
        ],
      ),
    );
  }
}

/// 이름/사원번호 검색 바 (레거시 search_top).
class _SearchBar extends StatelessWidget {
  final TextEditingController controller;
  final ValueChanged<String> onChanged;

  const _SearchBar({required this.controller, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.md,
        AppSpacing.lg,
        AppSpacing.sm,
      ),
      child: TextField(
        controller: controller,
        onChanged: onChanged,
        textInputAction: TextInputAction.search,
        decoration: InputDecoration(
          isDense: true,
          hintText: '이름, 사원번호 입력',
          prefixIcon: const Icon(Icons.search, size: 20),
          contentPadding: const EdgeInsets.symmetric(
            vertical: AppSpacing.sm,
          ),
          border: OutlineInputBorder(
            borderRadius: AppSpacing.inputBorderRadius,
          ),
        ),
      ),
    );
  }
}

/// "여사원 (N)" 목록 타이틀.
class _ListTitle extends StatelessWidget {
  final int count;

  const _ListTitle({required this.count});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.lg,
        AppSpacing.sm,
        AppSpacing.lg,
        AppSpacing.sm,
      ),
      child: Row(
        children: [
          Text(
            '여사원 ',
            style: AppTypography.bodyLarge.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          Text(
            '($count)',
            style: AppTypography.bodyLarge.copyWith(
              color: AppColors.error,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

/// 조원 1행 — `이름(사번)` + (전화번호 있을 때) 전화 버튼.
///
/// 헤더 탭 시 아코디언으로 펼쳐져 [상세]/[일정] 버튼을 노출한다.
/// - 상세: 여사원 상세(비밀번호/단말 초기화) 화면.
/// - 일정: 해당 조원 월간 일정 관리 화면(기존 동작).
class _FemaleStaffRow extends StatelessWidget {
  final LeaderTeamMember member;
  final bool isExpanded;
  final VoidCallback onHeaderTap;
  final VoidCallback onDetailTap;
  final VoidCallback onScheduleTap;
  final VoidCallback onCallTap;

  const _FemaleStaffRow({
    required this.member,
    required this.isExpanded,
    required this.onHeaderTap,
    required this.onDetailTap,
    required this.onScheduleTap,
    required this.onCallTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        InkWell(
          onTap: onHeaderTap,
          child: Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.md,
            ),
            child: Row(
              children: [
                AnimatedRotation(
                  turns: isExpanded ? 0.25 : 0,
                  duration: const Duration(milliseconds: 150),
                  child: const Icon(
                    Icons.chevron_right,
                    color: AppColors.textSecondary,
                  ),
                ),
                const SizedBox(width: AppSpacing.xs),
                Expanded(
                  child: Text(
                    '${member.name} (${member.employeeCode})',
                    style: AppTypography.bodyLarge.copyWith(
                      fontWeight: FontWeight.w700,
                      color: AppColors.textPrimary,
                    ),
                  ),
                ),
                if (member.hasPhone)
                  IconButton(
                    onPressed: onCallTap,
                    icon: const Icon(Icons.phone, color: AppColors.white),
                    style: IconButton.styleFrom(
                      backgroundColor: AppColors.success,
                      minimumSize: const Size(40, 40),
                    ),
                  ),
              ],
            ),
          ),
        ),
        if (isExpanded)
          Padding(
            padding: const EdgeInsets.fromLTRB(
              AppSpacing.lg,
              0,
              AppSpacing.lg,
              AppSpacing.md,
            ),
            child: Row(
              children: [
                Expanded(
                  child: FilledButton.icon(
                    onPressed: onDetailTap,
                    icon: const Icon(Icons.person_outline, size: 18),
                    label: const Text('상세'),
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.otokiBlue,
                      foregroundColor: AppColors.white,
                    ),
                  ),
                ),
                const SizedBox(width: AppSpacing.md),
                Expanded(
                  child: FilledButton.icon(
                    onPressed: onScheduleTap,
                    icon: const Icon(Icons.calendar_month, size: 18),
                    label: const Text('일정'),
                    style: FilledButton.styleFrom(
                      backgroundColor: AppColors.otokiBlue,
                      foregroundColor: AppColors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}
