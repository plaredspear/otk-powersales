import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/add_product_state.dart';
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
                  onPressed: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate:
                          state.historyDateFrom ?? DateTime.now().subtract(const Duration(days: 3)),
                      firstDate:
                          DateTime.now().subtract(const Duration(days: 365)),
                      lastDate: DateTime.now(),
                      locale: const Locale('ko', 'KR'),
                    );
                    if (picked != null) {
                      notifier.setHistoryDateRange(
                        picked,
                        state.historyDateTo ?? DateTime.now(),
                      );
                    }
                  },
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
                  onPressed: () async {
                    final picked = await showDatePicker(
                      context: context,
                      initialDate: state.historyDateTo ?? DateTime.now(),
                      firstDate:
                          DateTime.now().subtract(const Duration(days: 365)),
                      lastDate: DateTime.now(),
                      locale: const Locale('ko', 'KR'),
                    );
                    if (picked != null) {
                      notifier.setHistoryDateRange(
                        state.historyDateFrom ??
                            DateTime.now()
                                .subtract(const Duration(days: 3)),
                        picked,
                      );
                    }
                  },
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
