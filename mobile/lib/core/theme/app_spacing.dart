import 'package:flutter/material.dart';

/// 오뚜기 파워세일즈 앱 간격 및 사이즈 체계
///
/// PPTX 스크린샷 기반으로 추출한 간격/사이즈 규칙.
/// 디자인 가이드: apps/mobile/docs/DESIGN_GUIDE.md
abstract final class AppSpacing {
  // ─── Spacing Scale ───────────────────────────────────────
  /// 2dp - 미세 간격
  static const double xxs = 2;

  /// 4dp - 아이콘과 텍스트 사이
  static const double xs = 4;

  /// 8dp - 인접 요소 간격, 카드 내 작은 간격
  static const double sm = 8;

  /// 12dp - 카드 간 간격, 리스트 아이템 간격
  static const double md = 12;

  /// 16dp - 화면 좌우 패딩, 카드 내부 패딩
  static const double lg = 16;

  /// 20dp - 섹션 간 간격
  static const double xl = 20;

  /// 24dp - 주요 섹션 구분
  static const double xxl = 24;

  /// 32dp - 화면 상단/하단 여백
  static const double xxxl = 32;

  // ─── Common Padding ──────────────────────────────────────
  /// 화면 기본 좌우 패딩
  static const EdgeInsets screenHorizontal =
      EdgeInsets.symmetric(horizontal: lg);

  /// 화면 전체 패딩 (좌우 16, 상하 16)
  static const EdgeInsets screenAll = EdgeInsets.all(lg);

  /// 카드 내부 패딩
  static const EdgeInsets cardPadding = EdgeInsets.all(lg);

  /// 섹션 간 간격 (상단 24dp)
  static const EdgeInsets sectionTop = EdgeInsets.only(top: xxl);

  // ─── Border Radius ───────────────────────────────────────
  /// 4dp - 작은 태그, 배지
  static const double radiusSm = 4;

  /// 8dp - 입력 필드, 작은 버튼
  static const double radiusMd = 8;

  /// 12dp - 카드, 대화상자
  static const double radiusLg = 12;

  /// 16dp - 큰 카드, 모달
  static const double radiusXl = 16;

  /// 999dp - 원형 버튼, 아바타
  static const double radiusFull = 999;

  /// 카드 Border Radius
  static BorderRadius get cardBorderRadius =>
      BorderRadius.circular(radiusLg);

  /// 버튼 Border Radius
  static BorderRadius get buttonBorderRadius =>
      BorderRadius.circular(radiusMd);

  /// 입력 필드 Border Radius
  static BorderRadius get inputBorderRadius =>
      BorderRadius.circular(radiusMd);

  // ─── Home Layout (레거시 디자인 정렬용) ──────────────────
  /// 홈 노란 헤더 콘텐츠 영역 높이 (status bar 제외, 레거시 CSS height:116px)
  static const double homeHeaderHeight = 116;

  /// 스케줄 카드가 노란 헤더 위로 솟아오르는 깊이 (translate.y = -55)
  static const double homeCardOverlap = 55;

  /// 홈 카드 (스케줄/공지/검색) 모서리
  static const double homeCardRadius = 10;

  /// 스케줄 카드 등록 버튼 모서리
  static const double homeButtonRadius = 8;

  /// 출근 카운트 배지 (pill)
  static const double homePillRadius = 50;

  /// 홈 본문 좌우 거터
  static const double homeGutter = 20;

  /// 공지 카드 너비
  static const double homeNoticeCardWidth = 150;

  /// 공지 카드 높이
  static const double homeNoticeCardHeight = 146;

  /// 공지 카드 간 간격
  static const double homeNoticeCardGap = 7;

  /// 직원 정보 영역 캐릭터 일러스트 크기
  static const double homeProfileSize = 64;

  /// 카드 아래 ~ 프로필 사이 노란 strip 높이 (아바타가 걸치는 여백)
  static const double homeCardProfileGap = 14;

  /// 프로필 영역(아바타+텍스트)을 통째로 위로 끌어올려 노란 영역에 겹치는 깊이.
  /// 아바타와 텍스트의 세로 정렬은 유지(함께 이동)하고, 키가 큰 아바타 상단만 노랑에 걸친다.
  static const double homeProfileRaise = 14;

  // ─── Component Sizes ─────────────────────────────────────
  /// AppBar 높이
  static const double appBarHeight = 56;

  /// 탭 바 높이
  static const double tabBarHeight = 48;

  /// 하단 네비게이션 높이
  static const double bottomNavHeight = 56;

  /// Primary 버튼 높이
  static const double buttonHeight = 44;

  /// Small 버튼 높이
  static const double buttonHeightSmall = 32;

  /// 검색바 높이
  static const double searchBarHeight = 44;

  /// 아이콘 크기 - 기본
  static const double iconSize = 24;

  /// 아이콘 크기 - 메뉴 그리드
  static const double iconSizeMenu = 40;

  /// 아이콘 크기 - 네비게이션 화살표
  static const double iconSizeSmall = 16;

  /// 아바타 크기
  static const double avatarSize = 40;

  /// 날짜 네비게이션 화살표 버튼 크기
  static const double dateNavButtonSize = 32;

  /// 실적 카드 좌측 테두리 두께
  static const double metricCardBorderWidth = 4;

  /// 탭 인디케이터 두께
  static const double tabIndicatorWeight = 3;

  // ─── Elevation / Shadow ──────────────────────────────────
  /// 카드 기본 그림자 (기존 소스 CSS: box-shadow: 0 3px 10px rgba(0,0,0,0.1))
  static List<BoxShadow> get cardShadow => [
        BoxShadow(
          // ignore: deprecated_member_use
          color: const Color(0xFF000000).withOpacity(0.1),
          blurRadius: 10,
          offset: const Offset(0, 3),
        ),
      ];

  /// 플로팅 요소 그림자
  static List<BoxShadow> get floatingShadow => [
        BoxShadow(
          // ignore: deprecated_member_use
          color: const Color(0xFF000000).withOpacity(0.12),
          blurRadius: 6,
          offset: const Offset(0, 2),
        ),
      ];

  /// 모달/바텀시트 그림자
  static List<BoxShadow> get modalShadow => [
        BoxShadow(
          // ignore: deprecated_member_use
          color: const Color(0xFF000000).withOpacity(0.15),
          blurRadius: 12,
          offset: const Offset(0, 4),
        ),
      ];
}
