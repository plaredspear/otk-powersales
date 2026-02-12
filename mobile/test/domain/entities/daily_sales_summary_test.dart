import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/daily_sales_summary.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  final testSummary = DailySalesSummary(
    dailySalesId: 'DS001',
    salesDate: DateTime(2026, 2, 12),
    totalAmount: 5000000,
    status: 'REGISTERED',
  );

  group('DailySalesSummary Entity 생성 테스트', () {
    test('DailySalesSummary 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testSummary.dailySalesId, 'DS001');
      expect(testSummary.salesDate, DateTime(2026, 2, 12));
      expect(testSummary.totalAmount, 5000000);
      expect(testSummary.status, 'REGISTERED');
    });

    test('DRAFT 상태 생성', () {
      final summary = DailySalesSummary(
        dailySalesId: 'DS002',
        salesDate: DateTime(2026, 2, 11),
        totalAmount: 3000000,
        status: 'DRAFT',
      );

      expect(summary.status, 'DRAFT');
    });

    test('금액 0원 케이스', () {
      final summary = DailySalesSummary(
        dailySalesId: 'DS003',
        salesDate: DateTime(2026, 2, 10),
        totalAmount: 0,
        status: 'REGISTERED',
      );

      expect(summary.totalAmount, 0);
    });
  });

  group('DailySalesSummary copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testSummary.copyWith(
        totalAmount: 6000000,
        status: 'DRAFT',
      );

      expect(updated.dailySalesId, testSummary.dailySalesId);
      expect(updated.salesDate, testSummary.salesDate);
      expect(updated.totalAmount, 6000000);
      expect(updated.status, 'DRAFT');
    });

    test('날짜 필드 변경', () {
      final newDate = DateTime(2026, 2, 13);
      final updated = testSummary.copyWith(salesDate: newDate);

      expect(updated.salesDate, newDate);
      expect(updated.dailySalesId, testSummary.dailySalesId);
      expect(updated.totalAmount, testSummary.totalAmount);
      expect(updated.status, testSummary.status);
    });

    test('모든 필드 변경', () {
      final updated = testSummary.copyWith(
        dailySalesId: 'DS999',
        salesDate: DateTime(2026, 3, 1),
        totalAmount: 8000000,
        status: 'DRAFT',
      );

      expect(updated.dailySalesId, 'DS999');
      expect(updated.salesDate, DateTime(2026, 3, 1));
      expect(updated.totalAmount, 8000000);
      expect(updated.status, 'DRAFT');
    });
  });

  group('DailySalesSummary toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testSummary.toJson();

      expect(json['dailySalesId'], 'DS001');
      expect(json['salesDate'], '2026-02-12T00:00:00.000');
      expect(json['totalAmount'], 5000000);
      expect(json['status'], 'REGISTERED');
    });

    test('fromJson 역직렬화', () {
      final json = {
        'dailySalesId': 'DS001',
        'salesDate': '2026-02-12T00:00:00.000',
        'totalAmount': 5000000,
        'status': 'REGISTERED',
      };

      final summary = DailySalesSummary.fromJson(json);

      expect(summary.dailySalesId, 'DS001');
      expect(summary.salesDate, DateTime(2026, 2, 12));
      expect(summary.totalAmount, 5000000);
      expect(summary.status, 'REGISTERED');
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testSummary.toJson();
      final summary = DailySalesSummary.fromJson(json);

      expect(summary, testSummary);
    });
  });

  group('DailySalesSummary equality 테스트', () {
    test('같은 값을 가진 DailySalesSummary는 같은 객체', () {
      final summary1 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      final summary2 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      expect(summary1, summary2);
    });

    test('다른 값을 가진 DailySalesSummary는 다른 객체', () {
      final summary1 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      final summary2 = DailySalesSummary(
        dailySalesId: 'DS002',
        salesDate: DateTime(2026, 2, 13),
        totalAmount: 6000000,
        status: 'DRAFT',
      );

      expect(summary1, isNot(summary2));
    });

    test('상태만 다른 DailySalesSummary는 다른 객체', () {
      final summary1 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      final summary2 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'DRAFT',
      );

      expect(summary1, isNot(summary2));
    });
  });

  group('DailySalesSummary hashCode 테스트', () {
    test('같은 값을 가진 DailySalesSummary는 같은 hashCode', () {
      final summary1 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      final summary2 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      expect(summary1.hashCode, summary2.hashCode);
    });

    test('다른 값을 가진 DailySalesSummary는 다른 hashCode', () {
      final summary1 = DailySalesSummary(
        dailySalesId: 'DS001',
        salesDate: DateTime(2026, 2, 12),
        totalAmount: 5000000,
        status: 'REGISTERED',
      );

      final summary2 = DailySalesSummary(
        dailySalesId: 'DS002',
        salesDate: DateTime(2026, 2, 13),
        totalAmount: 6000000,
        status: 'DRAFT',
      );

      expect(summary1.hashCode, isNot(summary2.hashCode));
    });
  });

  group('DailySalesSummary toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testSummary.toString();

      expect(result, contains('DailySalesSummary'));
      expect(result, contains('dailySalesId: DS001'));
      expect(result, contains('totalAmount: 5000000'));
      expect(result, contains('status: REGISTERED'));
    });
  });
}
