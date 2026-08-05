import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../providers/add_product_provider.dart';
import '../../providers/add_product_state.dart';
import '../common/date_range_filter_field.dart';
import '../common/loading_indicator.dart';
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
      return const LoadingIndicator();
    }

    if (state.orderHistoryGroups.isEmpty) {
      // 거래처 미선택과 "이력 없음"을 구분해 안내한다(빈 화면 금지).
      final noAccount = !state.hasOrderHistoryAccount;
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              noAccount ? Icons.storefront_outlined : Icons.history,
              // 안내 문구 굵기(w300)에 맞춰 아이콘도 작고 연하게 둔다
              // (MaterialIcons 는 가변 폰트가 아니라 weight 조절이 불가).
              size: 40,
              // 거래처 미선택은 사용자 액션 안내라 강조색(error), 이력 없음은 중립색으로 구분.
              color: noAccount
                  ? AppColors.error.withValues(alpha: 0.7)
                  : AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.md),
            Text(
              noAccount ? '거래처를 먼저 선택해 주세요.' : '주문 이력이 없습니다.',
              // 액션 안내(거래처 미선택)는 강조색, 단순 결과 없음은 보조색으로
              // 정보 레벨을 차등화한다.
              style: noAccount
                  ? AppTypography.bodyLarge.copyWith(
                      color: AppColors.error,
                    )
                  : AppTypography.bodyMedium.copyWith(
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
        // 주문별 전체 선택 대상 — 선택이 차단된 제품(전용상품)은 제외한다.
        final selectableCodes = groupProducts
            .where((p) => !(blockExclusive && p.isExclusiveBlocked))
            .map((p) => p.productCode)
            .toList();
        final allSelected = selectableCodes.isNotEmpty &&
            selectableCodes.every((code) => state.isProductSelected(code));
        final anySelected =
            selectableCodes.any((code) => state.isProductSelected(code));
        return Card(
          margin: const EdgeInsets.only(bottom: AppSpacing.md),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            side: BorderSide(color: AppColors.border, width: 1),
          ),
          child: ExpansionTile(
            initiallyExpanded: group.isExpanded,
            // 펼침 시 기본값(colorScheme.primary=노랑) 대신 접힘 상태와 같은 회색 유지
            iconColor: AppColors.textSecondary,
            collapsedIconColor: AppColors.textSecondary,
            onExpansionChanged: (_) {
              notifier.toggleOrderHistoryExpansion(group.orderId);
            },
            // 주문별 전체 선택(다건 선택 모드에서만 노출). 일부만 선택되면 중간 상태로 표시.
            leading: state.multiSelect && selectableCodes.isNotEmpty
                ? Checkbox(
                    tristate: true,
                    value: allSelected ? true : (anySelected ? null : false),
                    activeColor: AppColors.primary,
                    onChanged: (_) {
                      notifier.setSelectionForCodes(
                        selectableCodes,
                        !allSelected,
                      );
                    },
                  )
                : null,
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
