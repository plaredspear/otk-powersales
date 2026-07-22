import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_detail.dart';
import './approval_status_badge.dart';
import './order_status_info_sheet.dart';

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
          // SAP 주문번호 — SAP 응답이 있으면 마감 전에도 노출(distinct, 콤마 나열). 분할 주문 시 N개.
          if (orderDetail.hasSapOrderNumbers) ...[
            SizedBox(height: AppSpacing.sm),
            _buildInfoRow(
              'SAP 주문번호',
              orderDetail.sapOrderNumbers.join(', '),
            ),
          ],
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
            '주문 요청 상태',
            '',
            onInfoTap: () => OrderStatusInfoSheet.show(context),
            customValue: OrderRequestStatusBadge(
              statusCode: orderDetail.orderRequestStatus,
              statusName: orderDetail.orderRequestStatusName,
            ),
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

  Widget _buildInfoRow(
    String label,
    String value, {
    Widget? customValue,
    VoidCallback? onInfoTap,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 120,
          child: onInfoTap == null
              ? Text(
                  label,
                  style: AppTypography.bodyMedium.copyWith(
                    color: AppColors.textTertiary,
                  ),
                )
              : Row(
                  children: [
                    Flexible(
                      child: Text(
                        label,
                        style: AppTypography.bodyMedium.copyWith(
                          color: AppColors.textTertiary,
                        ),
                      ),
                    ),
                    const SizedBox(width: 4),
                    GestureDetector(
                      onTap: onInfoTap,
                      behavior: HitTestBehavior.opaque,
                      child: Icon(
                        Icons.info_outline,
                        size: 16,
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
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
