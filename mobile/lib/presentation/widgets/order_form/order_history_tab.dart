import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/add_product_state.dart';
import '../common/range_calendar_picker.dart';
import 'product_card_for_add.dart';

/// 주문 이력 탭
class OrderHistoryTab extends ConsumerWidget {
  final ScrollController scrollController;

  const OrderHistoryTab({
    super.key,
    required this.scrollController,
  });

  String _formatDate(DateTime? date) {
    if (date == null) return '';
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  /// 주문 이력 시작일~종료일을 클레임 현황과 동일한 달력 UI 로 선택한다.
  /// 조회 가능 기간은 주문 이력 조건(최근 1년 ~ 오늘)에 맞춘다. 범위 일수 제한은 없다.
  Future<void> _pickDateRange(
    BuildContext context,
    AddProductState state,
    AddProductNotifier notifier,
  ) async {
    final now = DateTime.now();
    final picked = await showRangeCalendar(
      context,
      initialStart:
          state.historyDateFrom ?? now.subtract(const Duration(days: 3)),
      initialEnd: state.historyDateTo ?? now,
      firstDate: now.subtract(const Duration(days: 365)),
      lastDate: now,
      maxRangeDays: null,
    );
    if (picked != null) {
      notifier.setHistoryDateRange(picked.start, picked.end);
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);

    return Column(
      children: [
        // 날짜 범위 선택
        Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _pickDateRange(context, state, notifier),
                  icon: const Icon(Icons.calendar_today, size: 16),
                  label: Text(
                    _formatDate(state.historyDateFrom),
                    style: AppTypography.bodySmall,
                  ),
                  style: OutlinedButton.styleFrom(
                    side: BorderSide(color: AppColors.border),
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusMd),
                    ),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: AppSpacing.sm),
                child: Text(
                  '~',
                  style: AppTypography.bodyMedium,
                ),
              ),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _pickDateRange(context, state, notifier),
                  icon: const Icon(Icons.calendar_today, size: 16),
                  label: Text(
                    _formatDate(state.historyDateTo),
                    style: AppTypography.bodySmall,
                  ),
                  style: OutlinedButton.styleFrom(
                    side: BorderSide(color: AppColors.border),
                    shape: RoundedRectangleBorder(
                      borderRadius:
                          BorderRadius.circular(AppSpacing.radiusMd),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        // 주문 이력 목록
        Expanded(
          child: _buildOrderHistoryList(state, notifier, ref),
        ),
      ],
    );
  }

  Widget _buildOrderHistoryList(
    dynamic state,
    dynamic notifier,
    WidgetRef ref,
  ) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.orderHistoryGroups.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.history,
              size: 48,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              '주문 이력이 없습니다.',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    return ListView.builder(
      controller: scrollController,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      itemCount: state.orderHistoryGroups.length,
      itemBuilder: (context, index) {
        final OrderHistoryGroup group = state.orderHistoryGroups[index];
        return Card(
          margin: const EdgeInsets.only(bottom: AppSpacing.md),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            side: BorderSide(color: AppColors.border, width: 1),
          ),
          child: ExpansionTile(
            initiallyExpanded: group.isExpanded,
            onExpansionChanged: (_) {
              notifier.toggleOrderHistoryExpansion(group.orderId);
            },
            title: Text(
              '${group.orderDate} - ${group.clientName}',
              style: AppTypography.labelLarge,
            ),
            subtitle: Text(
              '${group.products.length}개 제품',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            children: group.products.map((product) {
              return ProductCardForAdd(
                product: product,
                isSelected: state.isProductSelected(product.productCode),
                onSelectionChanged: (_) {
                  ref
                      .read(addProductProvider.notifier)
                      .toggleProductSelection(product.productCode);
                },
                onFavoriteToggle: null,
                isFavoriteTab: false,
                showFavoriteButton: false,
              );
            }).toList(),
          ),
        );
      },
    );
  }
}
