import 'package:flutter/material.dart';
import '../../domain/entities/menu_item.dart';

/// 전체메뉴 상수 데이터
///
/// 7개 그룹, 16개 메뉴 아이템으로 구성.
/// route가 null인 항목은 미구현 상태(탭 시 "준비 중입니다" 안내).
abstract final class MenuConstants {
  /// 전체 메뉴 그룹 목록
  static const List<MenuGroup> menuGroups = [
    // 1. 거래처
    MenuGroup(
      id: 'trade',
      icon: Icons.store,
      iconAsset: 'assets/images/icon_nav1.png',
      label: '거래처',
      items: [
        MenuItem(id: 'my-accounts', label: '내 거래처', route: '/my-accounts'),
      ],
    ),
    // 2. 주문
    MenuGroup(
      id: 'order',
      icon: Icons.shopping_cart,
      iconAsset: 'assets/images/icon_nav2.png',
      label: '주문',
      items: [
        MenuItem(id: 'orders', label: '내 주문 현황', route: '/order-list'),
        MenuItem(
          id: 'orders-by-account',
          label: '거래처별 주문 현황',
          route: '/order-list',
          arguments: 1, // 거래처별 주문 탭 (index 1)
        ),
      ],
    ),
    // 3. 제품
    MenuGroup(
      id: 'product',
      icon: Icons.inventory_2,
      iconAsset: 'assets/images/icon_nav3.png',
      label: '제품',
      items: [
        MenuItem(id: 'expiry', label: '유통기한 관리', route: '/product-expiration'),
      ],
    ),
    // 4. 매출 현황
    MenuGroup(
      id: 'sales',
      icon: Icons.monetization_on,
      iconAsset: 'assets/images/icon_nav4.png',
      label: '매출 현황',
      items: [
        MenuItem(id: 'event-sales', label: '행사 매출', route: '/promotions'),
        MenuItem(id: 'monthly-sales', label: '월 매출', route: '/monthly-sales'),
      ],
    ),
    // 5. 현장톡
    MenuGroup(
      id: 'field',
      icon: Icons.assignment,
      iconAsset: 'assets/images/icon_nav5.png',
      label: '현장톡',
      items: [
        MenuItem(id: 'field-check', label: '현장 점검', route: '/inspection-list'),
        MenuItem(id: 'claim-list', label: '클레임 현황', route: '/claim/list'),
        MenuItem(id: 'claims', label: '클레임 등록', route: '/claim/register'),
        MenuItem(
          id: 'suggestions',
          label: '제안하기',
          route: '/suggestion/register',
        ),
        MenuItem(
          id: 'suggestion-list',
          label: '내 제안/물류클레임',
          route: '/suggestion/list',
        ),
      ],
    ),
    // 6. 커뮤니티
    MenuGroup(
      id: 'community',
      icon: Icons.campaign,
      iconAsset: 'assets/images/icon_nav6.png',
      label: '커뮤니티',
      items: [
        MenuItem(id: 'education', label: '교육', route: '/education'),
        MenuItem(id: 'notices', label: '공지사항', route: '/notices'),
      ],
    ),
    // 7. 마이페이지
    MenuGroup(
      id: 'mypage',
      icon: Icons.person,
      iconAsset: 'assets/images/icon_nav8.png',
      label: '마이페이지',
      items: [
        MenuItem(id: 'my-schedule', label: '내 일정/등록', route: '/my-schedule'),
        MenuItem(
          id: 'change-password',
          label: '비밀번호 변경',
          route: '/verify-password',
        ),
        MenuItem(
          id: 'app-info',
          label: '앱 정보',
          route: '/app-info',
        ),
      ],
    ),
  ];

  /// 조장/지점장 전용: "팀 관리" 메뉴 그룹
  ///
  /// FullMenuDrawer에서 user.role이 LEADER 또는 ADMIN일 때
  /// "거래처" 그룹 다음 위치에 삽입한다.
  static const MenuGroup teamManagementGroup = MenuGroup(
    id: 'team-management',
    icon: Icons.groups,
    label: '팀 관리',
    items: [
      MenuItem(
        id: 'team-schedule',
        label: '팀원 일정 관리',
        route: '/leader/team-members',
      ),
      MenuItem(
        id: 'team-daily-status',
        label: '여사원 일별현황',
        route: '/leader/daily-status',
      ),
    ],
  );

  /// 총 메뉴 그룹 수
  static int get groupCount => menuGroups.length;

  /// 총 메뉴 아이템 수
  static int get itemCount =>
      menuGroups.fold(0, (sum, group) => sum + group.items.length);
}
