import 'package:flutter/material.dart';

/// 에러 화면 위젯
///
/// 에러 발생 시 사용자에게 표시하는 공통 위젯입니다.
///
/// 사용 예시:
/// ```dart
/// ErrorView(
///   message: '데이터를 불러올 수 없습니다',
///   onRetry: () => _loadData(),
/// )
/// ```
class ErrorView extends StatelessWidget {
  /// 에러 메시지
  final String message;

  /// 재시도 콜백
  final VoidCallback? onRetry;

  /// 에러 아이콘
  final IconData icon;

  /// 아이콘 색상
  final Color? iconColor;

  /// 재시도 버튼 텍스트
  final String retryButtonText;

  /// 추가 설명 텍스트 (선택사항)
  final String? description;

  /// 전체 화면 모드 여부
  final bool isFullScreen;

  const ErrorView({
    super.key,
    required this.message,
    this.onRetry,
    this.icon = Icons.error_outline,
    this.iconColor,
    this.retryButtonText = '다시 시도',
    this.description,
    this.isFullScreen = true,
  });

  /// 네트워크 에러 (편의 생성자)
  const ErrorView.network({
    super.key,
    this.message = '네트워크 연결을 확인해주세요',
    this.onRetry,
    this.retryButtonText = '다시 시도',
    this.description,
    this.isFullScreen = true,
  })  : icon = Icons.wifi_off,
        iconColor = null;

  /// 데이터 없음 (편의 생성자)
  const ErrorView.noData({
    super.key,
    this.message = '데이터가 없습니다',
    this.onRetry,
    this.retryButtonText = '새로고침',
    this.description,
    this.isFullScreen = true,
  })  : icon = Icons.inbox_outlined,
        iconColor = null;

  /// 권한 없음 (편의 생성자)
  const ErrorView.unauthorized({
    super.key,
    this.message = '접근 권한이 없습니다',
    this.onRetry,
    this.retryButtonText = '다시 시도',
    this.description = '로그인이 필요하거나 권한이 부족합니다',
    this.isFullScreen = true,
  })  : icon = Icons.lock_outline,
        iconColor = null;

  @override
  Widget build(BuildContext context) {
    final content = _buildContent(context);

    if (isFullScreen) {
      return Center(child: content);
    }

    return content;
  }

  Widget _buildContent(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        mainAxisSize: isFullScreen ? MainAxisSize.max : MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            icon,
            size: 64,
            color: iconColor ?? Colors.grey[400],
          ),
          const SizedBox(height: 16),
          Text(
            message,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
            textAlign: TextAlign.center,
          ),
          if (description != null) ...[
            const SizedBox(height: 8),
            Text(
              description!,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
          ],
          if (onRetry != null) ...[
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: Text(retryButtonText),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 12,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

/// 인라인 에러 메시지
///
/// 작은 영역에서 간단한 에러 메시지를 표시합니다.
class InlineErrorMessage extends StatelessWidget {
  /// 에러 메시지
  final String message;

  /// 재시도 콜백
  final VoidCallback? onRetry;

  /// 배경 색상
  final Color? backgroundColor;

  /// 텍스트 색상
  final Color? textColor;

  const InlineErrorMessage({
    super.key,
    required this.message,
    this.onRetry,
    this.backgroundColor,
    this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: backgroundColor ?? Colors.red[50],
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: Colors.red[200]!,
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Icon(
            Icons.error_outline,
            size: 20,
            color: textColor ?? Colors.red[700],
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontSize: 13,
                color: textColor ?? Colors.red[700],
              ),
            ),
          ),
          if (onRetry != null) ...[
            const SizedBox(width: 8),
            InkWell(
              onTap: onRetry,
              child: Padding(
                padding: const EdgeInsets.all(4),
                child: Icon(
                  Icons.refresh,
                  size: 20,
                  color: textColor ?? Colors.red[700],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
