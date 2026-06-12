import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/promotion.dart';
import '../../providers/promotion_list_provider.dart';

/// "담당 행사 선택" 바텀시트.
///
/// 홈 "행사매출 등록" → 일 매출 등록 진입화면에서 오늘 담당 행사 목록을 보여주고
/// 1건을 선택하게 한다(레거시 `eventlistapi` 팝업 정합). 선택 결과로
/// [MyPromotionAssignment] 를 반환하며, 마감완료(isClosed) 건은 선택 불가.
class MyAssignmentPickerSheet extends ConsumerWidget {
  const MyAssignmentPickerSheet({super.key});

  /// 화면 표시 (선택된 행사를 반환, 취소 시 null).
  static Future<MyPromotionAssignment?> show(BuildContext context) {
    return showModalBottomSheet<MyPromotionAssignment>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (_) => const MyAssignmentPickerSheet(),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final assignmentsAsync = ref.watch(myPromotionAssignmentsProvider);

    return SafeArea(
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.7,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: AppSpacing.screenAll,
              child: Row(
                children: [
                  Text('담당 행사 선택', style: AppTypography.headlineSmall),
                ],
              ),
            ),
            const Divider(height: 1, color: AppColors.divider),
            Flexible(
              child: assignmentsAsync.when(
                loading: () => const Padding(
                  padding: EdgeInsets.all(40),
                  child: Center(child: CircularProgressIndicator()),
                ),
                error: (_, _) => _ErrorView(
                  onRetry: () =>
                      ref.invalidate(myPromotionAssignmentsProvider),
                ),
                data: (assignments) {
                  if (assignments.isEmpty) {
                    return const _EmptyView();
                  }
                  return ListView.separated(
                    shrinkWrap: true,
                    itemCount: assignments.length,
                    separatorBuilder: (_, _) =>
                        const Divider(height: 1, color: AppColors.divider),
                    itemBuilder: (context, index) => _AssignmentRow(
                      assignment: assignments[index],
                      onSelect: (a) => Navigator.of(context).pop(a),
                    ),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AssignmentRow extends StatelessWidget {
  final MyPromotionAssignment assignment;
  final ValueChanged<MyPromotionAssignment> onSelect;

  const _AssignmentRow({required this.assignment, required this.onSelect});

  @override
  Widget build(BuildContext context) {
    final subtitleParts = <String>[
      if (assignment.accountName != null) assignment.accountName!,
      if (assignment.promotionType != null) assignment.promotionType!,
      if (assignment.standLocation != null) assignment.standLocation!,
    ];

    return InkWell(
      onTap: assignment.isClosed ? null : () => onSelect(assignment),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    assignment.promotionNumber,
                    style: AppTypography.bodyLarge
                        .copyWith(fontWeight: FontWeight.w700),
                  ),
                  if (subtitleParts.isNotEmpty) ...[
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      subtitleParts.join(' · '),
                      style: AppTypography.bodySmall
                          .copyWith(color: AppColors.textSecondary),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(width: AppSpacing.md),
            if (assignment.isClosed)
              _ClosedBadge()
            else
              const Icon(Icons.chevron_right, color: AppColors.textTertiary),
          ],
        ),
      ),
    );
  }
}

class _ClosedBadge extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: AppColors.textTertiary.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
      ),
      child: Text(
        '등록완료',
        style: AppTypography.labelMedium.copyWith(
          color: AppColors.textSecondary,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _EmptyView extends StatelessWidget {
  const _EmptyView();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(40),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.event_busy, size: 40, color: AppColors.textTertiary),
          const SizedBox(height: AppSpacing.md),
          Text(
            '오늘 담당하는 행사가 없습니다',
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary),
          ),
        ],
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  final VoidCallback onRetry;

  const _ErrorView({required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(40),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.error_outline, size: 40, color: AppColors.textTertiary),
          const SizedBox(height: AppSpacing.md),
          Text(
            '담당 행사를 불러올 수 없습니다',
            style: AppTypography.bodyMedium
                .copyWith(color: AppColors.textSecondary),
          ),
          const SizedBox(height: AppSpacing.md),
          OutlinedButton(onPressed: onRetry, child: const Text('재시도')),
        ],
      ),
    );
  }
}
