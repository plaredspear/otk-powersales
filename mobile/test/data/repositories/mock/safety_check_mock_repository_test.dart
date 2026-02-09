import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/safety_check_mock_repository.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';

void main() {
  group('SafetyCheckMockRepository', () {
    late SafetyCheckMockRepository repository;

    setUp(() {
      repository = SafetyCheckMockRepository();
    });

    group('getItems()', () {
      test('Returns default 2 categories with 15 total items', () async {
        // Act
        final result = await repository.getItems();

        // Assert
        expect(result.length, 2);

        // 첫 번째 카테고리: 안전예방 장비 착용
        expect(result[0].id, 1);
        expect(result[0].name, '안전예방 장비 착용');
        expect(result[0].description, '아래 항목을 모두 체크하세요');
        expect(result[0].items.length, 7);

        // 두 번째 카테고리: 사고 예방
        expect(result[1].id, 2);
        expect(result[1].name, '사고 예방');
        expect(result[1].description, '아래 항목을 모두 체크하세요');
        expect(result[1].items.length, 8);

        // 총 항목 수 확인
        final totalItems =
            result.fold<int>(0, (sum, category) => sum + category.items.length);
        expect(totalItems, 15);
      });

      test('Returns customCategories when set', () async {
        // Arrange
        final customCategories = [
          const SafetyCheckCategory(
            id: 99,
            name: '커스텀 카테고리',
            items: [
              SafetyCheckItem(
                id: 999,
                label: '커스텀 항목',
                sortOrder: 1,
                required: true,
              ),
            ],
          ),
        ];
        repository.customCategories = customCategories;

        // Act
        final result = await repository.getItems();

        // Assert
        expect(result, customCategories);
        expect(result.length, 1);
        expect(result[0].id, 99);
        expect(result[0].name, '커스텀 카테고리');
        expect(result[0].items[0].label, '커스텀 항목');
      });

      test('Throws exceptionToThrow when set', () async {
        // Arrange
        final exception = Exception('Network error');
        repository.exceptionToThrow = exception;

        // Act & Assert
        expect(
          () => repository.getItems(),
          throwsA(exception),
        );
      });

      test('All items are required=true', () async {
        // Act
        final result = await repository.getItems();

        // Assert
        for (final category in result) {
          for (final item in category.items) {
            expect(item.required, true,
                reason: 'Item ${item.id} (${item.label}) should be required');
          }
        }
      });
    });

    group('getTodayStatus()', () {
      test('Returns completed=false initially', () async {
        // Act
        final result = await repository.getTodayStatus();

        // Assert
        expect(result.completed, false);
        expect(result.submittedAt, isNull);
      });

      test('Returns customTodayStatus when set', () async {
        // Arrange
        final customStatus = SafetyCheckTodayStatus(
          completed: true,
          submittedAt: DateTime(2025, 2, 8, 9, 30),
        );
        repository.customTodayStatus = customStatus;

        // Act
        final result = await repository.getTodayStatus();

        // Assert
        expect(result, customStatus);
        expect(result.completed, true);
        expect(result.submittedAt, DateTime(2025, 2, 8, 9, 30));
      });

      test('Returns completed=true after submit()', () async {
        // Arrange
        await repository.submit([1, 2, 3]);

        // Act
        final result = await repository.getTodayStatus();

        // Assert
        expect(result.completed, true);
        expect(result.submittedAt, isNotNull);
      });

      test('Throws exceptionToThrow when set', () async {
        // Arrange
        final exception = Exception('Network error');
        repository.exceptionToThrow = exception;

        // Act & Assert
        expect(
          () => repository.getTodayStatus(),
          throwsA(exception),
        );
      });
    });

    group('submit()', () {
      test('Returns SafetyCheckSubmitResult on success', () async {
        // Act
        final result = await repository.submit([1, 2, 3, 4, 5]);

        // Assert
        expect(result, isA<SafetyCheckSubmitResult>());
        expect(result.submissionId, 1);
        expect(result.submittedAt, isNotNull);
        expect(result.safetyCheckCompleted, true);
      });

      test('Throws exception on duplicate submit', () async {
        // Arrange
        await repository.submit([1, 2, 3]);

        // Act & Assert
        expect(
          () => repository.submit([1, 2, 3]),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('이미 안전점검을 완료하였습니다.'),
            ),
          ),
        );
      });

      test('Throws exceptionToThrow when set', () async {
        // Arrange
        final exception = Exception('Network error');
        repository.exceptionToThrow = exception;

        // Act & Assert
        expect(
          () => repository.submit([1, 2, 3]),
          throwsA(exception),
        );
      });
    });
  });
}
