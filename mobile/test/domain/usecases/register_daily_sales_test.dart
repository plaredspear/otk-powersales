import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales.dart';
import 'package:mobile/domain/repositories/daily_sales_repository.dart';
import 'package:mobile/domain/usecases/register_daily_sales.dart';

/// Mock DailySalesRepository for testing
class MockDailySalesRepository implements DailySalesRepository {
  DailySales? registerResult;
  DailySales? draftResult;
  Exception? exceptionToThrow;

  String? lastEventId;
  DateTime? lastSalesDate;
  int? lastMainProductPrice;
  int? lastMainProductQuantity;
  int? lastMainProductAmount;
  String? lastSubProductCode;
  String? lastSubProductName;
  int? lastSubProductQuantity;
  int? lastSubProductAmount;
  File? lastPhoto;

  bool registerCalled = false;
  bool saveDraftCalled = false;

  @override
  Future<DailySales> registerDailySales({
    required String eventId,
    required DateTime salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    required File photo,
  }) async {
    registerCalled = true;
    lastEventId = eventId;
    lastSalesDate = salesDate;
    lastMainProductPrice = mainProductPrice;
    lastMainProductQuantity = mainProductQuantity;
    lastMainProductAmount = mainProductAmount;
    lastSubProductCode = subProductCode;
    lastSubProductName = subProductName;
    lastSubProductQuantity = subProductQuantity;
    lastSubProductAmount = subProductAmount;
    lastPhoto = photo;

    if (exceptionToThrow != null) throw exceptionToThrow!;
    return registerResult!;
  }

  @override
  Future<DailySales> saveDraft({
    required String eventId,
    required DateTime salesDate,
    int? mainProductPrice,
    int? mainProductQuantity,
    int? mainProductAmount,
    String? subProductCode,
    String? subProductName,
    int? subProductQuantity,
    int? subProductAmount,
    File? photo,
  }) async {
    saveDraftCalled = true;
    lastEventId = eventId;
    lastSalesDate = salesDate;
    lastMainProductPrice = mainProductPrice;
    lastMainProductQuantity = mainProductQuantity;
    lastMainProductAmount = mainProductAmount;
    lastSubProductCode = subProductCode;
    lastSubProductName = subProductName;
    lastSubProductQuantity = subProductQuantity;
    lastSubProductAmount = subProductAmount;
    lastPhoto = photo;

    if (exceptionToThrow != null) throw exceptionToThrow!;
    return draftResult!;
  }
}

void main() {
  group('DailySalesRequest', () {
    final testDate = DateTime(2026, 2, 12);
    final testPhoto = File('/path/to/photo.jpg');

    test('hasMainProduct가 올바르게 동작한다 - true', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        mainProductPrice: 1000,
        mainProductQuantity: 10,
        mainProductAmount: 10000,
      );

      expect(request.hasMainProduct, true);
    });

    test('hasMainProduct가 올바르게 동작한다 - false (일부 필드 누락)', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        mainProductPrice: 1000,
        // mainProductQuantity, mainProductAmount 누락
      );

      expect(request.hasMainProduct, false);
    });

    test('hasSubProduct가 올바르게 동작한다 - true', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        subProductCode: 'P001',
        subProductName: '라면',
        subProductQuantity: 5,
        subProductAmount: 5000,
      );

      expect(request.hasSubProduct, true);
    });

    test('hasSubProduct가 올바르게 동작한다 - false (일부 필드 누락)', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        subProductCode: 'P001',
        // subProductName, subProductQuantity, subProductAmount 누락
      );

      expect(request.hasSubProduct, false);
    });

    test('hasAnyProduct가 올바르게 동작한다 - 대표제품만', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        mainProductPrice: 1000,
        mainProductQuantity: 10,
        mainProductAmount: 10000,
      );

      expect(request.hasAnyProduct, true);
    });

    test('hasAnyProduct가 올바르게 동작한다 - 기타제품만', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        subProductCode: 'P001',
        subProductName: '라면',
        subProductQuantity: 5,
        subProductAmount: 5000,
      );

      expect(request.hasAnyProduct, true);
    });

    test('hasAnyProduct가 올바르게 동작한다 - 둘 다 없음', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
      );

      expect(request.hasAnyProduct, false);
    });

    test('canRegister가 올바르게 동작한다 - 등록 가능', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        mainProductPrice: 1000,
        mainProductQuantity: 10,
        mainProductAmount: 10000,
        photo: testPhoto,
      );

      expect(request.canRegister, true);
    });

    test('canRegister가 올바르게 동작한다 - 등록 불가 (제품 없음)', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        photo: testPhoto,
      );

      expect(request.canRegister, false);
    });

    test('canRegister가 올바르게 동작한다 - 등록 불가 (사진 없음)', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
        mainProductPrice: 1000,
        mainProductQuantity: 10,
        mainProductAmount: 10000,
      );

      expect(request.canRegister, false);
    });

    test('canSaveDraft가 항상 true를 반환한다', () {
      final request = DailySalesRequest(
        eventId: 'event-001',
        salesDate: testDate,
      );

      expect(request.canSaveDraft, true);
    });
  });

  group('RegisterDailySales UseCase', () {
    late RegisterDailySales useCase;
    late MockDailySalesRepository mockRepository;

    final testDate = DateTime(2026, 2, 12);
    final testPhoto = File('/path/to/photo.jpg');

    setUp(() {
      mockRepository = MockDailySalesRepository();
      useCase = RegisterDailySales(mockRepository);
    });

    group('일매출 등록 (call)', () {
      test('정상 등록 - 대표제품만', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        mockRepository.registerResult = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.registered,
          registeredAt: DateTime.now(),
        );

        // Act
        final result = await useCase.call(request);

        // Assert
        expect(mockRepository.registerCalled, true);
        expect(mockRepository.lastEventId, 'event-001');
        expect(mockRepository.lastSalesDate, testDate);
        expect(mockRepository.lastMainProductPrice, 1000);
        expect(mockRepository.lastMainProductQuantity, 10);
        expect(mockRepository.lastMainProductAmount, 10000);
        expect(mockRepository.lastPhoto, testPhoto);
        expect(result.status, DailySalesStatus.registered);
      });

      test('정상 등록 - 기타제품만', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photo: testPhoto,
        );

        mockRepository.registerResult = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          subProductCode: 'P001',
          subProductName: '라면',
          subProductQuantity: 5,
          subProductAmount: 5000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.registered,
          registeredAt: DateTime.now(),
        );

        // Act
        final result = await useCase.call(request);

        // Assert
        expect(mockRepository.registerCalled, true);
        expect(mockRepository.lastSubProductCode, 'P001');
        expect(mockRepository.lastSubProductName, '라면');
        expect(result.status, DailySalesStatus.registered);
      });

      test('정상 등록 - 대표제품 + 기타제품', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
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

        mockRepository.registerResult = DailySales(
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
          registeredAt: DateTime.now(),
        );

        // Act
        final result = await useCase.call(request);

        // Assert
        expect(mockRepository.registerCalled, true);
        expect(result.status, DailySalesStatus.registered);
      });

      test('등록 실패 - 제품 정보 없음', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          photo: testPhoto,
        );

        // Act & Assert
        expect(
          () => useCase.call(request),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('대표 제품 또는 기타 제품'),
            ),
          ),
        );

        expect(mockRepository.registerCalled, false);
      });

      test('등록 실패 - 사진 없음', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          // photo 누락
        );

        // Act & Assert
        expect(
          () => useCase.call(request),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('사진을 첨부'),
            ),
          ),
        );

        expect(mockRepository.registerCalled, false);
      });

      test('Repository 예외 전파', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        mockRepository.exceptionToThrow = Exception('오늘 매출이 이미 등록되었습니다');

        // Act & Assert
        expect(
          () => useCase.call(request),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('이미 등록'),
            ),
          ),
        );
      });
    });

    group('일매출 임시저장 (saveDraft)', () {
      test('정상 임시저장 - 모든 필드', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photo: testPhoto,
        );

        mockRepository.draftResult = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          mainProductQuantity: 10,
          mainProductAmount: 10000,
          photoUrl: 'https://example.com/photo.jpg',
          status: DailySalesStatus.draft,
        );

        // Act
        final result = await useCase.saveDraft(request);

        // Assert
        expect(mockRepository.saveDraftCalled, true);
        expect(mockRepository.lastEventId, 'event-001');
        expect(result.status, DailySalesStatus.draft);
      });

      test('정상 임시저장 - 일부 필드만 (검증 없음)', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          // 일부 필드만 입력
        );

        mockRepository.draftResult = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          mainProductPrice: 1000,
          status: DailySalesStatus.draft,
        );

        // Act
        final result = await useCase.saveDraft(request);

        // Assert
        expect(mockRepository.saveDraftCalled, true);
        expect(result.status, DailySalesStatus.draft);
      });

      test('정상 임시저장 - 빈 데이터 (검증 없음)', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
        );

        mockRepository.draftResult = DailySales(
          id: 'ds-001',
          eventId: 'event-001',
          salesDate: testDate,
          status: DailySalesStatus.draft,
        );

        // Act
        final result = await useCase.saveDraft(request);

        // Assert
        expect(mockRepository.saveDraftCalled, true);
        expect(result.status, DailySalesStatus.draft);
      });

      test('Repository 예외 전파', () async {
        // Arrange
        final request = DailySalesRequest(
          eventId: 'event-001',
          salesDate: testDate,
        );

        mockRepository.exceptionToThrow = Exception('행사 기간이 아닙니다');

        // Act & Assert
        expect(
          () => useCase.saveDraft(request),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('행사 기간'),
            ),
          ),
        );
      });
    });
  });
}
