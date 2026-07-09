import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';

class DeliveryInfoPopup extends StatelessWidget {
  final ProcessingItem processingItem;

  const DeliveryInfoPopup({
    super.key,
    required this.processingItem,
  });

  static void show(BuildContext context, {required ProcessingItem item}) {
    showDialog(
      context: context,
      builder: (ctx) => DeliveryInfoPopup(processingItem: item),
    );
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
    }
  }

  String _getStatusMessage(DeliveryStatus status) {
    switch (status) {
      case DeliveryStatus.pending:
        return '배송 대기 중입니다.';
      case DeliveryStatus.shipping:
        return '배송이 진행 중입니다.';
      case DeliveryStatus.delivered:
        return '배송이 완료되었습니다.';
      case DeliveryStatus.outOfStock:
        return '결품으로 인해 배송할 수 없습니다.';
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
          SizedBox(height: AppSpacing.md),
          _buildInfoRow(
            '배송 상태',
            processingItem.deliveryStatus.displayName,
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
