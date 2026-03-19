import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../providers/alt_holiday_provider.dart';

/// 대체휴무 신청 페이지
class AltHolidayRequestPage extends ConsumerStatefulWidget {
  const AltHolidayRequestPage({super.key});

  @override
  ConsumerState<AltHolidayRequestPage> createState() =>
      _AltHolidayRequestPageState();
}

class _AltHolidayRequestPageState
    extends ConsumerState<AltHolidayRequestPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final year = DateTime.now().year;
      ref.read(altHolidayRequestProvider.notifier).loadHolidays(year);
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(altHolidayRequestProvider);
    final notifier = ref.read(altHolidayRequestProvider.notifier);

    ref.listen<AltHolidayRequestState>(
      altHolidayRequestProvider,
      (prev, next) {
        if (next.errorMessage != null) {
          showDialog(
            context: context,
            builder: (ctx) => AlertDialog(
              title: const Text('신청 실패'),
              content: Text(next.errorMessage!),
              actions: [
                TextButton(
                  onPressed: () {
                    Navigator.of(ctx).pop();
                    notifier.clearError();
                  },
                  child: const Text('확인'),
                ),
              ],
            ),
          );
        }
        if (next.isSubmitted) {
          showDialog(
            context: context,
            barrierDismissible: false,
            builder: (ctx) => AlertDialog(
              title: const Text('신청 완료'),
              content: const Text('대체휴무가 신청되었습니다.'),
              actions: [
                TextButton(
                  onPressed: () {
                    Navigator.of(ctx).pop();
                    Navigator.of(context).pushReplacementNamed(
                      AppRouter.altHolidayHistory,
                    );
                  },
                  child: const Text('확인'),
                ),
              ],
            ),
          );
        }
      },
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('대체휴무 신청'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _DateField(
              label: '대체휴무 대상일',
              hint: '주말 또는 공휴일만 선택 가능',
              selectedDate: state.actualWorkDate,
              onTap: () => _selectActualWorkDate(context, state, notifier),
            ),
            const SizedBox(height: AppSpacing.lg),
            _DateField(
              label: '대체휴무일',
              hint: '평일만 선택 가능',
              selectedDate: state.targetAltHolidayDate,
              onTap: () => _selectTargetDate(context, state, notifier),
            ),
            const Spacer(),
          ],
        ),
      ),
      bottomNavigationBar: _buildBottomButton(state, notifier),
    );
  }

  Widget _buildBottomButton(
    AltHolidayRequestState state,
    AltHolidayRequestNotifier notifier,
  ) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SizedBox(
        height: AppSpacing.buttonHeight,
        child: ElevatedButton(
          onPressed: state.canSubmit ? () => notifier.submit() : null,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.primary,
            disabledBackgroundColor: AppColors.surfaceVariant,
          ),
          child: state.isLoading
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text(
                  '신청하기',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.black,
                  ),
                ),
        ),
      ),
    );
  }

  Future<void> _selectActualWorkDate(
    BuildContext context,
    AltHolidayRequestState state,
    AltHolidayRequestNotifier notifier,
  ) async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: state.actualWorkDate ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 1, 12, 31),
      selectableDayPredicate: (date) {
        // 주말 또는 공휴일만 선택 가능
        final isWeekend =
            date.weekday == DateTime.saturday || date.weekday == DateTime.sunday;
        final isHoliday = state.holidays.any(
          (h) => h.year == date.year && h.month == date.month && h.day == date.day,
        );
        return isWeekend || isHoliday;
      },
    );
    if (date != null) {
      notifier.selectActualWorkDate(date);
    }
  }

  Future<void> _selectTargetDate(
    BuildContext context,
    AltHolidayRequestState state,
    AltHolidayRequestNotifier notifier,
  ) async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: state.targetAltHolidayDate ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 1, 12, 31),
      selectableDayPredicate: (date) {
        // 평일만 선택 가능 (주말/공휴일 제외)
        final isWeekend =
            date.weekday == DateTime.saturday || date.weekday == DateTime.sunday;
        final isHoliday = state.holidays.any(
          (h) => h.year == date.year && h.month == date.month && h.day == date.day,
        );
        return !isWeekend && !isHoliday;
      },
    );
    if (date != null) {
      notifier.selectTargetAltHolidayDate(date);
    }
  }
}

/// 날짜 선택 필드 위젯
class _DateField extends StatelessWidget {
  final String label;
  final String hint;
  final DateTime? selectedDate;
  final VoidCallback onTap;

  const _DateField({
    required this.label,
    required this.hint,
    required this.selectedDate,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 8),
        InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(8),
          child: Container(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: 14,
            ),
            decoration: BoxDecoration(
              color: AppColors.surfaceVariant,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppColors.divider),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    selectedDate != null
                        ? _formatDate(selectedDate!)
                        : '날짜를 선택하세요',
                    style: TextStyle(
                      fontSize: 16,
                      color: selectedDate != null
                          ? AppColors.textPrimary
                          : AppColors.textTertiary,
                    ),
                  ),
                ),
                const Icon(Icons.calendar_today, color: AppColors.textSecondary),
              ],
            ),
          ),
        ),
        const SizedBox(height: 4),
        Text(
          '* $hint',
          style: const TextStyle(
            fontSize: 12,
            color: AppColors.textSecondary,
          ),
        ),
      ],
    );
  }

  String _formatDate(DateTime date) {
    final weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[date.weekday - 1];
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')} ($weekday)';
  }
}
