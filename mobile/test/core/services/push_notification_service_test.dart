import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
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
}
