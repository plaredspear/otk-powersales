import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/get_safety_check_today_status.dart';
import 'package:mobile/domain/repositories/safety_check_repository.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';

void main() {
  group('GetSafetyCheckTodayStatus', () {
    late GetSafetyCheckTodayStatus useCase;
    late _TestSafetyCheckRepository repository;

    setUp(() {
      repository = _TestSafetyCheckRepository();
      useCase = GetSafetyCheckTodayStatus(repository);
    });

    test('Returns status when not completed', () async {
      // Arrange
      const expectedStatus = SafetyCheckTodayStatus(
        completed: false,
        submittedAt: null,
      );
      repository.statusToReturn = expectedStatus;

      // Act
      final result = await useCase.call();

      // Assert
      expect(result, expectedStatus);
      expect(result.completed, false);
      expect(result.submittedAt, isNull);
    });

    test('Returns status when completed', () async {
      // Arrange
      final submittedTime = DateTime(2025, 2, 8, 9, 30);
      final expectedStatus = SafetyCheckTodayStatus(
        completed: true,
        submittedAt: submittedTime,
      );
      repository.statusToReturn = expectedStatus;

      // Act
      final result = await useCase.call();

      // Assert
      expect(result, expectedStatus);
      expect(result.completed, true);
      expect(result.submittedAt, submittedTime);
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
