import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처 건수 헤더 위젯
///
/// "거래처 (N)" 형식으로 현재 표시 중인 거래처 수를 보여줍니다.
class AccountCountHeader extends StatelessWidget {
  /// 표시할 거래처 수
  final int count;

  const AccountCountHeader({
    super.key,
    required this.count,
  });

  @override
  Widget build(BuildContext context) {
    // 레거시(list.jsp): "거래처 (N)" — (N)만 빨강 #DC2C34, 하단 구분선
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      decoration: const BoxDecoration(
        border: Border(
          bottom: BorderSide(color: AppColors.divider, width: 1),
        ),
      ),
      child: Text.rich(
        TextSpan(
          style: AppTypography.headlineSmall.copyWith(
            color: AppColors.textPrimary,
          ),
          children: [
            const TextSpan(text: '거래처 '),
            TextSpan(
              text: '($count)',
              style: const TextStyle(color: AppColors.legacyDanger),
            ),
          ],
        ),
      ),
    );
  }
}
