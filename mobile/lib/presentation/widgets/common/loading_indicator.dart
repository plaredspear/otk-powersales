import 'package:flutter/material.dart';

/// 로딩 인디케이터 위젯
///
/// 데이터 로딩 중임을 사용자에게 표시하는 공통 위젯입니다.
///
/// 사용 예시:
/// ```dart
/// LoadingIndicator()  // 기본 스타일
/// LoadingIndicator(message: '데이터를 불러오는 중...')  // 메시지 포함
/// LoadingIndicator.fullScreen(message: '처리 중...')  // 전체 화면
/// ```
class LoadingIndicator extends StatelessWidget {
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

  const LoadingIndicator({
    super.key,
    this.message,
    this.size = 40.0,
    this.color,
    this.isFullScreen = false,
    this.backgroundColor,
  });

  /// 전체 화면 로딩 인디케이터 (편의 생성자)
  const LoadingIndicator.fullScreen({
    super.key,
    this.message,
    this.size = 40.0,
    this.color,
    this.backgroundColor,
  }) : isFullScreen = true;

  @override
  Widget build(BuildContext context) {
    final indicator = _buildIndicator(context);

    if (isFullScreen) {
      return Container(
        color: backgroundColor ?? Colors.white.withOpacity(0.9),
        child: Center(child: indicator),
      );
    }

    return Center(child: indicator);
  }

  Widget _buildIndicator(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: size,
          height: size,
          child: CircularProgressIndicator(
            valueColor: AlwaysStoppedAnimation<Color>(
              color ?? Theme.of(context).primaryColor,
            ),
            strokeWidth: 3.0,
          ),
        ),
        if (message != null) ...[
          const SizedBox(height: 16),
          Text(
            message!,
            style: TextStyle(
              fontSize: 14,
              color: color ?? Theme.of(context).primaryColor,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ],
    );
  }
}

/// 오버레이 로딩 인디케이터
///
/// 기존 UI 위에 반투명 배경과 함께 로딩 인디케이터를 표시합니다.
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
        Center(
          child: Container(
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
            ),
            child: LoadingIndicator(
              message: message,
              color: indicatorColor,
            ),
          ),
        ),
      ],
    );
  }
}
