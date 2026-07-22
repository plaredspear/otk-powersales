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

  Color _getStatusColor(String status) {
    switch (status) {
      case OrderDeliveryStatus.shipping:
        return AppColors.warning;
      case OrderDeliveryStatus.delivered:
        return AppColors.success;
      case OrderDeliveryStatus.outOfStock:
        return AppColors.error;
      // 취소 — 결품(빨강)과 구분되도록 회색 (내 주문 상세 처리현황 정합).
      case OrderDeliveryStatus.cancelled:
      // 대기 / 미정의 코드 — 회색(default 로 crash 방어).
      case OrderDeliveryStatus.pending:
      default:
        return AppColors.textSecondary;
    }
  }

  bool _canTapItem(ClientOrderItem item) {
    return item.deliveryStatus == OrderDeliveryStatus.shipping ||
        item.deliveryStatus == OrderDeliveryStatus.delivered;
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
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
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
                            OrderDeliveryStatus.displayName(item.deliveryStatus),
                            style: AppTypography.bodyMedium.copyWith(
                              color: _getStatusColor(item.deliveryStatus),
                              fontWeight: FontWeight.bold,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      ],
                    ),
                    // 배송수량 별도 행 (신규) — 실제 출하량 "N BOX (M EA)". 납품수량과 구분되도록
                    // 라벨 접두 + 보조 텍스트 스타일. 배송 전 라인은 "0 BOX (0 EA)".
                    SizedBox(height: AppSpacing.xs),
                    Text(
                      '배송수량: ${item.shippedQuantity}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
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
