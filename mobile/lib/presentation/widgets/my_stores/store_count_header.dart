import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 거래처 건수 헤더 위젯
///
/// "거래처 (N)" 형식으로 현재 표시 중인 거래처 수를 보여줍니다.
class StoreCountHeader extends StatelessWidget {
  /// 표시할 거래처 수
  final int count;

  const StoreCountHeader({
    super.key,
    required this.count,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Text(
        '거래처 ($count)',
        style: AppTypography.headlineSmall.copyWith(
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}
