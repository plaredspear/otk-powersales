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
class MyScheduleCalendarPage extends ConsumerWidget {
  const MyScheduleCalendarPage({super.key});

  void _onPreviousMonth(WidgetRef ref) {
    ref.read(myScheduleCalendarProvider.notifier).goToPreviousMonth();
  }

  void _onNextMonth(WidgetRef ref) {
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
  Widget build(BuildContext context, WidgetRef ref) {
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
                    fontWeight: FontWeight.w600,
                  ),
                ),

                // 이전/다음 월 버튼
                Row(
                  children: [
                    IconButton(
                      icon: const Icon(Icons.chevron_left),
                      onPressed: state.isLoading ? null : () => _onPreviousMonth(ref),
                      color: AppColors.secondary,
                    ),
                    const SizedBox(width: AppSpacing.xs),
                    IconButton(
                      icon: const Icon(Icons.chevron_right),
                      onPressed: state.isLoading ? null : () => _onNextMonth(ref),
                      color: AppColors.secondary,
                    ),
                  ],
                ),
              ],
            ),
          ),

          // 캘린더
          Expanded(
            child: state.isLoading
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
                                    .read(myScheduleCalendarProvider.notifier)
                                    .loadMonthlySchedule(
                                      state.currentYear,
                                      state.currentMonth,
                                    );
                              },
                              child: const Text('다시 시도'),
                            ),
                          ],
                        ),
                      )
                    : CalendarWidget(
                        year: state.currentYear,
                        month: state.currentMonth,
                        workDays: state.workDays,
                        onDateTap: (date) => _onDateTap(context, date),
                      ),
          ),
        ],
      ),
    );
  }
}
