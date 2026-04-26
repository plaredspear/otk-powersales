import 'package:flutter/widgets.dart';

/// 버튼/팝업 연속 탭 방지 Mixin.
///
/// [StatefulWidget] 또는 [ConsumerStatefulWidget]의 State 클래스에서 사용한다.
/// 동기 탭에는 [throttledTap], 비동기 탭에는 [throttledTapAsync]를 사용한다.
mixin ThrottledTapMixin<T extends StatefulWidget> on State<T> {
  DateTime? _lastTapTime;
  bool _isProcessing = false;

  /// 동기 탭 쓰로틀.
  ///
  /// [interval] 이내 재호출 시 [action]을 실행하지 않는다.
  /// 기본 interval: 500ms.
  void throttledTap(VoidCallback action, {Duration interval = const Duration(milliseconds: 500)}) {
    if (interval <= Duration.zero) {
      action();
      return;
    }

    final now = DateTime.now();
    if (_lastTapTime != null && now.difference(_lastTapTime!) < interval) {
      return;
    }
    _lastTapTime = now;
    action();
  }

  /// 비동기 탭 쓰로틀.
  ///
  /// [interval] 이내 재호출이거나 이전 [action]이 진행 중이면 실행하지 않는다.
  /// action 완료 후 [mounted] 체크를 호출자가 직접 수행해야 한다.
  void throttledTapAsync(
    Future<void> Function() action, {
    Duration interval = const Duration(milliseconds: 500),
  }) {
    if (_isProcessing) return;

    if (interval > Duration.zero) {
      final now = DateTime.now();
      if (_lastTapTime != null && now.difference(_lastTapTime!) < interval) {
        return;
      }
      _lastTapTime = now;
    }
    _runAsync(action);
  }

  Future<void> _runAsync(Future<void> Function() action) async {
    _isProcessing = true;
    try {
      await action();
    } catch (_) {
      // action 예외는 호출자가 action 내부에서 처리해야 한다.
      // 여기서는 _isProcessing 리셋만 보장한다.
    } finally {
      _isProcessing = false;
    }
  }
}
