import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order.dart';
import 'approval_status_badge.dart';

/// 주문 카드 위젯
///
/// 주문 요청번호, 거래처명, 주문일, 납기일, 총 주문금액, 승인상태 뱃지를 표시합니다.
/// 카드를 탭하면 주문 상세 화면으로 이동합니다.
class OrderCard extends StatelessWidget {
  /// 주문 정보
  final Order order;

  /// 카드 탭 콜백
  final VoidCallback? onTap;

  const OrderCard({
    super.key,
    required this.order,
    this.onTap,
  });

  /// 날짜를 "YYYY-MM-DD(요일)" 형식으로 포맷
  String _formatDate(DateTime date) {
    final weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[date.weekday - 1];
    final formatted = DateFormat('yyyy-MM-dd').format(date);
    return '$formatted($weekday)';
  }

  /// 금액을 천단위 콤마 포맷
  String _formatAmount(int amount) {
    final formatter = NumberFormat('#,###');
    return formatter.format(amount);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: BoxDecoration(
            color: AppColors.white,
            borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
            border: Border.all(color: AppColors.border, width: 1),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 주문 요청번호 + 승인상태 뱃지
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      '주문 요청번호 : ${order.orderRequestNumber}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ),
                  ApprovalStatusBadge(status: order.approvalStatus),
                ],
              ),
              const SizedBox(height: AppSpacing.xs),

              // 거래처명
              Text(
                order.clientName,
                style: AppTypography.headlineSmall,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: AppSpacing.sm),

              // 주문일
              _buildInfoRow('주문일', _formatDate(order.orderDate)),
              const SizedBox(height: AppSpacing.xxs),

              // 납기일
              _buildInfoRow('납기일', _formatDate(order.deliveryDate)),
              const SizedBox(height: AppSpacing.xs),

              // 총 주문금액
              _buildInfoRow(
                '총 주문금액',
                '${_formatAmount(order.totalAmount)}원',
                isHighlighted: true,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildInfoRow(
    String label,
    String value, {
    bool isHighlighted = false,
  }) {
    return Row(
      children: [
        Text(
          label,
          style: AppTypography.bodySmall.copyWith(
            color: AppColors.textTertiary,
          ),
        ),
        const SizedBox(width: AppSpacing.sm),
        Expanded(
          child: Text(
            value,
            style: isHighlighted
                ? AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.w600,
                    color: AppColors.textPrimary,
                  )
                : AppTypography.bodySmall.copyWith(
                    color: AppColors.textSecondary,
                  ),
          ),
        ),
      ],
    );
  }
}
