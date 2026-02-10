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
      case DeliveryStatus.waiting:
        return AppColors.textSecondary;
      case DeliveryStatus.shipping:
        return AppColors.warning;
      case DeliveryStatus.delivered:
        return AppColors.success;
    }
  }

  String _getStatusMessage(DeliveryStatus status) {
    switch (status) {
      case DeliveryStatus.waiting:
        return '배송 대기 중입니다.';
      case DeliveryStatus.shipping:
        return '배송이 진행 중입니다.';
      case DeliveryStatus.delivered:
        return '배송이 완료되었습니다.';
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
            foregroundColor: AppColors.primary,
          ),
          child: Text(
            '닫기',
            style: AppTypography.labelLarge.copyWith(
              color: AppColors.primary,
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
              fontWeight: valueColor != null ? FontWeight.w600 : FontWeight.normal,
            ),
          ),
        ),
      ],
    );
  }
}
