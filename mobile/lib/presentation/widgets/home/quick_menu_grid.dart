import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 빠른 메뉴 아이템 데이터
class QuickMenuItem {
  /// 메뉴 아이콘
  final IconData icon;

  /// 메뉴 이름
  final String label;

  /// 이동 라우트
  final String? route;

  const QuickMenuItem({
    required this.icon,
    required this.label,
    this.route,
  });
}

/// 빠른 메뉴 그리드 위젯
///
/// 홈 화면의 #5 영역: 3x2 그리드 (아이콘 + 텍스트) 빠른 메뉴.
/// 내 일정, 매출 현황, 주문 관리, 활동 등록, 교육, 행사매출 등록
class QuickMenuGrid extends StatelessWidget {
  /// 메뉴 아이템 탭 콜백
  final void Function(QuickMenuItem item)? onMenuTap;

  const QuickMenuGrid({
    super.key,
    this.onMenuTap,
  });

  /// 기본 메뉴 목록
  static const List<QuickMenuItem> defaultMenuItems = [
    QuickMenuItem(icon: Icons.calendar_month, label: '내 일정'),
    QuickMenuItem(icon: Icons.bar_chart, label: '매출 현황'),
    QuickMenuItem(icon: Icons.assignment, label: '주문 관리'),
    QuickMenuItem(icon: Icons.checklist, label: '활동 등록'),
    QuickMenuItem(icon: Icons.menu_book, label: '교육'),
    QuickMenuItem(icon: Icons.celebration, label: '행사매출\n등록'),
  ];

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 3,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: AppSpacing.md,
      crossAxisSpacing: AppSpacing.md,
      childAspectRatio: 1.1,
      children: defaultMenuItems.map((item) {
        return _buildMenuItem(item);
      }).toList(),
    );
  }

  /// 메뉴 아이템 UI
  Widget _buildMenuItem(QuickMenuItem item) {
    return InkWell(
      onTap: onMenuTap != null ? () => onMenuTap!(item) : null,
      borderRadius: AppSpacing.cardBorderRadius,
      child: Container(
        decoration: BoxDecoration(
          color: AppColors.card,
          borderRadius: AppSpacing.cardBorderRadius,
          boxShadow: AppSpacing.cardShadow,
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              item.icon,
              size: AppSpacing.iconSizeMenu,
              color: AppColors.secondary,
            ),
            const SizedBox(height: AppSpacing.sm),
            Text(
              item.label,
              style: AppTypography.labelMedium,
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
