import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 정보 안내 배너.
///
/// 좌측 info 아이콘 + 안내 문구를 연한 파란 배경(`AppColors.info` 8% 투명)으로 표시한다.
/// 거래처별 주문 안내 / 전송 처리 중 안내 등 화면 전반에서 재사용하는 공통 스타일.
class InfoNoticeBanner extends StatelessWidget {
  /// 안내 문구.
  final String message;

  const InfoNoticeBanner({
    super.key,
    required this.message,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      // ignore: deprecated_member_use
      color: AppColors.info.withOpacity(0.08),
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: AppSpacing.sm,
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, size: 16, color: AppColors.info),
          const SizedBox(width: AppSpacing.xs),
          Expanded(
            child: Text(
              message,
              style: AppTypography.bodySmall.copyWith(color: AppColors.info),
            ),
          ),
        ],
      ),
    );
  }
}
