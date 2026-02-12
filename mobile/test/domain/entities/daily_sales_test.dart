import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales.dart';

void main() {
  group('DailySalesStatus Enum', () {
    test('상태 값이 올바르게 정의된다', () {
      expect(DailySalesStatus.draft.value, 'DRAFT');
      expect(DailySalesStatus.registered.value, 'REGISTERED');
    });

    test('fromString이 올바르게 동작한다', () {
      expect(
        DailySalesStatus.fromString('DRAFT'),
        DailySalesStatus.draft,
      );
      expect(
        DailySalesStatus.fromString('REGISTERED'),
        DailySalesStatus.registered,
      );
    });

    test('fromString이 존재하지 않는 값을 기본값으로 처리한다', () {
      expect(
        DailySalesStatus.fromString('INVALID'),
        DailySalesStatus.draft,
      );
    });
  });

  group('DailySales Entity', () {
    final testDate = DateTime(2026, 2, 12);
    final testRegisteredAt = DateTime(2026, 2, 12, 14, 30);

    group('생성 테스트', () {
      test('DailySales 엔티티가 올바르게 생성된다 (대표제품만)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.registered,
          registeredAt: testRegisteredAt,
        );

        expect(dailySales.id, 'ds-001');
        expect(dailySales.eventId, 'event-001');
        expect(dailySales.salesDate, testDate);
        expect(dailySales.mainProductPrice, 1000);
        expect(dailySales.mainProductQuantity, 10);
        expect(dailySales.mainProductAmount, 10000);
        expect(dailySales.subProductCode, null);
        expect(dailySales.subProductName, null);
        expect(dailySales.subProductQuantity, null);
        expect(dailySales.subProductAmount, null);
        expect(dailySales.photoUrl, 'https://example.com/photo.jpg');
        expect(dailySales.status, DailySalesStatus.registered);
        expect(dailySales.registeredAt, testRegisteredAt);
      });

      test('DailySales 엔티티가 올바르게 생성된다 (기타제품만)', () {
        final dailySales = DailySales(
          id: 'ds-002',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        expect(dailySales.id, 'ds-002');
        expect(dailySales.eventId, 'event-001');
        expect(dailySales.mainProductPrice, null);
        expect(dailySales.subProductCode, 'P001');
        expect(dailySales.subProductName, '라면');
        expect(dailySales.subProductQuantity, 5);
        expect(dailySales.subProductAmount, 5000);
        expect(dailySales.status, DailySalesStatus.draft);
        expect(dailySales.registeredAt, null);
      });

      test('DailySales 엔티티가 올바르게 생성된다 (대표+기타 제품)', () {
        final dailySales = DailySales(
          id: 'ds-003',
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
        );

        expect(dailySales.hasMainProduct, true);
        expect(dailySales.hasSubProduct, true);
        expect(dailySales.hasAnyProduct, true);
      });
    });

    group('Getter 테스트', () {
      test('hasMainProduct가 올바르게 동작한다 - true 케이스', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasMainProduct, true);
      });

      test('hasMainProduct가 올바르게 동작한다 - false 케이스 (일부 필드 누락)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          // mainProductAmount 누락
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasMainProduct, false);
      });

      test('hasSubProduct가 올바르게 동작한다 - true 케이스', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasSubProduct, true);
      });

      test('hasSubProduct가 올바르게 동작한다 - false 케이스 (일부 필드 누락)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          // subProductQuantity, subProductAmount 누락
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasSubProduct, false);
      });

      test('hasAnyProduct가 올바르게 동작한다 - 대표제품만 있는 경우', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasAnyProduct, true);
      });

      test('hasAnyProduct가 올바르게 동작한다 - 기타제품만 있는 경우', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasAnyProduct, true);
      });

      test('hasAnyProduct가 올바르게 동작한다 - 둘 다 없는 경우', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasAnyProduct, false);
      });

      test('canRegister가 올바르게 동작한다 - 등록 가능 (대표제품 + 사진)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        expect(dailySales.canRegister, true);
      });

      test('canRegister가 올바르게 동작한다 - 등록 불가 (제품 없음)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        expect(dailySales.canRegister, false);
      });

      test('canRegister가 올바르게 동작한다 - 등록 불가 (사진 없음)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.canRegister, false);
      });

      test('canSaveDraft가 항상 true를 반환한다', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.canSaveDraft, true);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final original = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.draft,
        );

        final newDate = DateTime(2026, 2, 13);
        final copied = original.copyWith(
          id: 'ds-002',
          eventId: 'event-002',
          salesDate: newDate,
          mainProductPrice: 2000,
          mainProductQuantity: 20,
          mainProductAmount: 40000,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.registered,
          registeredAt: testRegisteredAt,
        );

        expect(copied.id, 'ds-002');
        expect(copied.eventId, 'event-002');
        expect(copied.salesDate, newDate);
        expect(copied.mainProductPrice, 2000);
        expect(copied.mainProductQuantity, 20);
        expect(copied.mainProductAmount, 40000);
        expect(copied.subProductCode, 'P001');
        expect(copied.subProductName, '라면');
        expect(copied.subProductQuantity, 5);
        expect(copied.subProductAmount, 5000);
        expect(copied.photoUrl, 'https://example.com/photo.jpg');
        expect(copied.status, DailySalesStatus.registered);
        expect(copied.registeredAt, testRegisteredAt);
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final original = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          status: DailySalesStatus.draft,
        );

        final copied = original.copyWith(
          mainProductQuantity: 20,
          status: DailySalesStatus.registered,
        );

        expect(copied.id, original.id);
        expect(copied.eventId, original.eventId);
        expect(copied.salesDate, original.salesDate);
        expect(copied.mainProductPrice, original.mainProductPrice);
        expect(copied.mainProductQuantity, 20);
        expect(copied.status, DailySalesStatus.registered);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          status: DailySalesStatus.draft,
        );
        final copied = original.copyWith(mainProductPrice: 9999);

        expect(original.mainProductPrice, 1000);
        expect(copied.mainProductPrice, 9999);
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다 (대표제품 + 기타제품)', () {
        final dailySales = DailySales(
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
        final json = dailySales.toJson();

        expect(json['id'], 'ds-001');
        expect(json['eventId'], 'event-001');
        expect(json['salesDate'], testDate.toIso8601String());
        expect(json['mainProductPrice'], 1000);
        expect(json['mainProductQuantity'], 10);
        expect(json['mainProductAmount'], 10000);
        expect(json['subProductCode'], 'P001');
        expect(json['subProductName'], '라면');
        expect(json['subProductQuantity'], 5);
        expect(json['subProductAmount'], 5000);
        expect(json['photoUrl'], 'https://example.com/photo.jpg');
        expect(json['status'], 'REGISTERED');
        expect(json['registeredAt'], testRegisteredAt.toIso8601String());
      });

      test('toJson이 올바르게 동작한다 (nullable 필드가 null인 경우)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );
        final json = dailySales.toJson();

        expect(json['mainProductPrice'], null);
        expect(json['mainProductQuantity'], null);
        expect(json['mainProductAmount'], null);
        expect(json['subProductCode'], null);
        expect(json['subProductName'], null);
        expect(json['subProductQuantity'], null);
        expect(json['subProductAmount'], null);
        expect(json['photoUrl'], null);
        expect(json['registeredAt'], null);
        expect(json['status'], 'DRAFT');
      });

      test('fromJson이 올바르게 동작한다 (모든 필드)', () {
        final json = {
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

        final dailySales = DailySales.fromJson(json);

        expect(dailySales.id, 'ds-001');
        expect(dailySales.eventId, 'event-001');
        expect(dailySales.salesDate, testDate);
        expect(dailySales.mainProductPrice, 1000);
        expect(dailySales.mainProductQuantity, 10);
        expect(dailySales.mainProductAmount, 10000);
        expect(dailySales.subProductCode, 'P001');
        expect(dailySales.subProductName, '라면');
        expect(dailySales.subProductQuantity, 5);
        expect(dailySales.subProductAmount, 5000);
        expect(dailySales.photoUrl, 'https://example.com/photo.jpg');
        expect(dailySales.status, DailySalesStatus.registered);
        expect(dailySales.registeredAt, testRegisteredAt);
      });

      test('fromJson이 올바르게 동작한다 (nullable 필드가 null인 경우)', () {
        final json = {
          'id': 'ds-001',
          'eventId': 'event-001',
          'salesDate': testDate.toIso8601String(),
          'status': 'DRAFT',
        };

        final dailySales = DailySales.fromJson(json);

        expect(dailySales.mainProductPrice, null);
        expect(dailySales.mainProductQuantity, null);
        expect(dailySales.mainProductAmount, null);
        expect(dailySales.subProductCode, null);
        expect(dailySales.subProductName, null);
        expect(dailySales.subProductQuantity, null);
        expect(dailySales.subProductAmount, null);
        expect(dailySales.photoUrl, null);
        expect(dailySales.registeredAt, null);
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.registered,
          registeredAt: testRegisteredAt,
        );
        final json = original.toJson();
        final restored = DailySales.fromJson(json);

        expect(restored, original);
      });

      test('fromJson이 존재하지 않는 status를 기본값(draft)으로 처리한다', () {
        final json = {
          'id': 'ds-001',
          'eventId': 'event-001',
          'salesDate': testDate.toIso8601String(),
          'status': 'INVALID_STATUS',
        };

        final dailySales = DailySales.fromJson(json);

        expect(dailySales.status, DailySalesStatus.draft);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final dailySales1 = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.registered,
        );
        final dailySales2 = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.registered,
        );

        expect(dailySales1, dailySales2);
        expect(dailySales1.hashCode, dailySales2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final dailySales1 = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );
        final dailySales2 = DailySales(
          id: 'ds-002',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        expect(dailySales1, isNot(dailySales2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        expect(dailySales, dailySales);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final dailySales = DailySales(
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
        );
        final str = dailySales.toString();

        expect(str, contains('ds-001'));
        expect(str, contains('event-001'));
        expect(str, contains('1000'));
        expect(str, contains('10'));
        expect(str, contains('10000'));
        expect(str, contains('P001'));
        expect(str, contains('라면'));
        expect(str, contains('5'));
        expect(str, contains('5000'));
        expect(str, contains('photo.jpg'));
        expect(str, contains('DailySalesStatus.registered'));
      });
    });

    group('비즈니스 로직 검증', () {
      test('대표제품만 입력하면 등록 가능하다 (사진 포함)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasMainProduct, true);
        expect(dailySales.hasSubProduct, false);
        expect(dailySales.hasAnyProduct, true);
        expect(dailySales.canRegister, true);
      });

      test('기타제품만 입력해도 등록 가능하다 (사진 포함)', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasMainProduct, false);
        expect(dailySales.hasSubProduct, true);
        expect(dailySales.hasAnyProduct, true);
        expect(dailySales.canRegister, true);
      });

      test('제품 정보가 없으면 등록 불가능하다', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasAnyProduct, false);
        expect(dailySales.canRegister, false);
      });

      test('사진이 없으면 등록 불가능하다', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasAnyProduct, true);
        expect(dailySales.canRegister, false);
      });

      test('임시저장은 항상 가능하다', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        expect(dailySales.canSaveDraft, true);
      });

      test('대표제품 일부 필드만 입력하면 hasMainProduct는 false이다', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          // mainProductQuantity, mainProductAmount 누락
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasMainProduct, false);
        expect(dailySales.hasAnyProduct, false);
      });

      test('기타제품 일부 필드만 입력하면 hasSubProduct는 false이다', () {
        final dailySales = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          // subProductQuantity, subProductAmount 누락
          status: DailySalesStatus.draft,
        );

        expect(dailySales.hasSubProduct, false);
        expect(dailySales.hasAnyProduct, false);
      });
    });
  });
}
