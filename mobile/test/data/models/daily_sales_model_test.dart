import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/daily_sales_model.dart';
import 'package:mobile/domain/entities/daily_sales.dart';

void main() {
  group('DailySalesModel', () {
    final testDate = DateTime(2026, 2, 12);
    final testRegisteredAt = DateTime(2026, 2, 12, 14, 30);

    final testModel = DailySalesModel(
      id: 'ds-001',
      eventId: 'event-001',
      salesDate: testDate.toIso8601String(),
      mainProductPrice: 1000,
      mainProductQuantity: 10,
      mainProductAmount: 10000,
      subProductCode: 'P001',
      subProductName: '라면',
      subProductQuantity: 5,
      subProductAmount: 5000,
      photoUrl: 'https://example.com/photo.jpg',
      status: 'REGISTERED',
      registeredAt: testRegisteredAt.toIso8601String(),
    );

    final testJson = {
      'id': 'ds-001',
      'eventId': 'event-001',
      'salesDate': testDate.toIso8601String(),
      'mainProductPrice': 1000,
      'mainProductQuantity': 10,
      'mainProductAmount': 10000,
      'subProductCode': 'P001',
      'subProductName': '라면',
      'subProductQuantity': 5,
      'subProductAmount': 5000,
      'photoUrl': 'https://example.com/photo.jpg',
      'status': 'REGISTERED',
      'registeredAt': testRegisteredAt.toIso8601String(),
    };

    final testEntity = DailySales(
      id: 'ds-001',
      eventId: 'event-001',
      salesDate: testDate,
      mainProductPrice: 1000,
      mainProductQuantity: 10,
      mainProductAmount: 10000,
      subProductCode: 'P001',
      subProductName: '라면',
      subProductQuantity: 5,
      subProductAmount: 5000,
      photoUrl: 'https://example.com/photo.jpg',
      status: DailySalesStatus.registered,
      registeredAt: testRegisteredAt,
    );

    group('fromJson', () {
      test('JSON을 올바르게 파싱해야 한다 (모든 필드)', () {
        final result = DailySalesModel.fromJson(testJson);

        expect(result.id, 'ds-001');
        expect(result.eventId, 'event-001');
        expect(result.salesDate, testDate.toIso8601String());
        expect(result.mainProductPrice, 1000);
        expect(result.mainProductQuantity, 10);
        expect(result.mainProductAmount, 10000);
        expect(result.subProductCode, 'P001');
        expect(result.subProductName, '라면');
        expect(result.subProductQuantity, 5);
        expect(result.subProductAmount, 5000);
        expect(result.photoUrl, 'https://example.com/photo.jpg');
        expect(result.status, 'REGISTERED');
        expect(result.registeredAt, testRegisteredAt.toIso8601String());
      });

      test('JSON을 올바르게 파싱해야 한다 (nullable 필드가 null)', () {
        final json = {
          'id': 'ds-001',
          'eventId': 'event-001',
          'salesDate': testDate.toIso8601String(),
          'status': 'DRAFT',
        };

        final result = DailySalesModel.fromJson(json);

        expect(result.id, 'ds-001');
        expect(result.eventId, 'event-001');
        expect(result.mainProductPrice, null);
        expect(result.mainProductQuantity, null);
        expect(result.mainProductAmount, null);
        expect(result.subProductCode, null);
        expect(result.subProductName, null);
        expect(result.subProductQuantity, null);
        expect(result.subProductAmount, null);
        expect(result.photoUrl, null);
        expect(result.registeredAt, null);
        expect(result.status, 'DRAFT');
      });
    });

    group('toJson', () {
      test('JSON으로 올바르게 직렬화해야 한다 (모든 필드)', () {
        final result = testModel.toJson();

        expect(result['id'], 'ds-001');
        expect(result['eventId'], 'event-001');
        expect(result['salesDate'], testDate.toIso8601String());
        expect(result['mainProductPrice'], 1000);
        expect(result['mainProductQuantity'], 10);
        expect(result['mainProductAmount'], 10000);
        expect(result['subProductCode'], 'P001');
        expect(result['subProductName'], '라면');
        expect(result['subProductQuantity'], 5);
        expect(result['subProductAmount'], 5000);
        expect(result['photoUrl'], 'https://example.com/photo.jpg');
        expect(result['status'], 'REGISTERED');
        expect(result['registeredAt'], testRegisteredAt.toIso8601String());
      });

      test('JSON으로 올바르게 직렬화해야 한다 (nullable 필드가 null)', () {
        const model = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: '2026-02-12T00:00:00.000',
          status: 'DRAFT',
        );

        final result = model.toJson();

        expect(result['mainProductPrice'], null);
        expect(result['mainProductQuantity'], null);
        expect(result['mainProductAmount'], null);
        expect(result['subProductCode'], null);
        expect(result['subProductName'], null);
        expect(result['subProductQuantity'], null);
        expect(result['subProductAmount'], null);
        expect(result['photoUrl'], null);
        expect(result['registeredAt'], null);
      });
    });

    group('toEntity', () {
      test('올바른 DailySales 엔티티를 생성해야 한다', () {
        final result = testModel.toEntity();

        expect(result.id, testModel.id);
        expect(result.eventId, testModel.eventId);
        expect(result.salesDate, testDate);
        expect(result.mainProductPrice, testModel.mainProductPrice);
        expect(result.mainProductQuantity, testModel.mainProductQuantity);
        expect(result.mainProductAmount, testModel.mainProductAmount);
        expect(result.subProductCode, testModel.subProductCode);
        expect(result.subProductName, testModel.subProductName);
        expect(result.subProductQuantity, testModel.subProductQuantity);
        expect(result.subProductAmount, testModel.subProductAmount);
        expect(result.photoUrl, testModel.photoUrl);
        expect(result.status, DailySalesStatus.registered);
        expect(result.registeredAt, testRegisteredAt);
      });

      test('status 문자열을 DailySalesStatus enum으로 변환해야 한다', () {
        const draftModel = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: '2026-02-12T00:00:00.000',
          status: 'DRAFT',
        );

        final result = draftModel.toEntity();

        expect(result.status, DailySalesStatus.draft);
      });

      test('salesDate 문자열을 DateTime으로 변환해야 한다', () {
        final result = testModel.toEntity();

        expect(result.salesDate, isA<DateTime>());
        expect(result.salesDate, testDate);
      });

      test('registeredAt이 null인 경우 엔티티의 registeredAt도 null이어야 한다', () {
        const model = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: '2026-02-12T00:00:00.000',
          status: 'DRAFT',
        );

        final result = model.toEntity();

        expect(result.registeredAt, null);
      });
    });

    group('fromEntity', () {
      test('DailySales 엔티티로부터 올바른 DailySalesModel을 생성해야 한다', () {
        final result = DailySalesModel.fromEntity(testEntity);

        expect(result.id, testEntity.id);
        expect(result.eventId, testEntity.eventId);
        expect(result.salesDate, testEntity.salesDate.toIso8601String());
        expect(result.mainProductPrice, testEntity.mainProductPrice);
        expect(result.mainProductQuantity, testEntity.mainProductQuantity);
        expect(result.mainProductAmount, testEntity.mainProductAmount);
        expect(result.subProductCode, testEntity.subProductCode);
        expect(result.subProductName, testEntity.subProductName);
        expect(result.subProductQuantity, testEntity.subProductQuantity);
        expect(result.subProductAmount, testEntity.subProductAmount);
        expect(result.photoUrl, testEntity.photoUrl);
        expect(result.status, 'REGISTERED');
        expect(
          result.registeredAt,
          testEntity.registeredAt?.toIso8601String(),
        );
      });

      test('DailySalesStatus enum을 문자열로 변환해야 한다', () {
        final draftEntity = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        final result = DailySalesModel.fromEntity(draftEntity);

        expect(result.status, 'DRAFT');
      });

      test('DateTime을 ISO8601 문자열로 변환해야 한다', () {
        final result = DailySalesModel.fromEntity(testEntity);

        expect(result.salesDate, isA<String>());
        expect(result.salesDate, testDate.toIso8601String());
      });

      test('registeredAt이 null인 경우 모델의 registeredAt도 null이어야 한다', () {
        final entity = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        final result = DailySalesModel.fromEntity(entity);

        expect(result.registeredAt, null);
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        final modelFromJson = DailySalesModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = DailySalesModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        expect(jsonResult, testJson);
      });

      test('entity -> fromEntity -> toEntity 변환이 일관성 있어야 한다', () {
        final modelFromEntity = DailySalesModel.fromEntity(testEntity);
        final entityResult = modelFromEntity.toEntity();

        expect(entityResult, testEntity);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 DailySalesModel은 동일해야 한다', () {
        final model1 = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate.toIso8601String(),
          mainProductPrice: 1000,
          status: 'REGISTERED',
        );
        final model2 = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate.toIso8601String(),
          mainProductPrice: 1000,
          status: 'REGISTERED',
        );

        expect(model1, model2);
      });

      test('다른 값을 가진 두 DailySalesModel은 동일하지 않아야 한다', () {
        final model1 = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate.toIso8601String(),
          status: 'DRAFT',
        );
        final model2 = DailySalesModel(
          id: 'ds-002',
          eventId: 'event-001',
          salesDate: testDate.toIso8601String(),
          status: 'DRAFT',
        );

        expect(model1, isNot(model2));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 DailySalesModel은 같은 hashCode를 가져야 한다', () {
        final model1 = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate.toIso8601String(),
          status: 'DRAFT',
        );
        final model2 = DailySalesModel(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate.toIso8601String(),
          status: 'DRAFT',
        );

        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        final result = testModel.toString();

        expect(result, contains('DailySalesModel'));
        expect(result, contains('id: ds-001'));
        expect(result, contains('eventId: event-001'));
        expect(result, contains('mainProductPrice: 1000'));
        expect(result, contains('subProductCode: P001'));
        expect(result, contains('status: REGISTERED'));
      });
    });
  });
}
