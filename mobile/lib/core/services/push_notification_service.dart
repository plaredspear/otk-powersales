import 'dart:async';
import 'dart:convert';
import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart' show visibleForTesting;
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
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
///   포그라운드/탭 메시지 리스너 등록, 포그라운드 수신 시 로컬 알림 표시.
/// - 네이티브 설정 파일(google-services.json / GoogleService-Info.plist)이
///   아직 없으면 [Firebase.initializeApp] 이 실패하므로, 그 경우 초기화를
///   조용히 skip 하여 앱 구동에 영향을 주지 않는다.
class PushNotificationService {
  PushNotificationService({
    Logger? logger,
    FlutterLocalNotificationsPlugin? localNotifications,
  })  : _logger = logger ?? Logger(),
        _localNotifications =
            localNotifications ?? FlutterLocalNotificationsPlugin();

  final Logger _logger;
  final FlutterLocalNotificationsPlugin _localNotifications;

  /// 포그라운드 로컬 알림이 사용하는 Android 채널 ID.
  ///
  /// FCM 백그라운드 알림이 쓰는 채널(AndroidManifest 의
  /// `default_notification_channel_id`)과 동일 ID 로 맞춰, 포그라운드/백그라운드
  /// 알림이 같은 채널(중요도/소리 설정)로 노출되게 한다.
  static const String _androidChannelId = 'otoki_default_channel';
  static const String _androidChannelName = '오뚜기 파워세일즈 알림';
  static const String _androidChannelDescription = '공지사항 등 앱 알림';

  /// 지연 평가 — Firebase 미초기화 상태에서 [FirebaseMessaging.instance] 에
  /// 접근하면 예외가 발생하므로, 사용 시점(모두 [isAvailable] 가드 하)에만 평가한다.
  FirebaseMessaging get _messaging => FirebaseMessaging.instance;

  bool _initialized = false;

  /// 로컬 알림 탭으로 앱 화면 이동이 필요할 때 호출되는 콜백.
  /// [initialize] 에서 전달받은 `onMessageOpened` 를 재사용한다.
  void Function(RemoteMessage message)? _onMessageOpened;

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
  /// [onMessageOpened]: 알림 탭으로 앱이 열렸을 때(백그라운드/종료 상태 FCM 탭,
  /// 또는 포그라운드 로컬 알림 탭) 호출되는 콜백. 딥링크 라우팅에 사용한다.
  Future<void> initialize({
    void Function(RemoteMessage message)? onMessageOpened,
  }) async {
    if (_initialized) return;
    _onMessageOpened = onMessageOpened;

    // 네이티브 설정 파일이 없어 Firebase 가 초기화되지 않은 경우 skip.
    if (!isAvailable) {
      _logger.i('FCM 설정 파일(google-services.json / plist) 미존재 — 푸시 초기화 skip');
      return;
    }

    try {
      // 0. 로컬 알림 플러그인 초기화 (포그라운드 알림 표시 + 탭 라우팅)
      await _initLocalNotifications();

      // 1. 알림 권한 요청 (iOS / Android 13+)
      final settings = await _messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );
      _logger.i('FCM 권한 상태: ${settings.authorizationStatus}');

      // 2. iOS 포그라운드 알림 표시 옵션
      //    iOS 는 OS 가 직접 포그라운드 배너를 표시하므로 로컬 알림 중복 표시를
      //    하지 않는다(아래 onMessage 는 Android 에서만 로컬 알림 생성).
      await _messaging.setForegroundNotificationPresentationOptions(
        alert: true,
        badge: true,
        sound: true,
      );

      // 3. 포그라운드 수신 메시지 → Android 는 로컬 알림으로 직접 표시
      FirebaseMessaging.onMessage.listen(_showForegroundNotification);

      // 4. 알림 탭으로 앱이 백그라운드→포그라운드 전환된 경우
      FirebaseMessaging.onMessageOpenedApp.listen((message) {
        _logger.i('FCM 알림 탭(백그라운드): ${message.notification?.title}');
        _onMessageOpened?.call(message);
      });

      // 5. 종료 상태에서 알림 탭으로 앱이 실행된 경우
      final initialMessage = await _messaging.getInitialMessage();
      if (initialMessage != null) {
        _logger.i('FCM 알림 탭(종료 상태): ${initialMessage.notification?.title}');
        _onMessageOpened?.call(initialMessage);
      }

      // 6. 토큰 조회 (서버 등록 연동 지점)
      final token = await getToken();
      _logger.i('FCM 토큰: ${token ?? '(없음)'}');

      _initialized = true;
    } catch (e, st) {
      _logger.e('FCM 초기화 실패', error: e, stackTrace: st);
    }
  }

  /// 로컬 알림 플러그인 초기화 + Android 알림 채널 생성.
  ///
  /// Android 는 포그라운드 수신 시 OS 가 자동 알림을 띄우지 않으므로 이 플러그인으로
  /// 직접 표시한다. iOS 는 [setForegroundNotificationPresentationOptions] 로 OS 가
  /// 배너를 띄우므로 로컬 표시는 하지 않되, 탭 콜백 라우팅을 위해 초기화만 해 둔다.
  Future<void> _initLocalNotifications() async {
    const androidInit =
        AndroidInitializationSettings('@mipmap/ic_launcher');
    // iOS 권한은 FCM(requestPermission)이 담당하므로 여기서는 중복 요청하지 않는다.
    const iosInit = DarwinInitializationSettings(
      requestAlertPermission: false,
      requestBadgePermission: false,
      requestSoundPermission: false,
    );
    await _localNotifications.initialize(
      settings: const InitializationSettings(android: androidInit, iOS: iosInit),
      onDidReceiveNotificationResponse: _onLocalNotificationTapped,
    );

    // Android 8.0+ 는 채널 사전 생성 필요. FCM 백그라운드 알림과 동일 ID 사용.
    final androidPlugin =
        _localNotifications.resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>();
    await androidPlugin?.createNotificationChannel(
      const AndroidNotificationChannel(
        _androidChannelId,
        _androidChannelName,
        description: _androidChannelDescription,
        importance: Importance.high,
      ),
    );
  }

  /// 포그라운드 FCM 수신 시 Android 로컬 알림을 표시한다.
  ///
  /// notification 페이로드(title/body)가 있을 때만 표시하며, FCM data payload 를
  /// JSON 문자열로 직렬화해 로컬 알림 payload 에 실어 탭 시 딥링크에 재사용한다.
  /// iOS 는 OS 가 배너를 직접 띄우므로 중복 방지를 위해 표시하지 않는다.
  void _showForegroundNotification(RemoteMessage message) {
    _logger.i('FCM 포그라운드 수신: ${message.notification?.title}');
    if (Platform.isIOS) return;

    final notification = message.notification;
    if (notification == null) return;

    _localNotifications.show(
      id: notification.hashCode,
      title: notification.title,
      body: notification.body,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          _androidChannelId,
          _androidChannelName,
          channelDescription: _androidChannelDescription,
          importance: Importance.high,
          priority: Priority.high,
          icon: '@mipmap/ic_launcher',
        ),
      ),
      payload: jsonEncode(message.data),
    );
  }

  /// 포그라운드 로컬 알림 탭 → FCM data payload 를 복원해 딥링크 콜백 호출.
  ///
  /// 로컬 알림 payload(JSON)를 [RemoteMessage] 의 data 로 재구성해
  /// [initialize] 에서 받은 `onMessageOpened` 로 넘긴다. 이로써 포그라운드 알림 탭이
  /// 백그라운드/종료 상태 FCM 탭과 동일한 라우팅 경로를 타게 한다.
  void _onLocalNotificationTapped(NotificationResponse response) {
    final message = decodePayloadToMessage(response.payload);
    if (message == null) {
      if (response.payload != null && response.payload!.isNotEmpty) {
        _logger.w('로컬 알림 payload 파싱 실패: ${response.payload}');
      }
      return;
    }
    _onMessageOpened?.call(message);
  }

  /// 로컬 알림 payload(JSON 문자열)를 FCM data 를 담은 [RemoteMessage] 로 복원한다.
  ///
  /// payload 가 null/빈문자열/비-Map JSON/파싱 실패이면 null 을 반환한다. 성공 시
  /// 모든 값은 String 으로 정규화한다(FCM data payload 와 동일 형태). 딥링크 라우팅이
  /// 포그라운드 알림 탭과 백그라운드/종료 탭에서 동일하게 동작하도록 하는 핵심 변환.
  @visibleForTesting
  static RemoteMessage? decodePayloadToMessage(String? payload) {
    if (payload == null || payload.isEmpty) return null;
    try {
      final decoded = jsonDecode(payload);
      if (decoded is! Map) return null;
      final data = decoded.map((k, v) => MapEntry(k.toString(), v.toString()));
      return RemoteMessage(data: data);
    } catch (_) {
      return null;
    }
  }
}

/// PushNotificationService Riverpod Provider
final pushNotificationServiceProvider = Provider<PushNotificationService>((ref) {
  return PushNotificationService();
});
