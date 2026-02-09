import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'app_colors.dart';
import 'app_typography.dart';
import 'app_spacing.dart';

/// 오뚜기 파워세일즈 앱 테마 설정
///
/// Material 3 기반 ThemeData 구성.
/// 디자인 가이드: apps/mobile/docs/DESIGN_GUIDE.md
abstract final class AppTheme {
  /// 라이트 테마 (기본)
  static ThemeData get light {
    // ignore: deprecated_member_use
    const colorScheme = ColorScheme(
      brightness: Brightness.light,
      // Primary = Yellow (주요 액션)
      primary: AppColors.primary,
      onPrimary: AppColors.onPrimary,
      primaryContainer: AppColors.primaryLight,
      onPrimaryContainer: AppColors.textPrimary,
      // Secondary = Blue (탭, 네비게이션)
      secondary: AppColors.secondary,
      onSecondary: AppColors.onSecondary,
      secondaryContainer: AppColors.secondaryLight,
      onSecondaryContainer: AppColors.textPrimary,
      // Error
      error: AppColors.error,
      onError: AppColors.white,
      errorContainer: AppColors.errorLight,
      onErrorContainer: AppColors.error,
      // Surface & Background
      // ignore: deprecated_member_use
      background: AppColors.background,
      // ignore: deprecated_member_use
      onBackground: AppColors.textPrimary,
      surface: AppColors.surface,
      onSurface: AppColors.textPrimary,
      onSurfaceVariant: AppColors.textSecondary,
      // Outline
      outline: AppColors.border,
      outlineVariant: AppColors.divider,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      textTheme: AppTypography.textTheme,

      // ─── Scaffold ──────────────────────────────────────
      scaffoldBackgroundColor: AppColors.background,

      // ─── AppBar ────────────────────────────────────────
      appBarTheme: const AppBarTheme(
        backgroundColor: AppColors.white,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
        scrolledUnderElevation: 0.5,
        centerTitle: true,
        titleTextStyle: AppTypography.headlineLarge,
        systemOverlayStyle: SystemUiOverlayStyle.dark,
        iconTheme: IconThemeData(
          color: AppColors.textPrimary,
          size: AppSpacing.iconSize,
        ),
      ),

      // ─── TabBar ────────────────────────────────────────
      tabBarTheme: TabBarTheme(
        labelColor: AppColors.textPrimary,
        unselectedLabelColor: AppColors.textSecondary,
        labelStyle: AppTypography.labelMedium.copyWith(
          fontWeight: FontWeight.w600,
        ),
        unselectedLabelStyle: AppTypography.labelMedium,
        indicatorColor: AppColors.secondary,
        indicatorSize: TabBarIndicatorSize.tab,
      ),

      // ─── ElevatedButton (Primary Button) ───────────────
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.primary,
          foregroundColor: AppColors.onPrimary,
          elevation: 0,
          minimumSize: const Size.fromHeight(AppSpacing.buttonHeight),
          padding: const EdgeInsets.symmetric(horizontal: 24),
          shape: RoundedRectangleBorder(
            borderRadius: AppSpacing.buttonBorderRadius,
          ),
          textStyle: AppTypography.labelLarge,
        ),
      ),

      // ─── OutlinedButton (Secondary Button) ─────────────
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: AppColors.textPrimary,
          minimumSize: const Size.fromHeight(AppSpacing.buttonHeight),
          padding: const EdgeInsets.symmetric(horizontal: 24),
          shape: RoundedRectangleBorder(
            borderRadius: AppSpacing.buttonBorderRadius,
          ),
          side: const BorderSide(color: AppColors.border),
          textStyle: AppTypography.labelLarge,
        ),
      ),

      // ─── TextButton ────────────────────────────────────
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.secondary,
          textStyle: AppTypography.labelLarge,
        ),
      ),

      // ─── Card ──────────────────────────────────────────
      cardTheme: CardTheme(
        color: AppColors.card,
        elevation: 1,
        // ignore: deprecated_member_use
        shadowColor: AppColors.black.withOpacity(0.08),
        shape: RoundedRectangleBorder(
          borderRadius: AppSpacing.cardBorderRadius,
        ),
        margin: const EdgeInsets.symmetric(vertical: AppSpacing.md / 2),
      ),

      // ─── Input (TextField) ─────────────────────────────
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.surfaceVariant,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: AppSpacing.md,
          vertical: AppSpacing.md,
        ),
        hintStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.textTertiary,
        ),
        border: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.secondary, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: AppSpacing.inputBorderRadius,
          borderSide: const BorderSide(color: AppColors.error),
        ),
      ),

      // ─── Divider ───────────────────────────────────────
      dividerTheme: const DividerThemeData(
        color: AppColors.divider,
        thickness: 1,
        space: 0,
      ),

      // ─── BottomNavigationBar ───────────────────────────
      bottomNavigationBarTheme: const BottomNavigationBarThemeData(
        backgroundColor: AppColors.bottomNavBackground,
        selectedItemColor: AppColors.secondary,
        unselectedItemColor: AppColors.textSecondary,
        type: BottomNavigationBarType.fixed,
        elevation: 0,
        selectedLabelStyle: AppTypography.labelMedium,
        unselectedLabelStyle: AppTypography.labelMedium,
      ),

      // ─── SnackBar ──────────────────────────────────────
      snackBarTheme: SnackBarThemeData(
        backgroundColor: AppColors.snackbarBackground,
        contentTextStyle: AppTypography.bodyMedium.copyWith(
          color: AppColors.white,
        ),
        shape: RoundedRectangleBorder(
          borderRadius: AppSpacing.buttonBorderRadius,
        ),
        behavior: SnackBarBehavior.floating,
        elevation: 0,
      ),

      // ─── Dialog ────────────────────────────────────────
      dialogTheme: DialogTheme(
        backgroundColor: AppColors.card,
        elevation: 3,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusXl),
        ),
        titleTextStyle: AppTypography.headlineMedium,
        contentTextStyle: AppTypography.bodyMedium,
      ),

      // ─── BottomSheet ───────────────────────────────────
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: AppColors.card,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(
            top: Radius.circular(AppSpacing.radiusXl),
          ),
        ),
        elevation: 3,
      ),

      // ─── Progress Indicator ────────────────────────────
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.primary,
        linearTrackColor: AppColors.surfaceVariant,
        circularTrackColor: AppColors.surfaceVariant,
      ),

      // ─── Chip ──────────────────────────────────────────
      chipTheme: ChipThemeData(
        backgroundColor: AppColors.surface,
        labelStyle: AppTypography.labelMedium,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(AppSpacing.radiusSm),
        ),
        side: const BorderSide(color: AppColors.border),
      ),
    );
  }
}
