import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:package_info_plus/package_info_plus.dart';
import '../../../app_router.dart';
import '../../../core/constants/menu_constants.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/menu_item.dart' as domain;
import '../../providers/auth_provider.dart';
import '../home/activity_registration_popup.dart';
import 'menu_group_widget.dart';
import 'menu_header.dart';
import 'quick_action_bar.dart';

/// 전체메뉴 Drawer 위젯
///
/// endDrawer로 사용되며 우측에서 슬라이드 인한다.
/// 화면 너비의 약 85%를 차지하며, 배경 딤 처리는 Scaffold가 자동 관리.
///
/// 구성:
/// - 헤더: 사용자명, 소속/직군, 닫기 버튼
/// - 빠른 액션: 제품 검색, 활동 등록
/// - 메뉴 그룹: 7개 그룹, 13개 아이템
///
/// 로그아웃은 프로필(내 정보) 화면에 있으므로 여기서는 제공하지 않는다.
class FullMenuDrawer extends ConsumerWidget {
  const FullMenuDrawer({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final user = authState.user;
    final screenWidth = MediaQuery.of(context).size.width;

    return SizedBox(
      width: screenWidth * 0.85,
      child: Drawer(
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.zero,
        ),
        child: Container(
          color: AppColors.white,
          child: Column(
            children: [
              // 헤더
              MenuHeader(
                userName: user?.name ?? '사용자',
                userInfo: _buildUserInfo(user),
                onClose: () => Navigator.of(context).pop(),
                onProfileTap: () {
                  Navigator.of(context).pop();
                  _navigateToRoute(context, AppRouter.profile);
                },
              ),
              // 빠른 액션 바
              QuickActionBar(
                onProductSearchTap: () {
                  Navigator.of(context).pop();
                  _navigateToRoute(context, '/product-search');
                },
                onActivityRegisterTap: () {
                  Navigator.of(context).pop();
                  // 홈 빠른 메뉴 "활동 등록"과 동일하게 활동등록 바텀시트를 띄운다.
                  ActivityRegistrationPopup.show(
                    context,
                    onMenuTap: (item) {
                      if (item.route != null) {
                        AppRouter.navigateTo(context, item.route!,
                            arguments: item.arguments);
                      }
                    },
                  );
                },
              ),
              // 스크롤 가능한 메뉴 영역
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      // 메뉴 그룹 목록 (조장은 "거래처" 다음에 "여사원 관리",
                      // AccountViewAll 은 "대리출근" 삽입)
                      ..._buildMenuGroups(context, user?.role, user?.rawRole),
                      // 메뉴 가장 하단에 현재 버전 표시 (메뉴와 함께 스크롤)
                      const _MenuVersionFooter(),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 메뉴 그룹 위젯 목록 생성
  ///
  /// 조장(LEADER)일 때만 "거래처" 그룹 다음에 "여사원 관리" 그룹을 삽입한다.
  /// 레거시 GNB(gnb.jsp:212-218) nav7 조건이 `eq '조장'` 정확 일치이므로,
  /// 지점장(ADMIN)·부서장(AccountViewAll)에게는 노출하지 않는다.
  ///
  /// 또한 조장(LEADER)·지점장(ADMIN)은 소비기한 기능을 사용하지 않으므로 'expiry'
  /// 메뉴를 제외하며, 제외 후 항목이 비는 그룹(제품)은 그룹 자체를 노출하지 않는다.
  List<Widget> _buildMenuGroups(
    BuildContext context,
    String? role,
    String? rawRole,
  ) {
    final isLeader = role == 'LEADER';
    final hideExpiry = role == 'LEADER' || role == 'ADMIN';
    // AccountViewAll(부서장)은 도메인 role 로는 USER 로 뭉개지므로 SF 원본 rawRole 로 판별한다.
    final isAccountViewAll = rawRole == 'AccountViewAll';

    // 조건부 삽입할 그룹 목록 구성
    final groups = <domain.MenuGroup>[];
    for (final group in MenuConstants.menuGroups) {
      if (hideExpiry) {
        final items =
            group.items.where((item) => item.id != 'expiry').toList();
        if (items.isEmpty) continue;
        groups.add(domain.MenuGroup(
          id: group.id,
          icon: group.icon,
          iconAsset: group.iconAsset,
          label: group.label,
          items: items,
        ));
      } else {
        groups.add(group);
      }
      if (isLeader && group.id == 'trade') {
        groups.add(MenuConstants.teamManagementGroup);
      }
      // AccountViewAll 전용 대리출근 그룹 — "거래처" 그룹 다음에 삽입.
      if (isAccountViewAll && group.id == 'trade') {
        groups.add(MenuConstants.proxyAttendanceGroup);
      }
    }

    return List.generate(groups.length, (index) {
      final group = groups[index];
      return MenuGroupWidget(
        group: group,
        isLast: index == groups.length - 1,
        onItemTap: (item) {
          Navigator.of(context).pop();
          _handleMenuItemTap(context, item);
        },
      );
    });
  }

  /// 사용자 정보 문자열 생성
  String _buildUserInfo(dynamic user) {
    if (user == null) return '';
    final orgName = user.orgName as String?;
    if (orgName != null && orgName.isNotEmpty) return orgName;
    return '';
  }

  /// 메뉴 아이템 탭 핸들러
  void _handleMenuItemTap(BuildContext context, domain.MenuItem item) {
    if (item.isImplemented && AppRouter.routes.containsKey(item.route)) {
      AppRouter.navigateTo(context, item.route!, arguments: item.arguments);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('준비 중입니다'),
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  /// 라우트 이동 (구현된 라우트만 Navigator, 미구현 시 스낵바)
  void _navigateToRoute(BuildContext context, String route) {
    // AppRouter에 정의된 라우트인지 확인
    if (AppRouter.routes.containsKey(route)) {
      AppRouter.navigateTo(context, route);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('준비 중입니다'),
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

}

/// 전체메뉴 목록 가장 하단에 현재 앱 버전을 표시하는 푸터.
///
/// 메뉴 영역과 함께 스크롤되며, pubspec.yaml 의 version(예: 1.0.1+3) 을
/// package_info_plus 로 읽어 "버전 1.0.1" 형태로 노출한다(빌드번호는 앱 정보 화면에서 확인).
class _MenuVersionFooter extends StatelessWidget {
  const _MenuVersionFooter();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: FutureBuilder<PackageInfo>(
        future: PackageInfo.fromPlatform(),
        builder: (context, snapshot) {
          final version = snapshot.data?.version;
          return Text(
            version != null ? '버전 $version' : '',
            textAlign: TextAlign.center,
            style: AppTypography.labelSmall.copyWith(
              color: AppColors.textTertiary,
            ),
          );
        },
      ),
    );
  }
}
