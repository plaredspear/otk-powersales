import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../data/datasources/app_version_api_datasource.dart';
import '../config/app_config.dart';
import 'app_version_checker.dart';

/// 앱 전역 강제 업데이트 게이트.
///
/// 강제 업데이트는 "어느 상태에서든" 걸려야 한다 — 로그인 전/후, 그리고 백그라운드에서
/// 복귀(resume)했을 때까지. 그래서 이 게이트는 다음 둘 중 어디에도 매이지 않는다.
///
/// 1. Riverpod `ProviderScope` — 로그인/로그아웃 시 루트가 통째로 재생성되므로(세션 리셋)
///    Provider 로 두면 상태가 날아가고, 재생성된 세션은 스플래시를 건너뛰어 게이트가
///    아예 실행되지 않는다. 그래서 싱글턴 + 전용 Dio 를 쓴다(버전 체크 API 는 무인증).
/// 2. 화면 스택 — 차단 UI 를 다이얼로그 라우트로 띄우면 인증 상태 전환의
///    `pushNamedAndRemoveUntil` 에 함께 제거된다. 대신 [blockedBy] 를 루트
///    `MaterialApp.builder` 의 오버레이가 구독해, Navigator 위에서 화면 전체를 덮는다.
class ForceUpdateGate {
  ForceUpdateGate._();

  static final ForceUpdateGate instance = ForceUpdateGate._();

  final AppVersionChecker _checker =
      AppVersionChecker(AppVersionApiDataSource(_buildDio()));

  /// 강제 업데이트로 차단된 경우의 버전 정보. null 이면 차단 아님.
  /// 한 번 세워지면 앱을 종료할 때까지 유지된다(해제 경로 없음 — 재실행으로만 풀린다).
  final ValueNotifier<AppVersionResult?> blockedBy =
      ValueNotifier<AppVersionResult?>(null);

  bool get isBlocking => blockedBy.value != null;

  Future<AppVersionResult?>? _inFlight;

  /// 버전을 조회하고, 강제 업데이트면 [blockedBy] 를 세워 전역 차단한다.
  ///
  /// 스플래시(콜드 스타트)와 루트(resume·세션 재생성)가 동시에 호출할 수 있으므로 진행 중인
  /// 요청에 합류시켜 중복 호출을 막는다. 이미 차단 상태면 재조회하지 않는다.
  ///
  /// 반환값은 조회 결과(권장 업데이트 판단용). 실패 시 null — 버전 체크 실패가 앱 진입을
  /// 막아선 안 되므로 fail-open 이다([AppVersionChecker] 참고).
  Future<AppVersionResult?> check() async {
    if (isBlocking) return blockedBy.value;

    final inFlight = _inFlight ??= _checker.check();
    try {
      final result = await inFlight;
      if (result != null && result.forceUpdate) {
        blockedBy.value = result;
      }
      return result;
    } finally {
      // 뒤늦게 끝난 옛 요청이 새 요청을 지우지 않도록 동일 인스턴스일 때만 비운다.
      if (identical(_inFlight, inFlight)) _inFlight = null;
    }
  }

  /// 버전 체크 전용 Dio — 앱 전역 [dioProvider] 와 분리한다.
  /// 무인증 엔드포인트라 인터셉터가 불필요하고, `ProviderScope` 재생성/요청 일괄 취소
  /// (백그라운드 전환 시 `requestCancelController`)의 영향도 받지 않아야 한다.
  static Dio _buildDio() => Dio(
        BaseOptions(
          baseUrl: AppConfig.baseUrl,
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 10),
        ),
      );
}
