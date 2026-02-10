import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/order_detail.dart';
import '../providers/order_cancel_provider.dart';
import '../providers/order_cancel_state.dart';
import '../widgets/order/cancel_confirm_dialog.dart';
import '../widgets/order/cancel_product_item.dart';

/// 주문 취소 페이지
///
/// 주문 상세에서 취소 버튼을 누르면 진입하는 전체 화면 모달입니다.
/// 취소 가능한 제품 목록을 체크박스로 선택하고, 선택된 제품을 일괄 취소합니다.
///
/// 진입 인자: [OrderCancelPageArgs] (orderId + allItems)
/// 반환값: true이면 취소 성공 (상세 화면 새로고침 트리거)
class OrderCancelPage extends ConsumerWidget {
  final OrderCancelPageArgs args;

  const OrderCancelPage({
    super.key,
    required this.args,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final params = OrderCancelParams(
      orderId: args.orderId,
      allItems: args.allItems,
    );
    final state = ref.watch(orderCancelProvider(params));

    // 에러 메시지 리스닝
    ref.listen(orderCancelProvider(params), (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(orderCancelProvider(params).notifier).clearError();
      }
    });

    // 취소 성공 리스닝
    ref.listen(orderCancelProvider(params), (previous, next) {
      if (next.cancelSuccess && !(previous?.cancelSuccess ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('주문이 취소되었습니다'),
            duration: Duration(seconds: 2),
          ),
        );
        Navigator.of(context).pop(true);
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('주문 취소'),
        automaticallyImplyLeading: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.close),
            onPressed: () => Navigator.of(context).pop(false),
          ),
        ],
      ),
      body: _buildBody(context, ref, state, params),
    );
  }

  Widget _buildBody(
    BuildContext context,
    WidgetRef ref,
    OrderCancelState state,
    OrderCancelParams params,
  ) {
    // 취소 가능한 제품이 없는 경우
    if (state.hasNoCancellableItems) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.info_outline,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '취소 가능한 제품이 없습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      );
    }

    return Column(
      children: [
        // 전체 선택 헤더
        _buildSelectAllHeader(context, ref, state, params),

        Divider(height: 1, color: AppColors.divider),

        // 제품 목록
        Expanded(
          child: ListView.separated(
            itemCount: state.cancellableItems.length,
            separatorBuilder: (context, index) => Divider(
              height: 1,
              color: AppColors.divider,
              indent: AppSpacing.lg,
              endIndent: AppSpacing.lg,
            ),
            itemBuilder: (context, index) {
              final item = state.cancellableItems[index];
              final isSelected =
                  state.selectedProductCodes.contains(item.productCode);
              return CancelProductItem(
                item: item,
                isSelected: isSelected,
                onToggle: () {
                  ref
                      .read(orderCancelProvider(params).notifier)
                      .toggleProduct(item.productCode);
                },
              );
            },
          ),
        ),

        // 하단 취소 버튼
        _buildBottomButton(context, ref, state, params),
      ],
    );
  }

  /// 전체 선택 헤더
  Widget _buildSelectAllHeader(
    BuildContext context,
    WidgetRef ref,
    OrderCancelState state,
    OrderCancelParams params,
  ) {
    return InkWell(
      onTap: () {
        ref.read(orderCancelProvider(params).notifier).toggleSelectAll();
      },
      child: Padding(
        padding: EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            SizedBox(
              width: 24,
              height: 24,
              child: Checkbox(
                value: state.isAllSelected,
                onChanged: (_) {
                  ref
                      .read(orderCancelProvider(params).notifier)
                      .toggleSelectAll();
                },
                activeColor: AppColors.error,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
                materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
            ),
            SizedBox(width: AppSpacing.md),
            Text(
              '전체 (${state.cancellableItems.length})',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// 하단 취소 버튼
  Widget _buildBottomButton(
    BuildContext context,
    WidgetRef ref,
    OrderCancelState state,
    OrderCancelParams params,
  ) {
    return SafeArea(
      child: Container(
        padding: EdgeInsets.all(AppSpacing.lg),
        decoration: BoxDecoration(
          color: AppColors.surface,
          border: Border(
            top: BorderSide(
              color: AppColors.divider,
              width: 1,
            ),
          ),
        ),
        child: SizedBox(
          width: double.infinity,
          height: 48,
          child: ElevatedButton(
            onPressed: state.canCancel
                ? () => _onCancelPressed(context, ref, state, params)
                : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.error,
              foregroundColor: Colors.white,
              disabledBackgroundColor: AppColors.border,
              disabledForegroundColor: AppColors.textTertiary,
              shape: RoundedRectangleBorder(
                borderRadius: AppSpacing.buttonBorderRadius,
              ),
            ),
            child: state.isCancelling
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                    ),
                  )
                : Text(
                    '주문 취소 (${state.selectedCount})',
                    style: AppTypography.labelLarge.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
          ),
        ),
      ),
    );
  }

  /// 취소 버튼 클릭 처리
  Future<void> _onCancelPressed(
    BuildContext context,
    WidgetRef ref,
    OrderCancelState state,
    OrderCancelParams params,
  ) async {
    // 확인 다이얼로그 표시
    final confirmed = await CancelConfirmDialog.show(
      context,
      selectedCount: state.selectedCount,
    );

    if (confirmed != true) return;

    // 주문 취소 실행
    await ref.read(orderCancelProvider(params).notifier).cancelOrder();
  }
}

/// 주문 취소 페이지 진입 인자
class OrderCancelPageArgs {
  final int orderId;
  final List<OrderedItem> allItems;

  const OrderCancelPageArgs({
    required this.orderId,
    required this.allItems,
  });
}
