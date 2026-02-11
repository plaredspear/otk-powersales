import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/client_order.dart';
import '../../domain/entities/order.dart';
import '../providers/client_order_list_provider.dart';
import '../providers/client_order_list_state.dart';
import '../providers/order_list_provider.dart';
import '../providers/order_list_state.dart';
import '../widgets/order/client_order_card.dart';
import '../widgets/order/client_order_filter_bar.dart';
import '../widgets/order/order_card.dart';
import '../widgets/order/order_filter_bar.dart';
import '../widgets/order/order_sort_bottom_sheet.dart';
import '../widgets/order/page_navigator.dart';

/// 주문 현황 페이지
///
/// "내 주문" 탭과 "거래처별 주문" 탭으로 구성됩니다.
/// 이 스펙에서는 "내 주문" 탭만 구현하며,
/// 필터(거래처/상태/납기일), 정렬, 무한 스크롤 페이지네이션을 지원합니다.
class OrderListPage extends ConsumerStatefulWidget {
  const OrderListPage({super.key});

  @override
  ConsumerState<OrderListPage> createState() => _OrderListPageState();
}

class _OrderListPageState extends ConsumerState<OrderListPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);

    // 스크롤 리스너 (무한 스크롤)
    _scrollController.addListener(_onScroll);

    // 페이지 진입 시 초기 데이터 로딩
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(orderListProvider.notifier).initialize();
      ref.read(clientOrderListProvider.notifier).initialize();
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  /// 스크롤 하단 도달 시 다음 페이지 로드
  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      ref.read(orderListProvider.notifier).loadNextPage();
    }
  }

  /// 주문 카드 탭 → 주문 상세 화면으로 이동
  void _onOrderTap(Order order) {
    AppRouter.navigateTo(
      context,
      AppRouter.orderDetail,
      arguments: order.id,
    );
  }

  /// 주문 FAB 탭
  void _onFabTap() {
    // 주문서 작성 화면은 별도 스펙으로 구현 예정
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('주문서 작성 화면은 준비 중입니다'),
        duration: Duration(seconds: 2),
      ),
    );
  }

  /// 정렬 버튼 탭
  void _onSortTap() {
    final state = ref.read(orderListProvider);
    OrderSortBottomSheet.show(
      context,
      currentSortType: state.sortType,
      onSortChanged: (sortType) {
        ref.read(orderListProvider.notifier).updateSortType(sortType);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(orderListProvider);

    // 에러 메시지 리스닝
    ref.listen(orderListProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            duration: const Duration(seconds: 2),
          ),
        );
        ref.read(orderListProvider.notifier).clearError();
      }
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
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _onFabTap,
        backgroundColor: AppColors.primary,
        foregroundColor: AppColors.onPrimary,
        icon: const Icon(Icons.edit),
        label: const Text('주문'),
      ),
    );
  }

  /// 내 주문 탭 콘텐츠
  Widget _buildMyOrdersTab(OrderListState state) {
    return Column(
      children: [
        // 필터 바
        OrderFilterBar(
          clients: state.clients,
          selectedClientId: state.selectedClientId,
          selectedStatus: state.selectedStatus,
          deliveryDateFrom: state.deliveryDateFrom,
          deliveryDateTo: state.deliveryDateTo,
          onClientChanged: (clientId, clientName) {
            ref
                .read(orderListProvider.notifier)
                .updateClientFilter(clientId, clientName);
          },
          onStatusChanged: (status) {
            ref.read(orderListProvider.notifier).updateStatusFilter(status);
          },
          onDateRangeChanged: (from, to) {
            ref
                .read(orderListProvider.notifier)
                .updateDeliveryDateRange(from, to);
          },
          onSearch: () {
            ref.read(orderListProvider.notifier).searchOrders();
          },
        ),
        // 결과 헤더 (건수 + 정렬)
        _buildResultHeader(state),
        // 주문 목록
        Expanded(
          child: _buildOrderList(state),
        ),
      ],
    );
  }

  /// 결과 헤더 (건수 + 정렬 버튼)
  Widget _buildResultHeader(OrderListState state) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            '주문 현황 (${state.totalElements})',
            style: AppTypography.headlineSmall,
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
  Widget _buildOrderList(OrderListState state) {
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
                ref.read(orderListProvider.notifier).searchOrders();
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

    // 주문 카드 리스트 (무한 스크롤)
    return ListView.builder(
      controller: _scrollController,
      itemCount: state.orders.length + (state.isLoadingMore ? 1 : 0),
      itemBuilder: (context, index) {
        // 로딩 인디케이터 (마지막 아이템)
        if (index == state.orders.length) {
          return const Padding(
            padding: EdgeInsets.all(AppSpacing.lg),
            child: Center(child: CircularProgressIndicator()),
          );
        }

        final order = state.orders[index];
        return OrderCard(
          order: order,
          onTap: () => _onOrderTap(order),
        );
      },
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
              stores: state.stores,
              selectedStoreId: state.selectedStoreId,
              selectedDeliveryDate: state.selectedDeliveryDate,
              canSearch: state.canSearch,
              onStoreChanged: (entry) {
                if (entry == null) {
                  ref
                      .read(clientOrderListProvider.notifier)
                      .selectStore(null, null);
                } else {
                  ref
                      .read(clientOrderListProvider.notifier)
                      .selectStore(entry.key, entry.value);
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
            // 결과 헤더 (검색 후에만 표시)
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
          Text(
            '주문 현황 (${state.totalElements})',
            style: AppTypography.headlineSmall,
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
