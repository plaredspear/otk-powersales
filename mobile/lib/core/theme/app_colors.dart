import 'package:flutter/material.dart';

/// 오뚜기 파워세일즈 앱 색상 팔레트
///
/// 오뚜기 공식 CI 컬러 시스템 기반 색상 체계.
/// CI 출처: https://www.otoki.com/about/ci
abstract final class AppColors {
  // ─── Brand (OTOKI CI 공식 컬러) ─────────────────────────
  /// OTOKI YELLOW - PANTONE 108, CMYK 0/0/100/0, RGB 255/243/0
  static const Color otokiYellow = Color(0xFFFFF300);

  /// OTOKI RED - PANTONE 186, CMYK 0/100/80/5, RGB 211/35/58
  static const Color otokiRed = Color(0xFFD3233A);

  /// OTOKI BLUE - PANTONE 2747, CMYK 100/79/0/9, RGB 10/48/158
  static const Color otokiBlue = Color(0xFF0A309E);

  // ─── Primary (Yellow) ────────────────────────────────────
  /// 주요 액션 버튼: 등록, 검색, 조회
  static const Color primary = Color(0xFFFFF300);

  /// Primary pressed 상태
  static const Color primaryDark = Color(0xFFD6CC00);

  /// 배지, 하이라이트 배경
  static const Color primaryLight = Color(0xFFFFF87A);

  /// Primary 버튼 위 텍스트 (검정)
  static const Color onPrimary = Color(0xFF212121);

  // ─── Secondary (Blue) ────────────────────────────────────
  /// 탭 인디케이터, 날짜 네비게이션, 링크
  static const Color secondary = Color(0xFF0A309E);

  /// Secondary pressed 상태
  static const Color secondaryDark = Color(0xFF071F6A);

  /// 배경 하이라이트
  static const Color secondaryLight = Color(0xFF3A5EC8);

  /// Secondary 위 텍스트 (흰색)
  static const Color onSecondary = Color(0xFFFFFFFF);

  // ─── Semantic ────────────────────────────────────────────
  /// 실적 카드 좌측 테두리, 긍정 수치 값
  static const Color success = Color(0xFF4CAF50);

  /// Success 강조
  static const Color successDark = Color(0xFF388E3C);

  /// 차트 막대, 데이터 시각화
  static const Color chartBlue = Color(0xFF5B9BD5);

  /// 에러 상태, 경고
  static const Color error = Color(0xFFD32F2F);

  /// 에러 배경
  static const Color errorLight = Color(0xFFFFEBEE);

  /// 주의 상태
  static const Color warning = Color(0xFFFF8F00);

  /// 정보성 메시지
  static const Color info = Color(0xFF1976D2);

  // ─── Neutral / Background ────────────────────────────────
  /// 메인 화면 배경
  static const Color background = Color(0xFFFFFFFF);

  /// 섹션 배경, 필터 영역
  static const Color surface = Color(0xFFF5F7FA);

  /// 입력 필드 배경, 검색바
  static const Color surfaceVariant = Color(0xFFF0F0F0);

  /// 카드 배경
  static const Color card = Color(0xFFFFFFFF);

  /// 구분선
  static const Color divider = Color(0xFFE0E0E0);

  /// 카드 테두리
  static const Color border = Color(0xFFEEEEEE);

  // ─── Text ────────────────────────────────────────────────
  /// 제목, 본문 텍스트
  static const Color textPrimary = Color(0xFF212121);

  /// 보조 설명, 단위 텍스트
  static const Color textSecondary = Color(0xFF757575);

  /// 플레이스홀더, 비활성 텍스트
  static const Color textTertiary = Color(0xFF9E9E9E);

  // ─── Navigation ──────────────────────────────────────────
  /// 하단 네비게이션 바 배경
  static const Color bottomNavBackground = Color(0xFFFAFAFA);

  // ─── Utility ─────────────────────────────────────────────
  static const Color white = Color(0xFFFFFFFF);
  static const Color black = Color(0xFF000000);
  static const Color transparent = Color(0x00000000);

  /// 오버레이 배경 (반투명 검정)
  static const Color overlay = Color(0x4D000000); // 30%

  /// Snackbar 배경
  static const Color snackbarBackground = Color(0xE6212121); // 90%
}
