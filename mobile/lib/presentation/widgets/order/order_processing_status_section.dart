import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class OrderProcessingStatusSection extends StatelessWidget {
  final OrderProcessingStatus? processingStatus;
  final Function(ProcessingItem)? onItemTap;

  const OrderProcessingStatusSection({
    super.key,
    this.processingStatus,
    this.onItemTap,
  });

  /// 주문상세 처리현황의 배송상태 라벨.
  ///
  /// 배송완료는 SF **조회 클래스**(IF_REST_MOBILE_OrderRequestDetail.cls:157) 정합으로 공백 없는
  /// '배송완료' 로 표시한다. enum 의 displayName('배송 완료', 공백)은 거래처주문(SF inbound cls:158)
  /// 도메인 표기라, 주문상세에서는 이 함수로 별도 매핑한다.
  ///
  /// DefaultReason 라인은 코드 분류에 따라 서버가 결품(OUT_OF_STOCK, {F1,L1,L2,L3}) / 취소(CANCELLED,
  /// {L4,O1,S1,S2,S3})로 내려주며, 각각 '결품' / '취소' 로 표기한다(2026-07-23 사용자 결정 — 기존 '미납'
  /// 단일 표기에서 분리). displayName 이 두 코드를 각각 '결품'/'취소' 로 매핑하므로 별도 override 불필요.
  String _statusLabel(String status) {
    if (status == OrderDeliveryStatus.delivered) return '배송완료';
    return OrderDeliveryStatus.displayName(status);
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case OrderDeliveryStatus.shipping:
        return AppColors.warning;
      case OrderDeliveryStatus.delivered:
        return AppColors.success;
      case OrderDeliveryStatus.outOfStock:
        return AppColors.error;
      // 취소 — 결품(빨강)과 구분되도록 회색.
      case OrderDeliveryStatus.cancelled:
        return AppColors.textSecondary;
      // 대기 / 빈상태(unknown, status='') / 미정의 코드 — 회색(default 로 crash 방어).
      case OrderDeliveryStatus.pending:
      default:
        return AppColors.textSecondary;
    }
  }

  bool _isItemTappable(String status) {
    return status == OrderDeliveryStatus.shipping ||
        status == OrderDeliveryStatus.delivered;
  }

  @override
  Widget build(BuildContext context) {
    if (processingStatus == null) {
      return const SizedBox.shrink();
    }

    return Container(
      padding: EdgeInsets.all(AppSpacing.lg),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        border: Border.all(
          color: AppColors.border,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  color: AppColors.primary,
                  shape: BoxShape.circle,
                ),
              ),
              SizedBox(width: AppSpacing.sm),
              Text(
                '주문 처리 현황',
                style: AppTypography.headlineSmall.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          SizedBox(height: AppSpacing.sm),
          Text(
            'SAP 주문번호: ${processingStatus!.sapOrderNumber}',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
            ),
          ),
          SizedBox(height: AppSpacing.md),
          Divider(
            height: 1,
            color: AppColors.divider,
          ),
          SizedBox(height: AppSpacing.md),
          if (processingStatus!.items.isEmpty)
            Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: AppSpacing.xl),
                child: Text(
                  '주문 처리 현황이 없습니다',
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textTertiary,
                  ),
                ),
              ),
            )
          else
            ...processingStatus!.items.asMap().entries.map((entry) {
              final index = entry.key;
              final item = entry.value;
              return Column(
                children: [
                  if (index > 0)
                    Divider(
                      height: AppSpacing.lg,
                      color: AppColors.divider,
                    ),
                  _buildItemRow(item),
                ],
              );
            }),
        ],
      ),
    );
  }

  Widget _buildItemRow(ProcessingItem item) {
    final isTappable = _isItemTappable(item.deliveryStatus);

    Widget content = Padding(
      padding: EdgeInsets.symmetric(vertical: AppSpacing.xs),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 3,
            child: Text(
              '${item.productName} (${item.productCode})',
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textPrimary,
              ),
            ),
          ),
          SizedBox(width: AppSpacing.md),
          Expanded(
            flex: 2,
            child: Text(
              item.deliveredQuantity,
              style: AppTypography.bodyMedium.copyWith(
                color: AppColors.textSecondary,
              ),
              textAlign: TextAlign.center,
            ),
          ),
          SizedBox(width: AppSpacing.md),
          Expanded(
            flex: 2,
            child: Text(
              _statusLabel(item.deliveryStatus),
              style: AppTypography.bodyMedium.copyWith(
                color: _getStatusColor(item.deliveryStatus),
                fontWeight: FontWeight.w700,
              ),
              textAlign: TextAlign.right,
            ),
          ),
        ],
      ),
    );

    if (isTappable && onItemTap != null) {
      return InkWell(
        onTap: () => onItemTap!(item),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        child: content,
      );
    }

    return content;
  }
}
