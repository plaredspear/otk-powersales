import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/schedule.dart';

void main() {
  group('Schedule', () {
    const testSchedule = Schedule(
      id: 1,
      storeName: '이마트 부산점',
      startTime: '09:00',
      endTime: '12:00',
      type: '순회',
    );

    final testJson = {
      'id': 1,
      'storeName': '이마트 부산점',
      'startTime': '09:00',
      'endTime': '12:00',
      'type': '순회',
    };

    group('생성', () {
      test('Schedule 엔티티가 올바르게 생성된다', () {
        expect(testSchedule.id, 1);
        expect(testSchedule.storeName, '이마트 부산점');
        expect(testSchedule.startTime, '09:00');
        expect(testSchedule.endTime, '12:00');
        expect(testSchedule.type, '순회');
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testSchedule.copyWith(storeName: '홈플러스 서울점');

        expect(copied.id, testSchedule.id);
        expect(copied.storeName, '홈플러스 서울점');
        expect(copied.startTime, testSchedule.startTime);
        expect(copied.endTime, testSchedule.endTime);
        expect(copied.type, testSchedule.type);
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final copied = testSchedule.copyWith(
          id: 2,
          storeName: '롯데마트',
          startTime: '13:00',
          endTime: '17:00',
          type: '고정',
        );

        expect(copied.id, 2);
        expect(copied.storeName, '롯데마트');
        expect(copied.startTime, '13:00');
        expect(copied.endTime, '17:00');
        expect(copied.type, '고정');
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testSchedule.copyWith();

        expect(copied, testSchedule);
        expect(identical(copied, testSchedule), isFalse);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testSchedule.toJson();

        expect(result['id'], 1);
        expect(result['storeName'], '이마트 부산점');
        expect(result['startTime'], '09:00');
        expect(result['endTime'], '12:00');
        expect(result['type'], '순회');
      });
    });

    group('fromJson', () {
      test('JSON Map에서 올바르게 생성된다', () {
        final result = Schedule.fromJson(testJson);

        expect(result.id, 1);
        expect(result.storeName, '이마트 부산점');
        expect(result.startTime, '09:00');
        expect(result.endTime, '12:00');
        expect(result.type, '순회');
      });
    });

    group('round trip', () {
      test('toJson -> fromJson 변환이 일관성 있다', () {
        final json = testSchedule.toJson();
        final restored = Schedule.fromJson(json);

        expect(restored, testSchedule);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 Schedule은 동일하다', () {
        const schedule1 = Schedule(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );
        const schedule2 = Schedule(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );

        expect(schedule1, schedule2);
      });

      test('다른 값을 가진 두 Schedule은 동일하지 않다', () {
        const other = Schedule(
          id: 2,
          storeName: '홈플러스',
          startTime: '10:00',
          endTime: '14:00',
          type: '고정',
        );

        expect(testSchedule, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 Schedule은 같은 hashCode를 가진다', () {
        const schedule1 = Schedule(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );
        const schedule2 = Schedule(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );

        expect(schedule1.hashCode, schedule2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testSchedule.toString();

        expect(result, contains('Schedule'));
        expect(result, contains('id: 1'));
        expect(result, contains('storeName: 이마트 부산점'));
        expect(result, contains('startTime: 09:00'));
        expect(result, contains('endTime: 12:00'));
        expect(result, contains('type: 순회'));
      });
    });
  });
}
