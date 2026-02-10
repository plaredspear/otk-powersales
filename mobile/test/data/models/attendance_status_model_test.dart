import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/attendance_status_model.dart';
import 'package:mobile/domain/entities/attendance_status.dart';

void main() {
  group('AttendanceStatusModel', () {
    // 테스트용 샘플 데이터
    final testDateTime = DateTime(2026, 2, 9, 9, 30, 0);

    // 모든 필드가 있는 경우
    final testModelComplete = AttendanceStatusModel(
      storeId: 123,
      storeName: '이마트 강남점',
      status: 'COMPLETED',
      workType: '영업',
      registeredAt: testDateTime,
    );

    final testJsonComplete = {
      'store_id': 123,
      'store_name': '이마트 강남점',
      'status': 'COMPLETED',
      'work_type': '영업',
      'registered_at': '2026-02-09T09:30:00.000',
    };

    final testEntityComplete = AttendanceStatus(
      storeId: 123,
      storeName: '이마트 강남점',
      status: 'COMPLETED',
      workType: '영업',
      registeredAt: testDateTime,
    );

    // nullable 필드가 null인 경우
    const testModelPending = AttendanceStatusModel(
      storeId: 456,
      storeName: '롯데마트 잠실점',
      status: 'PENDING',
      workType: null,
      registeredAt: null,
    );

    const testJsonPending = {
      'store_id': 456,
      'store_name': '롯데마트 잠실점',
      'status': 'PENDING',
      'work_type': null,
      'registered_at': null,
    };

    const testEntityPending = AttendanceStatus(
      storeId: 456,
      storeName: '롯데마트 잠실점',
      status: 'PENDING',
      workType: null,
      registeredAt: null,
    );

    group('fromJson', () {
      test('모든 필드가 있는 snake_case JSON을 올바르게 파싱해야 함', () {
        // when
        final model = AttendanceStatusModel.fromJson(testJsonComplete);

        // then
        expect(model.storeId, 123);
        expect(model.storeName, '이마트 강남점');
        expect(model.status, 'COMPLETED');
        expect(model.workType, '영업');
        expect(model.registeredAt, testDateTime);
      });

      test('null workType과 null registeredAt을 올바르게 처리해야 함', () {
        // when
        final model = AttendanceStatusModel.fromJson(testJsonPending);

        // then
        expect(model.storeId, 456);
        expect(model.storeName, '롯데마트 잠실점');
        expect(model.status, 'PENDING');
        expect(model.workType, isNull);
        expect(model.registeredAt, isNull);
      });

      test('registered_at이 null이 아닌 경우 ISO8601 문자열에서 DateTime으로 파싱해야 함', () {
        // given
        final jsonWithIsoDate = {
          'store_id': 789,
          'store_name': '홈플러스 강서점',
          'status': 'COMPLETED',
          'work_type': '배송',
          'registered_at': '2026-02-09T14:45:30.123Z',
        };

        // when
        final model = AttendanceStatusModel.fromJson(jsonWithIsoDate);

        // then
        expect(model.registeredAt, DateTime.parse('2026-02-09T14:45:30.123Z'));
        expect(model.registeredAt?.isUtc, true);
      });

      test('workType만 null인 경우도 올바르게 처리해야 함', () {
        // given
        final jsonWithNullWorkType = {
          'store_id': 111,
          'store_name': '테스트 매장',
          'status': 'COMPLETED',
          'work_type': null,
          'registered_at': '2026-02-09T10:00:00.000',
        };

        // when
        final model = AttendanceStatusModel.fromJson(jsonWithNullWorkType);

        // then
        expect(model.workType, isNull);
        expect(model.registeredAt, isNotNull);
      });
    });

    group('toJson', () {
      test('모든 필드가 있는 경우 snake_case JSON으로 올바르게 직렬화해야 함', () {
        // when
        final json = testModelComplete.toJson();

        // then
        expect(json['store_id'], 123);
        expect(json['store_name'], '이마트 강남점');
        expect(json['status'], 'COMPLETED');
        expect(json['work_type'], '영업');
        expect(json['registered_at'], testDateTime.toIso8601String());
      });

      test('null 필드들을 올바르게 직렬화해야 함', () {
        // when
        final json = testModelPending.toJson();

        // then
        expect(json['store_id'], 456);
        expect(json['store_name'], '롯데마트 잠실점');
        expect(json['status'], 'PENDING');
        expect(json['work_type'], isNull);
        expect(json['registered_at'], isNull);
      });

      test('registered_at을 ISO8601 문자열로 직렬화해야 함', () {
        // when
        final json = testModelComplete.toJson();

        // then
        expect(json['registered_at'], isA<String>());
        expect(json['registered_at'], '2026-02-09T09:30:00.000');
      });

      test('UTC 시간도 올바르게 직렬화해야 함', () {
        // given
        final utcDateTime = DateTime.utc(2026, 2, 9, 14, 45, 30);
        final modelWithUtc = AttendanceStatusModel(
          storeId: 789,
          storeName: '홈플러스 강서점',
          status: 'COMPLETED',
          workType: '배송',
          registeredAt: utcDateTime,
        );

        // when
        final json = modelWithUtc.toJson();

        // then
        expect(json['registered_at'], utcDateTime.toIso8601String());
        expect(json['registered_at'], contains('Z'));
      });

      test('workType만 null인 경우도 올바르게 직렬화해야 함', () {
        // given
        final modelWithNullWorkType = AttendanceStatusModel(
          storeId: 111,
          storeName: '테스트 매장',
          status: 'COMPLETED',
          workType: null,
          registeredAt: DateTime(2026, 2, 9, 10, 0, 0),
        );

        // when
        final json = modelWithNullWorkType.toJson();

        // then
        expect(json['work_type'], isNull);
        expect(json['registered_at'], isNotNull);
      });
    });

    group('toEntity', () {
      test('모든 필드가 있는 경우 AttendanceStatus 엔티티로 올바르게 변환해야 함', () {
        // when
        final entity = testModelComplete.toEntity();

        // then
        expect(entity.storeId, testModelComplete.storeId);
        expect(entity.storeName, testModelComplete.storeName);
        expect(entity.status, testModelComplete.status);
        expect(entity.workType, testModelComplete.workType);
        expect(entity.registeredAt, testModelComplete.registeredAt);
      });

      test('null 필드가 있는 경우도 올바르게 변환해야 함', () {
        // when
        final entity = testModelPending.toEntity();

        // then
        expect(entity.storeId, testModelPending.storeId);
        expect(entity.storeName, testModelPending.storeName);
        expect(entity.status, testModelPending.status);
        expect(entity.workType, isNull);
        expect(entity.registeredAt, isNull);
      });

      test('toEntity 결과는 원본 엔티티와 동일해야 함 (complete)', () {
        // when
        final entity = testModelComplete.toEntity();

        // then
        expect(entity, testEntityComplete);
      });

      test('toEntity 결과는 원본 엔티티와 동일해야 함 (pending)', () {
        // when
        final entity = testModelPending.toEntity();

        // then
        expect(entity, testEntityPending);
      });
    });

    group('fromEntity', () {
      test('모든 필드가 있는 AttendanceStatus 엔티티에서 올바르게 생성해야 함', () {
        // when
        final model = AttendanceStatusModel.fromEntity(testEntityComplete);

        // then
        expect(model.storeId, testEntityComplete.storeId);
        expect(model.storeName, testEntityComplete.storeName);
        expect(model.status, testEntityComplete.status);
        expect(model.workType, testEntityComplete.workType);
        expect(model.registeredAt, testEntityComplete.registeredAt);
      });

      test('null 필드가 있는 엔티티에서도 올바르게 생성해야 함', () {
        // when
        final model = AttendanceStatusModel.fromEntity(testEntityPending);

        // then
        expect(model.storeId, testEntityPending.storeId);
        expect(model.storeName, testEntityPending.storeName);
        expect(model.status, testEntityPending.status);
        expect(model.workType, isNull);
        expect(model.registeredAt, isNull);
      });

      test('fromEntity 결과는 원본 모델과 동일해야 함 (complete)', () {
        // when
        final model = AttendanceStatusModel.fromEntity(testEntityComplete);

        // then
        expect(model, testModelComplete);
      });

      test('fromEntity 결과는 원본 모델과 동일해야 함 (pending)', () {
        // when
        final model = AttendanceStatusModel.fromEntity(testEntityPending);

        // then
        expect(model, testModelPending);
      });
    });

    group('roundtrip', () {
      test('entity -> model -> entity 변환이 데이터를 보존해야 함 (complete)', () {
        // when
        final model = AttendanceStatusModel.fromEntity(testEntityComplete);
        final convertedEntity = model.toEntity();

        // then
        expect(convertedEntity, testEntityComplete);
      });

      test('entity -> model -> entity 변환이 데이터를 보존해야 함 (pending)', () {
        // when
        final model = AttendanceStatusModel.fromEntity(testEntityPending);
        final convertedEntity = model.toEntity();

        // then
        expect(convertedEntity, testEntityPending);
      });

      test('model -> json -> model 변환이 데이터를 보존해야 함 (complete)', () {
        // when
        final json = testModelComplete.toJson();
        final convertedModel = AttendanceStatusModel.fromJson(json);

        // then
        expect(convertedModel, testModelComplete);
      });

      test('model -> json -> model 변환이 데이터를 보존해야 함 (pending)', () {
        // when
        final json = testModelPending.toJson();
        final convertedModel = AttendanceStatusModel.fromJson(json);

        // then
        expect(convertedModel, testModelPending);
      });

      test('json -> model -> json 변환이 데이터를 보존해야 함 (complete)', () {
        // when
        final model = AttendanceStatusModel.fromJson(testJsonComplete);
        final convertedJson = model.toJson();

        // then
        expect(convertedJson, testJsonComplete);
      });

      test('json -> model -> json 변환이 데이터를 보존해야 함 (pending)', () {
        // when
        final model = AttendanceStatusModel.fromJson(testJsonPending);
        final convertedJson = model.toJson();

        // then
        expect(convertedJson, testJsonPending);
      });

      test('전체 roundtrip: json -> model -> entity -> model -> json (complete)', () {
        // when
        final model1 = AttendanceStatusModel.fromJson(testJsonComplete);
        final entity = model1.toEntity();
        final model2 = AttendanceStatusModel.fromEntity(entity);
        final finalJson = model2.toJson();

        // then
        expect(finalJson, testJsonComplete);
        expect(model1, model2);
      });
    });

    group('equality', () {
      test('동일한 값을 가진 두 모델은 equal해야 함 (complete)', () {
        // given
        final model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        final model2 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        // then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('동일한 값을 가진 두 모델은 equal해야 함 (pending with nulls)', () {
        // given
        const model1 = AttendanceStatusModel(
          storeId: 456,
          storeName: '롯데마트 잠실점',
          status: 'PENDING',
          workType: null,
          registeredAt: null,
        );

        const model2 = AttendanceStatusModel(
          storeId: 456,
          storeName: '롯데마트 잠실점',
          status: 'PENDING',
          workType: null,
          registeredAt: null,
        );

        // then
        expect(model1, model2);
        expect(model1.hashCode, model2.hashCode);
      });

      test('다른 값을 가진 두 모델은 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        const model2 = AttendanceStatusModel(
          storeId: 456,
          storeName: '롯데마트 잠실점',
          status: 'PENDING',
          workType: null,
          registeredAt: null,
        );

        // then
        expect(model1, isNot(model2));
        expect(model1.hashCode, isNot(model2.hashCode));
      });

      test('status만 다르면 equal하지 않아야 함', () {
        // given
        const model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: null,
          registeredAt: null,
        );

        const model2 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'PENDING',
          workType: null,
          registeredAt: null,
        );

        // then
        expect(model1, isNot(model2));
      });

      test('workType만 다르면 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        final model2 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '배송',
          registeredAt: testDateTime,
        );

        // then
        expect(model1, isNot(model2));
      });

      test('registeredAt만 다르면 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        final model2 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: DateTime(2026, 2, 9, 10, 30, 0),
        );

        // then
        expect(model1, isNot(model2));
      });

      test('workType이 null vs non-null이면 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        final model2 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: null,
          registeredAt: testDateTime,
        );

        // then
        expect(model1, isNot(model2));
      });

      test('registeredAt이 null vs non-null이면 equal하지 않아야 함', () {
        // given
        final model1 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: testDateTime,
        );

        const model2 = AttendanceStatusModel(
          storeId: 123,
          storeName: '이마트 강남점',
          status: 'COMPLETED',
          workType: '영업',
          registeredAt: null,
        );

        // then
        expect(model1, isNot(model2));
      });
    });

    group('toString', () {
      test('toString은 모든 필드를 포함해야 함 (complete)', () {
        // when
        final string = testModelComplete.toString();

        // then
        expect(string, contains('AttendanceStatusModel'));
        expect(string, contains('storeId: 123'));
        expect(string, contains('storeName: 이마트 강남점'));
        expect(string, contains('status: COMPLETED'));
        expect(string, contains('workType: 영업'));
        expect(string, contains('registeredAt: $testDateTime'));
      });

      test('toString은 null 필드도 포함해야 함 (pending)', () {
        // when
        final string = testModelPending.toString();

        // then
        expect(string, contains('AttendanceStatusModel'));
        expect(string, contains('storeId: 456'));
        expect(string, contains('storeName: 롯데마트 잠실점'));
        expect(string, contains('status: PENDING'));
        expect(string, contains('workType: null'));
        expect(string, contains('registeredAt: null'));
      });
    });
  });
}
