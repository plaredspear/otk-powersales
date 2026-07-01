import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../app_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/suggestion_form.dart';
import '../../providers/auth_provider.dart';

/// 활동등록 메뉴 아이템 데이터
class ActivityMenuItem {
  /// 메뉴 아이콘 자산 경로 (레거시 GNB와 동일한 PNG)
  final String iconAsset;

  /// 메뉴 이름
  final String label;

  /// 이동 라우트 (null이면 "준비 중" 처리)
  final String? route;

  /// 라우트 이동 시 전달할 인자 (예: 물류클레임 전용 진입 분류)
  final Object? arguments;

  const ActivityMenuItem({
    required this.iconAsset,
    required this.label,
    this.route,
    this.arguments,
  });
}

/// 활동등록 팝업 위젯
///
/// 영업사원이 현장 활동을 등록하기 위한 진입점.
/// 7개의 메뉴(소비기한 관리, 현장 점검 등록, 제안하기(신제품 제안 등),
/// 제품 클레임 등록, 제품 클레임 조회, 물류 클레임 등록, 물류 클레임 조회)를
/// BottomSheet 형태로 제공하며, 각 메뉴 선택 시 해당 화면으로 이동한다.
/// (레거시 GNB "활동 등록" 정합)
class ActivityRegistrationPopup extends ConsumerWidget {
  /// 메뉴 아이템 탭 콜백
  final void Function(ActivityMenuItem item)? onMenuTap;

  const ActivityRegistrationPopup({
    super.key,
    this.onMenuTap,
  });

  /// 기본 메뉴 목록 (7개, 레거시 GNB "활동 등록" 순서 정합)
  ///
  /// 아이콘은 레거시 GNB(gnb.jsp `.active_navN`)의 PNG(ico_activeN.png)를
  /// 그대로 가져와 적용한다. (active_nav 매핑: 소비기한=2, 현장점검=4,
  /// 제안하기=5, 클레임등록=3, 내클레임/내물류클레임=1)
  static const List<ActivityMenuItem> defaultMenuItems = [
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active2.png',
      label: '소비기한 관리',
      route: AppRouter.productExpiration,
    ),
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active4.png',
      label: '현장 점검 등록',
      route: AppRouter.inspectionRegister,
    ),
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active5.png',
      label: '제안하기(신제품 제안 등)',
      route: AppRouter.suggestionRegister,
      // 제안하기 진입 — 기본 분류를 신제품 제안으로 연다(물류 클레임 제외).
      arguments: SuggestionCategory.newProduct,
    ),
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active3.png',
      label: '제품 클레임 등록',
      route: AppRouter.claimRegister,
    ),
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active1.png',
      label: '제품 클레임 조회',
      route: AppRouter.claimList,
    ),
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active5.png',
      label: '물류 클레임 등록',
      route: AppRouter.suggestionRegister,
      // 물류 클레임 전용 진입 — 기본 분류 물류 클레임.
      arguments: SuggestionCategory.logisticsClaim,
    ),
    ActivityMenuItem(
      iconAsset: 'assets/images/ico_active1.png',
      label: '물류 클레임 조회',
      // 레거시 logisticsclaimlist 정합: 물류클레임 전용 목록(suggestionList 는 항상 물류클레임만 조회)
      route: AppRouter.suggestionList,
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
  Widget build(BuildContext context, WidgetRef ref) {
    // 소비기한 관리는 여사원(role == 'USER')에게만 노출.
    // 조장(LEADER)/지점장(ADMIN)에게는 숨긴다. (드로워 hideExpiry 정합)
    final isFemaleStaff = ref.watch(authProvider).user?.role == 'USER';
    final menuItems = isFemaleStaff
        ? defaultMenuItems
        : defaultMenuItems
            .where((item) => item.route != AppRouter.productExpiration)
            .toList();

    return Container(
      color: AppColors.background,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 우측 상단 X 닫기 버튼 (레거시 정합: 진한 색, 큰 크기)
          Align(
            alignment: Alignment.centerRight,
            child: Padding(
              padding: const EdgeInsets.only(
                top: AppSpacing.md,
                right: AppSpacing.md,
              ),
              child: IconButton(
                icon: const Icon(Icons.close, size: AppSpacing.iconSizeMenu),
                color: AppColors.textPrimary,
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
          ),

          // 메뉴 리스트 (레거시 정합: 구분선 없이 넉넉한 간격)
          for (final item in menuItems)
            _buildMenuItem(context, item),

          // SafeArea 하단 여백
          SizedBox(
            height: AppSpacing.xl + MediaQuery.of(context).padding.bottom,
          ),
        ],
      ),
    );
  }

  /// 개별 메뉴 아이템 UI - 레거시 노랑 아이콘 + 라벨, 화살표 없음
  Widget _buildMenuItem(BuildContext context, ActivityMenuItem item) {
    return InkWell(
      onTap: () {
        Navigator.of(context).pop();
        onMenuTap?.call(item);
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.xl,
          vertical: AppSpacing.lg + AppSpacing.xs,
        ),
        child: Row(
          children: [
            Image.asset(
              item.iconAsset,
              width: AppSpacing.iconSize,
              height: AppSpacing.iconSize,
              fit: BoxFit.contain,
            ),
            const SizedBox(width: AppSpacing.lg),
            Expanded(
              child: Text(
                item.label,
                style: AppTypography.bodyLarge.copyWith(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
