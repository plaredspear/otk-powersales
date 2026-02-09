import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/notice_model.dart';
import 'package:mobile/domain/entities/notice.dart';

void main() {
  group('NoticeModel', () {
    final testCreatedAt = DateTime.parse('2026-02-05T10:00:00.000Z');

    final testModel = NoticeModel(
      id: 1,
      title: '2월 영업 목표 달성 현황',
      type: 'BRANCH',
      createdAt: testCreatedAt,
    );

    final testJson = {
      'id': 1,
      'title': '2월 영업 목표 달성 현황',
      'type': 'BRANCH',
      'created_at': '2026-02-05T10:00:00.000Z',
    };

    final testEntity = Notice(
      id: 1,
      title: '2월 영업 목표 달성 현황',
      type: 'BRANCH',
      createdAt: testCreatedAt,
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        final result = NoticeModel.fromJson(testJson);

        expect(result.id, 1);
        expect(result.title, '2월 영업 목표 달성 현황');
        expect(result.type, 'BRANCH');
        expect(result.createdAt, testCreatedAt);
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        final result = testModel.toJson();

        expect(result['id'], 1);
        expect(result['title'], '2월 영업 목표 달성 현황');
        expect(result['type'], 'BRANCH');
        expect(result['created_at'], isA<String>());
      });
    });

    group('toEntity', () {
      test('올바른 Notice 엔티티를 생성해야 한다', () {
        final result = testModel.toEntity();

        expect(result.id, testModel.id);
        expect(result.title, testModel.title);
        expect(result.type, testModel.type);
        expect(result.createdAt, testModel.createdAt);
      });
    });

    group('fromEntity', () {
      test('Notice 엔티티로부터 올바른 NoticeModel을 생성해야 한다', () {
        final result = NoticeModel.fromEntity(testEntity);

        expect(result.id, testEntity.id);
        expect(result.title, testEntity.title);
        expect(result.type, testEntity.type);
        expect(result.createdAt, testEntity.createdAt);
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        final modelFromJson = NoticeModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = NoticeModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        expect(jsonResult, testJson);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 NoticeModel은 동일해야 한다', () {
        final model1 = NoticeModel(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );
        final model2 = NoticeModel(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );

        expect(model1, model2);
      });

      test('다른 값을 가진 두 NoticeModel은 동일하지 않아야 한다', () {
        final other = NoticeModel(
          id: 2,
          title: '신제품 출시 안내',
          type: 'ALL',
          createdAt: DateTime.parse('2026-02-04T09:00:00.000Z'),
        );

        expect(testModel, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 NoticeModel은 같은 hashCode를 가져야 한다', () {
        final model1 = NoticeModel(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );
        final model2 = NoticeModel(
          id: 1,
          title: '2월 영업 목표 달성 현황',
          type: 'BRANCH',
          createdAt: testCreatedAt,
        );

        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        final result = testModel.toString();

        expect(result, contains('NoticeModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('title: 2월 영업 목표 달성 현황'));
      });
    });
  });
}
