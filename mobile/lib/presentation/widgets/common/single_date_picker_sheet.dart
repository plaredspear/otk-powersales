import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 단일 날짜 선택 모달.
///
/// 기본 [showDatePicker] 는 날짜 선택 후 "확인"을 다시 눌러야 하지만,
/// 하루만 고르는 상황에서는 불편하므로 날짜를 탭하면 즉시 확정한다.
/// 선택 없이 닫으려면 하단 "닫기" 버튼(또는 바깥 탭)을 사용한다.
class SingleDatePickerSheet {
  /// 날짜 선택 모달을 띄우고 선택된 날짜를 반환한다.
  /// 닫기/바깥 탭으로 취소하면 `null` 을 반환한다.
  static Future<DateTime?> show(
    BuildContext context, {
    required DateTime initialDate,
    required DateTime firstDate,
    required DateTime lastDate,
    String title = '날짜 선택',
  }) {
    // showDatePicker 와 동일하게 날짜 단위로 정규화 (시각 성분 제거).
    final DateTime first = DateUtils.dateOnly(firstDate);
    final DateTime last = DateUtils.dateOnly(lastDate);
    DateTime initial = DateUtils.dateOnly(initialDate);
    if (initial.isBefore(first)) initial = first;
    if (initial.isAfter(last)) initial = last;

    return showDialog<DateTime>(
      context: context,
      builder: (context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSpacing.md),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  AppSpacing.lg,
                  AppSpacing.lg,
                  AppSpacing.lg,
                  0,
                ),
                child: Text(
                  title,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textSecondary,
                  ),
                ),
              ),
              CalendarDatePicker(
                initialDate: initial,
                firstDate: first,
                lastDate: last,
                // 날짜를 탭하는 즉시 확정하고 모달을 닫는다.
                onDateChanged: (date) => Navigator.of(context).pop(date),
              ),
              Align(
                alignment: Alignment.centerRight,
                child: Padding(
                  padding: const EdgeInsets.only(
                    right: AppSpacing.md,
                    bottom: AppSpacing.sm,
                  ),
                  child: TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('닫기'),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
