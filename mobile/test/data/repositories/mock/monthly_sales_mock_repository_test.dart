import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/monthly_sales.dart';
import 'package:mobile/data/repositories/mock/monthly_sales_mock_repository.dart';

void main() {
  group('MonthlySalesMockRepository', () {
    late MonthlySalesMockRepository repository;

    setUp(() {
      repository = MonthlySalesMockRepository();
    });

    group('getMonthlySales', () {
      test('월매출 데이터를 반환한다', () async {
        final result = await repository.getMonthlySales(yearMonth: '202601');

        expect(result.yearMonth, '202601');
        expect(result.customerId, 'ALL');
      });

      test('거래처별 월매출 데이터를 반환한다', () async {
        final result = await repository.getMonthlySales(
          customerId: 'C001',
          yearMonth: '202601',
        );

        expect(result.customerId, 'C001');
        expect(result.yearMonth, '202601');
        expect(result.customerName, '이마트 부산점');
      });

      test('제품유형별 매출 데이터를 포함한다', () async {
        final result = await repository.getMonthlySales(yearMonth: '202601');

        expect(result.categorySales, isNotEmpty);
        expect(
          result.categorySales.any((cat) => cat.category == '상온'),
          true,
        );
        expect(
          result.categorySales.any((cat) => cat.category == '냉장/냉동'),
          true,
        );
      });

      test('월 평균 실적 데이터를 포함한다', () async {
        final result = await repository.getMonthlySales(yearMonth: '202601');

        expect(result.monthlyAverage, isNotNull);
        expect(result.monthlyAverage.currentYearAverage, greaterThan(0));
        expect(result.monthlyAverage.startMonth, 1);
      });

      test('전년 동월 매출 데이터를 포함한다', () async {
        final result = await repository.getMonthlySales(yearMonth: '202601');

        expect(result.previousYearSameMonth, greaterThan(0));
      });

      test('customerId가 null이면 전체 거래처 합산 데이터를 반환한다', () async {
        final result = await repository.getMonthlySales(
          customerId: null,
          yearMonth: '202601',
        );

        expect(result.customerId, 'ALL');
        expect(result.customerName, '전체');
      });

      test('customerId가 빈 문자열이면 전체 거래처 합산 데이터를 반환한다', () async {
        final result = await repository.getMonthlySales(
          customerId: '',
          yearMonth: '202601',
        );

        expect(result.customerId, 'ALL');
      });

      test('존재하지 않는 데이터는 Exception을 throw한다', () async {
        expect(
          () => repository.getMonthlySales(yearMonth: '999999'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('MONTHLY_SALES_NOT_FOUND'),
            ),
          ),
        );
      });

      test('다른 월의 데이터를 조회할 수 있다', () async {
        final result202601 = await repository.getMonthlySales(yearMonth: '202601');
        final result202602 = await repository.getMonthlySales(yearMonth: '202602');

        expect(result202601.yearMonth, '202601');
        expect(result202602.yearMonth, '202602');
        expect(result202601.targetAmount, isNot(result202602.targetAmount));
      });

      test('커스텀 데이터를 사용할 수 있다', () async {
        const customMonthlySales = MonthlySales(
          customerId: 'TEST001',
          customerName: '테스트 거래처',
          yearMonth: '202603',
          targetAmount: 5000000,
          achievedAmount: 4000000,
          achievementRate: 80.0,
          categorySales: [
            CategorySales(
              category: '상온',
              targetAmount: 3000000,
              achievedAmount: 2500000,
              achievementRate: 83.3,
            ),
          ],
          previousYearSameMonth: 3500000,
          monthlyAverage: MonthlyAverage(
            currentYearAverage: 4500000,
            previousYearAverage: 4000000,
            startMonth: 1,
            endMonth: 3,
          ),
        );

        repository.customMonthlySales = {
          'TEST001-202603': customMonthlySales,
        };

        final result = await repository.getMonthlySales(
          customerId: 'TEST001',
          yearMonth: '202603',
        );

        expect(result, customMonthlySales);
      });

      test('Exception을 throw할 수 있다', () async {
        repository.exceptionToThrow = Exception('네트워크 오류');

        expect(
          () => repository.getMonthlySales(yearMonth: '202601'),
          throwsA(isA<Exception>()),
        );
      });
    });
  });
}
