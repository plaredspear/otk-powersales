import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/notice.dart';

void main() {
  group('Notice', () {
    final testCreatedAt = DateTime.parse('2026-02-05T10:00:00.000Z');

    final testNotice = Notice(
      id: 1,
      title: '2월 영업 목표 달성 현황',
      type: 'BRANCH',
      createdAt: testCreatedAt,
    );

    final testJson = {
      'id': 1,
      'title': '2월 영업 목표 달성 현황',
      'type': 'BRANCH',
      'createdAt': '2026-02-05T10:00:00.000Z',
    };

    group('생성', () {
      test('Notice 엔티티가 올바르게 생성된다', () {
        expect(testNotice.id, 1);
        expect(testNotice.title, '2월 영업 목표 달성 현황');
        expect(testNotice.type, 'BRANCH');
        expect(testNotice.createdAt, testCreatedAt);
      });
    });

    group('typeDisplayName', () {
      test('BRANCH 유형은 "지점공지"를 반환한다', () {
        final notice = testNotice.copyWith(type: 'BRANCH');
        expect(notice.typeDisplayName, '지점공지');
      });

      test('ALL 유형은 "전체공지"를 반환한다', () {
        final notice = testNotice.copyWith(type: 'ALL');
        expect(notice.typeDisplayName, '전체공지');
      });

      test('알 수 없는 유형은 그대로 반환한다', () {
        final notice = testNotice.copyWith(type: 'UNKNOWN');
        expect(notice.typeDisplayName, 'UNKNOWN');
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testNotice.copyWith(title: '신제품 출시 안내');

        expect(copied.id, testNotice.id);
        expect(copied.title, '신제품 출시 안내');
        expect(copied.type, testNotice.type);
        expect(copied.createdAt, testNotice.createdAt);
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final newDate = DateTime.parse('2026-02-07T09:00:00.000Z');
        final copied = testNotice.copyWith(
          id: 2,
          title: '신제품 출시 안내',
          type: 'ALL',
          createdAt: newDate,
        );

        expect(copied.id, 2);
        expect(copied.title, '신제품 출시 안내');
        expect(copied.type, 'ALL');
        expect(copied.createdAt, newDate);
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testNotice.copyWith();

        expect(copied, testNotice);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testNotice.toJson();

        expect(result['id'], 1);
        expect(result['title'], '2월 영업 목표 달성 현황');
        expect(result['type'], 'BRANCH');
        expect(result['createdAt'], isA<String>());
      });
    });

    group('fromJson', () {
      test('JSON Map에서 올바르게 생성된다', () {
        final result = Notice.fromJson(testJson);

        expect(result.id, 1);
        expect(result.title, '2월 영업 목표 달성 현황');
        expect(result.type, 'BRANCH');
        expect(result.createdAt, testCreatedAt);
      });
    });

    group('round trip', () {
      test('toJson -> fromJson 변환이 일관성 있다', () {
        final json = testNotice.toJson();
        final restored = Notice.fromJson(json);

        expect(restored, testNotice);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 Notice는 동일하다', () {
        final notice1 = Notice(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );
        final notice2 = Notice(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );

        expect(notice1, notice2);
      });

      test('다른 값을 가진 두 Notice는 동일하지 않다', () {
        final other = Notice(
          id: 2,
          title: '신제품 출시 안내',
          type: 'ALL',
          createdAt: DateTime.parse('2026-02-04T09:00:00.000Z'),
        );

        expect(testNotice, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 Notice는 같은 hashCode를 가진다', () {
        final notice1 = Notice(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );
        final notice2 = Notice(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );

        expect(notice1.hashCode, notice2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testNotice.toString();

        expect(result, contains('Notice'));
        expect(result, contains('id: 1'));
        expect(result, contains('title: 2월 영업 목표 달성 현황'));
        expect(result, contains('type: BRANCH'));
      });
    });
  });
}
