import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/target.dart';
import 'package:mobile/domain/entities/progress.dart';
import 'package:mobile/domain/usecases/get_targets.dart';
import 'package:mobile/data/repositories/mock/target_mock_repository.dart';

void main() {
  late GetTargets useCase;
  late TargetMockRepository repository;

  setUp(() {
    repository = TargetMockRepository();
    useCase = GetTargets(repository);
  });

  group('GetTargets UseCase', () {
    const testYearMonth = '202601';

    test('call() 메서드가 월별 모든 목표를 반환한다', () async {
      // Act
      final result = await useCase(testYearMonth);

      // Assert
      expect(result, isNotEmpty);
      expect(result, isA<List<Target>>());
      expect(result.every((t) => t.yearMonth == testYearMonth), isTrue);
    });

    test('getById() 메서드가 특정 목표를 반환한다', () async {
      // Arrange
      final targets = await repository.getTargets(yearMonth: testYearMonth);
      final targetId = targets.first.id;

      // Act
      final result = await useCase.getById(targetId);

      // Assert
      expect(result, isNotNull);
      expect(result.id, equals(targetId));
    });

    test('getByCategory() 메서드가 카테고리별로 목표를 필터링한다', () async {
      // Act
      final result = await useCase.getByCategory(
        yearMonth: testYearMonth,
        category: '전산매출',
      );

      // Assert
      expect(result, isNotEmpty);
      expect(
        result.every((t) => t.category == '전산매출'),
        isTrue,
      );
    });

    test('getByCustomer() 메서드가 거래처별로 목표를 조회한다', () async {
      // Arrange
      final targets = await repository.getTargets(yearMonth: testYearMonth);
      final customerCode = targets.first.customerCode;

      // Act
      final result = await useCase.getByCustomer(
        yearMonth: testYearMonth,
        customerCode: customerCode,
      );

      // Assert
      expect(result, isNotNull);
      expect(result!.customerCode, equals(customerCode));
    });

    test('getInsufficient() 메서드가 진도율 부족 목표만 반환한다', () async {
      // Act
      final result = await useCase.getInsufficient(yearMonth: testYearMonth);

      // Assert
      expect(result, isNotEmpty);
      for (final target in result) {
        final percentage = (target.actualAmount / target.targetAmount) * 100;
        expect(percentage, lessThan(100.0));
      }
    });

    test('getProgress() 메서드가 특정 목표의 진도율을 반환한다', () async {
      // Arrange
      final targets = await repository.getTargets(yearMonth: testYearMonth);
      final targetId = targets.first.id;

      // Act
      final result = await useCase.getProgress(targetId);

      // Assert
      expect(result, isA<Progress>());
    });

    test('getProgressList() 메서드가 월별 전체 진도율 목록을 반환한다', () async {
      // Act
      final result = await useCase.getProgressList(testYearMonth);

      // Assert
      expect(result, isNotEmpty);
      expect(result, isA<Map<String, Progress>>());
    });

    test('calculateTotalTargetAmount() 메서드가 총 목표 금액을 계산한다', () async {
      // Arrange
      final targets = await repository.getTargets(yearMonth: testYearMonth);
      final expectedTotal = targets.fold<int>(
        0,
        (sum, t) => sum + t.targetAmount,
      );

      // Act
      final result = await useCase.calculateTotalTargetAmount(testYearMonth);

      // Assert
      expect(result, equals(expectedTotal));
      expect(result, greaterThan(0));
    });

    test('calculateTotalActualAmount() 메서드가 총 실적 금액을 계산한다', () async {
      // Arrange
      final targets = await repository.getTargets(yearMonth: testYearMonth);
      final expectedTotal = targets.fold<int>(
        0,
        (sum, t) => sum + t.actualAmount,
      );

      // Act
      final result = await useCase.calculateTotalActualAmount(testYearMonth);

      // Assert
      expect(result, equals(expectedTotal));
      expect(result, greaterThan(0));
    });

    test('calculateOverallProgress() 메서드가 전체 진도율을 계산한다', () async {
      // Act
      final result = await useCase.calculateOverallProgress(testYearMonth);

      // Assert
      expect(result, isA<Progress>());
      expect(result.percentage, greaterThanOrEqualTo(0));
    });

    test('존재하지 않는 년월에 대해 빈 목표 목록을 반환한다', () async {
      // Act
      final result = await useCase('999912');

      // Assert
      expect(result, isEmpty);
    });

    test('빈 목표 목록에 대해 총 금액 계산이 0을 반환한다', () async {
      // Arrange
      const emptyYearMonth = '999912';

      // Act
      final targetTotal = await useCase.calculateTotalTargetAmount(emptyYearMonth);
      final actualTotal = await useCase.calculateTotalActualAmount(emptyYearMonth);

      // Assert
      expect(targetTotal, equals(0));
      expect(actualTotal, equals(0));
    });

    test('빈 목표 목록에 대해 전체 진도율이 0%를 반환한다', () async {
      // Arrange
      const emptyYearMonth = '999912';

      // Act
      final result = await useCase.calculateOverallProgress(emptyYearMonth);

      // Assert
      expect(result.percentage, equals(0));
      expect(result.status, equals(ProgressStatus.insufficient));
    });
  });
}
