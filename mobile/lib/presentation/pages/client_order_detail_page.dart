import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_spacing.dart';
import '../../core/theme/app_typography.dart';
import '../../domain/entities/client_order.dart';
import '../../domain/entities/order_detail.dart';
import '../providers/client_order_detail_provider.dart';
import '../providers/client_order_detail_state.dart';
import '../widgets/order/client_order_info_header.dart';
import '../widgets/order/client_order_item_table.dart';
import '../widgets/order/delivery_info_popup.dart';

/// 거래처별 주문 상세 페이지
///
/// SAP 주문번호를 받아 상세 정보를 조회하고 표시합니다.
class ClientOrderDetailPage extends ConsumerStatefulWidget {
  final String sapOrderNumber;

  const ClientOrderDetailPage({super.key, required this.sapOrderNumber});

  @override
  ConsumerState<ClientOrderDetailPage> createState() =>
      _ClientOrderDetailPageState();
}

class _ClientOrderDetailPageState
    extends ConsumerState<ClientOrderDetailPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref
          .read(clientOrderDetailProvider.notifier)
          .loadDetail(widget.sapOrderNumber);
    });
  }

  /// _onItemTap: When a ClientOrderItem with shipping/delivered status is tapped,
  /// create a ProcessingItem and show DeliveryInfoPopup
  void _onItemTap(ClientOrderItem item) {
    if (item.deliveryStatus == DeliveryStatus.waiting) return;

    // Convert ClientOrderItem to ProcessingItem for the popup
    final processingItem = ProcessingItem(
      productCode: item.productCode,
      productName: item.productName,
      deliveredQuantity: item.deliveredQuantity,
      deliveryStatus: item.deliveryStatus,
    );

    DeliveryInfoPopup.show(context, item: processingItem);
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(clientOrderDetailProvider);

    // Error listening
    ref.listen(clientOrderDetailProvider, (previous, next) {
      if (next.errorMessage != null && previous?.errorMessage == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(next.errorMessage!)),
        );
        ref.read(clientOrderDetailProvider.notifier).clearError();
        // If order not found, go back
        if (next.errorMessage!.contains('찾을 수 없습니다')) {
          AppRouter.goBack(context);
        }
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
      body: _buildBody(state),
    );
  }

  Widget _buildBody(ClientOrderDetailState state) {
    if (state.isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (state.errorMessage != null && !state.hasData) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline,
                size: 64, color: AppColors.textTertiary),
            const SizedBox(height: AppSpacing.lg),
            Text('오류가 발생했습니다',
                style: AppTypography.bodyLarge
                    .copyWith(color: AppColors.textSecondary)),
            const SizedBox(height: AppSpacing.lg),
            ElevatedButton(
              onPressed: () => ref
                  .read(clientOrderDetailProvider.notifier)
                  .loadDetail(widget.sapOrderNumber),
              child: const Text('재시도'),
            ),
          ],
        ),
      );
    }

    if (!state.hasData) {
      return const Center(child: CircularProgressIndicator());
    }

    final detail = state.orderDetail!;
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClientOrderInfoHeader(detail: detail),
          // "주문한 제품 (N)" header
          Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: AppSpacing.lg, vertical: AppSpacing.md),
            child: Text(
              '주문한 제품 (${detail.orderedItemCount})',
              style: AppTypography.headlineSmall,
            ),
          ),
          // Product table
          ClientOrderItemTable(
            items: detail.orderedItems,
            onItemTap: _onItemTap,
          ),
        ],
      ),
    );
  }
}
