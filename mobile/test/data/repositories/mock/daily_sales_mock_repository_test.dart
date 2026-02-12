import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/mock/daily_sales_mock_data.dart';
import 'package:mobile/data/repositories/mock/daily_sales_mock_repository.dart';
import 'package:mobile/domain/entities/daily_sales.dart';

void main() {
  group('DailySalesMockRepository', () {
    late DailySalesMockRepository repository;
    final testDate = DateTime(2026, 2, 13);
    final testPhoto = File('/test/photo.jpg');

    setUp(() {
      repository = DailySalesMockRepository();
    });

    group('초기화', () {
      test('Mock 데이터가 올바르게 로드된다', () {
        final all = repository.getAll();

        expect(all.length, DailySalesMockData.data.length);
        expect(all.first.id, DailySalesMockData.data.first.id);
      });

      test('특정 행사의 일매출을 조회할 수 있다', () {
        final eventSales = repository.getByEventId('event-001');

        expect(eventSales, isNotEmpty);
        expect(
          eventSales.every((sales) => sales.eventId == 'event-001'),
          true,
        );
      });
    });

    group('registerDailySales', () {
      test('정상 등록 - 대표제품만', () async {
        final result = await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        expect(result.id, startsWith('ds-'));
        expect(result.eventId, 'event-999');
        expect(result.salesDate, testDate);
        expect(result.mainProductPrice, 1000);
        expect(result.mainProductQuantity, 10);
        expect(result.mainProductAmount, 10000);
        expect(result.photoUrl, contains('photo.jpg'));
        expect(result.status, DailySalesStatus.registered);
        expect(result.registeredAt, isNotNull);
      });

      test('정상 등록 - 기타제품만', () async {
        final result = await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photo: testPhoto,
        );

        expect(result.eventId, 'event-999');
        expect(result.subProductCode, 'P001');
        expect(result.subProductName, '라면');
        expect(result.subProductQuantity, 5);
        expect(result.subProductAmount, 5000);
        expect(result.status, DailySalesStatus.registered);
      });

      test('정상 등록 - 대표제품 + 기타제품', () async {
        final result = await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photo: testPhoto,
        );

        expect(result.mainProductPrice, 1000);
        expect(result.subProductCode, 'P001');
        expect(result.status, DailySalesStatus.registered);
      });

      test('등록 후 목록에 추가된다', () async {
        final beforeCount = repository.getAll().length;

        await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        final afterCount = repository.getAll().length;
        expect(afterCount, beforeCount + 1);
      });

      test('등록 실패 - 제품 정보 없음', () async {
        expect(
          () => repository.registerDailySales(
            eventId: 'event-999',
            salesDate: testDate,
            photo: testPhoto,
          ),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('대표 제품 또는 기타 제품'),
            ),
          ),
        );
      });

      test('등록 실패 - 중복 등록 (같은 행사, 같은 날짜)', () async {
        // 첫 번째 등록 성공
        await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        // 같은 행사, 같은 날짜에 두 번째 등록 시도
        expect(
          () => repository.registerDailySales(
            eventId: 'event-999',
            salesDate: testDate,
            mainProductPrice: 2000,
            mainProductQuantity: 20,
            mainProductAmount: 40000,
            photo: testPhoto,
          ),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('이미 등록'),
            ),
          ),
        );
      });

      test('등록 성공 - 다른 날짜에는 중복 등록 가능', () async {
        // 첫 번째 날짜 등록
        await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        // 다른 날짜에 등록 시도 (성공해야 함)
        final result = await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate.add(const Duration(days: 1)),
          mainProductPrice: 2000,
          mainProductQuantity: 20,
          mainProductAmount: 40000,
          photo: testPhoto,
        );

        expect(result.salesDate, testDate.add(const Duration(days: 1)));
        expect(result.status, DailySalesStatus.registered);
      });

      test('등록 성공 - 다른 행사에는 중복 등록 가능', () async {
        // 첫 번째 행사 등록
        await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        // 다른 행사에 등록 시도 (성공해야 함)
        final result = await repository.registerDailySales(
          eventId: 'event-998',
          salesDate: testDate,
          mainProductPrice: 2000,
          mainProductQuantity: 20,
          mainProductAmount: 40000,
          photo: testPhoto,
        );

        expect(result.eventId, 'event-998');
        expect(result.status, DailySalesStatus.registered);
      });

      test('photoUrl이 photo 파일명을 포함한다', () async {
        final result = await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: File('/path/to/my-photo-123.jpg'),
        );

        expect(result.photoUrl, contains('my-photo-123.jpg'));
      });
    });

    group('saveDraft', () {
      test('정상 임시저장 - 모든 필드', () async {
        final result = await repository.saveDraft(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        expect(result.id, startsWith('ds-draft-'));
        expect(result.eventId, 'event-999');
        expect(result.mainProductPrice, 1000);
        expect(result.photoUrl, contains('photo.jpg'));
        expect(result.status, DailySalesStatus.draft);
        expect(result.registeredAt, isNull);
      });

      test('정상 임시저장 - 일부 필드만 (검증 없음)', () async {
        final result = await repository.saveDraft(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          // 일부 필드만 입력
        );

        expect(result.eventId, 'event-999');
        expect(result.mainProductPrice, 1000);
        expect(result.mainProductQuantity, isNull);
        expect(result.status, DailySalesStatus.draft);
      });

      test('정상 임시저장 - 빈 데이터 (검증 없음)', () async {
        final result = await repository.saveDraft(
          eventId: 'event-999',
          salesDate: testDate,
        );

        expect(result.eventId, 'event-999');
        expect(result.mainProductPrice, isNull);
        expect(result.subProductCode, isNull);
        expect(result.status, DailySalesStatus.draft);
      });

      test('임시저장 후 목록에 추가된다', () async {
        final beforeCount = repository.getAll().length;

        await repository.saveDraft(
          eventId: 'event-999',
          salesDate: testDate,
        );

        final afterCount = repository.getAll().length;
        expect(afterCount, beforeCount + 1);
      });

      test('임시저장 시 photoUrl은 photo가 있을 때만 설정된다', () async {
        final resultWithPhoto = await repository.saveDraft(
          eventId: 'event-999',
          salesDate: testDate,
          photo: testPhoto,
        );

        final resultWithoutPhoto = await repository.saveDraft(
          eventId: 'event-999',
          salesDate: testDate,
        );

        expect(resultWithPhoto.photoUrl, isNotNull);
        expect(resultWithoutPhoto.photoUrl, isNull);
      });
    });

    group('Helper 메서드', () {
      test('reset()으로 Mock 데이터를 초기화할 수 있다', () async {
        // 새로운 데이터 추가
        await repository.registerDailySales(
          eventId: 'event-999',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        final beforeReset = repository.getAll().length;
        expect(beforeReset, greaterThan(DailySalesMockData.data.length));

        // 초기화
        repository.reset();

        final afterReset = repository.getAll().length;
        expect(afterReset, DailySalesMockData.data.length);
      });

      test('getByEventId()로 특정 행사의 일매출을 조회할 수 있다', () async {
        await repository.registerDailySales(
          eventId: 'event-test',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        final result = repository.getByEventId('event-test');

        expect(result.length, 1);
        expect(result.first.eventId, 'event-test');
      });

      test('getAll()로 전체 일매출을 조회할 수 있다', () {
        final result = repository.getAll();

        expect(result, isA<List<DailySales>>());
        expect(result.length, greaterThanOrEqualTo(DailySalesMockData.data.length));
      });
    });
  });
}
