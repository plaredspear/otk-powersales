import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class DeliveryInfoPopup extends StatelessWidget {
  final ProcessingItem processingItem;

  /// 배송수량 `"N BOX (M EA)"` — 거래처주문 상세에서만 채워지는 선택적 필드(신규, 2026-07-21).
  /// null 이면 배송수량 행 미표시(F18 내 주문상세는 처리현황 그룹에서 이미 배송수량을 다루므로 null).
  final String? shippedQuantity;

  const DeliveryInfoPopup({
    super.key,
    required this.processingItem,
    this.shippedQuantity,
  });

  static void show(
    BuildContext context, {
    required ProcessingItem item,
    String? shippedQuantity,
  }) {
    showDialog(
      context: context,
      builder: (ctx) => DeliveryInfoPopup(
        processingItem: item,
        shippedQuantity: shippedQuantity,
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case OrderDeliveryStatus.shipping:
        return AppColors.warning;
      case OrderDeliveryStatus.delivered:
        return AppColors.success;
      case OrderDeliveryStatus.outOfStock:
        return AppColors.error;
      // 대기 / 빈상태(unknown) / 미정의 코드 — 회색(default 로 crash 방어).
      case OrderDeliveryStatus.pending:
      default:
        return AppColors.textSecondary;
    }
  }

  String _getStatusMessage(String status) {
    switch (status) {
      case OrderDeliveryStatus.pending:
        return '배송 대기 중입니다.';
      case OrderDeliveryStatus.shipping:
        return '배송이 진행 중입니다.';
      case OrderDeliveryStatus.delivered:
        return '배송이 완료되었습니다.';
      case OrderDeliveryStatus.outOfStock:
        return '결품으로 인해 배송할 수 없습니다.';
      // 빈상태(unknown) / 미정의 코드 — 빈 메시지(default 로 crash 방어).
      default:
        return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(
        '배송 정보',
        style: AppTypography.headlineMedium.copyWith(
          color: AppColors.textPrimary,
          fontWeight: FontWeight.bold,
        ),
      ),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildInfoRow('제품', '${processingItem.productName} (${processingItem.productCode})'),
          SizedBox(height: AppSpacing.md),
          _buildInfoRow('납품 수량', processingItem.deliveredQuantity),
          // 배송수량 — 거래처주문 상세에서만 병기 (실제 출하량 "N BOX (M EA)").
          if (shippedQuantity != null) ...[
            SizedBox(height: AppSpacing.md),
            _buildInfoRow('배송 수량', shippedQuantity!),
          ],
          SizedBox(height: AppSpacing.md),
          _buildInfoRow(
            '배송 상태',
            OrderDeliveryStatus.displayName(processingItem.deliveryStatus),
            valueColor: _getStatusColor(processingItem.deliveryStatus),
          ),
          // 차량/기사 정보 (Spec #595 Q5) — SHIPPING/DELIVERED 라인의 운영 추적 정보.
          // 빈 값(`null`) 인 필드는 표시 생략.
          if (processingItem.vehicle != null) ...[
            SizedBox(height: AppSpacing.md),
            _buildInfoRow('차량번호', processingItem.vehicle!),
          ],
          if (processingItem.driverName != null) ...[
            SizedBox(height: AppSpacing.md),
            _buildInfoRow('기사명', processingItem.driverName!),
          ],
          if (processingItem.driverPhone != null) ...[
            SizedBox(height: AppSpacing.md),
            _buildInfoRow('연락처', processingItem.driverPhone!),
          ],
          if (processingItem.scheduleTime != null) ...[
            SizedBox(height: AppSpacing.md),
            _buildInfoRow('배송 예정', processingItem.scheduleTime!),
          ],
          if (processingItem.completeTime != null) ...[
            SizedBox(height: AppSpacing.md),
            _buildInfoRow('배송 완료', processingItem.completeTime!),
          ],
          SizedBox(height: AppSpacing.lg),
          Container(
            width: double.infinity,
            padding: EdgeInsets.all(AppSpacing.md),
            decoration: BoxDecoration(
              // ignore: deprecated_member_use
              color: _getStatusColor(processingItem.deliveryStatus).withOpacity(0.1),
              borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
            ),
            child: Text(
              _getStatusMessage(processingItem.deliveryStatus),
              style: AppTypography.bodyMedium.copyWith(
                color: _getStatusColor(processingItem.deliveryStatus),
              ),
              textAlign: TextAlign.center,
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          style: TextButton.styleFrom(
            foregroundColor: AppColors.secondaryDark,
          ),
          child: Text(
            '닫기',
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.secondaryDark,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInfoRow(String label, String value, {Color? valueColor}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 80,
          child: Text(
            label,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ),
        SizedBox(width: AppSpacing.md),
        Expanded(
          child: Text(
            value,
            style: AppTypography.bodyMedium.copyWith(
              color: valueColor ?? AppColors.textPrimary,
              fontWeight: valueColor != null ? FontWeight.w700 : FontWeight.normal,
            ),
          ),
        ),
      ],
    );
  }
}
