import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/target.dart';

void main() {
  group('Target Entity', () {
    // 테스트용 기본 데이터
    final testDateTime = DateTime(2026, 1, 31, 10, 0, 0);
    final testTarget = Target(
      id: 'target-001',
      customerName: '이마트',
      customerCode: 'CUST001',
      yearMonth: '202601',
      targetAmount: 100000000,
      actualAmount: 120000000,
      category: '상온',
      note: '1월 목표',
      createdAt: testDateTime,
      updatedAt: testDateTime,
    );

    group('생성 테스트', () {
      test('Target 엔티티가 올바르게 생성된다', () {
        expect(testTarget.id, 'target-001');
        expect(testTarget.customerName, '이마트');
        expect(testTarget.customerCode, 'CUST001');
        expect(testTarget.yearMonth, '202601');
        expect(testTarget.targetAmount, 100000000);
        expect(testTarget.actualAmount, 120000000);
        expect(testTarget.category, '상온');
        expect(testTarget.note, '1월 목표');
        expect(testTarget.createdAt, testDateTime);
        expect(testTarget.updatedAt, testDateTime);
      });

      test('선택적 필드(category, note)가 null일 수 있다', () {
        final targetWithoutOptional = Target(
          id: 'target-002',
          customerName: '홈플러스',
          customerCode: 'CUST002',
          yearMonth: '202601',
          targetAmount: 50000000,
          actualAmount: 45000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(targetWithoutOptional.category, isNull);
        expect(targetWithoutOptional.note, isNull);
      });

      test('목표 미달 케이스가 생성된다', () {
        final underTarget = Target(
          id: 'target-003',
          customerName: '롯데마트',
          customerCode: 'CUST003',
          yearMonth: '202601',
          targetAmount: 80000000,
          actualAmount: 60000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(underTarget.actualAmount < underTarget.targetAmount, true);
      });

      test('목표 초과 케이스가 생성된다', () {
        final overTarget = Target(
          id: 'target-004',
          customerName: '농협',
          customerCode: 'CUST004',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 150000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(overTarget.actualAmount > overTarget.targetAmount, true);
      });

      test('목표금액이 0인 경우도 생성된다', () {
        final zeroTarget = Target(
          id: 'target-005',
          customerName: '신규 거래처',
          customerCode: 'CUST005',
          yearMonth: '202601',
          targetAmount: 0,
          actualAmount: 0,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(zeroTarget.targetAmount, 0);
        expect(zeroTarget.actualAmount, 0);
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final updatedDateTime = DateTime(2026, 1, 31, 15, 0, 0);
        final copied = testTarget.copyWith(
          id: 'target-999',
          customerName: 'GS25',
          customerCode: 'CUST999',
          yearMonth: '202602',
          targetAmount: 200000000,
          actualAmount: 250000000,
          category: '라면',
          note: '2월 목표',
          createdAt: updatedDateTime,
          updatedAt: updatedDateTime,
        );

        expect(copied.id, 'target-999');
        expect(copied.customerName, 'GS25');
        expect(copied.customerCode, 'CUST999');
        expect(copied.yearMonth, '202602');
        expect(copied.targetAmount, 200000000);
        expect(copied.actualAmount, 250000000);
        expect(copied.category, '라면');
        expect(copied.note, '2월 목표');
        expect(copied.createdAt, updatedDateTime);
        expect(copied.updatedAt, updatedDateTime);
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final copied = testTarget.copyWith(
          actualAmount: 130000000,
          note: '목표 수정',
        );

        expect(copied.id, testTarget.id);
        expect(copied.customerName, testTarget.customerName);
        expect(copied.customerCode, testTarget.customerCode);
        expect(copied.yearMonth, testTarget.yearMonth);
        expect(copied.targetAmount, testTarget.targetAmount);
        expect(copied.actualAmount, 130000000);
        expect(copied.category, testTarget.category);
        expect(copied.note, '목표 수정');
        expect(copied.createdAt, testTarget.createdAt);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = testTarget;
        final copied = testTarget.copyWith(actualAmount: 999999999);

        expect(original.actualAmount, 120000000);
        expect(copied.actualAmount, 999999999);
      });

      test('copyWith로 실적금액을 업데이트할 수 있다', () {
        final copied = testTarget.copyWith(
          actualAmount: 110000000,
          updatedAt: DateTime(2026, 1, 31, 12, 0, 0),
        );

        expect(copied.actualAmount, 110000000);
        expect(copied.updatedAt.isAfter(testTarget.updatedAt), true);
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final json = testTarget.toJson();

        expect(json['id'], 'target-001');
        expect(json['customerName'], '이마트');
        expect(json['customerCode'], 'CUST001');
        expect(json['yearMonth'], '202601');
        expect(json['targetAmount'], 100000000);
        expect(json['actualAmount'], 120000000);
        expect(json['category'], '상온');
        expect(json['note'], '1월 목표');
        expect(json['createdAt'], testDateTime.toIso8601String());
        expect(json['updatedAt'], testDateTime.toIso8601String());
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'id': 'target-002',
          'customerName': '홈플러스',
          'customerCode': 'CUST002',
          'yearMonth': '202602',
          'targetAmount': 80000000,
          'actualAmount': 90000000,
          'category': '냉동/냉장',
          'note': '2월 목표',
          'createdAt': '2026-02-01T10:00:00.000',
          'updatedAt': '2026-02-01T15:00:00.000',
        };

        final target = Target.fromJson(json);

        expect(target.id, 'target-002');
        expect(target.customerName, '홈플러스');
        expect(target.customerCode, 'CUST002');
        expect(target.yearMonth, '202602');
        expect(target.targetAmount, 80000000);
        expect(target.actualAmount, 90000000);
        expect(target.category, '냉동/냉장');
        expect(target.note, '2월 목표');
        expect(target.createdAt, DateTime.parse('2026-02-01T10:00:00.000'));
        expect(target.updatedAt, DateTime.parse('2026-02-01T15:00:00.000'));
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final json = testTarget.toJson();
        final restored = Target.fromJson(json);

        expect(restored, testTarget);
      });

      test('fromJson이 선택적 필드가 null인 경우를 처리한다', () {
        final json = {
          'id': 'target-003',
          'customerName': '롯데마트',
          'customerCode': 'CUST003',
          'yearMonth': '202601',
          'targetAmount': 70000000,
          'actualAmount': 75000000,
          'category': null,
          'note': null,
          'createdAt': '2026-01-31T10:00:00.000',
          'updatedAt': '2026-01-31T10:00:00.000',
        };

        final target = Target.fromJson(json);

        expect(target.category, isNull);
        expect(target.note, isNull);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final target1 = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 120000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        final target2 = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 120000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(target1, target2);
        expect(target1.hashCode, target2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final target1 = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 120000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        final target2 = Target(
          id: 'target-002',
          customerName: '홈플러스',
          customerCode: 'CUST002',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 120000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(target1, isNot(target2));
      });

      test('ID가 다르면 다르게 비교된다', () {
        final target1 = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 120000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        final target2 = Target(
          id: 'target-999',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 120000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(target1, isNot(target2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        expect(testTarget, testTarget);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final str = testTarget.toString();

        expect(str, contains('target-001'));
        expect(str, contains('이마트'));
        expect(str, contains('CUST001'));
        expect(str, contains('202601'));
        expect(str, contains('100000000'));
        expect(str, contains('120000000'));
        expect(str, contains('상온'));
        expect(str, contains('1월 목표'));
      });
    });

    group('Validation 테스트', () {
      test('실적이 목표보다 클 수 있다', () {
        final target = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 150000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(target.actualAmount > target.targetAmount, true);
      });

      test('실적이 목표보다 작을 수 있다', () {
        final target = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 80000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(target.actualAmount < target.targetAmount, true);
      });

      test('실적과 목표가 같을 수 있다', () {
        final target = Target(
          id: 'target-001',
          customerName: '이마트',
          customerCode: 'CUST001',
          yearMonth: '202601',
          targetAmount: 100000000,
          actualAmount: 100000000,
          createdAt: testDateTime,
          updatedAt: testDateTime,
        );

        expect(target.actualAmount, target.targetAmount);
      });
    });
  });
}
