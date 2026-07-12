import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import 'order_cancel_page.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/order_detail.dart';
import '../providers/order_request_detail_provider.dart';
import '../providers/order_request_detail_state.dart';
import '../widgets/order/delivery_info_popup.dart';
import '../widgets/order/order_action_buttons.dart';
import '../widgets/order/order_info_header.dart';
import '../widgets/order/order_processing_status_section.dart';
import '../widgets/order/ordered_item_expandable.dart';
import '../widgets/order/ordered_item_list.dart';
import '../widgets/order/rejected_item_list.dart';
import '../widgets/order/resend_confirm_dialog.dart';

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
    final state = ref.watch(orderRequestDetailProvider(orderId));

    // 에러 메시지 리스닝
    ref.listen(orderRequestDetailProvider(orderId), (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(orderRequestDetailProvider(orderId).notifier).clearError();
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
    OrderRequestDetailState state,
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
                    .read(orderRequestDetailProvider(orderId).notifier)
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
            .read(orderRequestDetailProvider(orderId).notifier)
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
                onCancel: () => _onCancelOrder(context, ref),
                onResend: () => _onResendOrder(context, ref),
              ),

              if (state.showCancelButton || state.showResendButton)
                const SizedBox(height: AppSpacing.lg),

              // 전송 처리 중 안내 — 등록 SAP 전송이 진행 중이라 아직 취소할 수 없음
              if (state.showRegistrationInFlightNotice) ...[
                _buildInFlightNotice(),
                const SizedBox(height: AppSpacing.lg),
              ],

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
                      .read(orderRequestDetailProvider(orderId).notifier)
                      .toggleItemsExpanded();
                },
              ),

              const SizedBox(height: AppSpacing.lg),

              // 반려 제품 목록 (마감후 + 반려제품 존재 시)
              if (detail.hasRejectedItems) ...[
                RejectedItemList(rejectedItems: detail.rejectedItems!),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 주문 처리 현황 (마감후) — SAP 주문번호별 그룹 N개 (Spec #595 Q1 옵션 2)
              if (detail.orderProcessingStatusList != null &&
                  detail.orderProcessingStatusList!.isNotEmpty)
                ..._buildProcessingStatusSections(context, detail),
            ],

            // 하단 여백
            const SizedBox(height: AppSpacing.xxxl),
          ],
        ),
      ),
    );
  }

  /// 전송 처리 중 안내 배너 — 등록 SAP 전송이 진행 중이라 취소 버튼 대신 노출.
  Widget _buildInFlightNotice() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      decoration: BoxDecoration(
        // ignore: deprecated_member_use
        color: AppColors.info.withOpacity(0.08),
        borderRadius: AppSpacing.buttonBorderRadius,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.sync, size: 16, color: AppColors.info),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              '주문 등록을 전송 처리 중입니다. 잠시 후 취소할 수 있어요. '
              '화면을 아래로 당겨 새로고침해 주세요.',
              style: AppTypography.bodySmall.copyWith(color: AppColors.info),
            ),
          ),
        ],
      ),
    );
  }

  /// 주문 취소 버튼 탭
  Future<void> _onCancelOrder(BuildContext context, WidgetRef ref) async {
    final state = ref.read(orderRequestDetailProvider(orderId));
    final detail = state.orderDetail;
    if (detail == null) return;

    final result = await AppRouter.navigateTo<bool>(
      context,
      AppRouter.orderCancel,
      arguments: OrderCancelPageArgs(
        orderId: detail.id,
        allItems: detail.orderedItems,
      ),
    );

    // 취소 성공 시 상세 화면 새로고침
    if (result == true && context.mounted) {
      ref
          .read(orderRequestDetailProvider(orderId).notifier)
          .loadOrderDetail(orderId: orderId);
    }
  }

  /// 주문 재전송 버튼 탭
  Future<void> _onResendOrder(BuildContext context, WidgetRef ref) async {
    // 레거시 정합: 재전송 전 사용자 확인 (confirm('재전송 하시겠습니까?'))
    final confirmed = await ResendConfirmDialog.show(context);
    if (confirmed != true) return;

    if (!context.mounted) return;

    final success = await ref
        .read(orderRequestDetailProvider(orderId).notifier)
        .resendOrderRequest(orderId: orderId);

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

  /// SAP 주문번호별 그룹 위젯 N개 + 그룹 간 spacing (Spec #595 P2-M).
  List<Widget> _buildProcessingStatusSections(
    BuildContext context,
    OrderDetail detail,
  ) {
    final sections = <Widget>[];
    final groups = detail.orderProcessingStatusList!;
    for (var i = 0; i < groups.length; i++) {
      if (i > 0) {
        sections.add(const SizedBox(height: AppSpacing.md));
      }
      sections.add(
        OrderProcessingStatusSection(
          processingStatus: groups[i],
          onItemTap: (item) => _onProcessingItemTap(context, item),
        ),
      );
    }
    return sections;
  }

  /// 처리 현황 항목 탭 (배송중/배송완료).
  /// 차량/기사 5필드 모두 null 인 라인은 팝업 미표시 (Spec #595 Q5).
  void _onProcessingItemTap(BuildContext context, ProcessingItem item) {
    if (item.hasNoDeliveryDetail) return;
    DeliveryInfoPopup.show(context, item: item);
  }
}
