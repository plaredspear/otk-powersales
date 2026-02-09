import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/submit_safety_check.dart';
import 'package:mobile/domain/repositories/safety_check_repository.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';

void main() {
  group('SubmitSafetyCheck', () {
    late SubmitSafetyCheck useCase;
    late _TestSafetyCheckRepository repository;

    setUp(() {
      repository = _TestSafetyCheckRepository();
      useCase = SubmitSafetyCheck(repository);
    });

    test('Returns submit result on success', () async {
      // Arrange
      final submittedTime = DateTime(2025, 2, 8, 9, 30);
      final expectedResult = SafetyCheckSubmitResult(
        submissionId: 123,
        submittedAt: submittedTime,
        safetyCheckCompleted: true,
      );
      repository.submitResultToReturn = expectedResult;

      // Act
      final result = await useCase.call(checkedItemIds: [1, 2, 3]);

      // Assert
      expect(result, expectedResult);
      expect(result.submissionId, 123);
      expect(result.submittedAt, submittedTime);
      expect(result.safetyCheckCompleted, true);
    });

    test('Throws ArgumentError when checkedItemIds is empty', () async {
      // Act & Assert
      expect(
        () => useCase.call(checkedItemIds: []),
        throwsA(isA<ArgumentError>().having(
          (e) => e.message,
          'message',
          '체크된 항목이 없습니다. 모든 필수 항목을 체크해주세요.',
        )),
      );
    });

    test('Passes correct checkedItemIds to repository', () async {
      // Arrange
      final checkedIds = [1, 2, 3, 4, 5];

      // Act
      await useCase.call(checkedItemIds: checkedIds);

      // Assert
      expect(repository.lastSubmittedIds, checkedIds);
    });

    test('Propagates repository exception', () async {
      // Arrange
      final expectedException = Exception('이미 안전점검을 완료하였습니다.');
      repository.exceptionToThrow = expectedException;

      // Act & Assert
      expect(
        () => useCase.call(checkedItemIds: [1, 2, 3]),
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
  List<int>? lastSubmittedIds;

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
    lastSubmittedIds = checkedItemIds;

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
