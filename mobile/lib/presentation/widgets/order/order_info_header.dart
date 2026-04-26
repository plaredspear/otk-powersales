import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';
import './approval_status_badge.dart';

class OrderInfoHeader extends StatelessWidget {
  final OrderDetail orderDetail;

  const OrderInfoHeader({
    super.key,
    required this.orderDetail,
  });

  String _formatDate(DateTime date) {
    final weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final formatted = DateFormat('yyyy-MM-dd').format(date);
    final weekday = weekdays[date.weekday - 1];
    return '$formatted ($weekday)';
  }

  String _formatAmount(int amount) {
    return '${NumberFormat('#,###').format(amount)}원';
  }

  @override
  Widget build(BuildContext context) {
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
          _buildInfoRow(
            '주문요청번호',
            orderDetail.orderRequestNumber,
          ),
          SizedBox(height: AppSpacing.sm),
          _buildInfoRow(
            '거래처',
            orderDetail.clientDeadlineTime != null
                ? '${orderDetail.clientName} (${orderDetail.clientDeadlineTime} 마감)'
                : orderDetail.clientName,
          ),
          SizedBox(height: AppSpacing.sm),
          _buildInfoRow(
            '주문일',
            _formatDate(orderDetail.orderDate),
          ),
          SizedBox(height: AppSpacing.sm),
          _buildInfoRow(
            '납기일',
            _formatDate(orderDetail.deliveryDate),
          ),
          SizedBox(height: AppSpacing.sm),
          _buildInfoRow(
            '총 주문 금액',
            _formatAmount(orderDetail.totalAmount),
          ),
          if (orderDetail.isClosed && orderDetail.totalApprovedAmount != null) ...[
            SizedBox(height: AppSpacing.sm),
            _buildInfoRow(
              '총 승인 금액',
              _formatAmount(orderDetail.totalApprovedAmount!),
            ),
          ],
          SizedBox(height: AppSpacing.sm),
          _buildInfoRow(
            '승인 상태',
            '',
            customValue: ApprovalStatusBadge(status: orderDetail.approvalStatus),
          ),
          if (orderDetail.isClosed) ...[
            SizedBox(height: AppSpacing.sm),
            _buildInfoRow(
              '주문한 제품 수',
              '${orderDetail.orderedItemCount}개',
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value, {Widget? customValue}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 120,
          child: Text(
            label,
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textTertiary,
            ),
          ),
        ),
        SizedBox(width: AppSpacing.md),
        Expanded(
          child: customValue ??
              Text(
                value,
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.textPrimary,
                ),
              ),
        ),
      ],
    );
  }
}
