import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/usecases/submit_safety_check.dart';
import 'package:mobile/domain/repositories/safety_check_repository.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';

void main() {
  group('SubmitSafetyCheck', () {
    late SubmitSafetyCheck useCase;
    late _TestSafetyCheckRepository repository;

    setUp(() {
      repository = _TestSafetyCheckRepository();
      useCase = SubmitSafetyCheck(repository);
    });

    test('Returns submit result on success', () async {
      final submittedTime = DateTime(2026, 3, 15, 9, 2, 30);
      final expectedResult = SafetyCheckSubmitResult(
        submittedAt: submittedTime,
        safetyCheckCompleted: true,
      );
      repository.submitResultToReturn = expectedResult;

      final result = await useCase.call(
        startTime: DateTime(2026, 3, 15, 9, 0, 0),
        completeTime: submittedTime,
        equipments: [
          const EquipmentAnswer(seqNum: 1, answer: '예'),
          const EquipmentAnswer(seqNum: 2, answer: '해당없음'),
        ],
        precautions: ['예방사항 1'],
      );

      expect(result, expectedResult);
      expect(result.submittedAt, submittedTime);
      expect(result.safetyCheckCompleted, true);
    });

    test('Throws ArgumentError when equipments is empty', () async {
      expect(
        () => useCase.call(
          startTime: DateTime.now(),
          completeTime: DateTime.now(),
          equipments: [],
        ),
        throwsA(isA<ArgumentError>()),
      );
    });

    test('Passes correct parameters to repository', () async {
      final equipments = [
        const EquipmentAnswer(seqNum: 1, answer: '예'),
        const EquipmentAnswer(seqNum: 2, answer: '해당없음'),
      ];
      final startTime = DateTime(2026, 3, 15, 9, 0);
      final completeTime = DateTime(2026, 3, 15, 9, 2);

      await useCase.call(
        startTime: startTime,
        completeTime: completeTime,
        equipments: equipments,
        precautions: ['예방사항 1', '예방사항 3'],
      );

      expect(repository.lastStartTime, startTime);
      expect(repository.lastCompleteTime, completeTime);
      expect(repository.lastEquipments, equipments);
      expect(repository.lastPrecautions, ['예방사항 1', '예방사항 3']);
    });

    test('Propagates repository exception', () async {
      final expectedException = Exception('이미 안전점검을 완료하였습니다.');
      repository.exceptionToThrow = expectedException;

      expect(
        () => useCase.call(
          startTime: DateTime.now(),
          completeTime: DateTime.now(),
          equipments: [const EquipmentAnswer(seqNum: 1, answer: '예')],
        ),
        throwsA(expectedException),
      );
    });
  });
}

class _TestSafetyCheckRepository implements SafetyCheckRepository {
  SafetyCheckSubmitResult? submitResultToReturn;
  Exception? exceptionToThrow;
  DateTime? lastStartTime;
  DateTime? lastCompleteTime;
  List<EquipmentAnswer>? lastEquipments;
  List<String>? lastPrecautions;

  @override
  Future<List<SafetyCheckCategory>> getItems() async => [];

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() async =>
      const SafetyCheckTodayStatus(completed: false, submittedAt: null);

  @override
  Future<SafetyCheckSubmitResult> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<EquipmentAnswer> equipments,
    List<String>? precautions,
  }) async {
    lastStartTime = startTime;
    lastCompleteTime = completeTime;
    lastEquipments = equipments;
    lastPrecautions = precautions;

    if (exceptionToThrow != null) throw exceptionToThrow!;

    return submitResultToReturn ??
        SafetyCheckSubmitResult(
          submittedAt: completeTime,
          safetyCheckCompleted: true,
        );
  }
}
