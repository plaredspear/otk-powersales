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
  String _statusLabel(DeliveryStatus status) {
    if (status == DeliveryStatus.delivered) return '배송완료';
    return status.displayName;
  }

  Color _getStatusColor(DeliveryStatus status) {
    switch (status) {
      case DeliveryStatus.pending:
        return AppColors.textSecondary;
      case DeliveryStatus.shipping:
        return AppColors.warning;
      case DeliveryStatus.delivered:
        return AppColors.success;
      case DeliveryStatus.outOfStock:
        return AppColors.error;
      // 레거시 빈 상태(status='') — 대기와 동일하게 회색 처리, 라벨은 빈 문자열.
      case DeliveryStatus.unknown:
        return AppColors.textSecondary;
    }
  }

  bool _isItemTappable(DeliveryStatus status) {
    return status == DeliveryStatus.shipping || status == DeliveryStatus.delivered;
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
