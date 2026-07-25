import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/utils/order_deadline.dart';

/// 납기일 선택 필드
///
/// 필드 아래에 **주문 마감 안내**를 함께 표시한다. 마감 규칙(납기일 하루 전 13:50)을 모르면
/// 승인요청을 눌러 서버가 거부(`ORD_DEADLINE_PASSED`)할 때까지 알 수 없어, 납기일을 고르는
/// 자리에서 미리 알려준다 (2026-07-25 사용자 요청).
class DeliveryDatePicker extends StatelessWidget {
  final DateTime? selectedDate;
  final VoidCallback onTap;

  /// 마감 판정 기준 시각 (테스트 주입용, 기본 현재 시각).
  final DateTime? now;

  const DeliveryDatePicker({
    super.key,
    required this.selectedDate,
    required this.onTap,
    this.now,
  });

  static const List<String> _dayNames = ['월', '화', '수', '목', '금', '토', '일'];

  String _formatDate(DateTime date) {
    final formatter = DateFormat('yyyy-MM-dd');
    final dayOfWeek = _dayNames[date.weekday - 1];
    return '${formatter.format(date)} ($dayOfWeek)';
  }

  /// 마감 안내 문구 — 납기일 미선택 시 규칙만, 선택 시 그 납기일의 마감 시각을 알려준다.
  String _deadlineGuide(DateTime? deadline, bool isPassed) {
    if (deadline == null) return '납기일 하루 전 13:50까지 주문할 수 있습니다.';
    final label =
        '${DateFormat('M/d').format(deadline)}(${_dayNames[deadline.weekday - 1]}) 13:50';
    if (isPassed) return '마감시간이 지났습니다. (주문 마감: $label)';
    return '주문 마감: $label 까지';
  }

  @override
  Widget build(BuildContext context) {
    final deadline =
        selectedDate == null ? null : OrderDeadline.deadlineFor(selectedDate!);
    final isPassed = selectedDate != null &&
        !OrderDeadline.isWithinDeadline(selectedDate!, now: now);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        RichText(
          text: TextSpan(
            text: '납기일 ',
            style: AppTypography.headlineSmall.copyWith(
              color: AppColors.textPrimary,
            ),
            children: [
              TextSpan(
                text: '*',
                style: TextStyle(
                  color: AppColors.error,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.sm),
        OutlinedButton(
          onPressed: onTap,
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(double.infinity, 48),
            side: BorderSide(color: AppColors.border),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            ),
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.md,
              vertical: AppSpacing.md,
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: Text(
                  selectedDate != null ? _formatDate(selectedDate!) : '선택하세요',
                  style: AppTypography.bodyMedium.copyWith(
                    color: selectedDate != null
                        ? AppColors.textPrimary
                        : AppColors.textSecondary,
                  ),
                ),
              ),
              // 레거시 HTML5 date input 의 캘린더 아이콘 외형 정합.
              Icon(
                Icons.calendar_today_outlined,
                size: AppSpacing.iconSize,
                color: AppColors.textSecondary,
              ),
            ],
          ),
        ),
        const SizedBox(height: AppSpacing.xs),
        // 마감 안내 — 마감이 지난 납기일이면 붉게 강조해 승인요청 전에 알아채게 한다.
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(
              isPassed ? Icons.error_outline : Icons.info_outline,
              size: 14,
              color: isPassed ? AppColors.error : AppColors.textSecondary,
            ),
            const SizedBox(width: AppSpacing.xxs),
            Expanded(
              child: Text(
                _deadlineGuide(deadline, isPassed),
                style: AppTypography.bodySmall.copyWith(
                  color: isPassed ? AppColors.error : AppColors.textSecondary,
                  fontWeight: isPassed ? FontWeight.bold : null,
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }
}
