import 'dart:async';
import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:logger/logger.dart';

/// FCM 백그라운드/종료 상태 메시지 핸들러.
///
/// 반드시 최상위(top-level) 함수여야 하며, 별도 isolate 에서 실행되므로
/// UI·Provider 에 접근할 수 없다. 데이터 페이로드 처리·로깅만 수행한다.
/// (notification 페이로드는 OS 가 알림으로 자동 표시함)
@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  // 백그라운드 isolate 에서는 Firebase 를 다시 초기화해야 한다.
  if (Firebase.apps.isEmpty) {
    await Firebase.initializeApp();
  }
  // 필요 시 데이터 메시지 처리 로직 추가.
}

/// FCM(푸시 알림) 초기화 및 토큰 관리 서비스.
///
/// - 책임: Firebase 초기화 확인, 알림 권한 요청, 디바이스 토큰 조회/갱신 구독,
///   포그라운드/탭 메시지 리스너 등록.
/// - 네이티브 설정 파일(google-services.json / GoogleService-Info.plist)이
///   아직 없으면 [Firebase.initializeApp] 이 실패하므로, 그 경우 초기화를
///   조용히 skip 하여 앱 구동에 영향을 주지 않는다.
class PushNotificationService {
  PushNotificationService({Logger? logger}) : _logger = logger ?? Logger();

  final Logger _logger;

  /// 지연 평가 — Firebase 미초기화 상태에서 [FirebaseMessaging.instance] 에
  /// 접근하면 예외가 발생하므로, 사용 시점(모두 [isAvailable] 가드 하)에만 평가한다.
  FirebaseMessaging get _messaging => FirebaseMessaging.instance;

  bool _initialized = false;

  /// FCM 사용 가능 여부 (네이티브 설정 파일이 있어 초기화에 성공한 경우 true).
  bool get isAvailable => Firebase.apps.isNotEmpty;

  /// 디바이스 FCM 토큰 갱신 스트림. 서버 토큰 동기화에 사용.
  Stream<String> get onTokenRefresh => _messaging.onTokenRefresh;

  /// 현재 디바이스의 FCM 토큰을 반환한다. 미초기화/미설정 시 null.
  ///
  /// iOS 는 APNS 토큰이 먼저 세팅돼야 FCM 토큰이 발급된다. 앱 첫 실행 직후엔
  /// APNS 토큰이 (스위즐링으로) 비동기 도착하므로, 도착할 때까지 짧게 재시도한 뒤
  /// FCM 토큰을 조회한다. 미도착 시 null (등록은 onTokenRefresh 가 이어받음).
  Future<String?> getToken() async {
    if (!isAvailable) return null;
    try {
      if (Platform.isIOS) {
        var apns = await _messaging.getAPNSToken();
        for (var i = 0; i < 10 && apns == null; i++) {
          await Future.delayed(const Duration(seconds: 1));
          apns = await _messaging.getAPNSToken();
        }
        if (apns == null) {
          _logger.w('APNS 토큰 미수신 — FCM 토큰 발급 불가(푸시 권한/프로파일/네트워크 확인)');
          return null;
        }
      }
      return await _messaging.getToken();
    } catch (e) {
      _logger.w('FCM 토큰 조회 실패: $e');
      return null;
    }
  }

  /// FCM 초기화. main 또는 인증 완료 후 1회 호출.
  ///
  /// [onMessageOpened]: 알림 탭으로 앱이 열렸을 때(백그라운드/종료) 호출되는 콜백.
  Future<void> initialize({
    void Function(RemoteMessage message)? onMessageOpened,
  }) async {
    if (_initialized) return;

    // 네이티브 설정 파일이 없어 Firebase 가 초기화되지 않은 경우 skip.
    if (!isAvailable) {
      _logger.i('FCM 설정 파일(google-services.json / plist) 미존재 — 푸시 초기화 skip');
      return;
    }

    try {
      // 1. 알림 권한 요청 (iOS / Android 13+)
      final settings = await _messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );
      _logger.i('FCM 권한 상태: ${settings.authorizationStatus}');

      // 2. iOS 포그라운드 알림 표시 옵션
      await _messaging.setForegroundNotificationPresentationOptions(
        alert: true,
        badge: true,
        sound: true,
      );

      // 3. 포그라운드 수신 메시지
      FirebaseMessaging.onMessage.listen((message) {
        _logger.i('FCM 포그라운드 수신: ${message.notification?.title}');
        // 포그라운드 알림 UI 표시는 flutter_local_notifications 도입 시 연동.
      });

      // 4. 알림 탭으로 앱이 백그라운드→포그라운드 전환된 경우
      FirebaseMessaging.onMessageOpenedApp.listen((message) {
        _logger.i('FCM 알림 탭(백그라운드): ${message.notification?.title}');
        onMessageOpened?.call(message);
      });

      // 5. 종료 상태에서 알림 탭으로 앱이 실행된 경우
      final initialMessage = await _messaging.getInitialMessage();
      if (initialMessage != null) {
        _logger.i('FCM 알림 탭(종료 상태): ${initialMessage.notification?.title}');
        onMessageOpened?.call(initialMessage);
      }

      // 6. 토큰 조회 (서버 등록 연동 지점)
      final token = await getToken();
      _logger.i('FCM 토큰: ${token ?? '(없음)'}');

      _initialized = true;
    } catch (e, st) {
      _logger.e('FCM 초기화 실패', error: e, stackTrace: st);
    }
  }
}

/// PushNotificationService Riverpod Provider
final pushNotificationServiceProvider = Provider<PushNotificationService>((ref) {
  return PushNotificationService();
});
