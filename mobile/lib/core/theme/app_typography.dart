import 'package:flutter/material.dart';
import 'app_colors.dart';

/// 오뚜기 파워세일즈 앱 타이포그래피 체계
///
/// PPTX 스크린샷 기반으로 추출한 텍스트 스타일.
/// 디자인 가이드: apps/mobile/docs/DESIGN_GUIDE.md
abstract final class AppTypography {
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

  /// 섹션 제목 (18sp, SemiBold)
  /// 사용: "당월 매출 실적", "전년 대비 동월 실적"
  static const TextStyle headlineMedium = TextStyle(
    fontSize: 18,
    fontWeight: FontWeight.w600,
    height: 1.35,
    color: AppColors.textPrimary,
  );

  /// 카드 제목, 날짜 (16sp, SemiBold)
  /// 사용: "마감합계 실적", "2025년 12월"
  static const TextStyle headlineSmall = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w600,
    height: 1.35,
    color: AppColors.textPrimary,
  );

  // ─── Body ────────────────────────────────────────────────
  /// 본문 텍스트 (16sp, Regular)
  static const TextStyle bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.w400,
    height: 1.5,
    color: AppColors.textPrimary,
  );

  /// 일반 본문, 리스트 항목 (14sp, Regular)
  static const TextStyle bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.5,
    color: AppColors.textPrimary,
  );

  /// 날짜, 부가 설명 (12sp, Regular)
  static const TextStyle bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w400,
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
