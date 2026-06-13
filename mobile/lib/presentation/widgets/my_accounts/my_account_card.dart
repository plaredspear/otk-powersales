import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/my_account.dart';

/// 내 거래처 카드 위젯
///
/// 거래처명(코드), 주소, 대표자, 전화 버튼을 표시합니다.
/// 카드를 탭하면 거래처 상세 팝업이 열립니다.
class MyAccountCard extends StatelessWidget {
  /// 거래처 정보
  final MyAccount account;

  /// 카드 탭 콜백 (거래처 선택)
  final VoidCallback onTap;

  /// 전화 버튼 탭 콜백
  final VoidCallback? onPhoneTap;

  const MyAccountCard({
    super.key,
    required this.account,
    required this.onTap,
    this.onPhoneTap,
  });

  @override
  Widget build(BuildContext context) {
    // 레거시(list.jsp): 카드가 아닌 평면 full-width 행 + 하단 구분선
    return InkWell(
      onTap: onTap,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.lg,
          vertical: AppSpacing.md,
        ),
        decoration: const BoxDecoration(
          color: AppColors.white,
          border: Border(
            bottom: BorderSide(color: AppColors.divider, width: 1),
          ),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            // 거래처 정보 영역
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 거래처명(코드)
                  Text(
                    '${account.accountName}(${account.accountCode})',
                    style: AppTypography.headlineSmall,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: AppSpacing.xs),
                  // 주소
                  if (account.address != null)
                    Text(
                      account.address!,
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                  if (account.representativeName != null) ...[
                    const SizedBox(height: AppSpacing.xxs),
                    // 대표자 (레거시: "대표자 :" 콜론 앞 공백)
                    Text(
                      '대표자 : ${account.representativeName}',
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textTertiary,
                      ),
                    ),
                  ],
                ],
              ),
            ),
            // 전화 버튼 (레거시 ico_tel.png: 초록 원형 + 흰 수화기)
            if (account.phoneNumber != null && account.phoneNumber!.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(left: AppSpacing.md),
                child: InkWell(
                  onTap: onPhoneTap,
                  customBorder: const CircleBorder(),
                  child: Padding(
                    padding: const EdgeInsets.all(4),
                    child: Image.asset(
                      'assets/images/ico_tel.png',
                      width: 36,
                      height: 36,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
