import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/event_sales_info.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  const testSalesInfo = EventSalesInfo(
    eventId: 'EVT001',
    targetAmount: 10000000,
    achievedAmount: 8000000,
    achievementRate: 80.0,
    progressRate: 50.0,
  );

  group('EventSalesInfo Entity 생성 테스트', () {
    test('EventSalesInfo 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testSalesInfo.eventId, 'EVT001');
      expect(testSalesInfo.targetAmount, 10000000);
      expect(testSalesInfo.achievedAmount, 8000000);
      expect(testSalesInfo.achievementRate, 80.0);
      expect(testSalesInfo.progressRate, 50.0);
    });

    test('달성율 0% 케이스', () {
      const salesInfo = EventSalesInfo(
        eventId: 'EVT002',
        targetAmount: 10000000,
        achievedAmount: 0,
        achievementRate: 0.0,
        progressRate: 10.0,
      );

      expect(salesInfo.achievedAmount, 0);
      expect(salesInfo.achievementRate, 0.0);
    });

    test('달성율 100% 초과 케이스', () {
      const salesInfo = EventSalesInfo(
        eventId: 'EVT003',
        targetAmount: 10000000,
        achievedAmount: 12000000,
        achievementRate: 120.0,
        progressRate: 100.0,
      );

      expect(salesInfo.achievedAmount, 12000000);
      expect(salesInfo.achievementRate, 120.0);
    });
  });

  group('EventSalesInfo copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testSalesInfo.copyWith(
        achievedAmount: 9000000,
        achievementRate: 90.0,
      );

      expect(updated.eventId, testSalesInfo.eventId);
      expect(updated.targetAmount, testSalesInfo.targetAmount);
      expect(updated.achievedAmount, 9000000);
      expect(updated.achievementRate, 90.0);
      expect(updated.progressRate, testSalesInfo.progressRate);
    });

    test('모든 필드 변경', () {
      final updated = testSalesInfo.copyWith(
        eventId: 'EVT999',
        targetAmount: 20000000,
        achievedAmount: 15000000,
        achievementRate: 75.0,
        progressRate: 60.0,
      );

      expect(updated.eventId, 'EVT999');
      expect(updated.targetAmount, 20000000);
      expect(updated.achievedAmount, 15000000);
      expect(updated.achievementRate, 75.0);
      expect(updated.progressRate, 60.0);
    });
  });

  group('EventSalesInfo toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testSalesInfo.toJson();

      expect(json['eventId'], 'EVT001');
      expect(json['targetAmount'], 10000000);
      expect(json['achievedAmount'], 8000000);
      expect(json['achievementRate'], 80.0);
      expect(json['progressRate'], 50.0);
    });

    test('fromJson 역직렬화', () {
      final json = {
        'eventId': 'EVT001',
        'targetAmount': 10000000,
        'achievedAmount': 8000000,
        'achievementRate': 80.0,
        'progressRate': 50.0,
      };

      final salesInfo = EventSalesInfo.fromJson(json);

      expect(salesInfo.eventId, 'EVT001');
      expect(salesInfo.targetAmount, 10000000);
      expect(salesInfo.achievedAmount, 8000000);
      expect(salesInfo.achievementRate, 80.0);
      expect(salesInfo.progressRate, 50.0);
    });

    test('fromJson int to double 변환', () {
      final json = {
        'eventId': 'EVT001',
        'targetAmount': 10000000,
        'achievedAmount': 8000000,
        'achievementRate': 80,
        'progressRate': 50,
      };

      final salesInfo = EventSalesInfo.fromJson(json);

      expect(salesInfo.achievementRate, 80.0);
      expect(salesInfo.progressRate, 50.0);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testSalesInfo.toJson();
      final salesInfo = EventSalesInfo.fromJson(json);

      expect(salesInfo, testSalesInfo);
    });
  });

  group('EventSalesInfo equality 테스트', () {
    test('같은 값을 가진 EventSalesInfo는 같은 객체', () {
      const salesInfo1 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      const salesInfo2 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      expect(salesInfo1, salesInfo2);
    });

    test('다른 값을 가진 EventSalesInfo는 다른 객체', () {
      const salesInfo1 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      const salesInfo2 = EventSalesInfo(
        eventId: 'EVT002',
        targetAmount: 20000000,
        achievedAmount: 15000000,
        achievementRate: 75.0,
        progressRate: 60.0,
      );

      expect(salesInfo1, isNot(salesInfo2));
    });

    test('달성율만 다른 EventSalesInfo는 다른 객체', () {
      const salesInfo1 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      const salesInfo2 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 85.0,
        progressRate: 50.0,
      );

      expect(salesInfo1, isNot(salesInfo2));
    });
  });

  group('EventSalesInfo hashCode 테스트', () {
    test('같은 값을 가진 EventSalesInfo는 같은 hashCode', () {
      const salesInfo1 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      const salesInfo2 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      expect(salesInfo1.hashCode, salesInfo2.hashCode);
    });

    test('다른 값을 가진 EventSalesInfo는 다른 hashCode', () {
      const salesInfo1 = EventSalesInfo(
        eventId: 'EVT001',
        targetAmount: 10000000,
        achievedAmount: 8000000,
        achievementRate: 80.0,
        progressRate: 50.0,
      );

      const salesInfo2 = EventSalesInfo(
        eventId: 'EVT002',
        targetAmount: 20000000,
        achievedAmount: 15000000,
        achievementRate: 75.0,
        progressRate: 60.0,
      );

      expect(salesInfo1.hashCode, isNot(salesInfo2.hashCode));
    });
  });

  group('EventSalesInfo toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testSalesInfo.toString();

      expect(result, contains('EventSalesInfo'));
      expect(result, contains('eventId: EVT001'));
      expect(result, contains('targetAmount: 10000000'));
      expect(result, contains('achievedAmount: 8000000'));
      expect(result, contains('achievementRate: 80.0'));
      expect(result, contains('progressRate: 50.0'));
    });
  });
}
