import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/expiry_alert.dart';

/// 소비기한 알림 카드 위젯
///
/// 홈 화면의 #2 영역: 소비기한 임박제품 알림을 표시한다.
/// - expiryAlert != null: 카드 표시 (0건이어도 표시)
/// - expiryAlert == null: 카드 숨김 (API 미응답)
class ExpiryAlertCard extends StatelessWidget {
  /// 소비기한 알림 데이터 (null이면 카드 숨김)
  final ExpiryAlert? expiryAlert;

  /// 카드 탭 콜백 (소비기한 관리 화면으로 이동)
  final VoidCallback? onTap;

  /// 소비기한 임박제품 라인 노출 여부.
  /// 조장(LEADER)·지점장(ADMIN)은 소비기한 기능을 사용하지 않으므로 false로
  /// 전달해 라인을 숨기고 프로필(지점/이름) 행만 표시한다.
  final bool showExpiryCount;

  const ExpiryAlertCard({
    super.key,
    this.expiryAlert,
    this.onTap,
    this.showExpiryCount = true,
  });

  @override
  Widget build(BuildContext context) {
    // null이면 숨김 (API 미응답). 0건이어도 카드 표시.
    if (expiryAlert == null) {
      return const SizedBox.shrink();
    }

    final alert = expiryAlert!;

    return InkWell(
      // 소비기한 라인을 숨긴 경우(조장·지점장) 소비기한 화면 이동도 비활성화한다.
      onTap: showExpiryCount ? onTap : null,
      borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // 프로필 일러스트 (레거시 64×64)
            Image.asset(
              'assets/images/img_profile.png',
              width: AppSpacing.homeProfileSize,
              height: AppSpacing.homeProfileSize,
              fit: BoxFit.contain,
            ),
            const SizedBox(width: 10),

            // 지점/이름 + 소비기한 알림
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${alert.branchName}, ${alert.employeeName}(${alert.employeeCode})',
                    style: AppTypography.legacyTitleLG,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (showExpiryCount) ...[
                    const SizedBox(height: 5),
                    Row(
                      children: [
                        Image.asset(
                          'assets/images/ico_alert.png',
                          width: 15,
                          height: 18,
                          fit: BoxFit.contain,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          '소비기한 임박제품 : ',
                          style: AppTypography.legacyBody,
                        ),
                        Text(
                          '${alert.expiryCount}건',
                          style: AppTypography.legacyBody.copyWith(
                            color: AppColors.legacyDanger,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
