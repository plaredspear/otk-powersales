import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/attendance_result_model.dart';
import 'package:mobile/domain/entities/attendance_result.dart';

void main() {
  group('AttendanceResultModel', () {
    // 테스트용 샘플 데이터
    final testDateTime = DateTime(2026, 2, 9, 9, 30, 0);
    final testModel = AttendanceResultModel(
      attendanceId: 100,
      storeId: 123,
      storeName: '이마트 강남점',
      workType: '영업',
      registeredAt: testDateTime,
      totalCount: 5,
      registeredCount: 3,
    );

    final testJson = {
      'attendance_id': 100,
      'store_id': 123,
      'store_name': '이마트 강남점',
      'work_type': '영업',
      'registered_at': '2026-02-09T09:30:00.000',
      'total_count': 5,
      'registered_count': 3,
    };

    final testEntity = AttendanceResult(
      attendanceId: 100,
      storeId: 123,
      storeName: '이마트 강남점',
      workType: '영업',
      registeredAt: testDateTime,
      totalCount: 5,
      registeredCount: 3,
    );

    group('fromJson', () {
      test('snake_case JSON을 올바르게 파싱해야 함', () {
        // when
        final model = AttendanceResultModel.fromJson(testJson);

        // then
        expect(model.attendanceId, 100);
        expect(model.storeId, 123);
        expect(model.storeName, '이마트 강남점');
        expect(model.workType, '영업');
        expect(model.registeredAt, testDateTime);
        expect(model.totalCount, 5);
        expect(model.registeredCount, 3);
      });

      test('registered_at을 ISO8601 문자열에서 DateTime으로 파싱해야 함', () {
        // given
        final jsonWithIsoDate = {
          'attendance_id': 200,
          'store_id': 456,
          'store_name': '롯데마트 잠실점',
          'work_type': '배송',
          'registered_at': '2026-02-09T14:45:30.123Z',
          'total_count': 10,
          'registered_count': 7,
        };

        // when
        final model = AttendanceResultModel.fromJson(jsonWithIsoDate);

        // then
        expect(model.registeredAt, DateTime.parse('2026-02-09T14:45:30.123Z'));
        expect(model.registeredAt.isUtc, true);
      });

      test('모든 필드가 올바르게 매핑되어야 함', () {
        // when
        final model = AttendanceResultModel.fromJson(testJson);

        // then
        expect(model.attendanceId, testJson['attendance_id']);
        expect(model.storeId, testJson['store_id']);
        expect(model.storeName, testJson['store_name']);
        expect(model.workType, testJson['work_type']);
        expect(model.registeredAt.toIso8601String(), testJson['registered_at']);
        expect(model.totalCount, testJson['total_count']);
        expect(model.registeredCount, testJson['registered_count']);
      });
    });

    group('toJson', () {
      test('snake_case JSON으로 올바르게 직렬화해야 함', () {
        // when
        final json = testModel.toJson();

        // then
        expect(json['attendance_id'], 100);
        expect(json['store_id'], 123);
        expect(json['store_name'], '이마트 강남점');
        expect(json['work_type'], '영업');
        expect(json['registered_at'], testDateTime.toIso8601String());
        expect(json['total_count'], 5);
        expect(json['registered_count'], 3);
      });

      test('registered_at을 ISO8601 문자열로 직렬화해야 함', () {
        // when
        final json = testModel.toJson();

        // then
        expect(json['registered_at'], isA<String>());
        expect(json['registered_at'], '2026-02-09T09:30:00.000');
      });

      test('UTC 시간도 올바르게 직렬화해야 함', () {
        // given
        final utcDateTime = DateTime.utc(2026, 2, 9, 14, 45, 30);
        final modelWithUtc = AttendanceResultModel(
          attendanceId: 200,
          storeId: 456,
          storeName: '롯데마트 잠실점',
          workType: '배송',
          registeredAt: utcDateTime,
          totalCount: 10,
          registeredCount: 7,
        );

        // when
        final json = modelWithUtc.toJson();

        // then
        expect(json['registered_at'], utcDateTime.toIso8601String());
        expect(json['registered_at'], contains('Z'));
      });
    });

    group('toEntity', () {
      test('AttendanceResult 엔티티로 올바르게 변환해야 함', () {
        // when
        final entity = testModel.toEntity();

        // then
        expect(entity.attendanceId, testModel.attendanceId);
        expect(entity.storeId, testModel.storeId);
        expect(entity.storeName, testModel.storeName);
        expect(entity.workType, testModel.workType);
        expect(entity.registeredAt, testModel.registeredAt);
        expect(entity.totalCount, testModel.totalCount);
        expect(entity.registeredCount, testModel.registeredCount);
      });

      test('toEntity 결과는 원본 엔티티와 동일해야 함', () {
        // when
        final entity = testModel.toEntity();

        // then
        expect(entity, testEntity);
      });
    });

    group('fromEntity', () {
      test('AttendanceResult 엔티티에서 올바르게 생성해야 함', () {
        // when
        final model = AttendanceResultModel.fromEntity(testEntity);

        // then
        expect(model.attendanceId, testEntity.attendanceId);
        expect(model.storeId, testEntity.storeId);
        expect(model.storeName, testEntity.storeName);
        expect(model.workType, testEntity.workType);
        expect(model.registeredAt, testEntity.registeredAt);
        expect(model.totalCount, testEntity.totalCount);
        expect(model.registeredCount, testEntity.registeredCount);
      });

      test('fromEntity 결과는 원본 모델과 동일해야 함', () {
        // when
        final model = AttendanceResultModel.fromEntity(testEntity);

        // then
        expect(model, testModel);
      });
    });

    group('roundtrip', () {
      test('entity -> model -> entity 변환이 데이터를 보존해야 함', () {
        // when
        final model = AttendanceResultModel.fromEntity(testEntity);
        final convertedEntity = model.toEntity();

        // then
        expect(convertedEntity, testEntity);
      });

      test('model -> json -> model 변환이 데이터를 보존해야 함', () {
        // when
        final json = testModel.toJson();
        final convertedModel = AttendanceResultModel.fromJson(json);

        // then
        expect(convertedModel, testModel);
      });

      test('json -> model -> json 변환이 데이터를 보존해야 함', () {
        // when
        final model = AttendanceResultModel.fromJson(testJson);
        final convertedJson = model.toJson();

        // then
        expect(convertedJson, testJson);
      });

      test('전체 roundtrip: json -> model -> entity -> model -> json', () {
        // when
        final model1 = AttendanceResultModel.fromJson(testJson);
        final entity = model1.toEntity();
        final model2 = AttendanceResultModel.fromEntity(entity);
        final finalJson = model2.toJson();

        // then
        expect(finalJson, testJson);
        expect(model1, model2);
      });
    });

    group('equality', () {
      test('동일한 값을 가진 두 모델은 equal해야 함', () {
        // given
        final model1 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: testDateTime,
          totalCount: 5,
          registeredCount: 3,
        );

        final model2 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: testDateTime,
          totalCount: 5,
          registeredCount: 3,
        );

        // then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 두 모델은 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: testDateTime,
          totalCount: 5,
          registeredCount: 3,
        );

        final model2 = AttendanceResultModel(
          attendanceId: 200,
          storeId: 456,
          storeName: '롯데마트 잠실점',
          workType: '배송',
          registeredAt: DateTime(2026, 2, 10, 10, 0, 0),
          totalCount: 10,
          registeredCount: 7,
        );

        // then
        expect(model1, isNot(model2));
        expect(model1.hashCode, isNot(model2.hashCode));
      });

      test('registeredAt만 다르면 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: testDateTime,
          totalCount: 5,
          registeredCount: 3,
        );

        final model2 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: DateTime(2026, 2, 9, 10, 30, 0), // 다른 시간
          totalCount: 5,
          registeredCount: 3,
        );

        // then
        expect(model1, isNot(model2));
      });

      test('totalCount와 registeredCount가 다르면 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: testDateTime,
          totalCount: 5,
          registeredCount: 3,
        );

        final model2 = AttendanceResultModel(
          attendanceId: 100,
          storeId: 123,
          storeName: '이마트 강남점',
          workType: '영업',
          registeredAt: testDateTime,
          totalCount: 5,
          registeredCount: 5, // 다른 registeredCount
        );

        // then
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('toString은 모든 필드를 포함해야 함', () {
        // when
        final string = testModel.toString();

        // then
        expect(string, contains('AttendanceResultModel'));
        expect(string, contains('attendanceId: 100'));
        expect(string, contains('storeId: 123'));
        expect(string, contains('storeName: 이마트 강남점'));
        expect(string, contains('workType: 영업'));
        expect(string, contains('registeredAt: $testDateTime'));
        expect(string, contains('totalCount: 5'));
        expect(string, contains('registeredCount: 3'));
      });
    });
  });
}
