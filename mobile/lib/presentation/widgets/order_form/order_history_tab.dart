import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/add_product_state.dart';
import '../common/date_range_filter_field.dart';
import 'product_card_for_add.dart';

/// 주문 이력 탭
class OrderHistoryTab extends ConsumerWidget {
  final ScrollController scrollController;
  final bool requireBarcode;
  final bool blockExclusive;

  const OrderHistoryTab({
    super.key,
    required this.scrollController,
    this.requireBarcode = false,
    this.blockExclusive = false,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(addProductProvider);
    final notifier = ref.read(addProductProvider.notifier);
    final now = DateTime.now();

    return Column(
      children: [
        // 기간(주문 현황 납기일과 동일한 인라인 UI). 조건: 최근 1년 ~ 오늘.
        Padding(
          padding: const EdgeInsets.all(AppSpacing.lg),
          child: DateRangeFilterField(
            label: '기간',
            startDate:
                state.historyDateFrom ?? now.subtract(const Duration(days: 3)),
            endDate: state.historyDateTo ?? now,
            firstDate: now.subtract(const Duration(days: 365)),
            lastDate: now,
            onChanged: notifier.setHistoryDateRange,
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
        // 바코드 필수 화면(POS/전산 매출조회)에서는 바코드 없는 제품을 제외한다
        // (레거시 productMapper `is not null` 정합).
        final groupProducts = requireBarcode
            ? group.products
                .where((p) => p.barcode.trim().isNotEmpty)
                .toList()
            : group.products;
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
              group.clientName.isEmpty
                  ? group.orderDate
                  : '${group.orderDate} - ${group.clientName}',
              style: AppTypography.labelLarge,
            ),
            subtitle: Text(
              '${groupProducts.length}개 제품',
              style: AppTypography.bodySmall.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            children: groupProducts.map((product) {
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
                blockExclusive: blockExclusive,
              );
            }).toList(),
          ),
        );
      },
    );
  }
}
