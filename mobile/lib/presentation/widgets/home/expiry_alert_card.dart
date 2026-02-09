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
      child: Container(
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: AppSpacing.cardBorderRadius,
          boxShadow: AppSpacing.cardShadow,
          border: Border.all(
            color: AppColors.warning.withOpacity(0.3),
            width: 1,
          ),
        ),
        child: Padding(
          padding: AppSpacing.cardPadding,
          child: Row(
            children: [
              // 경고 아이콘
              Container(
                padding: const EdgeInsets.all(AppSpacing.sm),
                decoration: BoxDecoration(
                  color: AppColors.warning.withOpacity(0.1),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.warning_amber_rounded,
                  color: AppColors.warning,
                  size: AppSpacing.iconSize,
                ),
              ),
              const SizedBox(width: AppSpacing.md),

              // 알림 내용
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${alert.branchName}, ${alert.employeeName}(${alert.employeeId})',
                      style: AppTypography.bodyMedium,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: AppSpacing.xxs),
                    Text(
                      '유통기한 임박제품 : ${alert.expiryCount}건',
                      style: AppTypography.bodyMedium.copyWith(
                        color: AppColors.warning,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),

              // 화살표
              const Icon(
                Icons.chevron_right,
                color: AppColors.textTertiary,
                size: AppSpacing.iconSize,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
