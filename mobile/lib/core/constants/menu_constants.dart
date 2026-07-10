import 'package:flutter/material.dart';
import '../../domain/entities/menu_item.dart';
import '../../domain/entities/suggestion_form.dart';

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
        MenuItem(id: 'expiry', label: '소비기한 관리', route: '/product-expiration'),
      ],
    ),
    // 4. 매출 현황
    MenuGroup(
      id: 'sales',
      icon: Icons.monetization_on,
      iconAsset: 'assets/images/icon_nav4.png',
      label: '매출 현황',
      // 레거시(promotion/event/list.jsp + month/list.jsp)는 행사 매출/월 매출이
      // 상단 tab_menu로 묶인 하나의 "매출 현황" 화면이다. 두 항목 모두 통합 2탭
      // 화면(/sales-status)으로 보내되 각자 해당 탭으로 진입한다.
      items: [
        MenuItem(
          id: 'event-sales',
          label: '행사 매출',
          route: '/sales-status',
          arguments: 0, // 행사 매출 탭
        ),
        MenuItem(
          id: 'monthly-sales',
          label: '월 매출',
          route: '/sales-status',
          arguments: 1, // 월 매출 탭
        ),
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
        // 제안하기 진입 — 기본 분류를 신제품 제안으로 연다(물류 클레임 제외).
        MenuItem(
          id: 'proposals',
          label: '제안하기',
          route: '/suggestion/register',
          arguments: SuggestionCategory.newProduct,
        ),
        MenuItem(id: 'claims', label: '제품 클레임 등록', route: '/claim/register'),
        MenuItem(id: 'claim-list', label: '제품 클레임 조회', route: '/claim/list'),
        MenuItem(
          id: 'suggestions',
          label: '물류 클레임 등록',
          route: '/suggestion/register',
        ),
        // 레거시 GNB "내 물류클레임 조회"(logisticsclaimlist) 정합 — 물류클레임 전용 목록.
        // (레거시에는 제안/물류클레임 통합 목록이 없음)
        MenuItem(
          id: 'logistics-claim-list',
          label: '물류 클레임 조회',
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

  /// 조장 전용: "여사원 관리" 메뉴 그룹
  ///
  /// FullMenuDrawer에서 user.role이 LEADER(조장)일 때만
  /// "거래처" 그룹 다음 위치에 삽입한다. 레거시 GNB(gnb.jsp) nav7 조건이
  /// `eq '조장'` 정확 일치이므로 지점장/부서장에게는 노출하지 않는다.
  ///
  /// 레거시 Heroku GNB "여사원 관리" 그룹 정합(gnb.jsp): 항목은 "여사원"(/employee/main)
  /// 과 "일정 / 등록"(/employee/mgnSchedule) 2개뿐이다. "일정 / 등록"은 월간 캘린더
  /// 진입점이며, 날짜를 탭하면 일별현황(mngDaily)으로 이어져 변경/등록을 수행한다.
  static const MenuGroup teamManagementGroup = MenuGroup(
    id: 'team-management',
    icon: Icons.groups,
    label: '여사원 관리',
    items: [
      MenuItem(
        id: 'female-staff',
        label: '여사원',
        route: '/leader/female-staff',
      ),
      MenuItem(
        id: 'team-schedule',
        label: '일정 / 등록',
        route: '/leader/schedule',
      ),
    ],
  );

  /// AccountViewAll(부서장) 전용 그룹 — 지점 선택형 대리출근.
  ///
  /// AccountViewAll 은 전사 성격이라 지점을 직접 선택해 여사원 대리출근을 등록한다
  /// (조장의 costCenterCode 자동 고정과 다름). `full_menu_drawer` 에서 rawRole=AccountViewAll
  /// 일 때만 삽입한다.
  static const MenuGroup proxyAttendanceGroup = MenuGroup(
    id: 'proxy-attendance',
    icon: Icons.how_to_reg,
    label: '대리출근',
    items: [
      MenuItem(
        id: 'proxy-attendance',
        label: '대리출근 등록',
        route: '/proxy-attendance',
      ),
    ],
  );

  /// 총 메뉴 그룹 수
  static int get groupCount => menuGroups.length;

  /// 총 메뉴 아이템 수
  static int get itemCount =>
      menuGroups.fold(0, (sum, group) => sum + group.items.length);
}
