import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../providers/my_schedule_provider.dart';
import '../widgets/common/loading_indicator.dart';
import '../widgets/my_schedule/calendar_widget.dart';

/// 마이페이지 일정 캘린더 화면
///
/// 월간 일정을 캘린더 형태로 표시하고, 근무일을 선택하면 상세 화면으로 이동합니다.
class MyScheduleCalendarPage extends ConsumerStatefulWidget {
  const MyScheduleCalendarPage({super.key});

  @override
  ConsumerState<MyScheduleCalendarPage> createState() =>
      _MyScheduleCalendarPageState();
}

class _MyScheduleCalendarPageState
    extends ConsumerState<MyScheduleCalendarPage> {
  @override
  void initState() {
    super.initState();
    // 화면 진입 시마다 현재 표시 중인 월을 재조회한다.
    // (provider 가 autoDispose 가 아니라 재진입 시 자동 재생성되지 않으므로,
    //  다른 화면에서 일정이 변경된 경우를 반영하려면 명시적으로 갱신해야 한다.)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _refresh();
    });
  }

  Future<void> _refresh() {
    final notifier = ref.read(myScheduleCalendarProvider.notifier);
    final state = ref.read(myScheduleCalendarProvider);
    return notifier.loadMonthlySchedule(state.currentYear, state.currentMonth);
  }

  void _onPreviousMonth() {
    ref.read(myScheduleCalendarProvider.notifier).goToPreviousMonth();
  }

  void _onNextMonth() {
    ref.read(myScheduleCalendarProvider.notifier).goToNextMonth();
  }

  void _onDateTap(BuildContext context, DateTime date) {
    AppRouter.navigateTo(
      context,
      AppRouter.myScheduleDetail,
      arguments: date,
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(myScheduleCalendarProvider);

    // 에러 리스닝
    ref.listen(myScheduleCalendarProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('일정'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: Column(
        children: [
          // 월 네비게이션 헤더
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.lg,
              vertical: AppSpacing.md,
            ),
            decoration: BoxDecoration(
              color: AppColors.surface,
              border: Border(
                bottom: BorderSide(
                  color: AppColors.divider,
                  width: 1,
                ),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                // 연월 표시
                Text(
                  '${state.currentYear}년 ${state.currentMonth}월',
                  style: AppTypography.headlineSmall.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),

                // 이전/다음 월 버튼
                Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.chevron_left),
                      onPressed: state.isLoading ? null : _onPreviousMonth,
                      color: AppColors.secondary,
                    ),
                    const SizedBox(width: AppSpacing.xs),
                    IconButton(
                      icon: const Icon(Icons.chevron_right),
                      onPressed: state.isLoading ? null : _onNextMonth,
                      color: AppColors.secondary,
                    ),
                  ],
                ),
              ],
            ),
          ),

          // 캘린더 (아래로 당겨 새로고침)
          Expanded(
            child: RefreshIndicator(
              onRefresh: _refresh,
              child: state.isLoading
                  ? const _RefreshableCenter(child: LoadingIndicator())
                  : state.errorMessage != null
                      ? _RefreshableCenter(
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
                                onPressed: _refresh,
                                child: const Text('다시 시도'),
                              ),
                            ],
                          ),
                        )
                      : LayoutBuilder(
                          builder: (context, constraints) {
                            return SingleChildScrollView(
                              physics: const AlwaysScrollableScrollPhysics(),
                              child: ConstrainedBox(
                                constraints: BoxConstraints(
                                  minHeight: constraints.maxHeight,
                                ),
                                // CalendarWidget 내부 Spacer 가 스크롤 뷰(무한 높이)
                                // 안에서도 배치되도록 IntrinsicHeight 로 유한 높이를 강제한다.
                                child: IntrinsicHeight(
                                  child: CalendarWidget(
                                    year: state.currentYear,
                                    month: state.currentMonth,
                                    width: constraints.maxWidth,
                                    workDays: state.workDays,
                                    onDateTap: (date) =>
                                        _onDateTap(context, date),
                                    annualLeaveCount: state.annualLeaveCount,
                                  ),
                                ),
                              ),
                            );
                          },
                        ),
            ),
          ),
        ],
      ),
    );
  }
}

/// 로딩/에러 상태에서도 아래로 당겨 새로고침 제스처가 동작하도록,
/// 자식을 뷰포트 높이만큼 채워 중앙 정렬하는 스크롤 가능한 컨테이너.
class _RefreshableCenter extends StatelessWidget {
  const _RefreshableCenter({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          child: ConstrainedBox(
            constraints: BoxConstraints(minHeight: constraints.maxHeight),
            child: Center(child: child),
          ),
        );
      },
    );
  }
}
