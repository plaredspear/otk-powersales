import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mobile/presentation/providers/target_provider.dart';
import 'package:mobile/presentation/providers/target_state.dart';
import 'package:mobile/domain/entities/progress.dart';

void main() {
  group('TargetProvider Tests', () {
    late ProviderContainer container;

    setUp(() {
      container = ProviderContainer();
    });

    tearDown(() {
      container.dispose();
    });

    test('초기 상태가 올바르게 설정된다', () {
      // Act
      final state = container.read(targetProvider);

      // Assert
      expect(state.targets, isEmpty);
      expect(state.progressList, isEmpty);
      expect(state.isLoading, isFalse);
      expect(state.errorMessage, isNull);
      expect(state.totalTargetAmount, equals(0));
      expect(state.totalActualAmount, equals(0));
    });

    test('fetchTargets() 호출 시 데이터를 성공적으로 로드한다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);

      // Act
      await notifier.fetchTargets();
      final state = container.read(targetProvider);

      // Assert
      expect(state.isLoading, isFalse);
      expect(state.errorMessage, isNull);
      expect(state.targets, isNotEmpty);
      expect(state.progressList, isNotEmpty);
      expect(state.totalTargetAmount, greaterThan(0));
      expect(state.totalActualAmount, greaterThan(0));
      expect(state.overallProgress, isNotNull);
    });

    test('changeYearMonth() 호출 시 년월이 변경되고 데이터를 다시 로드한다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      const newYearMonth = '202601';

      // Act
      await notifier.changeYearMonth(newYearMonth);
      final state = container.read(targetProvider);

      // Assert
      expect(state.filter.yearMonth, equals(newYearMonth));
      expect(state.targets, isNotEmpty);
    });

    test('filterByCategory() 호출 시 카테고리 필터가 적용된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets(); // 먼저 데이터 로드

      // Act
      await notifier.filterByCategory('전산매출');
      final state = container.read(targetProvider);

      // Assert
      expect(state.filter.category, equals('전산매출'));
    });

    test('filterByCustomer() 호출 시 거래처 필터가 적용된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets(); // 먼저 데이터 로드
      final firstTarget = container.read(targetProvider).targets.first;

      // Act
      await notifier.filterByCustomer(firstTarget.customerCode);
      final state = container.read(targetProvider);

      // Assert
      expect(state.filter.customerCode, equals(firstTarget.customerCode));
    });

    test('toggleInsufficientFilter() 호출 시 진도율 부족 필터가 토글된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      final initialValue = container.read(targetProvider).filter.onlyInsufficient;

      // Act
      await notifier.toggleInsufficientFilter();
      final state = container.read(targetProvider);

      // Assert
      expect(state.filter.onlyInsufficient, equals(!initialValue));
    });

    test('clearFilter() 호출 시 필터가 초기화된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      await notifier.filterByCategory('전산매출');
      await notifier.filterByCustomer('CUST001');
      await notifier.toggleInsufficientFilter();

      // Act
      await notifier.clearFilter();
      final state = container.read(targetProvider);

      // Assert
      expect(state.filter.category, isNull);
      expect(state.filter.customerCode, isNull);
      expect(state.filter.onlyInsufficient, isFalse);
    });

    test('refresh() 호출 시 데이터를 다시 로드한다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      final initialTargetsCount = container.read(targetProvider).targets.length;

      // Act
      await notifier.refresh();
      final state = container.read(targetProvider);

      // Assert
      expect(state.targets.length, equals(initialTargetsCount));
      expect(state.isLoading, isFalse);
    });

    test('TargetState의 insufficientCount가 올바르게 계산된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      final state = container.read(targetProvider);

      // Act
      final insufficientCount = state.insufficientCount;

      // Assert
      final expectedCount = state.progressList.values
          .where((p) => p.status == ProgressStatus.insufficient)
          .length;
      expect(insufficientCount, equals(expectedCount));
    });

    test('TargetState의 exceededCount가 올바르게 계산된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      final state = container.read(targetProvider);

      // Act
      final exceededCount = state.exceededCount;

      // Assert
      final expectedCount = state.progressList.values
          .where((p) => p.status == ProgressStatus.exceeded)
          .length;
      expect(exceededCount, equals(expectedCount));
    });

    test('TargetState의 achievedCount가 올바르게 계산된다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      final state = container.read(targetProvider);

      // Act
      final achievedCount = state.achievedCount;

      // Assert
      final expectedCount = state.progressList.values
          .where((p) => p.status == ProgressStatus.achieved)
          .length;
      expect(achievedCount, equals(expectedCount));
    });

    test('TargetState의 filteredTargets가 카테고리 필터를 올바르게 적용한다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      await notifier.filterByCategory('전산매출');
      final state = container.read(targetProvider);

      // Act
      final filtered = state.filteredTargets;

      // Assert
      expect(
        filtered.every((t) => t.category == '전산매출'),
        isTrue,
      );
    });

    test('TargetState의 filteredTargets가 거래처 필터를 올바르게 적용한다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      final firstTarget = container.read(targetProvider).targets.first;
      await notifier.filterByCustomer(firstTarget.customerCode);
      final state = container.read(targetProvider);

      // Act
      final filtered = state.filteredTargets;

      // Assert
      expect(
        filtered.every((t) => t.customerCode == firstTarget.customerCode),
        isTrue,
      );
    });

    test('TargetState의 filteredTargets가 진도율 부족 필터를 올바르게 적용한다', () async {
      // Arrange
      final notifier = container.read(targetProvider.notifier);
      await notifier.fetchTargets();
      await notifier.toggleInsufficientFilter();
      final state = container.read(targetProvider);

      // Act
      final filtered = state.filteredTargets;

      // Assert
      for (final target in filtered) {
        final progress = state.progressList[target.id];
        expect(progress, isNotNull);
        expect(progress!.percentage, lessThan(100.0));
      }
    });

    test('TargetFilter의 copyWith가 올바르게 동작한다', () {
      // Arrange
      const original = TargetFilter(yearMonth: '202601');

      // Act
      final updated = original.copyWith(
        category: '전산매출',
        onlyInsufficient: true,
      );

      // Assert
      expect(updated.yearMonth, equals('202601'));
      expect(updated.category, equals('전산매출'));
      expect(updated.onlyInsufficient, isTrue);
    });

    test('TargetFilter의 clear가 올바르게 동작한다', () {
      // Arrange
      const filter = TargetFilter(
        yearMonth: '202601',
        category: '전산매출',
        customerCode: 'CUST001',
        onlyInsufficient: true,
      );

      // Act
      final cleared = filter.clear();

      // Assert
      expect(cleared.yearMonth, equals('202601'));
      expect(cleared.category, isNull);
      expect(cleared.customerCode, isNull);
      expect(cleared.onlyInsufficient, isFalse);
    });
  });
}
