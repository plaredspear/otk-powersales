import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/progress.dart';

void main() {
  group('ProgressStatus Enum', () {
    test('진도율 상태 이름이 올바르게 정의된다', () {
      expect(ProgressStatus.exceeded.displayName, '초과');
      expect(ProgressStatus.insufficient.displayName, '부족');
      expect(ProgressStatus.achieved.displayName, '달성');
    });

    test('진도율 상태별 색상이 올바르게 정의된다', () {
      expect(ProgressStatus.exceeded.color, Colors.green);
      expect(ProgressStatus.insufficient.color, Colors.red);
      expect(ProgressStatus.achieved.color, Colors.blue);
    });
  });

  group('Progress Entity', () {
    group('생성 테스트', () {
      test('Progress 엔티티가 올바르게 생성된다', () {
        final progress = Progress(
          targetAmount: 100000000,
          actualAmount: 120000000,
          percentage: 120.0,
          difference: 20000000,
          status: ProgressStatus.exceeded,
        );

        expect(progress.targetAmount, 100000000);
        expect(progress.actualAmount, 120000000);
        expect(progress.percentage, 120.0);
        expect(progress.difference, 20000000);
        expect(progress.status, ProgressStatus.exceeded);
      });
    });

    group('진도율 계산 테스트 (Progress.calculate)', () {
      test('진도율이 100% 초과인 경우 (목표 초과)', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        expect(progress.targetAmount, 100000000);
        expect(progress.actualAmount, 120000000);
        expect(progress.percentage, 120.0);
        expect(progress.difference, 20000000);
        expect(progress.status, ProgressStatus.exceeded);
        expect(progress.isExceeded, true);
        expect(progress.isInsufficient, false);
        expect(progress.isAchieved, false);
      });

      test('진도율이 100% 미만인 경우 (목표 부족)', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(progress.targetAmount, 100000000);
        expect(progress.actualAmount, 80000000);
        expect(progress.percentage, 80.0);
        expect(progress.difference, -20000000);
        expect(progress.status, ProgressStatus.insufficient);
        expect(progress.isExceeded, false);
        expect(progress.isInsufficient, true);
        expect(progress.isAchieved, false);
      });

      test('진도율이 정확히 100%인 경우 (목표 달성)', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 100000000,
        );

        expect(progress.targetAmount, 100000000);
        expect(progress.actualAmount, 100000000);
        expect(progress.percentage, 100.0);
        expect(progress.difference, 0);
        expect(progress.status, ProgressStatus.achieved);
        expect(progress.isExceeded, false);
        expect(progress.isInsufficient, false);
        expect(progress.isAchieved, true);
      });

      test('목표가 0인 경우 진도율은 0%이다', () {
        final progress = Progress.calculate(
          targetAmount: 0,
          actualAmount: 50000000,
        );

        expect(progress.percentage, 0.0);
        expect(progress.difference, 50000000);
        expect(progress.status, ProgressStatus.insufficient);
      });

      test('실적이 0인 경우 진도율은 0%이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 0,
        );

        expect(progress.percentage, 0.0);
        expect(progress.difference, -100000000);
        expect(progress.status, ProgressStatus.insufficient);
      });

      test('목표와 실적이 모두 0인 경우', () {
        final progress = Progress.calculate(
          targetAmount: 0,
          actualAmount: 0,
        );

        expect(progress.percentage, 0.0);
        expect(progress.difference, 0);
        expect(progress.status, ProgressStatus.insufficient);
      });

      test('진도율 150% 케이스', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 150000000,
        );

        expect(progress.percentage, 150.0);
        expect(progress.difference, 50000000);
        expect(progress.status, ProgressStatus.exceeded);
      });

      test('진도율 50% 케이스', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 50000000,
        );

        expect(progress.percentage, 50.0);
        expect(progress.difference, -50000000);
        expect(progress.status, ProgressStatus.insufficient);
      });

      test('진도율 소수점 계산이 정확하다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 123456789,
        );

        expect(progress.percentage, closeTo(123.456789, 0.000001));
        expect(progress.difference, 23456789);
        expect(progress.status, ProgressStatus.exceeded);
      });
    });

    group('Getter 테스트', () {
      test('isExceeded가 올바르게 동작한다', () {
        final exceededProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final insufficientProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(exceededProgress.isExceeded, true);
        expect(insufficientProgress.isExceeded, false);
      });

      test('isInsufficient가 올바르게 동작한다', () {
        final insufficientProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );
        final exceededProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        expect(insufficientProgress.isInsufficient, true);
        expect(exceededProgress.isInsufficient, false);
      });

      test('isAchieved가 올바르게 동작한다', () {
        final achievedProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 100000000,
        );
        final insufficientProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(achievedProgress.isAchieved, true);
        expect(insufficientProgress.isAchieved, false);
      });

      test('color가 상태별로 올바른 색상을 반환한다', () {
        final exceededProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final insufficientProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );
        final achievedProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 100000000,
        );

        expect(exceededProgress.color, Colors.green);
        expect(insufficientProgress.color, Colors.red);
        expect(achievedProgress.color, Colors.blue);
      });

      test('statusDisplayName이 올바른 상태명을 반환한다', () {
        final exceededProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final insufficientProgress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(exceededProgress.statusDisplayName, '초과');
        expect(insufficientProgress.statusDisplayName, '부족');
      });

      test('formattedPercentage가 소수점 1자리로 포맷된다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 123456789,
        );

        expect(progress.formattedPercentage, '123.5');
      });
    });

    group('copyWith 테스트', () {
      test('copyWith가 올바르게 동작한다 - 모든 필드 변경', () {
        final original = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        final copied = original.copyWith(
          targetAmount: 200000000,
          actualAmount: 180000000,
          percentage: 90.0,
          difference: -20000000,
          status: ProgressStatus.insufficient,
        );

        expect(copied.targetAmount, 200000000);
        expect(copied.actualAmount, 180000000);
        expect(copied.percentage, 90.0);
        expect(copied.difference, -20000000);
        expect(copied.status, ProgressStatus.insufficient);
      });

      test('copyWith가 일부 필드만 변경한다', () {
        final original = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        final copied = original.copyWith(
          actualAmount: 110000000,
          percentage: 110.0,
        );

        expect(copied.targetAmount, original.targetAmount);
        expect(copied.actualAmount, 110000000);
        expect(copied.percentage, 110.0);
        expect(copied.difference, original.difference);
        expect(copied.status, original.status);
      });

      test('copyWith가 원본을 변경하지 않는다 (불변성)', () {
        final original = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final copied = original.copyWith(actualAmount: 999999999);

        expect(original.actualAmount, 120000000);
        expect(copied.actualAmount, 999999999);
      });
    });

    group('직렬화 테스트', () {
      test('toJson이 올바르게 동작한다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final json = progress.toJson();

        expect(json['targetAmount'], 100000000);
        expect(json['actualAmount'], 120000000);
        expect(json['percentage'], 120.0);
        expect(json['difference'], 20000000);
        expect(json['status'], 'exceeded');
      });

      test('fromJson이 올바르게 동작한다', () {
        final json = {
          'targetAmount': 100000000,
          'actualAmount': 80000000,
          'percentage': 80.0,
          'difference': -20000000,
          'status': 'insufficient',
        };

        final progress = Progress.fromJson(json);

        expect(progress.targetAmount, 100000000);
        expect(progress.actualAmount, 80000000);
        expect(progress.percentage, 80.0);
        expect(progress.difference, -20000000);
        expect(progress.status, ProgressStatus.insufficient);
      });

      test('toJson과 fromJson이 정확히 왕복 변환된다', () {
        final original = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final json = original.toJson();
        final restored = Progress.fromJson(json);

        expect(restored, original);
      });

      test('fromJson이 정수형 percentage를 double로 변환한다', () {
        final json = {
          'targetAmount': 100000000,
          'actualAmount': 100000000,
          'percentage': 100, // int 타입
          'difference': 0,
          'status': 'achieved',
        };

        final progress = Progress.fromJson(json);

        expect(progress.percentage, 100.0);
        expect(progress.percentage, isA<double>());
      });

      test('fromJson이 존재하지 않는 status를 기본값으로 처리한다', () {
        final json = {
          'targetAmount': 100000000,
          'actualAmount': 80000000,
          'percentage': 80.0,
          'difference': -20000000,
          'status': 'invalid_status',
        };

        final progress = Progress.fromJson(json);

        expect(progress.status, ProgressStatus.insufficient);
      });
    });

    group('Equality 테스트', () {
      test('같은 값을 가진 엔티티가 동일하게 비교된다', () {
        final progress1 = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final progress2 = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        expect(progress1, progress2);
        expect(progress1.hashCode, progress2.hashCode);
      });

      test('다른 값을 가진 엔티티가 다르게 비교된다', () {
        final progress1 = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final progress2 = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(progress1, isNot(progress2));
      });

      test('자기 자신과 비교하면 동일하다 (identical)', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        expect(progress, progress);
      });
    });

    group('toString 테스트', () {
      test('toString이 모든 필드를 포함한다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );
        final str = progress.toString();

        expect(str, contains('100000000'));
        expect(str, contains('120000000'));
        expect(str, contains('120.0'));
        expect(str, contains('20000000'));
        expect(str, contains('초과'));
      });
    });

    group('비즈니스 로직 검증', () {
      test('초과 상태의 색상은 녹색이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        expect(progress.color, Colors.green);
      });

      test('부족 상태의 색상은 빨강이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(progress.color, Colors.red);
      });

      test('달성 상태의 색상은 파랑이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 100000000,
        );

        expect(progress.color, Colors.blue);
      });

      test('차액이 양수면 목표 초과이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 120000000,
        );

        expect(progress.difference > 0, true);
        expect(progress.isExceeded, true);
      });

      test('차액이 음수면 목표 부족이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 80000000,
        );

        expect(progress.difference < 0, true);
        expect(progress.isInsufficient, true);
      });

      test('차액이 0이면 정확히 목표 달성이다', () {
        final progress = Progress.calculate(
          targetAmount: 100000000,
          actualAmount: 100000000,
        );

        expect(progress.difference, 0);
        expect(progress.isAchieved, true);
      });
    });
  });
}
