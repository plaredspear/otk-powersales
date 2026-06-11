import 'package:flutter/foundation.dart';

/// 강제 로그아웃 사유 — 로그인 화면이 진입 시 1회 안내한다.
enum LogoutReason {
  /// 다른 기기에서 로그인되어 현재 단말이 차단됨 (서버 `DEVICE_REVOKED`).
  deviceRevoked,

  /// 세션 만료 / 토큰 갱신 실패로 재로그인 필요.
  sessionExpired,
}

/// 계정 전환(로그아웃) 시 앱 전역 상태를 초기화하기 위한 신호 컨트롤러.
///
/// 로그아웃/강제 로그아웃 시 [requestReset] 을 호출하면 루트 위젯([AppBootstrap])이
/// 루트 `ProviderScope` 를 새 인스턴스로 교체한다. 그 결과 모든 Provider(주문·공지·
/// 매출·거래처·안전점검 등 도메인 캐시 포함)가 폐기되어, 다음 사용자가 이전 사용자의
/// 잔여 데이터를 보게 되는 문제를 원천 차단한다.
///
/// 개별 Provider 를 일일이 `invalidate` 하지 않으므로, 새 도메인 Provider 가
/// 추가되어도 누락 없이 항상 초기화된다.
class SessionResetController {
  SessionResetController._();

  static final SessionResetController instance = SessionResetController._();

  /// 리셋 세대(generation). 값이 바뀔 때마다 루트가 `ProviderScope` 의 key 를 갱신한다.
  /// 값이 0 보다 크면 "로그아웃으로 재생성된 세션"을 의미한다.
  final ValueNotifier<int> generation = ValueNotifier<int>(0);

  /// 직전 강제 로그아웃 사유. 로그인 화면이 [consumeReason] 으로 1회 소비해 안내한다.
  /// 사용자가 직접 로그아웃한 경우는 null(무표시).
  LogoutReason? _reason;

  /// 전역 상태 초기화 요청(로그아웃 직후 호출). [reason] 전달 시 로그인 화면에서 안내한다.
  void requestReset({LogoutReason? reason}) {
    _reason = reason;
    generation.value++;
  }

  /// 표시 대기 중인 로그아웃 사유를 반환하고 즉시 비운다(1회성).
  LogoutReason? consumeReason() {
    final reason = _reason;
    _reason = null;
    return reason;
  }
}
