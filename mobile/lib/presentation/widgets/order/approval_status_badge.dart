import 'package:flutter/material.dart';

import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order_request.dart';

/// 승인상태 뱃지 위젯
///
/// 주문의 승인상태를 색상 뱃지로 표시합니다.
/// 표시명(한글)은 서버가 내려준 [statusName] 을 그대로 출력하고, 색상만 상태 코드([statusCode])로 결정합니다.
class OrderRequestStatusBadge extends StatelessWidget {
  /// 승인상태 코드 (예: APPROVED) — 색상 결정용. 서버 `null`(SF NULL row) 시 회색.
  final String? statusCode;

  /// 승인상태 표시명 (예: 승인완료) — 서버 제공 라벨. `null` 이면 빈 라벨.
  final String? statusName;

  const OrderRequestStatusBadge({
    super.key,
    required this.statusCode,
    required this.statusName,
  });

  @override
  Widget build(BuildContext context) {
    final color = OrderStatusCode.color(statusCode);
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        // ignore: deprecated_member_use
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        border: Border.all(
          // ignore: deprecated_member_use
          color: color.withOpacity(0.4),
          width: 1,
        ),
      ),
      child: Text(
        statusName ?? '',
        style: AppTypography.labelMedium.copyWith(
          color: color,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}
