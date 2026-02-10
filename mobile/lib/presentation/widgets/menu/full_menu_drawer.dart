import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../app_router.dart';
import '../../../core/constants/menu_constants.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';
import '../../../domain/entities/menu_item.dart' as domain;
import '../../providers/auth_provider.dart';
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
/// - 로그아웃 버튼
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
                  _navigateToRoute(context, '/my-schedule');
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
                  _navigateToRoute(context, '/activity-register');
                },
              ),
              const Divider(
                height: 1,
                thickness: 1,
                color: AppColors.divider,
              ),
              // 스크롤 가능한 메뉴 영역
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      // 메뉴 그룹 목록
                      ...List.generate(
                        MenuConstants.menuGroups.length,
                        (index) {
                          final group = MenuConstants.menuGroups[index];
                          return MenuGroupWidget(
                            group: group,
                            isLast:
                                index == MenuConstants.menuGroups.length - 1,
                            onItemTap: (item) {
                              Navigator.of(context).pop();
                              _handleMenuItemTap(context, item);
                            },
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ),
              // 하단 구분선
              const Divider(
                height: 1,
                thickness: 1,
                color: AppColors.divider,
              ),
              // 로그아웃 버튼
              _LogoutButton(
                onTap: () => _handleLogout(context, ref),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// 사용자 정보 문자열 생성
  String _buildUserInfo(dynamic user) {
    if (user == null) return '';
    final parts = <String>[];
    if (user.department.isNotEmpty) parts.add(user.department);
    if (user.branchName.isNotEmpty) parts.add(user.branchName);
    return parts.join(' / ');
  }

  /// 메뉴 아이템 탭 핸들러
  void _handleMenuItemTap(BuildContext context, domain.MenuItem item) {
    if (item.isImplemented) {
      _navigateToRoute(context, item.route!);
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

  /// 로그아웃 처리
  void _handleLogout(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('로그아웃'),
        content: const Text('로그아웃 하시겠습니까?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              '취소',
              style: TextStyle(color: AppColors.textSecondary),
            ),
          ),
          TextButton(
            onPressed: () async {
              Navigator.of(dialogContext).pop(); // 다이얼로그 닫기
              Navigator.of(context).pop(); // Drawer 닫기
              await ref.read(authProvider.notifier).logout();
              if (context.mounted) {
                AppRouter.navigateToAndRemoveAll(context, AppRouter.login);
              }
            },
            child: Text(
              '확인',
              style: TextStyle(color: AppColors.otokiRed),
            ),
          ),
        ],
      ),
    );
  }
}

/// 로그아웃 버튼 위젯
class _LogoutButton extends StatelessWidget {
  final VoidCallback onTap;

  const _LogoutButton({required this.onTap});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: InkWell(
        onTap: onTap,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.xl,
            vertical: AppSpacing.lg,
          ),
          child: Text(
            '로그아웃',
            style: AppTypography.bodyMedium.copyWith(
              color: AppColors.textSecondary,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ),
    );
  }
}
