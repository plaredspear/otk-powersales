import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/my_schedule_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/my_schedule/schedule_store_item.dart';

/// 마이페이지 일정 상세 화면
///
/// 선택한 날짜의 일정 정보를 표시하며, "일정" 및 "등록" 두 탭으로 구성됩니다.
class MyScheduleDetailPage extends ConsumerStatefulWidget {
  final DateTime selectedDate;

  const MyScheduleDetailPage({
    super.key,
    required this.selectedDate,
  });

  @override
  ConsumerState<MyScheduleDetailPage> createState() =>
      _MyScheduleDetailPageState();
}

class _MyScheduleDetailPageState extends ConsumerState<MyScheduleDetailPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);

    // 선택한 날짜의 일정 상세 데이터 로드
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(myScheduleDetailProvider.notifier)
          .loadDailySchedule(widget.selectedDate);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(myScheduleDetailProvider);

    // 에러 리스닝
    ref.listen(myScheduleDetailProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: Text(state.scheduleInfo?.memberName ?? '일정 상세'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '일정'),
            Tab(text: '등록'),
          ],
          labelColor: AppColors.otokiBlue,
          unselectedLabelColor: AppColors.textTertiary,
          indicatorColor: AppColors.otokiBlue,
          indicatorWeight: AppSpacing.tabIndicatorWeight,
        ),
      ),
      body: state.isLoading
          ? const Center(child: LoadingIndicator())
          : state.errorMessage != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        state.errorMessage!,
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.error,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: AppSpacing.md),
                      ElevatedButton(
                        onPressed: () {
                          ref
                              .read(myScheduleDetailProvider.notifier)
                              .loadDailySchedule(widget.selectedDate);
                        },
                        child: const Text('다시 시도'),
                      ),
                    ],
                  ),
                )
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildScheduleTab(state),
                    _buildRegistrationTab(state),
                  ],
                ),
    );
  }

  /// 일정 탭 콘텐츠
  Widget _buildScheduleTab(state) {
    if (state.scheduleInfo == null) {
      return const Center(child: Text('일정 정보가 없습니다'));
    }

    final info = state.scheduleInfo!;

    return Column(
      children: [
        // 날짜 및 보고 진행 정보
        Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: BoxDecoration(
            color: AppColors.surface,
            border: Border(
              bottom: BorderSide(color: AppColors.divider),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                info.date,
                style: AppTypography.headlineSmall,
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                '${info.reportProgress.completed} / ${info.reportProgress.total} 보고 완료 (${info.reportProgress.workType})',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),

        // 조원명 헤더
        Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm,
          ),
          color: AppColors.surface,
          child: Row(
            children: [
              Text(
                '${info.memberName} (${info.employeeNumber})',
                style: AppTypography.bodyMedium.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),

        // 거래처 목록
        Expanded(
          child: ListView.separated(
            itemCount: info.stores.length,
            separatorBuilder: (context, index) => Divider(
              height: 1,
              color: AppColors.divider,
            ),
            itemBuilder: (context, index) {
              final store = info.stores[index];
              return ScheduleStoreItem(
                storeName: store.storeName,
                workType1: store.workType1,
                workType2: store.workType2,
                workType3: store.workType3,
              );
            },
          ),
        ),
      ],
    );
  }

  /// 등록 탭 콘텐츠
  Widget _buildRegistrationTab(state) {
    if (state.scheduleInfo == null) {
      return const Center(child: Text('일정 정보가 없습니다'));
    }

    final info = state.scheduleInfo!;

    return Column(
      children: [
        // 날짜 및 보고 진행 정보
        Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: BoxDecoration(
            color: AppColors.surface,
            border: Border(
              bottom: BorderSide(color: AppColors.divider),
            ),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                info.date,
                style: AppTypography.headlineSmall,
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                '${info.reportProgress.completed} / ${info.reportProgress.total} 보고 완료 (${info.reportProgress.workType})',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
        ),

        // 등록 전 필터
        Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm,
          ),
          decoration: BoxDecoration(
            border: Border(
              bottom: BorderSide(color: AppColors.divider),
            ),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              Checkbox(
                value: state.showOnlyUnregistered,
                onChanged: (value) {
                  ref
                      .read(myScheduleDetailProvider.notifier)
                      .toggleUnregisteredFilter();
                },
                activeColor: AppColors.otokiBlue,
              ),
              Text(
                '등록 전',
                style: AppTypography.bodyMedium,
              ),
            ],
          ),
        ),

        // 조원명 헤더 (노란색 배경)
        Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm,
          ),
          color: AppColors.otokiYellow.withOpacity(0.3),
          child: Row(
            children: [
              Text(
                '${info.memberName} (${info.reportProgress.workType})',
                style: AppTypography.bodyMedium.copyWith(
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),

        // 거래처 목록 (필터 적용)
        Expanded(
          child: state.filteredStores.isEmpty
              ? Center(
                  child: Text(
                    state.showOnlyUnregistered
                        ? '등록 전 항목이 없습니다'
                        : '거래처가 없습니다',
                    style: AppTypography.bodyMedium.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                )
              : ListView.separated(
                  itemCount: state.filteredStores.length,
                  separatorBuilder: (context, index) => Divider(
                    height: 1,
                    color: AppColors.divider,
                  ),
                  itemBuilder: (context, index) {
                    final store = state.filteredStores[index];
                    return ScheduleStoreItem(
                      storeName: store.storeName,
                      workType1: store.workType1,
                      workType2: store.workType2,
                      workType3: store.workType3,
                      isRegistered: store.isRegistered,
                      showRegistrationStatus: true,
                    );
                  },
                ),
        ),
      ],
    );
  }
}
