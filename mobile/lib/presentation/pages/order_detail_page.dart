import 'dart:async';

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
import '../widgets/order/unfulfilled_item_list.dart';

/// 주문 상세 페이지
///
/// 주문 ID를 기반으로 주문 상세 정보를 조회하고,
/// 마감 상태와 미납/반려 여부에 따라 화면 구성을 동적으로 렌더링합니다.
///
/// 제품 표시 UI 배치 (마감 전후 공통): 미납 제품 → 주문반려제품 → 주문한 제품 (→ 처리현황).
///
/// - 마감전: 주문정보 + 주문취소 버튼 + 미납/반려 + 주문한 제품 목록
/// - 마감후: 주문정보 + 미납/반려 + 주문한 제품 접기/펼치기 + 주문처리현황
///
/// 반려 섹션은 레거시(view.jsp:284-322 before / 449-486 after) 동등으로 마감 전후 모두 표시.
/// 미납 섹션(LineItemStatus != "OK")은 신규 정책(2026-07-20 사용자 결정)으로 최상단 표시.
class OrderDetailPage extends ConsumerStatefulWidget {
  /// 주문 ID (라우트 arguments로 전달)
  final int orderId;

  const OrderDetailPage({
    super.key,
    required this.orderId,
  });

  @override
  ConsumerState<OrderDetailPage> createState() => _OrderDetailPageState();
}

class _OrderDetailPageState extends ConsumerState<OrderDetailPage> {
  /// 과도상태(SENT / 등록 SAP 전송 in-flight) 자동 폴링용 — 상세 화면에서도 상태 전이를
  /// 수동 새로고침 없이 반영. 목록(order_list_page)과 동일한 백오프 스케줄로 약 105초까지 커버한다.
  Timer? _pollTimer;
  int _pollCount = 0;
  static const List<int> _pollBackoffSeconds = [
    3, 3, 3, 3, 3, // 0~15s
    6, 6, 6, 6, 6, // 15~45s
    12, 12, 12, 12, 12, // 45~105s
  ];

  @override
  void dispose() {
    _stopPolling();
    super.dispose();
  }

  /// 과도상태 유무에 따라 한시적 폴링을 시작/중단한다.
  void _syncTransientPolling(OrderRequestDetailState state) {
    if (state.hasTransientRegistration) {
      _startPollingIfNeeded();
    } else {
      _stopPolling();
    }
  }

  void _startPollingIfNeeded() {
    if (_pollTimer != null) return;
    _pollCount = 0;
    _scheduleNextPoll();
  }

  /// 백오프 스케줄에 따라 다음 폴링을 예약한다 (회차별 간격 가변 → 단발 Timer 재귀).
  void _scheduleNextPoll() {
    if (_pollCount >= _pollBackoffSeconds.length) {
      _stopPolling();
      return;
    }
    final seconds = _pollBackoffSeconds[_pollCount];
    _pollTimer = Timer(Duration(seconds: seconds), () async {
      _pollCount++;
      await ref
          .read(orderRequestDetailProvider(widget.orderId).notifier)
          .refreshSilently(orderId: widget.orderId);
      if (!mounted) return;
      if (ref.read(orderRequestDetailProvider(widget.orderId)).hasTransientRegistration) {
        _scheduleNextPoll();
      } else {
        _stopPolling();
      }
    });
  }

  void _stopPolling() {
    _pollTimer?.cancel();
    _pollTimer = null;
  }

  @override
  Widget build(BuildContext context) {
    final orderId = widget.orderId;
    final state = ref.watch(orderRequestDetailProvider(orderId));

    // 에러 메시지 리스닝 + 과도상태 폴링 동기화
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
      _syncTransientPolling(next);
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('주문 상세'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
      ),
      body: _buildBody(context, state),
    );
  }

  Widget _buildBody(
    BuildContext context,
    OrderRequestDetailState state,
  ) {
    final orderId = widget.orderId;
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
        // pull-to-refresh 는 SAP(SD03052) 를 동기 호출하므로, 마지막 조회 30초 이내 재당김은
        // 억제한다(respectCooldown). 최초 진입/재전송·취소 후 재조회는 쿨다운 미적용.
        await ref
            .read(orderRequestDetailProvider(orderId).notifier)
            .loadOrderDetail(orderId: orderId, respectCooldown: true);
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
              // 주문취소 버튼
              OrderActionButtons(
                orderDetail: detail,
                showCancelButton: state.showCancelButton,
                onCancel: () => _onCancelOrder(context),
              ),

              if (state.showCancelButton)
                const SizedBox(height: AppSpacing.lg),

              // 전송실패 안내 — 재전송 버튼 대신 재주문을 유도
              if (state.showResendButton) ...[
                _buildResendGuideNotice(),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 전송 처리 중 안내 — 등록 SAP 전송이 진행 중이라 아직 취소할 수 없음
              if (state.showRegistrationInFlightNotice) ...[
                _buildInFlightNotice(),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 미납 제품 목록 (신규 정책) — 제품 UI 최상단.
              if (detail.hasUnfulfilledItems) ...[
                UnfulfilledItemList(unfulfilledItems: detail.unfulfilledItems!),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 반려 제품 목록 — 레거시(view.jsp:284-322)는 마감 전에도 반려 섹션을 표시.
              if (detail.hasRejectedItems) ...[
                RejectedItemList(rejectedItems: detail.rejectedItems!),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 주문한 제품 목록 (마감전에는 바로 표시)
              OrderedItemList(items: detail.orderedItems),
            ],

            // 마감후 전용 영역
            if (detail.isClosed) ...[
              // 미납 제품 목록 (신규 정책) — 제품 UI 최상단.
              if (detail.hasUnfulfilledItems) ...[
                UnfulfilledItemList(unfulfilledItems: detail.unfulfilledItems!),
                const SizedBox(height: AppSpacing.lg),
              ],

              // 반려 제품 목록 (반려제품 존재 시)
              if (detail.hasRejectedItems) ...[
                RejectedItemList(rejectedItems: detail.rejectedItems!),
                const SizedBox(height: AppSpacing.lg),
              ],

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
              '처리 결과는 자동으로 갱신됩니다.',
              style: AppTypography.bodySmall.copyWith(color: AppColors.info),
            ),
          ),
        ],
      ),
    );
  }

  /// 전송실패 안내 — 재전송 대신 재주문을 유도
  Widget _buildResendGuideNotice() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.sm,
      ),
      decoration: BoxDecoration(
        // ignore: deprecated_member_use
        color: AppColors.error.withOpacity(0.08),
        borderRadius: AppSpacing.buttonBorderRadius,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.error_outline, size: 16, color: AppColors.error),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              '다시 재주문해주세요',
              style: AppTypography.bodySmall.copyWith(color: AppColors.error),
            ),
          ),
        ],
      ),
    );
  }

  /// 주문 취소 버튼 탭
  Future<void> _onCancelOrder(BuildContext context) async {
    final orderId = widget.orderId;
    final state = ref.read(orderRequestDetailProvider(orderId));
    final detail = state.orderDetail;
    if (detail == null) return;

    final result = await AppRouter.navigateTo<OrderCancelResult>(
      context,
      AppRouter.orderCancel,
      arguments: OrderCancelPageArgs(
        orderId: detail.id,
        allItems: detail.orderedItems,
      ),
    );

    // 취소 성공(success) 뿐 아니라 결과 미확정(inconclusive, timeout/게이트웨이 5xx)에도
    // 상세를 재조회한다. 미확정 시 서버/SAP 가 취소를 마저 처리했을 수 있어, 재조회로 실제
    // 반영 여부를 화면에 정확히 반영해야 '실패 오표시 → 재취소 → SAP 중복 전송' 을 막는다.
    // dismissed(사용자가 그냥 닫음)만 재조회하지 않는다.
    final shouldRefresh = result == OrderCancelResult.success ||
        result == OrderCancelResult.inconclusive;
    if (shouldRefresh && context.mounted) {
      ref
          .read(orderRequestDetailProvider(orderId).notifier)
          .loadOrderDetail(orderId: orderId);
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
