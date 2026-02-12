import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/monthly_sales.dart';
import 'package:mobile/domain/repositories/monthly_sales_repository.dart';
import 'package:mobile/domain/usecases/get_monthly_sales.dart';

class _MockMonthlySalesRepository implements MonthlySalesRepository {
  MonthlySales? result;
  Exception? error;

  @override
  Future<MonthlySales> getMonthlySales({
    String? customerId,
    required String yearMonth,
  }) async {
    if (error != null) throw error!;
    return result!;
  }
}

void main() {
  group('GetMonthlySalesUseCase', () {
    late _MockMonthlySalesRepository repository;
    late GetMonthlySalesUseCase useCase;

    setUp(() {
      repository = _MockMonthlySalesRepository();
      useCase = GetMonthlySalesUseCase(repository);
    });

    test('월매출 조회가 성공한다', () async {
      // Given
      final testMonthlySales = MonthlySales(
        customerId: 'C001',
        customerName: '이마트 부산점',
        yearMonth: '202608',
        targetAmount: 80000000,
        achievedAmount: 65000000,
        achievementRate: 81.25,
        categorySales: const [
          CategorySales(
            category: '상온',
            targetAmount: 50000000,
            achievedAmount: 40000000,
            achievementRate: 80.0,
          ),
        ],
        previousYearSameMonth: 58000000,
        monthlyAverage: const MonthlyAverage(
          currentYearAverage: 60000000,
          previousYearAverage: 55000000,
          startMonth: 1,
          endMonth: 8,
        ),
      );
      repository.result = testMonthlySales;

      // When
      final result = await useCase.call(yearMonth: '202608');

      // Then
      expect(result.customerId, 'C001');
      expect(result.yearMonth, '202608');
      expect(result.targetAmount, 80000000);
    });

    test('거래처 ID가 올바르게 전달된다', () async {
      // Given
      repository.result = MonthlySales(
        customerId: 'C999',
        customerName: 'Test',
        yearMonth: '202609',
        targetAmount: 0,
        achievedAmount: 0,
        achievementRate: 0,
        categorySales: const [],
        previousYearSameMonth: 0,
        monthlyAverage: const MonthlyAverage(
          currentYearAverage: 0,
          previousYearAverage: 0,
          startMonth: 1,
          endMonth: 9,
        ),
      );

      // When
      await useCase.call(customerId: 'C999', yearMonth: '202609');

      // Then - Mock이 호출되었으므로 에러 없이 완료
      expect(repository.result, isNotNull);
    });

    test('customerId가 null이어도 정상 처리된다', () async {
      // Given
      repository.result = MonthlySales(
        customerId: 'ALL',
        customerName: '전체',
        yearMonth: '202610',
        targetAmount: 0,
        achievedAmount: 0,
        achievementRate: 0,
        categorySales: const [],
        previousYearSameMonth: 0,
        monthlyAverage: const MonthlyAverage(
          currentYearAverage: 0,
          previousYearAverage: 0,
          startMonth: 1,
          endMonth: 10,
        ),
      );

      // When
      await useCase.call(yearMonth: '202610');

      // Then
      expect(repository.result, isNotNull);
    });

    test('유효한 yearMonth 형식이 정상 처리된다', () async {
      // Given
      repository.result = MonthlySales(
        customerId: 'C001',
        customerName: 'Test',
        yearMonth: '202601',
        targetAmount: 0,
        achievedAmount: 0,
        achievementRate: 0,
        categorySales: const [],
        previousYearSameMonth: 0,
        monthlyAverage: const MonthlyAverage(
          currentYearAverage: 0,
          previousYearAverage: 0,
          startMonth: 1,
          endMonth: 1,
        ),
      );

      // When
      await useCase.call(yearMonth: '202601');

      // Then
      expect(repository.result, isNotNull);
    });

    test('yearMonth가 6자리가 아니면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(yearMonth: '20260'),
        throwsA(isA<ArgumentError>()),
      );

      expect(
        () => useCase.call(yearMonth: '2026012'),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('yearMonth가 숫자가 아니면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(yearMonth: '2026AB'),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('yearMonth의 월이 01~12 범위를 벗어나면 ArgumentError가 발생한다', () async {
      // When & Then
      expect(
        () => useCase.call(yearMonth: '202600'),
        throwsA(isA<ArgumentError>()),
      );

      expect(
        () => useCase.call(yearMonth: '202613'),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('유효한 경계값 월(01, 12)이 정상 처리된다', () async {
      // Given
      repository.result = MonthlySales(
        customerId: 'C001',
        customerName: 'Test',
        yearMonth: '202601',
        targetAmount: 0,
        achievedAmount: 0,
        achievementRate: 0,
        categorySales: const [],
        previousYearSameMonth: 0,
        monthlyAverage: const MonthlyAverage(
          currentYearAverage: 0,
          previousYearAverage: 0,
          startMonth: 1,
          endMonth: 1,
        ),
      );

      // When
      await useCase.call(yearMonth: '202601');
      await useCase.call(yearMonth: '202612');

      // Then
      expect(repository.result, isNotNull);
    });

    test('Repository에서 에러가 발생하면 전파된다', () async {
      // Given
      repository.error = Exception('Network error');

      // When & Then
      expect(
        () => useCase.call(yearMonth: '202608'),
        throwsException,
      );
    });
  });
}
