import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_team_member.dart';
import '../../providers/leader_schedule_provider.dart';
import '../../widgets/common/error_view.dart';
import '../../widgets/common/loading_indicator.dart';
import 'leader_schedule_create_screen.dart';

/// 조장 — 본인 팀원 목록 화면 (Spec #554 P2-M §3.2).
class LeaderTeamMembersScreen extends ConsumerStatefulWidget {
  const LeaderTeamMembersScreen({super.key});

  @override
  ConsumerState<LeaderTeamMembersScreen> createState() =>
      _LeaderTeamMembersScreenState();
}

class _LeaderTeamMembersScreenState
    extends ConsumerState<LeaderTeamMembersScreen> with ThrottledTapMixin {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(leaderTeamMembersProvider.notifier).load();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(leaderTeamMembersProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('팀원 일정 관리'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: _buildBody(state),
    );
  }

  Widget _buildBody(LeaderTeamMembersState state) {
    if (state.isLoading && state.members.isEmpty) {
      return const LoadingIndicator(message: '팀원 목록을 불러오는 중...');
    }
    if (state.errorMessage != null && state.members.isEmpty) {
      return ErrorView(
        message: state.errorMessage!,
        onRetry: () => ref.read(leaderTeamMembersProvider.notifier).load(),
      );
    }
    if (state.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(AppSpacing.xl),
          child: Text(
            '본인 팀원이 없습니다.',
            style: AppTypography.bodyMedium,
          ),
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: () => ref.read(leaderTeamMembersProvider.notifier).load(),
      child: ListView.builder(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        itemCount: state.members.length,
        itemBuilder: (context, index) {
          final member = state.members[index];
          return _TeamMemberCard(
            member: member,
            onCreateTap: () => throttledTap(
              () => _navigateToCreateSchedule(member),
            ),
          );
        },
      ),
    );
  }

  void _navigateToCreateSchedule(LeaderTeamMember member) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => LeaderScheduleCreateScreen(targetMember: member),
      ),
    );
  }
}

/// 팀원 단일 카드 위젯.
class _TeamMemberCard extends StatelessWidget {
  final LeaderTeamMember member;
  final VoidCallback onCreateTap;

  const _TeamMemberCard({
    required this.member,
    required this.onCreateTap,
  });

  @override
  Widget build(BuildContext context) {
    final isInactive = member.isInactive;
    return Container(
      margin: const EdgeInsets.only(bottom: AppSpacing.md),
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.card,
        borderRadius: AppSpacing.cardBorderRadius,
        border: Border.all(color: AppColors.border),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${member.name} (${member.employeeCode})',
                  style: AppTypography.bodyLarge.copyWith(
                    fontWeight: FontWeight.w600,
                    color: isInactive
                        ? AppColors.textTertiary
                        : AppColors.textPrimary,
                  ),
                ),
                const SizedBox(height: AppSpacing.xs),
                Text(
                  member.status ?? '-',
                  style: AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ],
            ),
          ),
          ElevatedButton.icon(
            onPressed: isInactive ? null : onCreateTap,
            icon: const Icon(Icons.event_note, size: 18),
            label: const Text('일정 등록'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.otokiBlue,
              foregroundColor: AppColors.white,
            ),
          ),
        ],
      ),
    );
  }
}
