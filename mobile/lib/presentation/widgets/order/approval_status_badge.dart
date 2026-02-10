import 'package:flutter/material.dart';

import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/order.dart';

/// 승인상태 뱃지 위젯
///
/// 주문의 승인상태를 색상 뱃지로 표시합니다.
/// 승인완료(녹색), 승인상태(노란색), 전송실패(빨간색), 재전송(주황색)
class ApprovalStatusBadge extends StatelessWidget {
  /// 승인상태
  final ApprovalStatus status;

  const ApprovalStatusBadge({
    super.key,
    required this.status,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.sm,
        vertical: AppSpacing.xxs,
      ),
      decoration: BoxDecoration(
        // ignore: deprecated_member_use
        color: status.color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        border: Border.all(
          // ignore: deprecated_member_use
          color: status.color.withOpacity(0.4),
          width: 1,
        ),
      ),
      child: Text(
        status.displayName,
        style: AppTypography.labelMedium.copyWith(
          color: status.color,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}
