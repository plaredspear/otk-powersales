import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mobile/domain/entities/client_order.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처별 주문 상세 정보 헤더
///
/// SAP 주문번호, 거래처(마감시간), 주문일, 납기일, 총 승인금액을 표시합니다.
class ClientOrderInfoHeader extends StatelessWidget {
  final ClientOrderDetail detail;

  const ClientOrderInfoHeader({
    super.key,
    required this.detail,
  });

  String _formatDate(DateTime date) {
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[date.weekday - 1];
    return '${DateFormat('yyyy-MM-dd').format(date)} ($weekday)';
  }

  String _formatAmount(int amount) {
    return '${NumberFormat('#,###').format(amount)}원';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.md),
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
              Text(
                'SAP 주문번호 : ',
                style: AppTypography.bodyMedium.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                detail.sapOrderNumber,
                style: AppTypography.bodyMedium,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  '거래처',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              Expanded(
                child: Text(
                  detail.clientDeadlineTime != null
                      ? '${detail.clientName} (${detail.clientDeadlineTime} 마감)'
                      : detail.clientName,
                  style: AppTypography.bodyMedium,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  '주문일',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              Text(
                _formatDate(detail.orderDate),
                style: AppTypography.bodyMedium,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  '납기일',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              Text(
                _formatDate(detail.deliveryDate),
                style: AppTypography.bodyMedium,
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.sm),
          Row(
            children: [
              SizedBox(
                width: 80,
                child: Text(
                  '총 승인 금액',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              Text(
                _formatAmount(detail.totalApprovedAmount),
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.primary,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
