import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 빠른 액션 버튼 바
///
/// 전체메뉴 상단에 "제품 검색"과 "활동 등록" 버튼을 가로로 나열한다.
class QuickActionBar extends StatelessWidget {
  /// 제품 검색 버튼 탭 콜백
  final VoidCallback? onProductSearchTap;

  /// 활동 등록 버튼 탭 콜백
  final VoidCallback? onActivityRegisterTap;

  const QuickActionBar({
    super.key,
    this.onProductSearchTap,
    this.onActivityRegisterTap,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xl,
        vertical: AppSpacing.md,
      ),
      child: Row(
        children: [
          // 제품 검색 버튼
          Expanded(
            child: _ActionButton(
              icon: Icons.search,
              label: '제품 검색',
              backgroundColor: AppColors.otokiBlue,
              textColor: AppColors.white,
              onTap: onProductSearchTap,
            ),
          ),
          const SizedBox(width: AppSpacing.sm),
          // 활동 등록 버튼
          Expanded(
            child: _ActionButton(
              icon: Icons.add,
              label: '활동 등록',
              backgroundColor: AppColors.otokiYellow,
              textColor: AppColors.textPrimary,
              onTap: onActivityRegisterTap,
            ),
          ),
        ],
      ),
    );
  }
}

/// 개별 액션 버튼
class _ActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color backgroundColor;
  final Color textColor;
  final VoidCallback? onTap;

  const _ActionButton({
    required this.icon,
    required this.label,
    required this.backgroundColor,
    required this.textColor,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: backgroundColor,
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
        child: Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.md,
            vertical: AppSpacing.sm + 2,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 18, color: textColor),
              const SizedBox(width: AppSpacing.xs),
              Text(
                label,
                style: AppTypography.labelLarge.copyWith(
                  color: textColor,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
