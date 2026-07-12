import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mobile/domain/entities/client_order.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처별 주문 카드 위젯
///
/// SAP 주문번호, 거래처명, 총 주문금액을 표시합니다.
class ClientOrderCard extends StatelessWidget {
  final ClientOrder order;
  final VoidCallback? onTap;

  const ClientOrderCard({
    super.key,
    required this.order,
    this.onTap,
  });

  String _formatAmount(int amount) {
    return NumberFormat('#,###').format(amount);
  }

  @override
  Widget build(BuildContext context) {
    final isMine = order.isMine;
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.md,
        vertical: AppSpacing.xs,
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.all(AppSpacing.md),
          decoration: BoxDecoration(
            // 내 주문은 네이비 테두리(굵게) + 옅은 배경으로 강조 — 담당자 무관 전체 목록에서 본인 주문 식별.
            // ignore: deprecated_member_use
            color: isMine ? AppColors.otokiBlue.withOpacity(0.06) : null,
            border: Border.all(
              color: isMine ? AppColors.otokiBlue : AppColors.border,
              width: isMine ? 1.5 : 1,
            ),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      'SAP 주문번호: ${order.sapOrderNumber}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ),
                  if (isMine) _buildMineBadge(),
                ],
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                order.clientName,
                style: AppTypography.headlineSmall,
              ),
              const SizedBox(height: AppSpacing.xs),
              Text(
                '총 주문금액 ${_formatAmount(order.totalAmount)}원',
                style: AppTypography.bodyMedium.copyWith(
                  color: AppColors.secondaryDark,
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// "내 주문" 뱃지 — 본인이 등록한 주문 표시.
  Widget _buildMineBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        color: AppColors.otokiBlue,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Text(
        '내 주문',
        style: AppTypography.labelSmall.copyWith(
          color: AppColors.white,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}
