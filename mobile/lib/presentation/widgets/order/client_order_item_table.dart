import 'package:flutter/material.dart';
import 'package:mobile/domain/entities/client_order.dart';
import 'package:mobile/domain/entities/order_detail.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처별 주문 제품 테이블
///
/// 제품명, 납품 수량, 배송 상태를 테이블 형태로 표시합니다.
/// 배송중/배송완료 상태의 행을 탭하면 콜백이 호출됩니다.
class ClientOrderItemTable extends StatelessWidget {
  final List<ClientOrderItem> items;
  final ValueChanged<ClientOrderItem>? onItemTap;

  const ClientOrderItemTable({
    super.key,
    required this.items,
    this.onItemTap,
  });

  Color _getStatusColor(DeliveryStatus status) {
    switch (status) {
      case DeliveryStatus.waiting:
        return AppColors.textSecondary;
      case DeliveryStatus.shipping:
        return AppColors.warning;
      case DeliveryStatus.delivered:
        return AppColors.success;
    }
  }

  bool _canTapItem(ClientOrderItem item) {
    return item.deliveryStatus == DeliveryStatus.shipping ||
        item.deliveryStatus == DeliveryStatus.delivered;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Header row
        Container(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm,
          ),
          decoration: BoxDecoration(
            color: AppColors.surface,
            border: Border(
              bottom: BorderSide(color: AppColors.border),
            ),
          ),
          child: Row(
            children: [
              Expanded(
                flex: 3,
                child: Text(
                  '제품',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              SizedBox(
                width: 80,
                child: Text(
                  '납품 수량',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
              SizedBox(
                width: 80,
                child: Text(
                  '상태',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ],
          ),
        ),
        // Item rows
        ListView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: items.length,
          itemBuilder: (context, index) {
            final item = items[index];
            final canTap = _canTapItem(item);

            return InkWell(
              onTap: canTap && onItemTap != null ? () => onItemTap!(item) : null,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.md,
                  vertical: AppSpacing.sm,
                ),
                decoration: const BoxDecoration(
                  border: Border(
                    bottom: BorderSide(color: AppColors.border),
                  ),
                ),
                child: Row(
                  children: [
                    Expanded(
                      flex: 3,
                      child: Text(
                        '${item.productName} (${item.productCode})',
                        style: AppTypography.bodyMedium,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    SizedBox(
                      width: 80,
                      child: Text(
                        item.deliveredQuantity,
                        style: AppTypography.bodyMedium,
                        textAlign: TextAlign.center,
                      ),
                    ),
                    SizedBox(
                      width: 80,
                      child: Text(
                        item.deliveryStatus.displayName,
                        style: AppTypography.bodyMedium.copyWith(
                          color: _getStatusColor(item.deliveryStatus),
                          fontWeight: FontWeight.bold,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ],
    );
  }
}
