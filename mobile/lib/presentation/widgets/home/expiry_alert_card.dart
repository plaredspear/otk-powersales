import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/expiry_alert.dart';

/// 유통기한 알림 카드 위젯
///
/// 홈 화면의 #2 영역: 유통기한 임박제품 알림을 표시한다.
/// - expiryCount > 0: 알림 카드 표시
/// - expiryAlert가 null이거나 expiryCount == 0: 카드 숨김 (빈 SizedBox)
class ExpiryAlertCard extends StatelessWidget {
  /// 유통기한 알림 데이터 (null이면 카드 숨김)
  final ExpiryAlert? expiryAlert;

  /// 카드 탭 콜백 (유통기한 관리 화면으로 이동)
  final VoidCallback? onTap;

  const ExpiryAlertCard({
    super.key,
    this.expiryAlert,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    // null이거나 expiryCount가 0이면 숨김
    if (expiryAlert == null || expiryAlert!.expiryCount <= 0) {
      return const SizedBox.shrink();
    }

    final alert = expiryAlert!;

    return InkWell(
      onTap: onTap,
      borderRadius: AppSpacing.cardBorderRadius,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        child: Row(
          children: [
            // 프로필 아바타
            const CircleAvatar(
              radius: 24,
              backgroundColor: AppColors.surfaceVariant,
              child: Icon(
                Icons.person,
                size: 28,
                color: AppColors.textTertiary,
              ),
            ),
            const SizedBox(width: AppSpacing.md),

            // 지점/이름 + 유통기한 알림
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${alert.branchName}, ${alert.employeeName}(${alert.employeeId})',
                    style: AppTypography.bodyMedium.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: AppSpacing.xxs),
                  Row(
                    children: [
                      const Icon(
                        Icons.notifications_outlined,
                        size: 16,
                        color: AppColors.textSecondary,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '유통기한 임박제품 : ',
                        style: AppTypography.bodySmall.copyWith(
                          color: AppColors.textSecondary,
                        ),
                      ),
                      Text(
                        '${alert.expiryCount}건',
                        style: AppTypography.bodySmall.copyWith(
                          color: AppColors.otokiBlue,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
