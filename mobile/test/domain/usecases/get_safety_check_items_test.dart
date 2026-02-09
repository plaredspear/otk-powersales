import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/get_safety_check_items.dart';
import 'package:mobile/domain/repositories/safety_check_repository.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';

void main() {
  group('GetSafetyCheckItems', () {
    late GetSafetyCheckItems useCase;
    late _TestSafetyCheckRepository repository;

    setUp(() {
      repository = _TestSafetyCheckRepository();
      useCase = GetSafetyCheckItems(repository);
    });

    test('Returns categories from repository', () async {
      // Arrange
      final expectedCategories = [
        const SafetyCheckCategory(
          id: 1,
          name: '테스트 카테고리',
          items: [
            SafetyCheckItem(
              id: 1,
              label: '테스트 항목',
              sortOrder: 1,
              required: true,
            ),
          ],
        ),
      ];
      repository.categoriesToReturn = expectedCategories;

      // Act
      final result = await useCase.call();

      // Assert
      expect(result, expectedCategories);
      expect(result.length, 1);
      expect(result[0].name, '테스트 카테고리');
      expect(result[0].items.length, 1);
      expect(result[0].items[0].label, '테스트 항목');
    });

    test('Propagates repository exception', () async {
      // Arrange
      final expectedException = Exception('Network error');
      repository.exceptionToThrow = expectedException;

      // Act & Assert
      expect(
        () => useCase.call(),
        throwsA(expectedException),
      );
    });
  });
}

/// Test implementation of SafetyCheckRepository
class _TestSafetyCheckRepository implements SafetyCheckRepository {
  List<SafetyCheckCategory>? categoriesToReturn;
  SafetyCheckTodayStatus? statusToReturn;
  SafetyCheckSubmitResult? submitResultToReturn;
  Exception? exceptionToThrow;

  @override
  Future<List<SafetyCheckCategory>> getItems() async {
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return categoriesToReturn ?? [];
  }

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() async {
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return statusToReturn ??
        const SafetyCheckTodayStatus(
          completed: false,
          submittedAt: null,
        );
  }

  @override
  Future<SafetyCheckSubmitResult> submit(List<int> checkedItemIds) async {
    if (exceptionToThrow != null) {
      throw exceptionToThrow!;
    }
    return submitResultToReturn ??
        SafetyCheckSubmitResult(
          submissionId: 1,
          submittedAt: DateTime.now(),
          safetyCheckCompleted: true,
        );
  }
}
