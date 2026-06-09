import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../core/utils/throttled_tap_mixin.dart';
import '../providers/promotion_list_provider.dart';
import '../widgets/promotion/my_assignment_picker_sheet.dart';

/// 일 매출 등록 진입화면 (행사 미선택, 레거시 p52 정합).
///
/// 홈 "행사매출 등록" → 본 화면. 오늘 날짜(읽기전용)와 "담당 행사 선택" 필드를
/// 보여주고, 행사를 선택하면 기존 일매출 마감 폼([AppRouter.promotionDailySales])
/// 으로 이동한다.
class DailySalesEntryPage extends ConsumerStatefulWidget {
  const DailySalesEntryPage({super.key});

  @override
  ConsumerState<DailySalesEntryPage> createState() =>
      _DailySalesEntryPageState();
}

class _DailySalesEntryPageState extends ConsumerState<DailySalesEntryPage>
    with ThrottledTapMixin {
  static const _weekdays = ['월', '화', '수', '목', '금', '토', '일'];

  String _formatToday(DateTime now) {
    String two(int n) => n.toString().padLeft(2, '0');
    final weekday = _weekdays[now.weekday - 1];
    return '${now.year}.${two(now.month)}.${two(now.day)}($weekday)';
  }

  Future<void> _selectAssignment() async {
    final assignment = await MyAssignmentPickerSheet.show(context);
    if (assignment == null || !mounted) return;

    await AppRouter.navigateTo<bool>(
      context,
      AppRouter.promotionDailySales,
      arguments: assignment.promotionEmployeeId,
    );
    // 등록/임시저장 후 복귀 시 목록 갱신 (마감 상태 반영).
    if (mounted) {
      ref.invalidate(myPromotionAssignmentsProvider);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('일 매출 등록')),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _DateRow(value: _formatToday(DateTime.now())),
            const Divider(height: 1, color: AppColors.divider),
            _SelectEventRow(
              onTap: () => throttledTapAsync(_selectAssignment),
            ),
            const Divider(height: 1, color: AppColors.divider),
          ],
        ),
      ),
    );
  }
}

/// 날짜 행 — 오늘 날짜 표시(수정 불가).
class _DateRow extends StatelessWidget {
  final String value;

  const _DateRow({required this.value});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.surface,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.md,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '날짜',
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary),
          ),
          Text(value, style: AppTypography.bodyMedium),
        ],
      ),
    );
  }
}

/// "담당 행사 선택" 행 — 탭 시 담당 행사 선택 바텀시트 표시.
class _SelectEventRow extends StatelessWidget {
  final VoidCallback onTap;

  const _SelectEventRow({required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            Text('행사', style: AppTypography.bodyMedium),
            Text(
              ' *',
              style: AppTypography.bodyMedium.copyWith(color: AppColors.error),
            ),
            const Spacer(),
            Text(
              '담당 행사 선택',
              style: AppTypography.bodyMedium
                  .copyWith(color: AppColors.textTertiary),
            ),
            const SizedBox(width: AppSpacing.xs),
            const Icon(Icons.chevron_right, color: AppColors.textTertiary),
          ],
        ),
      ),
    );
  }
}
