import 'package:flutter/foundation.dart';

/// 푸시 알림 탭으로 요청된 화면 이동 목적지(라우트 + 인자).
@immutable
class DeepLinkTarget {
  const DeepLinkTarget({required this.route, this.arguments});

  final String route;
  final Object? arguments;
}

/// 아직 이동하지 못한 푸시 딥링크를 보관했다가 인증 완료 후 1회 재생하는 대기열.
///
/// 푸시는 앱이 **미인증** 상태(로그아웃 상태 / 콜드 스타트로 자동로그인 진행 중)일 때도
/// 탭될 수 있다 — 로그아웃 시 서버 토큰을 해제해도 이미 알림함에 쌓인 알림은 회수되지
/// 않으므로, 어떤 토큰 정책을 쓰든 이 상황은 반드시 발생한다. 그 시점에 곧바로 상세
/// 화면으로 이동하면 화면이 인증 필요 API 를 호출해 401 에러 화면이 노출되고, 이어지는
/// 로그인 전환(`pushNamedAndRemoveUntil`)이 그 화면을 통째로 폐기한다.
///
/// 따라서 라우팅 판단은 인증 상태를 아는 루트 위젯이 맡고, 푸시 콜백은 여기에 목적지를
/// 적어두기만 한다. 루트 `ProviderScope` 재생성(로그인/로그아웃 세션 리셋)에도 값이
/// 유지돼야 하므로 Provider 가 아닌 프로세스 전역 싱글턴이다.
class PendingDeepLink {
  PendingDeepLink._();

  static final PendingDeepLink instance = PendingDeepLink._();

  /// 대기 중인 딥링크. 루트 위젯이 구독해, 인증 완료 상태가 되면 소비한다.
  final ValueNotifier<DeepLinkTarget?> pending =
      ValueNotifier<DeepLinkTarget?>(null);

  /// 딥링크를 대기열에 올린다 — 최신 1건만 유지한다.
  ///
  /// 주의: "최신 1건 유지" 는 소비 전 중복 store 만 합쳐줄 뿐, store → 소비 → store 가
  /// 반복되면 각각 이동이 발생한다. 알림 탭 1회당 store 가 정확히 1번 오도록 하는 책임은
  /// 발신 측에 있다([PushNotificationService] 가 FCM 리스너를 프로세스 1회만 등록).
  void store(DeepLinkTarget target) => pending.value = target;

  /// 대기 중인 딥링크를 꺼내고 즉시 비운다(1회성). 없으면 null.
  DeepLinkTarget? consume() {
    final target = pending.value;
    pending.value = null;
    return target;
  }
}
