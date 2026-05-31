import 'package:flutter/material.dart';
import '../../domain/entities/menu_item.dart';

/// 전체메뉴 상수 데이터
///
/// 7개 그룹, 15개 메뉴 아이템으로 구성.
/// route가 null인 항목은 미구현 상태(탭 시 "준비 중입니다" 안내).
abstract final class MenuConstants {
  /// 전체 메뉴 그룹 목록
  static const List<MenuGroup> menuGroups = [
    // 1. 거래처
    MenuGroup(
      id: 'trade',
      icon: Icons.store,
      label: '거래처',
      items: [
        MenuItem(id: 'my-accounts', label: '내 거래처', route: '/my-accounts'),
      ],
    ),
    // 2. 주문
    MenuGroup(
      id: 'order',
      icon: Icons.shopping_cart,
      label: '주문',
      items: [
        MenuItem(id: 'orders', label: '내 주문 현황'),
        MenuItem(id: 'orders-by-account', label: '거래처별 주문 현황'),
      ],
    ),
    // 3. 제품
    MenuGroup(
      id: 'product',
      icon: Icons.inventory_2,
      label: '제품',
      items: [
        MenuItem(id: 'expiry', label: '유통기한 관리', route: '/product-expiration'),
      ],
    ),
    // 4. 매출 현황
    MenuGroup(
      id: 'sales',
      icon: Icons.monetization_on,
      label: '매출 현황',
      items: [
        MenuItem(id: 'pos-sales', label: '행사 매출', route: '/pos-sales'),
        MenuItem(id: 'monthly-sales', label: '월 매출'),
      ],
    ),
    // 5. 현장톡
    MenuGroup(
      id: 'field',
      icon: Icons.assignment,
      label: '현장톡',
      items: [
        MenuItem(id: 'field-check', label: '현장 점검'),
        MenuItem(id: 'claim-list', label: '클레임 현황', route: '/claim/list'),
        MenuItem(id: 'claims', label: '클레임 등록'),
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
      label: '커뮤니티',
      items: [
        MenuItem(id: 'education', label: '교육'),
        MenuItem(id: 'notices', label: '공지사항', route: '/notices'),
      ],
    ),
    // 7. 마이페이지
    MenuGroup(
      id: 'mypage',
      icon: Icons.person,
      label: '마이페이지',
      items: [
        MenuItem(id: 'my-schedule', label: '내 일정/등록', route: '/my-schedule'),
        MenuItem(
          id: 'alt-holiday-request',
          label: '대체휴무 신청',
          route: '/alt-holiday/request',
        ),
        MenuItem(
          id: 'alt-holiday-history',
          label: '대체휴무 이력',
          route: '/alt-holiday/history',
        ),
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
    ],
  );

  /// 총 메뉴 그룹 수
  static int get groupCount => menuGroups.length;

  /// 총 메뉴 아이템 수
  static int get itemCount =>
      menuGroups.fold(0, (sum, group) => sum + group.items.length);
}
