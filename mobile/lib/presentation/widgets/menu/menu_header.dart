import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 전체메뉴 헤더 위젯
///
/// 사용자명, 소속/직군 정보, 닫기(X) 버튼을 표시한다.
/// 사용자명 영역의 ">" 화살표를 탭하면 마이페이지로 이동한다.
class MenuHeader extends StatelessWidget {
  /// 사용자 이름
  final String userName;

  /// 소속/직군 정보 (예: "G마트A (진열/전담/순회)")
  final String userInfo;

  /// 닫기 버튼 콜백
  final VoidCallback onClose;

  /// 사용자명 영역 탭 콜백 (마이페이지로 이동)
  final VoidCallback? onProfileTap;

  const MenuHeader({
    super.key,
    required this.userName,
    required this.userInfo,
    required this.onClose,
    this.onProfileTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(
        AppSpacing.xl,
        AppSpacing.lg,
        AppSpacing.lg,
        AppSpacing.lg,
      ),
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(
          bottom: BorderSide(color: AppColors.divider, width: 1),
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 사용자 정보 영역
            Expanded(
              child: GestureDetector(
                onTap: onProfileTap,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          '$userName님',
                          style: AppTypography.headlineMedium.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(width: AppSpacing.xs),
                        if (onProfileTap != null)
                          const Icon(
                            Icons.chevron_right,
                            size: 22,
                            color: AppColors.textPrimary,
                          ),
                      ],
                    ),
                    const SizedBox(height: AppSpacing.xs),
                    Text(
                      userInfo,
                      style: AppTypography.bodySmall.copyWith(
                        color: AppColors.textSecondary,
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // 닫기 버튼
            GestureDetector(
              onTap: onClose,
              child: const Padding(
                padding: EdgeInsets.all(AppSpacing.xs),
                child: Icon(
                  Icons.close,
                  size: AppSpacing.iconSize,
                  color: AppColors.textPrimary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
