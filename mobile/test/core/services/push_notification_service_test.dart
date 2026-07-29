import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/services/push_notification_service.dart';

/// 포그라운드 로컬 알림 탭 → FCM data payload 복원(딥링크 연동 핵심) 검증.
///
/// [PushNotificationService.decodePayloadToMessage] 는 로컬 알림 payload(JSON)를
/// 백그라운드/종료 상태 FCM 탭과 동일한 형태의 [RemoteMessage] 로 되돌린다.
void main() {
  group('PushNotificationService.decodePayloadToMessage', () {
    test('공지 payload(JSON) → RemoteMessage.data 로 복원', () {
      final payload = jsonEncode({'type': 'notice', 'noticeId': '42'});

      final message =
          PushNotificationService.decodePayloadToMessage(payload);

      expect(message, isNotNull);
      expect(message!.data['type'], 'notice');
      expect(message.data['noticeId'], '42');
    });

    test('숫자 값도 String 으로 정규화 (FCM data payload 형태 유지)', () {
      // jsonEncode 로 int 가 들어와도 복원 시 String 이어야 딥링크 int.tryParse 가 동작.
      final payload = jsonEncode({'type': 'notice', 'noticeId': 7});

      final message =
          PushNotificationService.decodePayloadToMessage(payload);

      expect(message!.data['noticeId'], '7');
      expect(message.data['noticeId'], isA<String>());
    });

    test('null payload → null 반환', () {
      expect(PushNotificationService.decodePayloadToMessage(null), isNull);
    });

    test('빈 문자열 payload → null 반환', () {
      expect(PushNotificationService.decodePayloadToMessage(''), isNull);
    });

    test('잘못된 JSON → null 반환 (예외 없이)', () {
      expect(
        PushNotificationService.decodePayloadToMessage('{not json'),
        isNull,
      );
    });

    test('Map 이 아닌 JSON(배열) → null 반환', () {
      final payload = jsonEncode([1, 2, 3]);
      expect(
        PushNotificationService.decodePayloadToMessage(payload),
        isNull,
      );
    });
  });

  group('pushNotificationServiceProvider', () {
    test('컨테이너(세션)가 재생성돼도 동일 싱글턴을 반환한다', () {
      // 세션 리셋으로 루트 ProviderScope 가 재생성될 때마다 새 인스턴스를 만들면
      // FCM 스트림 리스너가 중복 등록되어, 푸시 탭 1회에 딥링크 화면이 여러 장
      // 쌓인다(뒤로가기를 눌러도 같은 화면 반복). 싱글턴 유지가 그 방어선이다.
      final container1 = ProviderContainer();
      final container2 = ProviderContainer();
      addTearDown(container1.dispose);
      addTearDown(container2.dispose);

      expect(
        identical(
          container1.read(pushNotificationServiceProvider),
          container2.read(pushNotificationServiceProvider),
        ),
        isTrue,
      );
    });
  });

  group('PushNotificationService.deleteToken', () {
    test('Firebase 미초기화(설정 파일 없음)에서는 예외 없이 no-op', () async {
      // 강제 로그아웃 경로에서 호출되므로, 푸시 미설정 환경에서도 로그아웃을 막으면 안 된다.
      await expectLater(PushNotificationService().deleteToken(), completes);
    });
  });
}
