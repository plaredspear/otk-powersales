import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/theme/app_typography.dart';
import '../pages/home_page.dart';
import '../widgets/menu/full_menu_drawer.dart';

/// 메인 화면
///
/// 홈 화면 + 하단 3버튼 네비게이션 바 + 전체메뉴 endDrawer 구성.
///
/// 하단 네비게이션 바 구조 (기획서 기준):
/// | ← 뒤로 | 🏠 홈으로 | ≡ 전체메뉴 |
///
/// 기존 5탭(POS매출/전산매출/물류매출/목표진도율)은 전체메뉴 내에서 접근.
class MainScreen extends ConsumerStatefulWidget {
  const MainScreen({super.key});

  @override
  ConsumerState<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends ConsumerState<MainScreen> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  /// 전체메뉴 Drawer 열기 (외부에서 호출 가능하도록)
  void openFullMenu() {
    _scaffoldKey.currentState?.openEndDrawer();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      body: const HomePage(),
      endDrawer: const FullMenuDrawer(),
      // endDrawer의 scrim 기본 동작: 배경 딤 + 탭 시 닫힘
      endDrawerEnableOpenDragGesture: false,
      bottomNavigationBar: _BottomNavBar(
        onBackTap: _handleBack,
        onHomeTap: _handleHome,
        onMenuTap: openFullMenu,
      ),
    );
  }

  /// 뒤로 가기
  void _handleBack() {
    if (Navigator.of(context).canPop()) {
      Navigator.of(context).pop();
    }
  }

  /// 홈으로 가기 (스택 클리어)
  void _handleHome() {
    AppRouter.navigateToAndRemoveAll(context, AppRouter.main);
  }
}

/// 하단 3버튼 네비게이션 바
///
/// 기획서 기준: ← 뒤로 | 🏠 홈으로 | ≡ 전체메뉴
class _BottomNavBar extends StatelessWidget {
  final VoidCallback onBackTap;
  final VoidCallback onHomeTap;
  final VoidCallback onMenuTap;

  const _BottomNavBar({
    required this.onBackTap,
    required this.onHomeTap,
    required this.onMenuTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppColors.bottomNavBackground,
        border: Border(
          top: BorderSide(color: AppColors.divider, width: 1),
        ),
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 56,
          child: Row(
            children: [
              // ← 뒤로
              Expanded(
                child: _NavButton(
                  icon: Icons.arrow_back,
                  label: '뒤로',
                  onTap: onBackTap,
                ),
              ),
              // 🏠 홈으로
              Expanded(
                child: _NavButton(
                  icon: Icons.home,
                  label: '홈으로',
                  onTap: onHomeTap,
                ),
              ),
              // ≡ 전체메뉴
              Expanded(
                child: _NavButton(
                  icon: Icons.menu,
                  label: '전체메뉴',
                  onTap: onMenuTap,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// 개별 네비게이션 버튼
class _NavButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _NavButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            icon,
            size: 22,
            color: AppColors.textSecondary,
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: AppTypography.labelSmall.copyWith(
              fontSize: 11,
              color: AppColors.textSecondary,
            ),
          ),
        ],
      ),
    );
  }
}
