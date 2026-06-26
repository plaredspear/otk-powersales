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

  const QuickMenuItem({
    this.assetPath = '',
    required this.label,
    this.route,
  });
}

/// 빠른 메뉴 그리드 위젯
///
/// 홈 화면의 #5 영역: 3x2 그리드 (아이콘 + 텍스트) 빠른 메뉴.
/// 내 일정, 매출 현황, 주문 관리, 활동 등록, 교육, 행사매출 등록
/// (레거시 Heroku home.jsp .main_quick_nav 6개 항목과 1:1 정합)
///
/// 조장(LEADER):
/// - 조장은 본인 행사/진열 일정이 없어 "내 일정"이 무의미하므로, 첫 번째 항목을
///   "일정 관리"(팀원 월간 일정 캘린더, 날짜 카드 navy 버튼과 동일 목적지)로 대체한다.
///
/// 조장(LEADER)·지점장(ADMIN):
/// - 행사매출은 행사 담당자(여사원)만 등록하므로 "행사매출 등록" 항목을 숨긴다.
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

  /// 빠른 메뉴 목록 (레거시 Heroku home.jsp 정합 — 6개)
  static const List<QuickMenuItem> menuItems = [
    QuickMenuItem(assetPath: 'assets/images/ico_quick1.png', label: '내 일정', route: AppRouter.myScheduleCalendar),
    QuickMenuItem(assetPath: 'assets/images/ico_quick2.png', label: '매출 현황', route: AppRouter.salesOverview),
    QuickMenuItem(assetPath: 'assets/images/ico_quick3.png', label: '주문 관리', route: AppRouter.orderList),
    QuickMenuItem(assetPath: 'assets/images/ico_quick4.png', label: '활동 등록'),
    QuickMenuItem(assetPath: 'assets/images/ico_quick5.png', label: '교육', route: AppRouter.education),
    QuickMenuItem(assetPath: 'assets/images/ico_quick6.png', label: '행사매출\n등록'),
  ];

  /// 조장(LEADER): 첫 항목을 "일정 관리"(팀원 월간 일정)로 대체한 목록
  static const QuickMenuItem _leaderScheduleItem = QuickMenuItem(
    assetPath: 'assets/images/ico_quick1.png',
    label: '일정 관리',
    route: AppRouter.leaderSchedule,
  );

  /// 역할별 빠른 메뉴 목록
  /// - 조장(LEADER): 첫 항목을 "일정 관리"로 대체 (레거시 `eq '조장'` 정확 일치)
  /// - 조장(LEADER)·지점장(ADMIN): "행사매출 등록" 항목 제외
  List<QuickMenuItem> get _resolvedItems {
    final hidePromotionSales = userRole == 'LEADER' || userRole == 'ADMIN';
    final base = hidePromotionSales
        ? menuItems.where((item) => item.label != '행사매출\n등록').toList()
        : menuItems;
    if (userRole != 'LEADER') return base;
    return [_leaderScheduleItem, ...base.skip(1)];
  }

  @override
  Widget build(BuildContext context) {
    final items = _resolvedItems;
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
