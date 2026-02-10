import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../providers/attendance_provider.dart';
import '../widgets/attendance/attendance_status_counter.dart';
import '../widgets/attendance/attendance_status_popup.dart';
import '../widgets/attendance/store_list_item.dart';
import '../widgets/attendance/store_search_bar.dart';
import '../widgets/attendance/work_type_selector.dart';
import '../widgets/common/primary_button.dart';
import '../../app_router.dart';

/// 출근등록 화면 (메인)
class AttendancePage extends ConsumerStatefulWidget {
  const AttendancePage({super.key});

  @override
  ConsumerState<AttendancePage> createState() => _AttendancePageState();
}

class _AttendancePageState extends ConsumerState<AttendancePage> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // 화면 진입 시 거래처 목록 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(attendanceProvider.notifier).loadStores();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
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

      // 등록 완료 시 완료 화면으로 이동
      if (next.registrationResult != null &&
          previous?.registrationResult == null) {
        AppRouter.navigateTo(context, AppRouter.attendanceComplete);
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
          if (state.totalCount > 0)
            Padding(
              padding: const EdgeInsets.only(right: 12),
              child: AttendanceStatusCounter(
                registeredCount: state.registeredCount,
                totalCount: state.totalCount,
                onTap: () async {
                  await notifier.loadAttendanceStatus();
                  if (context.mounted) {
                    AttendanceStatusPopup.show(
                      context,
                      statusList: state.statusList,
                      totalCount: state.totalCount,
                      registeredCount: state.registeredCount,
                    );
                  }
                },
              ),
            ),
        ],
      ),
      body: state.isLoading
          ? const Center(child: CircularProgressIndicator())
          : _buildBody(state, notifier),
      bottomNavigationBar: _buildBottomBar(state, notifier),
    );
  }

  Widget _buildBody(dynamic state, AttendanceNotifier notifier) {
    if (state.allStores.isEmpty && !state.isLoading) {
      return _buildEmptyState();
    }

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              // #1 근무유형 선택
              WorkTypeSelector(
                selectedWorkType: state.selectedWorkType,
                onChanged: notifier.selectWorkType,
              ),
              const SizedBox(height: 12),

              // #2 검색 바 (고정근무자는 숨김)
              if (!state.isFixedWorker)
                StoreSearchBar(
                  controller: _searchController,
                  onChanged: notifier.searchStores,
                ),
            ],
          ),
        ),

        // 거래처 리스트
        Expanded(
          child: state.filteredStores.isEmpty
              ? _buildNoSearchResult()
              : ListView.separated(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16),
                  itemCount: state.filteredStores.length,
                  separatorBuilder: (_, _) =>
                      const SizedBox(height: 8),
                  itemBuilder: (context, index) {
                    final store = state.filteredStores[index];
                    return StoreListItem(
                      store: store,
                      isSelected:
                          store.storeId == state.selectedStoreId,
                      onTap: () {
                        if (!store.isRegistered) {
                          notifier.selectStore(store.storeId);
                        }
                      },
                    );
                  },
                ),
        ),
      ],
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
            style: TextStyle(
              fontSize: 14,
              color: AppColors.textTertiary,
            ),
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
    // 고정근무자는 선택 불필요 - 항상 첫 번째 거래처 사용
    final canRegister = state.isFixedWorker
        ? state.allStores.isNotEmpty &&
            !state.allStores.first.isRegistered
        : state.selectedStoreId != null;

    if (state.allStores.isEmpty) return null;

    return Container(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(
          top: BorderSide(color: AppColors.border),
        ),
      ),
      child: PrimaryButton(
        text: '등록하기',
        isLoading: state.isRegistering,
        onPressed: canRegister && !state.isRegistering
            ? () {
                if (state.isFixedWorker &&
                    state.allStores.isNotEmpty) {
                  notifier.selectStore(state.allStores.first.storeId);
                }
                notifier.register();
              }
            : null,
      ),
    );
  }
}
