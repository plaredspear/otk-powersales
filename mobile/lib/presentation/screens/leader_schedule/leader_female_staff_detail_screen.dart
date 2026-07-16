import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/error_utils.dart';
import '../../../core/utils/throttled_tap_mixin.dart';
import '../../../domain/entities/leader_team_member.dart';
import '../../providers/leader_schedule_provider.dart';

/// 조장 — 여사원 상세 화면.
///
/// 여사원 기본 정보 + 계정 상태(단말/앱 로그인) 표시 및
/// **비밀번호 초기화 / 단말 초기화** 실행 (레거시 SF `EmployeePasswordReset` /
/// `EmployeeUUIDReset` Quick Action 을 신규 모바일 조장 경로로 이관).
///
/// 초기화 대상 검증(조장 권한 + 본인 지점 소속)은 서버가 강제한다.
/// 앱 로그인 비활성(loginActive=false) 사원은 서버가 초기화를 거부하므로 버튼을 비활성화한다.
class LeaderFemaleStaffDetailScreen extends ConsumerStatefulWidget {
  final LeaderTeamMember member;

  const LeaderFemaleStaffDetailScreen({super.key, required this.member});

  @override
  ConsumerState<LeaderFemaleStaffDetailScreen> createState() =>
      _LeaderFemaleStaffDetailScreenState();
}

class _LeaderFemaleStaffDetailScreenState
    extends ConsumerState<LeaderFemaleStaffDetailScreen> with ThrottledTapMixin {
  /// 단말 초기화 성공 시 로컬로 갱신(뱃지 즉시 반영).
  late bool _deviceBound = widget.member.deviceBound;

  /// 초기화가 한 번이라도 실행되면 목록 갱신을 위해 true 로 pop.
  bool _changed = false;

  bool _busy = false;

  LeaderTeamMember get member => widget.member;

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: true,
      onPopInvokedWithResult: (didPop, _) {},
      child: Scaffold(
        backgroundColor: AppColors.background,
        appBar: AppBar(
          title: const Text('여사원 상세'),
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: () => Navigator.of(context).pop(_changed),
          ),
        ),
        body: SafeArea(
          child: ListView(
            padding: const EdgeInsets.all(AppSpacing.lg),
            children: [
              _infoCard(),
              const SizedBox(height: AppSpacing.lg),
              _statusCard(),
              const SizedBox(height: AppSpacing.xl),
              _actionSection(),
            ],
          ),
        ),
      ),
    );
  }

  // ── 기본 정보 ──────────────────────────────────────────────
  Widget _infoCard() {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            member.name,
            style: AppTypography.headlineSmall.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: AppSpacing.xs),
          _infoRow('사원번호', member.employeeCode),
          _infoRow('재직상태', member.status ?? '-'),
          _infoRow('연락처',
              member.hasPhone ? member.phone!.trim() : '등록된 연락처 없음'),
        ],
      ),
    );
  }

  Widget _infoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(
              label,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: AppTypography.bodyMedium.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ── 계정 상태 ──────────────────────────────────────────────
  Widget _statusCard() {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '계정 상태',
            style: AppTypography.bodyLarge.copyWith(
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: AppSpacing.sm),
          Wrap(
            spacing: AppSpacing.sm,
            runSpacing: AppSpacing.sm,
            children: [
              _badge(
                label: member.loginActive ? '앱 로그인 활성' : '앱 로그인 비활성',
                color: member.loginActive ? AppColors.success : AppColors.error,
              ),
              _badge(
                label: _deviceBound ? '단말 등록됨' : '단말 미등록',
                color: _deviceBound ? AppColors.otokiBlue : AppColors.textSecondary,
              ),
            ],
          ),
          if (!member.loginActive) ...[
            const SizedBox(height: AppSpacing.sm),
            Text(
              '앱 로그인이 비활성 상태인 사원은 초기화할 수 없습니다.',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.error,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _badge({required String label, required Color color}) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.xs,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Text(
        label,
        style: AppTypography.bodySmall.copyWith(
          color: color,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }

  // ── 초기화 액션 ────────────────────────────────────────────
  Widget _actionSection() {
    final enabled = member.loginActive && !_busy;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          '계정 관리',
          style: AppTypography.bodyLarge.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        _actionButton(
          icon: Icons.lock_reset,
          label: '비밀번호 초기화',
          description: "임시 비밀번호 'pwrs1234!' 로 초기화합니다.",
          enabled: enabled,
          onTap: _onResetPassword,
        ),
        const SizedBox(height: AppSpacing.md),
        _actionButton(
          icon: Icons.phonelink_erase,
          label: '단말 초기화',
          description: '기기 등록을 해제해 다른 단말로 로그인할 수 있게 합니다.',
          enabled: enabled,
          onTap: _onResetDevice,
        ),
      ],
    );
  }

  Widget _actionButton({
    required IconData icon,
    required String label,
    required String description,
    required bool enabled,
    required VoidCallback onTap,
  }) {
    return Opacity(
      opacity: enabled ? 1.0 : 0.5,
      child: _Card(
        onTap: enabled ? () => throttledTap(onTap) : null,
        child: Row(
          children: [
            Icon(icon, color: AppColors.otokiBlue),
            const SizedBox(width: AppSpacing.md),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: AppTypography.bodyLarge.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xxs),
                  Text(
                    description,
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: AppColors.textSecondary),
          ],
        ),
      ),
    );
  }

  Future<void> _onResetPassword() async {
    final ok = await _confirm(
      title: '비밀번호 초기화',
      message:
          "${member.name}(${member.employeeCode})님의 비밀번호를 임시 비밀번호 'pwrs1234!' 로 초기화합니다.\n"
          '사원은 다음 로그인 시 비밀번호 변경을 요구받습니다.',
    );
    if (ok != true) return;
    await _run(
      action: () => ref
          .read(leaderScheduleRepositoryProvider)
          .resetTeamMemberPassword(member.id),
      successMessage:
          "비밀번호가 초기화되었습니다. 임시 비밀번호 'pwrs1234!' 를 사원에게 전달해 주세요.",
    );
  }

  Future<void> _onResetDevice() async {
    final ok = await _confirm(
      title: '단말 초기화',
      message: '${member.name}(${member.employeeCode})님의 단말 등록을 초기화합니다.\n'
          '처리 후 사원이 다시 로그인하면 새 단말로 자동 등록됩니다.',
    );
    if (ok != true) return;
    await _run(
      action: () => ref
          .read(leaderScheduleRepositoryProvider)
          .resetTeamMemberDevice(member.id),
      successMessage: '단말이 초기화되었습니다. 사원이 다음 로그인 시 새 단말로 등록됩니다.',
      onSuccess: () => setState(() => _deviceBound = false),
    );
  }

  Future<bool?> _confirm({required String title, required String message}) {
    return showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('취소'),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('초기화'),
          ),
        ],
      ),
    );
  }

  Future<void> _run({
    required Future<void> Function() action,
    required String successMessage,
    VoidCallback? onSuccess,
  }) async {
    setState(() => _busy = true);
    try {
      await action();
      _changed = true;
      onSuccess?.call();
      if (!mounted) return;
      _snack(successMessage, isError: false);
    } catch (e) {
      if (!mounted) return;
      _snack(extractErrorMessage(e), isError: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _snack(String message, {required bool isError}) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: isError ? AppColors.error : AppColors.success,
          duration: const Duration(seconds: 3),
        ),
      );
  }
}

/// 공통 카드 컨테이너.
class _Card extends StatelessWidget {
  final Widget child;
  final VoidCallback? onTap;

  const _Card({required this.child, this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(AppSpacing.lg),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppColors.border),
          ),
          child: child,
        ),
      ),
    );
  }
}
