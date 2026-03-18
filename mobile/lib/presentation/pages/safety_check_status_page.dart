import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/safety_check_status_provider.dart';
import '../providers/safety_check_status_state.dart';
import '../widgets/safety_check_status/safety_check_member_card.dart';

/// 안전점검 현황 화면 (조장/관리자용)
class SafetyCheckStatusPage extends ConsumerStatefulWidget {
  const SafetyCheckStatusPage({super.key});

  @override
  ConsumerState<SafetyCheckStatusPage> createState() =>
      _SafetyCheckStatusPageState();
}

class _SafetyCheckStatusPageState
    extends ConsumerState<SafetyCheckStatusPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(safetyCheckStatusProvider.notifier).fetchStatus();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(safetyCheckStatusProvider);

    ref.listen<String?>(
      safetyCheckStatusProvider.select((s) => s.errorMessage),
      (prev, next) {
        if (next != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('조회에 실패했습니다')),
          );
          ref.read(safetyCheckStatusProvider.notifier).clearError();
        }
      },
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('안전점검 현황'),
        backgroundColor: AppColors.background,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: RefreshIndicator(
        onRefresh: () async {
          await ref.read(safetyCheckStatusProvider.notifier).fetchStatus();
        },
        child: _buildBody(state),
      ),
    );
  }

  Widget _buildBody(SafetyCheckStatusState state) {
    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      slivers: [
        SliverToBoxAdapter(child: _buildDateNavigation(state)),
        SliverToBoxAdapter(child: _buildSummary(state)),
        if (state.isLoading && state.data == null)
          const SliverFillRemaining(
            child: Center(child: CircularProgressIndicator()),
          )
        else if (state.isEmpty)
          SliverFillRemaining(
            child: Center(
              child: Text(
                '해당 날짜에 근무 스케줄이 없습니다',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          )
        else ...[
          _buildMemberList(state.submittedMembers, state),
          _buildMemberList(state.notSubmittedMembers, state),
          const SliverToBoxAdapter(
            child: SizedBox(height: AppSpacing.xxxl),
          ),
        ],
      ],
    );
  }

  Widget _buildDateNavigation(SafetyCheckStatusState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
            icon: const Icon(Icons.chevron_left),
            onPressed: () {
              ref
                  .read(safetyCheckStatusProvider.notifier)
                  .goToPreviousDay();
            },
          ),
          Text(
            state.dateString,
            style: AppTypography.headlineMedium,
          ),
          IconButton(
            icon: const Icon(Icons.chevron_right),
            onPressed: () {
              ref.read(safetyCheckStatusProvider.notifier).goToNextDay();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildSummary(SafetyCheckStatusState state) {
    final data = state.data;
    if (data == null) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Text(
        '제출 ${data.submittedCount}명 / 미제출 ${data.notSubmittedCount}명  (총 ${data.totalCount}명)',
        style: AppTypography.bodyMedium.copyWith(
          color: AppColors.textSecondary,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }

  SliverList _buildMemberList(
    List members,
    SafetyCheckStatusState state,
  ) {
    return SliverList(
      delegate: SliverChildBuilderDelegate(
        (context, index) {
          final member = members[index];
          return SafetyCheckMemberCard(
            member: member,
            isExpanded: state.expandedCardIds.contains(member.id),
            onTap: () {
              ref
                  .read(safetyCheckStatusProvider.notifier)
                  .toggleCard(member.id);
            },
          );
        },
        childCount: members.length,
      ),
    );
  }
}
