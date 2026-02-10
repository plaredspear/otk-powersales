import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 활동등록 메뉴 아이템 데이터
class ActivityMenuItem {
  /// 메뉴 아이콘
  final IconData icon;

  /// 메뉴 이름
  final String label;

  /// 이동 라우트 (null이면 "준비 중" 처리)
  final String? route;

  const ActivityMenuItem({
    required this.icon,
    required this.label,
    this.route,
  });
}

/// 활동등록 팝업 위젯
///
/// 영업사원이 현장 활동을 등록하기 위한 진입점.
/// 4개의 메뉴(유통기한 관리, 현장점검, 클레임 등록, 제안하기)를
/// BottomSheet 형태로 제공하며, 각 메뉴 선택 시 해당 화면으로 이동한다.
class ActivityRegistrationPopup extends StatelessWidget {
  /// 메뉴 아이템 탭 콜백
  final void Function(ActivityMenuItem item)? onMenuTap;

  const ActivityRegistrationPopup({
    super.key,
    this.onMenuTap,
  });

  /// 기본 메뉴 목록 (4개)
  static const List<ActivityMenuItem> defaultMenuItems = [
    ActivityMenuItem(
      icon: Icons.access_time,
      label: '유통기한 관리',
    ),
    ActivityMenuItem(
      icon: Icons.fact_check_outlined,
      label: '현장점검',
    ),
    ActivityMenuItem(
      icon: Icons.report_problem_outlined,
      label: '클레임 등록',
    ),
    ActivityMenuItem(
      icon: Icons.lightbulb_outline,
      label: '제안하기',
    ),
  ];

  /// BottomSheet로 활동등록 팝업을 표시
  static void show(
    BuildContext context, {
    void Function(ActivityMenuItem item)? onMenuTap,
  }) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppSpacing.radiusXl),
        ),
      ),
      builder: (context) => ActivityRegistrationPopup(
        onMenuTap: onMenuTap,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(AppSpacing.xl),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 핸들 바
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: AppColors.divider,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: AppSpacing.lg),

          // 타이틀
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '활동등록 하기',
                style: AppTypography.headlineMedium,
              ),
              // X 닫기 버튼
              GestureDetector(
                onTap: () => Navigator.of(context).pop(),
                child: const Icon(
                  Icons.close,
                  size: AppSpacing.iconSize,
                  color: AppColors.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppSpacing.lg),
          const Divider(height: 1, color: AppColors.divider),
          const SizedBox(height: AppSpacing.sm),

          // 메뉴 리스트
          ...defaultMenuItems.map(
            (item) => _buildMenuItem(context, item),
          ),

          const SizedBox(height: AppSpacing.sm),

          // 닫기 버튼
          SizedBox(
            width: double.infinity,
            child: OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(
                padding:
                    const EdgeInsets.symmetric(vertical: AppSpacing.md),
                shape: RoundedRectangleBorder(
                  borderRadius:
                      BorderRadius.circular(AppSpacing.radiusLg),
                ),
                side: const BorderSide(color: AppColors.border),
              ),
              child: Text(
                '닫기',
                style: AppTypography.labelLarge.copyWith(
                  color: AppColors.textSecondary,
                ),
              ),
            ),
          ),

          // SafeArea 하단 여백
          SizedBox(height: MediaQuery.of(context).padding.bottom),
        ],
      ),
    );
  }

  /// 개별 메뉴 아이템 UI
  Widget _buildMenuItem(BuildContext context, ActivityMenuItem item) {
    return InkWell(
      onTap: () {
        Navigator.of(context).pop();
        onMenuTap?.call(item);
      },
      borderRadius: BorderRadius.circular(AppSpacing.radiusMd),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.sm,
          vertical: AppSpacing.md,
        ),
        child: Row(
          children: [
            Icon(
              item.icon,
              size: AppSpacing.iconSize,
              color: AppColors.textPrimary,
            ),
            const SizedBox(width: AppSpacing.md),
            Text(
              item.label,
              style: AppTypography.bodyLarge.copyWith(
                fontWeight: FontWeight.w500,
              ),
            ),
            const Spacer(),
            const Icon(
              Icons.chevron_right,
              size: AppSpacing.iconSize,
              color: AppColors.textTertiary,
            ),
          ],
        ),
      ),
    );
  }
}
