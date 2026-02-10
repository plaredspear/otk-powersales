import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/order_detail.dart';
import '../providers/order_detail_provider.dart';
import '../providers/order_detail_state.dart';
import '../widgets/order/delivery_info_popup.dart';
import '../widgets/order/order_action_buttons.dart';
import '../widgets/order/order_info_header.dart';
import '../widgets/order/order_processing_status_section.dart';
import '../widgets/order/ordered_item_expandable.dart';
import '../widgets/order/ordered_item_list.dart';
import '../widgets/order/rejected_item_list.dart';

/// 주문 상세 페이지
///
/// 주문 ID를 기반으로 주문 상세 정보를 조회하고,
/// 마감 상태와 반려 여부에 따라 3가지 화면 구성을 동적으로 렌더링합니다.
///
/// - 마감전: 주문정보 + 주문취소/재전송 버튼 + 주문한 제품 목록
/// - 마감후: 주문정보 + 주문한 제품 접기/펼치기 + 주문처리현황
/// - 마감후_반려: 주문정보 + 주문반려제품 목록
class OrderDetailPage extends ConsumerWidget {
  /// 주문 ID (라우트 arguments로 전달)
  final int orderId;

  const OrderDetailPage({
    super.key,
    required this.orderId,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(orderDetailProvider(orderId));

    // 에러 메시지 리스닝
    ref.listen(orderDetailProvider(orderId), (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(orderDetailProvider(orderId).notifier).clearError();
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('주문 상세'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: _buildBody(context, ref, state),
    );
  }

  Widget _buildBody(
    BuildContext context,
    WidgetRef ref,
    OrderDetailState state,
  ) {
    // 로딩 상태
    if (state.isLoading && !state.hasData) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (데이터 없음)
    if (state.errorMessage != null && !state.hasData) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.error_outline,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              state.errorMessage!,
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () {
                ref
                    .read(orderDetailProvider(orderId).notifier)
                    .loadOrderDetail(orderId: orderId);
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 데이터 없음
    if (!state.hasData) {
      return const Center(child: CircularProgressIndicator());
    }

    final detail = state.orderDetail!;

    return RefreshIndicator(
      onRefresh: () async {
        await ref
            .read(orderDetailProvider(orderId).notifier)
            .loadOrderDetail(orderId: orderId);
      },
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 공통: 주문 정보 헤더
            OrderInfoHeader(orderDetail: detail),

            const SizedBox(height: AppSpacing.lg),

            // 마감전 전용 영역
            if (!detail.isClosed) ...[
              // 주문취소 / 재전송 버튼
              OrderActionButtons(
                orderDetail: detail,
                showCancelButton: state.showCancelButton,
                showResendButton: state.showResendButton,
                isResending: state.isResending,
                onCancel: () => _onCancelOrder(context),
                onResend: () => _onResendOrder(context, ref),
              ),

              if (state.showCancelButton || state.showResendButton)
                const SizedBox(height: AppSpacing.lg),

              // 주문한 제품 목록 (마감전에는 바로 표시)
              OrderedItemList(items: detail.orderedItems),
            ],

            // 마감후 전용 영역
            if (detail.isClosed) ...[
              // 주문한 제품 접기/펼치기
              OrderedItemExpandable(
                items: detail.orderedItems,
                itemCount: detail.orderedItemCount,
                isExpanded: state.isItemsExpanded,
                onToggle: () {
                  ref
                      .read(orderDetailProvider(orderId).notifier)
                      .toggleItemsExpanded();
                },
              ),

              const SizedBox(height: AppSpacing.lg),

              // 반려 제품 목록 (마감후 + 반려제품 존재 시)
              if (detail.hasRejectedItems) ...[
                RejectedItemList(rejectedItems: detail.rejectedItems!),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 주문 처리 현황 (마감후)
              if (detail.orderProcessingStatus != null)
                OrderProcessingStatusSection(
                  processingStatus: detail.orderProcessingStatus,
                  onItemTap: (item) => _onProcessingItemTap(context, item),
                ),
            ],

            // 하단 여백
            const SizedBox(height: AppSpacing.xxxl),
          ],
        ),
      ),
    );
  }

  /// 주문 취소 버튼 탭
  void _onCancelOrder(BuildContext context) {
    // 주문취소 화면(F19)으로 이동 (별도 스펙으로 구현 예정)
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('주문 취소 화면은 준비 중입니다'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  /// 주문 재전송 버튼 탭
  Future<void> _onResendOrder(BuildContext context, WidgetRef ref) async {
    final success = await ref
        .read(orderDetailProvider(orderId).notifier)
        .resendOrder(orderId: orderId);

    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            success
                ? '주문이 재전송되었습니다'
                : '재전송에 실패했습니다. 잠시 후 다시 시도해주세요',
          ),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }

  /// 처리 현황 항목 탭 (배송중/배송완료)
  void _onProcessingItemTap(BuildContext context, ProcessingItem item) {
    DeliveryInfoPopup.show(context, item: item);
  }
}
