import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_spacing.dart';

/// 로딩 인디케이터 위젯
///
/// 데이터 로딩 중임을 사용자에게 표시하는 공통 위젯입니다.
///
/// 기본 형태는 **카드형** — 흰 배경 위에 옅은 회색 카드(테두리 + 그림자) 안에
/// 네이비 스피너와 안내 문구를 함께 표시합니다. 스피너 선 하나만으로는
/// 흰 배경에서 인지되지 않아, "움직이는 덩어리" 로 보이도록 면적을 확보한 것입니다.
///
/// 리스트 하단 페이지네이션·버튼 내부처럼 좁은 영역에는 카드가 과하므로
/// [LoadingIndicator.inline] 을 사용합니다.
///
/// 사용 예시:
/// ```dart
/// LoadingIndicator()                                  // 카드형 + 기본 문구
/// LoadingIndicator(message: '주문 목록을 불러오는 중...')  // 카드형 + 지정 문구
/// LoadingIndicator.inline()                           // 카드 없이 스피너만
/// LoadingIndicator.fullScreen(message: '처리 중...')     // 화면 전체 덮기
/// ```
class LoadingIndicator extends StatelessWidget {
  /// 카드형에서 문구를 지정하지 않았을 때 사용하는 기본 문구.
  ///
  /// "빈 화면" 으로 오인되지 않도록 카드형은 항상 문구를 동반한다.
  static const String defaultMessage = '불러오는 중...';

  /// 로딩 메시지 (선택사항)
  final String? message;

  /// 인디케이터 크기
  final double size;

  /// 인디케이터 색상
  final Color? color;

  /// 전체 화면 모드 여부
  final bool isFullScreen;

  /// 배경 색상 (전체 화면 모드에서만 사용)
  final Color? backgroundColor;

  /// 카드(배경 + 테두리 + 그림자) 표시 여부
  final bool showCard;

  const LoadingIndicator({
    super.key,
    this.message,
    this.size = 40.0,
    this.color,
    this.isFullScreen = false,
    this.backgroundColor,
  }) : showCard = true;

  /// 좁은 영역용 인디케이터 (카드 없이 스피너만)
  const LoadingIndicator.inline({
    super.key,
    this.message,
    this.size = 40.0,
    this.color,
  }) : isFullScreen = false,
       backgroundColor = null,
       showCard = false;

  /// 전체 화면 로딩 인디케이터 (편의 생성자)
  const LoadingIndicator.fullScreen({
    super.key,
    this.message,
    this.size = 40.0,
    this.color,
    this.backgroundColor,
  }) : isFullScreen = true,
       showCard = true;

  @override
  Widget build(BuildContext context) {
    final indicator = _buildIndicator(context);

    if (isFullScreen) {
      return Container(
        color: backgroundColor ?? Colors.white.withValues(alpha: 0.9),
        child: Center(child: indicator),
      );
    }

    return Center(child: indicator);
  }

  Widget _buildIndicator(BuildContext context) {
    // primaryColor(노랑)는 흰 배경에서 보이지 않으므로
    // progressIndicatorTheme 의 인디케이터 전용 색을 따른다.
    final indicatorColor =
        color ??
        ProgressIndicatorTheme.of(context).color ??
        AppColors.secondary;

    final label = showCard ? (message ?? defaultMessage) : message;

    final content = Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: size,
          height: size,
          child: CircularProgressIndicator(
            valueColor: AlwaysStoppedAnimation<Color>(indicatorColor),
            strokeWidth: 3.5,
          ),
        ),
        if (label != null) ...[
          const SizedBox(height: AppSpacing.lg),
          Text(
            label,
            style: const TextStyle(
              fontSize: 14,
              color: AppColors.textSecondary,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ],
    );

    if (!showCard) return content;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.xxl,
        vertical: AppSpacing.xl,
      ),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(AppSpacing.radiusLg),
        border: Border.all(color: AppColors.divider),
        boxShadow: const [
          BoxShadow(
            color: Color(0x14000000),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: content,
    );
  }
}

/// 오버레이 로딩 인디케이터
///
/// 기존 UI 위에 반투명 배경과 함께 로딩 인디케이터를 표시합니다.
/// 카드는 [LoadingIndicator] 가 그리므로 별도 배경 상자를 두지 않습니다.
class OverlayLoadingIndicator extends StatelessWidget {
  /// 로딩 메시지
  final String? message;

  /// 배경 색상
  final Color backgroundColor;

  /// 인디케이터 색상
  final Color? indicatorColor;

  const OverlayLoadingIndicator({
    super.key,
    this.message,
    this.backgroundColor = const Color(0x80000000),
    this.indicatorColor,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Container(color: backgroundColor),
        LoadingIndicator(message: message, color: indicatorColor),
      ],
    );
  }
}
