import 'package:flutter/material.dart';
import 'app_colors.dart';

/// 오뚜기 파워세일즈 앱 타이포그래피 체계
///
/// PPTX 스크린샷 기반으로 추출한 텍스트 스타일.
/// 디자인 가이드: apps/mobile/docs/DESIGN_GUIDE.md
abstract final class AppTypography {
  /// 앱 기본 폰트 패밀리 (오뚜기 산스체)
  ///
  /// weight 매핑: ExtraLight(200) / Light(300) / Medium(500) / Bold(700) / ExtraBold(800)
  /// w400·w600 등 미보유 weight는 Flutter가 가장 가까운 weight로 폴백.
  static const String fontFamily = 'OtokiSans';

  // ─── Display ─────────────────────────────────────────────
  /// 실적 금액 등 대형 숫자 (28sp, Bold)
  /// 사용: "2,921만원" 등 핵심 수치
  static const TextStyle displayLarge = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.w700,
    height: 1.3,
    color: AppColors.textPrimary,
  );

  // ─── Headline ────────────────────────────────────────────
  /// 화면 제목 (20sp, Bold)
  /// 사용: "매출 현황", AppBar 제목
  static const TextStyle headlineLarge = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.w700,
    height: 1.3,
    color: AppColors.textPrimary,
  );

  /// 섹션 제목 (18sp, Bold)
  /// 사용: "당월 매출 실적", "전년 대비 동월 실적"
  /// OtokiSans 미보유 w600 → 보유 weight Bold(700)로 고정
  static const TextStyle headlineMedium = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w700,
    height: 1.35,
    color: AppColors.textPrimary,
  );

  /// 카드 제목, 날짜 (16sp, Bold)
  /// 사용: "마감합계 실적", "2025년 12월"
  /// OtokiSans 미보유 w600 → 보유 weight Bold(700)로 고정
  static const TextStyle headlineSmall = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w700,
    height: 1.35,
    color: AppColors.textPrimary,
  );

  // ─── Body ────────────────────────────────────────────────
  /// 본문 텍스트 (16sp, Light)
  /// OtokiSans 미보유 w400 → 보유 weight Light(300)로 고정
  static const TextStyle bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w300,
    height: 1.5,
    color: AppColors.textPrimary,
  );

  /// 일반 본문, 리스트 항목 (14sp, Light)
  static const TextStyle bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w300,
    height: 1.5,
    color: AppColors.textPrimary,
  );

  /// 날짜, 부가 설명 (12sp, Light)
  static const TextStyle bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w300,
    height: 1.4,
    color: AppColors.textSecondary,
  );

  // ─── Label ───────────────────────────────────────────────
  /// 버튼 텍스트 (14sp, Medium)
  static const TextStyle labelLarge = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w500,
    height: 1.4,
    color: AppColors.textPrimary,
  );

  /// 탭 레이블, 배지 (12sp, Medium)
  static const TextStyle labelMedium = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w500,
    height: 1.3,
    color: AppColors.textPrimary,
  );

  /// 단위 텍스트 (10sp, Medium)
  /// 사용: "단위 : 만원"
  static const TextStyle labelSmall = TextStyle(
    fontSize: 10,
    fontWeight: FontWeight.w500,
    height: 1.2,
    color: AppColors.textSecondary,
  );

  // ─── Legacy (Heroku 디자인 정렬용) ───────────────────────
  /// 레거시 폰트 패밀리 (앱 기본 폰트와 동일하게 오뚜기 산스체 사용)
  static const String legacyFontFamily = fontFamily;

  /// 스케줄 카드 날짜 헤더 (22/800)
  static const TextStyle legacyTitleXXL = TextStyle(
    fontFamily: legacyFontFamily,
    fontSize: 22,
    fontWeight: FontWeight.w800,
    height: 1.4,
    letterSpacing: -0.55,
    color: AppColors.textPrimary,
  );

  /// 직원 정보 지점·이름 (18/800)
  static const TextStyle legacyTitleLG = TextStyle(
    fontFamily: legacyFontFamily,
    fontSize: 18,
    fontWeight: FontWeight.w800,
    height: 1.4,
    letterSpacing: -0.9,
    color: AppColors.textPrimary,
  );

  /// 공지 카드 헤더 (16/800)
  static const TextStyle legacyTitleMD = TextStyle(
    fontFamily: legacyFontFamily,
    fontSize: 16,
    fontWeight: FontWeight.w800,
    height: 1.4,
    letterSpacing: -0.4,
    color: AppColors.textPrimary,
  );

  /// 등록 버튼 텍스트 (16/700)
  static const TextStyle legacyButton = TextStyle(
    fontFamily: legacyFontFamily,
    fontSize: 16,
    fontWeight: FontWeight.w700,
    height: 1.4,
    letterSpacing: -0.4,
    color: AppColors.white,
  );

  /// 퀵 메뉴 라벨, 일반 본문 (15/700)
  static const TextStyle legacyBody = TextStyle(
    fontFamily: legacyFontFamily,
    fontSize: 15,
    fontWeight: FontWeight.w700,
    height: 1.4,
    letterSpacing: -0.25,
    color: AppColors.textPrimary,
  );

  /// 공지 작성일 (14/300)
  /// OtokiSans 미보유 w400 → 보유 weight Light(300)로 고정
  static const TextStyle legacyCaption = TextStyle(
    fontFamily: legacyFontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w300,
    height: 1.4,
    letterSpacing: -0.25,
    color: AppColors.legacyTextMute,
  );

  // ─── TextTheme (Material 3 매핑) ─────────────────────────
  /// Material 3 TextTheme으로 변환
  static TextTheme get textTheme => const TextTheme(
        displayLarge: displayLarge,
        headlineLarge: headlineLarge,
        headlineMedium: headlineMedium,
        headlineSmall: headlineSmall,
        bodyLarge: bodyLarge,
        bodyMedium: bodyMedium,
        bodySmall: bodySmall,
        labelLarge: labelLarge,
        labelMedium: labelMedium,
        labelSmall: labelSmall,
      );
}
