import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/target_mock_repository.dart';
import 'package:mobile/domain/entities/target.dart';
import 'package:mobile/domain/entities/progress.dart';
import 'package:mobile/domain/repositories/target_repository.dart';

void main() {
  group('TargetMockRepository', () {
    late TargetRepository repository;

    setUp(() {
      repository = TargetMockRepository();
    });

    group('getTargets', () {
      test('년월로 목표를 조회한다', () async {
        final results = await repository.getTargets(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((target) => target.yearMonth == '202601'),
          true,
        );
      });

      test('거래처 코드로 목표를 필터링한다', () async {
        final results = await repository.getTargets(
          yearMonth: '202601',
          customerCode: 'CUST001',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((target) => target.customerCode == 'CUST001'),
          true,
        );
      });

      test('카테고리로 목표를 필터링한다', () async {
        final results = await repository.getTargets(
          yearMonth: '202601',
          category: '전산매출',
        );

        expect(results, isNotEmpty);
        expect(
          results.every((target) => target.category == '전산매출'),
          true,
        );
      });

      test('존재하지 않는 년월은 빈 리스트를 반환한다', () async {
        final results = await repository.getTargets(
          yearMonth: '202701',
        );

        expect(results, isEmpty);
      });

      test('결과가 목표금액순(내림차순)으로 정렬된다', () async {
        final results = await repository.getTargets(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);

        // 목표금액순 정렬 확인 (내림차순)
        for (var i = 0; i < results.length - 1; i++) {
          expect(
            results[i].targetAmount >= results[i + 1].targetAmount,
            true,
          );
        }
      });
    });

    group('getTargetById', () {
      test('ID로 특정 목표를 조회한다', () async {
        final result = await repository.getTargetById('T001');

        expect(result, isNotNull);
        expect(result.id, 'T001');
        expect(result.customerName, '농협');
      });

      test('존재하지 않는 ID는 Exception을 발생시킨다', () async {
        expect(
          () => repository.getTargetById('INVALID_ID'),
          throwsException,
        );
      });
    });

    group('getTargetByCustomer', () {
      test('년월 + 거래처 코드로 목표를 조회한다', () async {
        final result = await repository.getTargetByCustomer(
          yearMonth: '202601',
          customerCode: 'CUST001',
        );

        expect(result, isNotNull);
        expect(result!.yearMonth, '202601');
        expect(result.customerCode, 'CUST001');
      });

      test('존재하지 않는 거래처는 null을 반환한다', () async {
        final result = await repository.getTargetByCustomer(
          yearMonth: '202601',
          customerCode: 'INVALID_CODE',
        );

        expect(result, isNull);
      });

      test('존재하지 않는 년월은 null을 반환한다', () async {
        final result = await repository.getTargetByCustomer(
          yearMonth: '202701',
          customerCode: 'CUST001',
        );

        expect(result, isNull);
      });
    });

    group('saveTarget', () {
      test('새 목표를 생성한다', () async {
        final newTarget = Target(
          id: 'T_NEW',
          customerName: '신규 거래처',
          customerCode: 'CUST_NEW',
          yearMonth: '202603',
          targetAmount: 50000000,
          actualAmount: 0,
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        );

        final savedTarget = await repository.saveTarget(newTarget);

        expect(savedTarget, isNotNull);
        expect(savedTarget.id, 'T_NEW');
        expect(savedTarget.customerName, '신규 거래처');

        // 생성된 목표를 조회할 수 있는지 확인
        final retrieved = await repository.getTargetById('T_NEW');
        expect(retrieved.id, 'T_NEW');
      });

      test('기존 목표를 수정한다', () async {
        // 기존 목표 조회
        final existing = await repository.getTargetById('T001');

        // 실적금액 변경
        final updated = existing.copyWith(
          actualAmount: 120000000,
        );

        final savedTarget = await repository.saveTarget(updated);

        expect(savedTarget.id, 'T001');
        expect(savedTarget.actualAmount, 120000000);

        // 변경사항이 반영되었는지 확인
        final retrieved = await repository.getTargetById('T001');
        expect(retrieved.actualAmount, 120000000);
      });

      test('저장 시 updatedAt이 갱신된다', () async {
        final existing = await repository.getTargetById('T002');
        final originalUpdatedAt = existing.updatedAt;

        // 약간의 시간 지연
        await Future.delayed(const Duration(milliseconds: 10));

        final updated = existing.copyWith(
          actualAmount: 80000000,
        );

        final savedTarget = await repository.saveTarget(updated);

        expect(
          savedTarget.updatedAt.isAfter(originalUpdatedAt),
          true,
        );
      });
    });

    group('deleteTarget', () {
      test('목표를 삭제한다', () async {
        final result = await repository.deleteTarget('T007');

        expect(result, true);

        // 삭제된 목표를 조회하면 Exception 발생
        expect(
          () => repository.getTargetById('T007'),
          throwsException,
        );
      });

      test('존재하지 않는 ID는 false를 반환한다', () async {
        final result = await repository.deleteTarget('INVALID_ID');

        expect(result, false);
      });
    });

    group('getProgress', () {
      test('목표 ID로 진도율을 계산한다', () async {
        final progress = await repository.getProgress('T001');

        expect(progress, isNotNull);
        expect(progress.percentage, greaterThan(100)); // 목표 초과
        expect(progress.status, ProgressStatus.exceeded);
      });

      test('목표 미달 케이스의 진도율을 계산한다', () async {
        final progress = await repository.getProgress('T003');

        expect(progress, isNotNull);
        expect(progress.percentage, lessThan(100)); // 목표 미달
        expect(progress.status, ProgressStatus.insufficient);
      });

      test('목표 정확히 달성 케이스의 진도율을 계산한다', () async {
        final progress = await repository.getProgress('T005');

        expect(progress, isNotNull);
        expect(progress.percentage, 100.0); // 정확히 100%
        expect(progress.status, ProgressStatus.achieved);
      });

      test('존재하지 않는 목표 ID는 Exception을 발생시킨다', () async {
        expect(
          () => repository.getProgress('INVALID_ID'),
          throwsException,
        );
      });
    });

    group('getProgressList', () {
      test('월별 전체 진도율 목록을 조회한다', () async {
        final progressMap = await repository.getProgressList(
          yearMonth: '202601',
        );

        expect(progressMap, isNotEmpty);

        // 각 목표에 대한 진도율이 계산되었는지 확인
        for (final entry in progressMap.entries) {
          expect(entry.value, isA<Progress>());
          expect(entry.value.percentage, greaterThanOrEqualTo(0));
        }
      });

      test('존재하지 않는 년월은 빈 Map을 반환한다', () async {
        final progressMap = await repository.getProgressList(
          yearMonth: '202701',
        );

        expect(progressMap, isEmpty);
      });

      test('Map의 키는 목표 ID이다', () async {
        final progressMap = await repository.getProgressList(
          yearMonth: '202601',
        );

        expect(progressMap.containsKey('T001'), true);
        expect(progressMap.containsKey('T002'), true);
      });
    });

    group('getInsufficientTargets', () {
      test('진도율 부족 목표 목록을 조회한다', () async {
        final results = await repository.getInsufficientTargets(
          yearMonth: '202601',
        );

        expect(results, isNotEmpty);

        // 모든 결과가 진도율 100% 미만인지 확인
        for (final target in results) {
          final progress = Progress.calculate(
            targetAmount: target.targetAmount,
            actualAmount: target.actualAmount,
          );
          expect(progress.percentage, lessThan(100.0));
        }
      });

      test('커스텀 임계값으로 필터링한다', () async {
        final results = await repository.getInsufficientTargets(
          yearMonth: '202601',
          thresholdPercentage: 90.0,
        );

        expect(results, isNotEmpty);

        // 모든 결과가 진도율 90% 미만인지 확인
        for (final target in results) {
          final progress = Progress.calculate(
            targetAmount: target.targetAmount,
            actualAmount: target.actualAmount,
          );
          expect(progress.percentage, lessThan(90.0));
        }
      });

      test('결과가 진도율 낮은 순으로 정렬된다', () async {
        final results = await repository.getInsufficientTargets(
          yearMonth: '202601',
        );

        expect(results.length, greaterThan(1));

        // 진도율 낮은 순 정렬 확인
        for (var i = 0; i < results.length - 1; i++) {
          final progressA = Progress.calculate(
            targetAmount: results[i].targetAmount,
            actualAmount: results[i].actualAmount,
          );
          final progressB = Progress.calculate(
            targetAmount: results[i + 1].targetAmount,
            actualAmount: results[i + 1].actualAmount,
          );
          expect(
            progressA.percentage <= progressB.percentage,
            true,
          );
        }
      });

      test('모든 목표가 달성된 경우 빈 리스트를 반환한다', () async {
        // Mock 데이터에서 202601은 일부 목표 미달이 있으므로
        // 실제로 빈 리스트를 반환하는 케이스를 테스트하기 위해
        // 존재하지 않는 년월 사용
        final results = await repository.getInsufficientTargets(
          yearMonth: '202701',
        );

        expect(results, isEmpty);
      });
    });

    group('비동기 처리 테스트', () {
      test('getTargets는 Future를 반환한다', () {
        final result = repository.getTargets(
          yearMonth: '202601',
        );

        expect(result, isA<Future<List<Target>>>());
      });

      test('getTargetById는 Future를 반환한다', () {
        final result = repository.getTargetById('T001');

        expect(result, isA<Future<Target>>());
      });

      test('getTargetByCustomer는 Future를 반환한다', () {
        final result = repository.getTargetByCustomer(
          yearMonth: '202601',
          customerCode: 'CUST001',
        );

        expect(result, isA<Future<Target?>>());
      });

      test('saveTarget은 Future를 반환한다', () {
        final target = Target(
          id: 'TEST',
          customerName: 'Test',
          customerCode: 'TEST',
          yearMonth: '202601',
          targetAmount: 1000000,
          actualAmount: 0,
          createdAt: DateTime.now(),
          updatedAt: DateTime.now(),
        );

        final result = repository.saveTarget(target);

        expect(result, isA<Future<Target>>());
      });

      test('deleteTarget은 Future를 반환한다', () {
        final result = repository.deleteTarget('T001');

        expect(result, isA<Future<bool>>());
      });

      test('getProgress는 Future를 반환한다', () {
        final result = repository.getProgress('T001');

        expect(result, isA<Future<Progress>>());
      });

      test('getProgressList는 Future를 반환한다', () {
        final result = repository.getProgressList(
          yearMonth: '202601',
        );

        expect(result, isA<Future<Map<String, Progress>>>());
      });

      test('getInsufficientTargets는 Future를 반환한다', () {
        final result = repository.getInsufficientTargets(
          yearMonth: '202601',
        );

        expect(result, isA<Future<List<Target>>>());
      });
    });
  });
}
