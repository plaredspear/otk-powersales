import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/target.dart';
import 'package:mobile/domain/entities/progress.dart';
import 'package:mobile/domain/usecases/calculate_progress.dart';

void main() {
  late CalculateProgress useCase;

  setUp(() {
    useCase = CalculateProgress();
  });

  group('CalculateProgress UseCase', () {
    group('call() 메서드', () {
      test('목표와 실적으로 진도율을 계산한다', () {
        // Act
        final result = useCase(target: 1000, actual: 1200);

        // Assert
        expect(result, isA<Progress>());
        expect(result.percentage, equals(120));
        expect(result.status, equals(ProgressStatus.exceeded));
      });

      test('진도율이 100% 미만인 경우를 계산한다', () {
        // Act
        final result = useCase(target: 1000, actual: 800);

        // Assert
        expect(result.percentage, equals(80));
        expect(result.status, equals(ProgressStatus.insufficient));
      });

      test('진도율이 정확히 100%인 경우를 계산한다', () {
        // Act
        final result = useCase(target: 1000, actual: 1000);

        // Assert
        expect(result.percentage, equals(100));
        expect(result.status, equals(ProgressStatus.achieved));
      });

      test('목표가 0인 경우를 처리한다', () {
        // Act
        final result = useCase(target: 0, actual: 500);

        // Assert
        expect(result.percentage, equals(0));
        expect(result.status, equals(ProgressStatus.insufficient));
      });

      test('실적이 0인 경우를 처리한다', () {
        // Act
        final result = useCase(target: 1000, actual: 0);

        // Assert
        expect(result.percentage, equals(0));
        expect(result.status, equals(ProgressStatus.insufficient));
      });
    });

    group('fromTarget() 메서드', () {
      test('Target 엔티티로부터 진도율을 계산한다', () {
        // Arrange
        final target = Target(
          id: '1',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202602',
          category: '전산매출',
          targetAmount: 1000,
          actualAmount: 1200,
          createdAt: DateTime(2026, 2, 1),
          updatedAt: DateTime(2026, 2, 1),
        );

        // Act
        final result = useCase.fromTarget(target);

        // Assert
        expect(result.percentage, equals(120));
        expect(result.status, equals(ProgressStatus.exceeded));
      });

      test('실적이 부족한 Target의 진도율을 계산한다', () {
        // Arrange
        final target = Target(
          id: '2',
          customerName: '홈플러스',
          customerCode: 'CUST002',
          yearMonth: '202602',
          category: 'POS매출',
          targetAmount: 1000,
          actualAmount: 750,
          createdAt: DateTime(2026, 2, 1),
          updatedAt: DateTime(2026, 2, 1),
        );

        // Act
        final result = useCase.fromTarget(target);

        // Assert
        expect(result.percentage, equals(75));
        expect(result.status, equals(ProgressStatus.insufficient));
      });
    });

    group('calculateAverage() 메서드', () {
      test('여러 Target의 평균 진도율을 계산한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1200,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 2000,
            actualAmount: 1600,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.calculateAverage(targets);

        // Assert
        // 총 목표: 3000, 총 실적: 2800, 진도율: 93.3%
        expect(result.percentage, closeTo(93.3, 0.1));
        expect(result.status, equals(ProgressStatus.insufficient));
      });

      test('빈 Target 리스트에 대해 0% 진도율을 반환한다', () {
        // Act
        final result = useCase.calculateAverage([]);

        // Assert
        expect(result.percentage, equals(0));
        expect(result.status, equals(ProgressStatus.insufficient));
      });

      test('단일 Target의 평균 진도율을 계산한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1100,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.calculateAverage(targets);

        // Assert
        expect(result.percentage, closeTo(110, 0.01));
        expect(result.status, equals(ProgressStatus.exceeded));
      });
    });

    group('calculateByCategory() 메서드', () {
      test('카테고리별 진도율을 계산한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1200,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 800,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '3',
            customerName: '롯데마트',
            customerCode: 'CUST003',
            yearMonth: '202602',
            category: 'POS매출',
            targetAmount: 1000,
            actualAmount: 1100,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.calculateByCategory(targets);

        // Assert
        expect(result, containsPair('전산매출', isA<Progress>()));
        expect(result, containsPair('POS매출', isA<Progress>()));
        expect(result['전산매출']!.percentage, closeTo(100, 0.01)); // (1200 + 800) / (1000 + 1000)
        expect(result['POS매출']!.percentage, closeTo(110, 0.01)); // 1100 / 1000
      });

      test('빈 Target 리스트에 대해 빈 Map을 반환한다', () {
        // Act
        final result = useCase.calculateByCategory([]);

        // Assert
        expect(result, isEmpty);
      });
    });

    group('calculateByCustomer() 메서드', () {
      test('거래처별 진도율을 계산한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1200,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: 'POS매출',
            targetAmount: 1000,
            actualAmount: 800,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '3',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1100,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.calculateByCustomer(targets);

        // Assert
        expect(result, containsPair('이마트', isA<Progress>()));
        expect(result, containsPair('홈플러스', isA<Progress>()));
        expect(result['이마트']!.percentage, closeTo(100, 0.01)); // (1200 + 800) / (1000 + 1000)
        expect(result['홈플러스']!.percentage, closeTo(110, 0.01)); // 1100 / 1000
      });

      test('빈 Target 리스트에 대해 빈 Map을 반환한다', () {
        // Act
        final result = useCase.calculateByCustomer([]);

        // Assert
        expect(result, isEmpty);
      });
    });

    group('filterUnderPerforming() 메서드', () {
      test('진도율이 부족한 Target만 필터링한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1200, // 초과
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 800, // 부족
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '3',
            customerName: '롯데마트',
            customerCode: 'CUST003',
            yearMonth: '202602',
            category: 'POS매출',
            targetAmount: 1000,
            actualAmount: 1000, // 달성
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.filterUnderPerforming(targets);

        // Assert
        expect(result, hasLength(1));
        expect(result.first.customerName, equals('홈플러스'));
      });

      test('모든 Target이 목표를 달성한 경우 빈 리스트를 반환한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1200,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1100,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.filterUnderPerforming(targets);

        // Assert
        expect(result, isEmpty);
      });
    });

    group('filterOverPerforming() 메서드', () {
      test('진도율이 초과한 Target만 필터링한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 1200, // 초과
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 800, // 부족
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '3',
            customerName: '롯데마트',
            customerCode: 'CUST003',
            yearMonth: '202602',
            category: 'POS매출',
            targetAmount: 1000,
            actualAmount: 1000, // 달성
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.filterOverPerforming(targets);

        // Assert
        expect(result, hasLength(1));
        expect(result.first.customerName, equals('이마트'));
      });

      test('모든 Target이 목표에 미달한 경우 빈 리스트를 반환한다', () {
        // Arrange
        final targets = [
          Target(
            id: '1',
            customerName: '이마트',
            customerCode: 'CUST001',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 800,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
          Target(
            id: '2',
            customerName: '홈플러스',
            customerCode: 'CUST002',
            yearMonth: '202602',
            category: '전산매출',
            targetAmount: 1000,
            actualAmount: 900,
            createdAt: DateTime(2026, 2, 1),
            updatedAt: DateTime(2026, 2, 1),
          ),
        ];

        // Act
        final result = useCase.filterOverPerforming(targets);

        // Assert
        expect(result, isEmpty);
      });
    });
  });
}
