import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';
import '../../../core/theme/app_typography.dart';

/// 앱 공통 Primary 버튼
///
/// 활성 상태: OTOKI Blue 배경 + 흰색 텍스트
/// 비활성 상태 (onPressed == null): 옅은 회색 배경 + 회색 텍스트
/// 로딩 상태: 비활성 + CircularProgressIndicator
class PrimaryButton extends StatelessWidget {
  /// 버튼 텍스트
  final String text;

  /// 탭 콜백 (null이면 비활성 상태)
  final VoidCallback? onPressed;

  /// 로딩 중 여부
  final bool isLoading;

  /// 버튼 높이 (기본값: AppSpacing.buttonHeight = 44)
  final double? height;

  /// 텍스트 폰트 크기 (기본값: 15)
  final double fontSize;

  const PrimaryButton({
    super.key,
    required this.text,
    this.onPressed,
    this.isLoading = false,
    this.height,
    this.fontSize = 15,
  });

  bool get _isEnabled => onPressed != null && !isLoading;

  @override
  Widget build(BuildContext context) {
    final buttonHeight = height ?? AppSpacing.buttonHeight;

    return SizedBox(
      width: double.infinity,
      height: buttonHeight,
      child: ElevatedButton(
        onPressed: _isEnabled ? onPressed : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.otokiBlue,
          foregroundColor: AppColors.white,
          disabledBackgroundColor: const Color(0xFFE0E0E0),
          disabledForegroundColor: AppColors.textTertiary,
          shape: RoundedRectangleBorder(
            borderRadius: AppSpacing.buttonBorderRadius,
          ),
          elevation: 0,
        ),
        child: isLoading
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppColors.white,
                ),
              )
            : Text(
                text,
                style: AppTypography.labelLarge.copyWith(
                  fontSize: fontSize,
                  fontWeight: FontWeight.w600,
                  color: _isEnabled ? AppColors.white : AppColors.textTertiary,
                ),
              ),
      ),
    );
  }
}
