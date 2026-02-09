import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/schedule_model.dart';
import 'package:mobile/domain/entities/schedule.dart';

void main() {
  group('ScheduleModel', () {
    const testModel = ScheduleModel(
      id: 1,
      storeName: '이마트 부산점',
      startTime: '09:00',
      endTime: '12:00',
      type: '순회',
    );

    final testJson = {
      'id': 1,
      'store_name': '이마트 부산점',
      'start_time': '09:00',
      'end_time': '12:00',
      'type': '순회',
    };

    const testEntity = Schedule(
      id: 1,
      storeName: '이마트 부산점',
      startTime: '09:00',
      endTime: '12:00',
      type: '순회',
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        final result = ScheduleModel.fromJson(testJson);

        expect(result.id, 1);
        expect(result.storeName, '이마트 부산점');
        expect(result.startTime, '09:00');
        expect(result.endTime, '12:00');
        expect(result.type, '순회');
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        final result = testModel.toJson();

        expect(result['id'], 1);
        expect(result['store_name'], '이마트 부산점');
        expect(result['start_time'], '09:00');
        expect(result['end_time'], '12:00');
        expect(result['type'], '순회');
      });
    });

    group('toEntity', () {
      test('올바른 Schedule 엔티티를 생성해야 한다', () {
        final result = testModel.toEntity();

        expect(result.id, testModel.id);
        expect(result.storeName, testModel.storeName);
        expect(result.startTime, testModel.startTime);
        expect(result.endTime, testModel.endTime);
        expect(result.type, testModel.type);
      });
    });

    group('fromEntity', () {
      test('Schedule 엔티티로부터 올바른 ScheduleModel을 생성해야 한다', () {
        final result = ScheduleModel.fromEntity(testEntity);

        expect(result.id, testEntity.id);
        expect(result.storeName, testEntity.storeName);
        expect(result.startTime, testEntity.startTime);
        expect(result.endTime, testEntity.endTime);
        expect(result.type, testEntity.type);
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        final modelFromJson = ScheduleModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = ScheduleModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        expect(jsonResult, testJson);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ScheduleModel은 동일해야 한다', () {
        const model1 = ScheduleModel(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );
        const model2 = ScheduleModel(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );

        expect(model1, model2);
      });

      test('다른 값을 가진 두 ScheduleModel은 동일하지 않아야 한다', () {
        const other = ScheduleModel(
          id: 2,
          storeName: '홈플러스',
          startTime: '13:00',
          endTime: '17:00',
          type: '고정',
        );

        expect(testModel, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ScheduleModel은 같은 hashCode를 가져야 한다', () {
        const model1 = ScheduleModel(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );
        const model2 = ScheduleModel(
          id: 1,
          storeName: '이마트 부산점',
          startTime: '09:00',
          endTime: '12:00',
          type: '순회',
        );

        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        final result = testModel.toString();

        expect(result, contains('ScheduleModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('storeName: 이마트 부산점'));
      });
    });
  });
}
