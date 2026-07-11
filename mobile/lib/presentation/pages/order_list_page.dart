import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/client_order.dart';
import '../../domain/entities/order_request.dart';
import '../providers/client_order_list_provider.dart';
import '../providers/client_order_list_state.dart';
import '../providers/order_request_list_provider.dart';
import '../providers/order_request_list_state.dart';
import '../widgets/order/client_order_card.dart';
import '../widgets/order/client_order_filter_bar.dart';
import '../widgets/order/order_request_card.dart';
import '../widgets/order/order_request_filter_bar.dart';
import '../widgets/order/order_request_sort_bottom_sheet.dart';
import '../widgets/order/page_navigator.dart';

/// 주문 현황 페이지
///
/// "내 주문" 탭과 "거래처별 주문" 탭으로 구성됩니다.
/// 이 스펙에서는 "내 주문" 탭만 구현하며,
/// 필터(거래처/상태/납기일), 정렬, 무한 스크롤 페이지네이션을 지원합니다.
class OrderListPage extends ConsumerStatefulWidget {
  /// 초기 선택 탭 인덱스 (0: 내 주문, 1: 거래처별 주문)
  final int initialTabIndex;

  const OrderListPage({super.key, this.initialTabIndex = 0});

  @override
  ConsumerState<OrderListPage> createState() => _OrderListPageState();
}

class _OrderListPageState extends ConsumerState<OrderListPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  /// 과도상태(SENT) 주문 자동 폴링용 — 등록 직후 상태 전이를 새로고침 없이 반영.
  Timer? _pollTimer;
  int _pollCount = 0;
  static const int _maxPolls = 5; // 3초 × 5회 ≈ 15초 후 중단
  static const Duration _pollInterval = Duration(seconds: 3);

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 2,
      vsync: this,
      initialIndex: widget.initialTabIndex,
    );

    // 페이지 진입 시 초기 데이터 로딩 (거래처별 주문은 사용자 선택 후 검색이라 별도 init 없음)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(orderRequestListProvider.notifier).initialize();
    });
  }

  @override
  void dispose() {
    _stopPolling();
    _tabController.dispose();
    super.dispose();
  }

  /// 과도상태 주문 유무에 따라 한시적 폴링을 시작/중단한다.
  void _syncTransientPolling(OrderRequestListState state) {
    if (state.hasTransientOrders) {
      _startPollingIfNeeded();
    } else {
      _stopPolling();
    }
  }

  void _startPollingIfNeeded() {
    if (_pollTimer != null) return;
    _pollCount = 0;
    _pollTimer = Timer.periodic(_pollInterval, (timer) {
      _pollCount++;
      if (_pollCount > _maxPolls) {
        _stopPolling();
        return;
      }
      ref.read(orderRequestListProvider.notifier).refreshSilently();
    });
  }

  void _stopPolling() {
    _pollTimer?.cancel();
    _pollTimer = null;
  }

  /// 주문 카드 탭 → 주문 상세 화면으로 이동
  void _onOrderTap(OrderRequest order) {
    AppRouter.navigateTo(
      context,
      AppRouter.orderDetail,
      arguments: order.id,
    );
  }

  /// 주문 FAB 탭 → 주문서 작성 화면으로 이동
  void _onFabTap() {
    AppRouter.navigateTo(context, AppRouter.orderForm);
  }

  /// 정렬 버튼 탭
  void _onSortTap() {
    final state = ref.read(orderRequestListProvider);
    OrderRequestSortBottomSheet.show(
      context,
      currentSortType: state.sortType,
      onSortChanged: (sortType) {
        ref.read(orderRequestListProvider.notifier).updateSortType(sortType);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(orderRequestListProvider);

    // 에러 메시지 리스닝 + 과도상태 폴링 동기화
    ref.listen(orderRequestListProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(orderRequestListProvider.notifier).clearError();
      }
      _syncTransientPolling(next);
    });

    return Scaffold(
      appBar: AppBar(
        title: const Text('주문 현황'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => AppRouter.goBack(context),
        ),
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '내 주문'),
            Tab(text: '거래처별 주문'),
          ],
          labelColor: AppColors.otokiBlue,
          unselectedLabelColor: AppColors.textTertiary,
          indicatorColor: AppColors.otokiBlue,
          indicatorWeight: AppSpacing.tabIndicatorWeight,
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: [
          // 내 주문 탭
          _buildMyOrdersTab(state),
          // 거래처별 주문 탭
          _buildClientOrdersTab(),
        ],
      ),
      // 레거시 .btn_add_order(노란 원형 "+주문")에 정합
      floatingActionButton: FloatingActionButton(
        onPressed: _onFabTap,
        backgroundColor: AppColors.legacyYellow,
        foregroundColor: AppColors.onPrimary,
        shape: const CircleBorder(),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: const [
            Icon(Icons.add, size: 22),
            SizedBox(height: 1),
            Text(
              '주문',
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
            ),
          ],
        ),
      ),
    );
  }

  /// 내 주문 탭 콘텐츠
  Widget _buildMyOrdersTab(OrderRequestListState state) {
    return Column(
      children: [
        // 필터 바
        OrderRequestFilterBar(
          selectedClientName: state.selectedClientName,
          selectedClientId: state.selectedClientId,
          selectedStatus: state.selectedStatus,
          deliveryDateFrom: state.deliveryDateFrom,
          deliveryDateTo: state.deliveryDateTo,
          onClientChanged: (clientId, clientName) {
            ref
                .read(orderRequestListProvider.notifier)
                .updateClientFilter(clientId, clientName);
          },
          onStatusChanged: (status) {
            ref.read(orderRequestListProvider.notifier).updateStatusFilter(status);
          },
          onDateRangeChanged: (from, to) {
            ref
                .read(orderRequestListProvider.notifier)
                .updateDeliveryDateRange(from, to);
          },
          onSearch: () {
            ref.read(orderRequestListProvider.notifier).searchOrders();
          },
        ),
        // 전송 처리 중 안내 (과도상태 주문이 있을 때만)
        if (state.hasTransientOrders) _buildTransientNotice(),
        // 결과 헤더 (건수 + 정렬)
        _buildResultHeader(state),
        // 주문 목록
        Expanded(
          child: _buildOrderList(state),
        ),
      ],
    );
  }

  /// 전송 처리 중 안내 문구 (비개발자 사용자용).
  ///
  /// 등록 직후 SAP 비동기 전송 대기 구간에는 상태가 '전송'으로 표시되며,
  /// 화면이 잠시 후 자동 갱신되고 수동 새로고침도 가능함을 안내한다.
  Widget _buildTransientNotice() {
    return Container(
      width: double.infinity,
      // ignore: deprecated_member_use
      color: AppColors.info.withOpacity(0.08),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.sync, size: 16, color: AppColors.info),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              "'전송' 상태 주문은 등록 처리 중입니다. 잠시 후 자동으로 갱신되며, "
              '화면을 아래로 당겨 새로고침할 수도 있어요.',
              style: AppTypography.bodySmall.copyWith(color: AppColors.info),
            ),
          ),
        ],
      ),
    );
  }

  /// 결과 헤더 (건수 + 정렬 버튼)
  Widget _buildResultHeader(OrderRequestListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text.rich(
            TextSpan(
              style: AppTypography.headlineSmall,
              children: [
                const TextSpan(text: '주문 현황 '),
                TextSpan(
                  text: '(${state.allOrderRequests.length})',
                  style: const TextStyle(color: AppColors.otokiRed),
                ),
              ],
            ),
          ),
          InkWell(
            onTap: _onSortTap,
            borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            child: Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.xs,
                vertical: AppSpacing.xxs,
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.sort,
                    size: 18,
                    color: AppColors.textSecondary,
                  ),
                  const SizedBox(width: AppSpacing.xxs),
                  Text(
                    state.sortType.displayName,
                    style: AppTypography.bodySmall.copyWith(
                      color: AppColors.textSecondary,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// 주문 목록 (로딩/에러/빈목록/데이터)
  Widget _buildOrderList(OrderRequestListState state) {
    // 로딩 상태
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (재시도 버튼)
    if (state.errorMessage != null && !state.hasResults) {
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
              '일시적인 오류가 발생했습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () {
                ref.read(orderRequestListProvider.notifier).searchOrders();
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 빈 목록
    if (state.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.inbox_outlined,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '조회된 주문이 없습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    // 검색 전 초기 상태
    if (!state.hasSearched) {
      return const Center(child: CircularProgressIndicator());
    }

    // 주문 카드 리스트 + 페이지네이터 (클라이언트 슬라이스)
    final pagedItems = state.pagedItems;
    return Column(
      children: [
        Expanded(
          child: RefreshIndicator(
            onRefresh: () =>
                ref.read(orderRequestListProvider.notifier).searchOrders(),
            child: ListView.builder(
              physics: const AlwaysScrollableScrollPhysics(),
              itemCount: pagedItems.length,
              itemBuilder: (context, index) {
                final order = pagedItems[index];
                return OrderRequestCard(
                  order: order,
                  onTap: () => _onOrderTap(order),
                );
              },
            ),
          ),
        ),
        if (state.totalPages > 1)
          PageNavigator(
            currentPage: state.currentPage,
            totalPages: state.totalPages,
            onPageChanged: (page) {
              ref.read(orderRequestListProvider.notifier).goToPage(page);
            },
          ),
      ],
    );
  }

  /// 거래처별 주문 탭
  Widget _buildClientOrdersTab() {
    return Consumer(
      builder: (context, ref, child) {
        final state = ref.watch(clientOrderListProvider);

        // 에러 메시지 리스닝
        ref.listen(clientOrderListProvider, (previous, next) {
          if (next.errorMessage != null && previous?.errorMessage == null) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(next.errorMessage!),
                duration: const Duration(seconds: 2),
              ),
            );
            ref.read(clientOrderListProvider.notifier).clearError();
          }
        });

        return Column(
          children: [
            // 필터 바
            ClientOrderFilterBar(
              selectedAccountName: state.selectedAccountName,
              selectedAccountId: state.selectedAccountId,
              selectedDeliveryDate: state.selectedDeliveryDate,
              canSearch: state.canSearch,
              onAccountChanged: (entry) {
                if (entry == null) {
                  ref
                      .read(clientOrderListProvider.notifier)
                      .selectAccount(null, null);
                } else {
                  ref
                      .read(clientOrderListProvider.notifier)
                      .selectAccount(entry.key, entry.value);
                }
              },
              onDeliveryDateChanged: (date) {
                ref
                    .read(clientOrderListProvider.notifier)
                    .updateDeliveryDate(date);
              },
              onSearch: () {
                ref.read(clientOrderListProvider.notifier).searchOrders();
              },
            ),
            // 안내 문구 (항상 표시) + 결과 헤더 (검색 후에만 표시)
            _buildClientOrderNotice(),
            if (state.hasSearched) _buildClientOrderResultHeader(state),
            // 주문 목록
            Expanded(
              child: _buildClientOrderList(state),
            ),
          ],
        );
      },
    );
  }

  /// 거래처별 주문 안내 문구
  ///
  /// 거래처별 주문은 담당 사원 조건 없이 (거래처 + 납기일)로만 조회하므로
  /// 본인이 아닌 다른 사원이 등록한 주문도 함께 표시된다는 점을 안내한다.
  /// (레거시 SF `IF_REST_MOBILE_ClientOrderSearch` — OwnerId/사원 필터 부재 정합)
  Widget _buildClientOrderNotice() {
    return Container(
      width: double.infinity,
      // ignore: deprecated_member_use
      color: AppColors.info.withOpacity(0.08),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, size: 16, color: AppColors.info),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              '거래처별 주문은 해당 거래처의 전체 주문을 표시합니다. '
              '본인이 아닌 다른 사원이 등록한 주문도 함께 조회됩니다.',
              style: AppTypography.bodySmall.copyWith(color: AppColors.info),
            ),
          ),
        ],
      ),
    );
  }

  /// 거래처별 주문 결과 헤더
  Widget _buildClientOrderResultHeader(ClientOrderListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text.rich(
            TextSpan(
              style: AppTypography.headlineSmall,
              children: [
                const TextSpan(text: '주문 현황 '),
                TextSpan(
                  text: '(${state.totalElements})',
                  style: const TextStyle(color: AppColors.otokiRed),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// 거래처별 주문 목록
  Widget _buildClientOrderList(ClientOrderListState state) {
    // 로딩 상태
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    // 에러 상태 (재시도 버튼)
    if (state.errorMessage != null && !state.hasResults) {
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
              '일시적인 오류가 발생했습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () {
                ref.read(clientOrderListProvider.notifier).searchOrders();
              },
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    // 빈 목록
    if (state.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.inbox_outlined,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '조회된 주문이 없습니다',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    // 검색 전 초기 상태
    if (!state.hasSearched) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search,
              size: 64,
              color: AppColors.textTertiary,
            ),
            const SizedBox(height: AppSpacing.lg),
            Text(
              '거래처와 납기일을 선택 후 검색해주세요',
              style: AppTypography.bodyLarge.copyWith(
                color: AppColors.textSecondary,
              ),
            ),
          ],
        ),
      );
    }

    // 주문 카드 리스트 + 페이지네이터
    return Column(
      children: [
        Expanded(
          child: ListView.builder(
            itemCount: state.orders.length,
            itemBuilder: (context, index) {
              final order = state.orders[index];
              return ClientOrderCard(
                order: order,
                onTap: () => _onClientOrderTap(order),
              );
            },
          ),
        ),
        // 페이지네이터 (totalPages > 1일 때만 표시)
        if (state.totalPages > 1)
          PageNavigator(
            currentPage: state.currentPage,
            totalPages: state.totalPages,
            onPageChanged: (page) {
              ref.read(clientOrderListProvider.notifier).goToPage(page);
            },
          ),
      ],
    );
  }

  /// 거래처별 주문 카드 탭 → 주문 상세 화면으로 이동
  void _onClientOrderTap(ClientOrder order) {
    AppRouter.navigateTo(
      context,
      AppRouter.clientOrderDetail,
      arguments: order.sapOrderNumber,
    );
  }
}
