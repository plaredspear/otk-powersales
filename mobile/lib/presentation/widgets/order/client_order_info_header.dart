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

  String _formatDate(DateTime? date) {
    if (date == null) return '-';
    const weekdays = ['월', '화', '수', '목', '금', '토', '일'];
    final weekday = weekdays[date.weekday - 1];
    return '${DateFormat('yyyy-MM-dd').format(date)} ($weekday)';
  }

  String _formatAmount(int? amount) {
    if (amount == null) return '-';
    return '${NumberFormat('#,###').format(amount)}원';
  }

  String _formatClient(ClientOrderDetail detail) {
    final name = detail.sapAccountName ?? '-';
    final deadline = detail.clientDeadlineTime;
    if (deadline == null) return name;
    return '$name ($deadline 마감)';
  }

  String _formatOrderer(ClientOrderDetail detail) {
    final name = detail.ordererName;
    if (name == null || name.isEmpty) return '-';
    final code = detail.ordererCode;
    if (code == null || code.isEmpty) return name;
    return '$name ($code)';
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
                  _formatClient(detail),
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
                  '주문자',
                  style: AppTypography.bodyMedium.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              Expanded(
                child: Text(
                  _formatOrderer(detail),
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
                  color: AppColors.secondaryDark,
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
