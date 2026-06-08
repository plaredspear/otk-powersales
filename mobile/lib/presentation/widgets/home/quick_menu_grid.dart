import 'package:flutter/material.dart';
import '../../../app_router.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../core/theme/app_colors.dart';

/// 빠른 메뉴 아이템 데이터
class QuickMenuItem {
  /// PNG 에셋 경로
  final String assetPath;

  /// 메뉴 이름
  final String label;

  /// 이동 라우트
  final String? route;

  /// Material 아이콘 (에셋이 없는 경우 대체)
  final IconData? iconData;

  const QuickMenuItem({
    this.assetPath = '',
    required this.label,
    this.route,
    this.iconData,
  });
}

/// 빠른 메뉴 그리드 위젯
///
/// 홈 화면의 #5 영역: 3x2 그리드 (아이콘 + 텍스트) 빠른 메뉴.
/// 내 일정, 매출 현황, 주문 관리, 활동 등록, 교육, 행사매출 등록
///
/// 조장(LEADER)/지점장(ADMIN): "행사매출 등록" 탭 시 스낵바 안내 + 이동 차단
class QuickMenuGrid extends StatelessWidget {
  /// 메뉴 아이템 탭 콜백
  final void Function(QuickMenuItem item)? onMenuTap;

  /// 사용자 역할 ("USER", "LEADER", "ADMIN")
  final String userRole;

  const QuickMenuGrid({
    super.key,
    this.onMenuTap,
    this.userRole = 'USER',
  });

  /// 기본 메뉴 목록
  static const List<QuickMenuItem> defaultMenuItems = [
    QuickMenuItem(assetPath: 'assets/images/ico_quick1.png', label: '내 일정', route: AppRouter.myScheduleCalendar),
    QuickMenuItem(assetPath: 'assets/images/ico_quick2.png', label: '매출 현황', route: AppRouter.salesOverview),
    QuickMenuItem(assetPath: 'assets/images/ico_quick3.png', label: '주문 관리', route: AppRouter.orderList),
    QuickMenuItem(assetPath: 'assets/images/ico_quick4.png', label: '활동 등록'),
    QuickMenuItem(assetPath: 'assets/images/ico_quick5.png', label: '교육', route: AppRouter.education),
    QuickMenuItem(assetPath: 'assets/images/ico_quick6.png', label: '행사매출\n등록'),
  ];

  /// 관리자 전용 메뉴 목록
  static const List<QuickMenuItem> adminMenuItems = [
    QuickMenuItem(iconData: Icons.fact_check_outlined, label: '안전점검\n현황', route: AppRouter.safetyCheckStatus),
  ];

  List<QuickMenuItem> get _menuItems {
    final isLeaderOrAdmin = userRole == 'LEADER' || userRole == 'ADMIN';
    if (isLeaderOrAdmin) {
      return [...defaultMenuItems, ...adminMenuItems];
    }
    return defaultMenuItems;
  }

  @override
  Widget build(BuildContext context) {
    final items = _menuItems;
    // 레거시(common.css .main_quick_nav): 3열, 행 간 15px, 셀 높이는 콘텐츠
    // (아이콘+라벨) 높이를 그대로 따른다. 정사각형(childAspectRatio:1.0)으로
    // 강제하지 않아 셀 위아래에 불필요한 여백이 생기지 않는다.
    const rowGap = 15.0;
    final rows = <Widget>[];
    for (var start = 0; start < items.length; start += 3) {
      final rowItems = items.skip(start).take(3).toList();
      rows.add(
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: List.generate(
            3,
            (col) => Expanded(
              child: col < rowItems.length
                  ? _buildMenuItem(rowItems[col])
                  : const SizedBox.shrink(),
            ),
          ),
        ),
      );
    }
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        for (var i = 0; i < rows.length; i++) ...[
          if (i > 0) const SizedBox(height: rowGap),
          rows[i],
        ],
      ],
    );
  }

  /// 메뉴 아이템 UI - 레거시 라벨 15/700/#333, 아이콘-라벨 간격 5
  Widget _buildMenuItem(QuickMenuItem item) {
    return InkWell(
      onTap: onMenuTap != null ? () => onMenuTap!(item) : null,
      borderRadius: BorderRadius.circular(AppSpacing.homeCardRadius),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (item.iconData != null)
            Icon(
              item.iconData,
              size: AppSpacing.iconSizeMenu,
              color: AppColors.secondary,
            )
          else
            Image.asset(
              item.assetPath,
              width: AppSpacing.iconSizeMenu,
              height: AppSpacing.iconSizeMenu,
            ),
          const SizedBox(height: 5),
          Text(
            item.label,
            style: AppTypography.legacyBody.copyWith(
              color: AppColors.legacyTextSub,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
