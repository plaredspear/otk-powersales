import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/services/location_permission_helper.dart';
import '../../core/services/location_service.dart';
import '../../core/theme/app_colors.dart';
import '../../domain/entities/account_schedule_item.dart';
import '../providers/attendance_provider.dart';
import '../providers/attendance_state.dart';
import '../widgets/attendance/attendance_status_counter.dart';
import '../widgets/attendance/attendance_status_popup.dart';
import '../widgets/attendance/account_list_item.dart';
import '../widgets/attendance/account_search_bar.dart';
import '../widgets/attendance/safety_check_required_banner.dart';
import '../widgets/common/primary_button.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../../app_router.dart';

/// 출근등록 화면 (메인)
class AttendancePage extends ConsumerStatefulWidget {
  const AttendancePage({super.key});

  @override
  ConsumerState<AttendancePage> createState() => _AttendancePageState();
}

class _AttendancePageState extends ConsumerState<AttendancePage>
    with ThrottledTapMixin {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 화면 진입 시 거래처 목록 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(attendanceProvider.notifier).loadAccounts();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  bool _isAccountSelected(AccountScheduleItem account, AttendanceState state) {
    if (state.selectedScheduleId == null) return false;
    if (account.source == 'master') {
      return state.selectedSource == 'master' &&
          account.displayWorkScheduleId == state.selectedScheduleId;
    }
    return state.selectedSource != 'master' &&
        account.scheduleId == state.selectedScheduleId;
  }

  /// 위치 획득 실패 사유별 안내 메시지
  static const Map<LocationFailureReason, String> _locationFailureMessages = {
    LocationFailureReason.cancelled:
        '출근등록에는 위치 정보가 필요합니다. 위치 권한을 허용해 주세요',
    LocationFailureReason.permissionDenied:
        '위치 권한이 거부되어 출근등록을 할 수 없습니다. 설정에서 위치 권한을 허용해 주세요',
    LocationFailureReason.serviceDisabled:
        '위치 서비스(GPS)가 꺼져 있습니다. 위치 서비스를 켠 후 다시 시도해 주세요',
    LocationFailureReason.positionUnavailable:
        '현재 위치를 확인할 수 없습니다. 위치 신호가 잘 잡히는 곳에서 다시 시도해 주세요',
  };

  void _showLocationFailureSnackBar(LocationFailureReason reason) {
    final messenger = ScaffoldMessenger.of(context);
    messenger.showSnackBar(
      SnackBar(
        content: Text(_locationFailureMessages[reason]!),
        backgroundColor: AppColors.error,
        behavior: SnackBarBehavior.floating,
        action: SnackBarAction(
          label: '확인',
          textColor: AppColors.white,
          onPressed: messenger.hideCurrentSnackBar,
        ),
      ),
    );
  }

  Future<void> _handleRegister(
    AttendanceNotifier notifier,
    dynamic state,
  ) async {
    // GPS 좌표 획득 — 실패 시 사유별 안내 후 등록 중단 (위치 필수)
    final locationService = ref.read(locationServiceProvider);
    final helper = LocationPermissionHelper(locationService);
    final location = await helper.ensurePermissionAndGetPosition(context);

    if (!mounted) return;

    if (!location.isSuccess) {
      _showLocationFailureSnackBar(location.failureReason!);
      return;
    }

    final result = await notifier.register(
      latitude: location.position!.latitude,
      longitude: location.position!.longitude,
    );

    if (!mounted) return;

    // 등록 성공 시에만 완료 화면으로 이동. 결과는 route argument 로 직접 전달해
    // provider state 변화에 의존하지 않는다 (검정 화면 방지).
    if (result != null) {
      AppRouter.navigateTo(
        context,
        AppRouter.attendanceComplete,
        arguments: result,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);

    // 에러 SnackBar 표시
    ref.listen(attendanceProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            backgroundColor: AppColors.error,
            behavior: SnackBarBehavior.floating,
            action: SnackBarAction(
              label: '확인',
              textColor: AppColors.white,
              onPressed: () => notifier.clearError(),
            ),
          ),
        );
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('출근등록'),
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios, size: 20),
          onPressed: () => AppRouter.goBack(context),
        ),
        actions: [
          if (state.totalCount > 0 && !state.isFixedWorker)
            Padding(
              padding: const EdgeInsets.only(right: 12),
              child: AttendanceStatusCounter(
                registeredCount: state.registeredCount,
                totalCount: state.totalCount,
                onTap: () => throttledTapAsync(() async {
                  await notifier.loadAttendanceStatus();
                  if (context.mounted) {
                    AttendanceStatusPopup.show(
                      context,
                      statusList: state.statusList,
                      totalCount: state.totalCount,
                      registeredCount: state.registeredCount,
                    );
                  }
                }),
              ),
            ),
        ],
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : _buildBody(state, notifier),
      // 고정 근무자는 거래처 목록 바로 아래에 버튼을 배치하므로 하단 고정 바를 두지 않는다.
      bottomNavigationBar: state.isFixedWorker
          ? null
          : _buildBottomBar(state, notifier),
    );
  }

  Widget _buildBody(dynamic state, AttendanceNotifier notifier) {
    if (state.allAccounts.isEmpty && !state.isLoading) {
      return _buildEmptyState();
    }

    return Column(
      children: [
        // 안전점검 미완료 배너
        if (!state.safetyCheckCompleted)
          SafetyCheckRequiredBanner(
            onNavigateToSafetyCheck: () => throttledTap(
              () => AppRouter.navigateTo(context, AppRouter.safetyCheck),
            ),
          ),

        if (!state.isFixedWorker)
          Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                // 검색 바
                AccountSearchBar(
                  controller: _searchController,
                  onChanged: notifier.searchAccounts,
                ),
              ],
            ),
          ),

        // 거래처 리스트
        // 고정 근무자는 거래처가 소수이므로 목록 높이만큼만 차지하고
        // 바로 아래에 등록 버튼을 이어 붙인다. 그 외에는 목록이 화면을 채우고
        // 버튼은 하단에 고정된다.
        if (state.isFixedWorker)
          _buildAccountList(state, notifier)
        else
          Expanded(child: _buildAccountList(state, notifier)),

        if (state.isFixedWorker) _buildRegisterButton(state, notifier),
      ],
    );
  }

  Widget _buildAccountList(dynamic state, AttendanceNotifier notifier) {
    if (state.filteredAccounts.isEmpty) return _buildNoSearchResult();

    return ListView.separated(
      shrinkWrap: state.isFixedWorker,
      physics: state.isFixedWorker
          ? const NeverScrollableScrollPhysics()
          : null,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      itemCount: state.filteredAccounts.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final account = state.filteredAccounts[index];
        return AccountListItem(
          account: account,
          isSelected: _isAccountSelected(account, state),
          onTap: () {
            if (!account.isRegistered) {
              notifier.selectAccount(account);
            }
          },
        );
      },
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.event_busy,
            size: 64,
            color: AppColors.textTertiary.withValues(alpha: 0.5),
          ),
          const SizedBox(height: 16),
          const Text(
            '오늘 출근할 거래처가 없습니다',
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w500,
              color: AppColors.textSecondary,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            '스케줄을 확인해 주세요',
            style: TextStyle(fontSize: 14, color: AppColors.textTertiary),
          ),
        ],
      ),
    );
  }

  Widget _buildNoSearchResult() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.search_off,
            size: 48,
            color: AppColors.textTertiary.withValues(alpha: 0.5),
          ),
          const SizedBox(height: 12),
          const Text(
            '검색 결과가 없습니다',
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w500,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }

  Widget? _buildBottomBar(dynamic state, AttendanceNotifier notifier) {
    if (state.allAccounts.isEmpty) return null;

    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
        decoration: const BoxDecoration(
          color: AppColors.white,
          border: Border(top: BorderSide(color: AppColors.border)),
        ),
        child: _buildRegisterButton(state, notifier),
      ),
    );
  }

  /// 등록하기 버튼. 고정 근무자는 거래처 목록 바로 아래에, 그 외에는 하단 고정 바에서 사용.
  Widget _buildRegisterButton(dynamic state, AttendanceNotifier notifier) {
    final canRegister =
        state.selectedScheduleId != null && state.safetyCheckCompleted;

    final button = PrimaryButton(
      text: '등록하기',
      isLoading: state.isRegistering,
      onPressed: canRegister && !state.isRegistering
          ? () => throttledTapAsync(() => _handleRegister(notifier, state))
          : null,
    );

    // 목록 바로 아래에 붙는 인라인 배치일 때만 상하 여백을 준다.
    if (state.isFixedWorker) {
      return Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
        child: button,
      );
    }
    return button;
  }
}
