import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_colors.dart';
import '../theme/app_spacing.dart';
import '../theme/app_typography.dart';

/// 값 복사 유틸 — 바코드처럼 다른 앱(거래처 앱 등)에 옮겨 붙여야 하는 값에 사용한다.
///
/// 레거시 웹뷰에서는 텍스트를 눌러 브라우저 기본 복사가 가능했지만 Flutter 의
/// `Text` 는 선택이 불가능하므로, 표시 지점에서 명시적으로 복사를 제공한다.
abstract final class ClipboardUtils {
  static OverlayEntry? _entry;
  static Timer? _timer;

  /// [value] 를 클립보드에 복사하고 화면 하단에 복사 결과를 토스트로 알린다.
  ///
  /// [label] 은 안내 문구의 항목명(예: `바코드`). 값이 비어 있으면 아무 것도 하지 않는다.
  static Future<void> copy(
    BuildContext context,
    String value, {
    required String label,
  }) async {
    final text = value.trim();
    if (text.isEmpty || text == '-') return;

    await Clipboard.setData(ClipboardData(text: text));
    unawaited(HapticFeedback.selectionClick());
    if (!context.mounted) return;

    _showToast(context, '$label 복사됨  $text');
  }

  /// 모달 시트 위에서도 보이도록 root Overlay 에 토스트를 띄운다.
  ///
  /// ScaffoldMessenger 스낵바를 쓰지 않는 이유: 제품 검색 공용 모달
  /// (`AddProductBottomSheet`) 처럼 모달 라우트 위에서 복사하는 경로가 있어
  /// 스낵바가 시트에 가려 보이지 않는다.
  static void _showToast(BuildContext context, String message) {
    final overlay = Overlay.maybeOf(context, rootOverlay: true);
    if (overlay == null) return;

    _dismiss();
    final entry = OverlayEntry(builder: (_) => _CopyToast(message: message));
    _entry = entry;
    overlay.insert(entry);
    _timer = Timer(const Duration(milliseconds: 1600), _dismiss);
  }

  static void _dismiss() {
    _timer?.cancel();
    _timer = null;
    final entry = _entry;
    _entry = null;
    if (entry != null && entry.mounted) entry.remove();
  }
}

/// 하단 중앙에 잠깐 떠오르는 복사 안내 토스트
class _CopyToast extends StatelessWidget {
  final String message;

  const _CopyToast({required this.message});

  @override
  Widget build(BuildContext context) {
    final media = MediaQuery.of(context);

    return Positioned(
      left: AppSpacing.xl,
      right: AppSpacing.xl,
      // 키보드가 올라와 있으면 그 위로 띄운다.
      bottom: media.viewInsets.bottom + media.padding.bottom + AppSpacing.xxxl,
      child: IgnorePointer(
        child: TweenAnimationBuilder<double>(
          tween: Tween<double>(begin: 0, end: 1),
          duration: const Duration(milliseconds: 150),
          builder: (_, opacity, child) => Opacity(opacity: opacity, child: child),
          child: Center(
            child: Material(
              color: AppColors.transparent,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.lg,
                  vertical: AppSpacing.sm + 2,
                ),
                decoration: BoxDecoration(
                  color: AppColors.snackbarBackground,
                  borderRadius: BorderRadius.circular(AppSpacing.radiusFull),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.check_circle_outline,
                      size: 16,
                      color: AppColors.white,
                    ),
                    const SizedBox(width: AppSpacing.xs),
                    Flexible(
                      child: Text(
                        message,
                        style: AppTypography.bodySmall.copyWith(
                          color: AppColors.white,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
