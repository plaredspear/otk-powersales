import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';
import 'package:mobile/domain/entities/safety_check_submit_result.dart';
import 'package:mobile/domain/entities/safety_check_today_status.dart';
import 'package:mobile/domain/repositories/safety_check_repository.dart';
import 'package:mobile/domain/usecases/get_safety_check_items.dart';
import 'package:mobile/domain/usecases/submit_safety_check.dart';
import 'package:mobile/presentation/providers/safety_check_provider.dart';

class MockSafetyCheckRepository implements SafetyCheckRepository {
  List<SafetyCheckCategory>? categoriesToReturn;
  SafetyCheckSubmitResult? submitResultToReturn;
  Exception? exceptionToThrow;
  Exception? submitExceptionToThrow;

  int submitCallCount = 0;
  List<EquipmentAnswer>? lastEquipments;
  List<String>? lastPrecautions;

  @override
  Future<List<SafetyCheckCategory>> getItems() async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return categoriesToReturn!;
  }

  @override
  Future<SafetyCheckSubmitResult> submit({
    required DateTime startTime,
    required DateTime completeTime,
    required List<EquipmentAnswer> equipments,
    List<String>? precautions,
  }) async {
    submitCallCount++;
    lastEquipments = equipments;
    lastPrecautions = precautions;

    if (submitExceptionToThrow != null) throw submitExceptionToThrow!;
    return submitResultToReturn!;
  }

  @override
  Future<SafetyCheckTodayStatus> getTodayStatus() {
    throw UnimplementedError();
  }
}

void main() {
  final testCategories = [
    const SafetyCheckCategory(
      questionNum: 1,
      title: '안전예방 장비 착용',
      inputType: 'RADIO',
      required: true,
      options: ['예', '해당없음'],
      items: [
        SafetyCheckItem(seqNum: 1, contents: '손목보호대를 착용했습니다'),
        SafetyCheckItem(seqNum: 2, contents: '안전화를 착용했습니다'),
        SafetyCheckItem(seqNum: 3, contents: '안전사다리를 사용합니다'),
      ],
    ),
    const SafetyCheckCategory(
      questionNum: 2,
      title: '안전사고 예방사항',
      inputType: 'CHECKBOX',
      required: false,
      items: [
        SafetyCheckItem(seqNum: 1, contents: '예방사항 항목 1'),
        SafetyCheckItem(seqNum: 2, contents: '예방사항 항목 2'),
      ],
    ),
  ];

  final testSubmitResult = SafetyCheckSubmitResult(
    submittedAt: DateTime.parse('2026-03-15T09:02:30'),
    safetyCheckCompleted: true,
  );

  group('SafetyCheckNotifier (V1)', () {
    late MockSafetyCheckRepository mockRepository;
    late GetSafetyCheckItems getItemsUseCase;
    late SubmitSafetyCheck submitUseCase;
    late SafetyCheckNotifier notifier;

    setUp(() {
      mockRepository = MockSafetyCheckRepository();
      getItemsUseCase = GetSafetyCheckItems(mockRepository);
      submitUseCase = SubmitSafetyCheck(mockRepository);
      notifier = SafetyCheckNotifier(getItemsUseCase, submitUseCase);
    });

    test('초기 상태는 SafetyCheckState.initial()이다', () {
      expect(notifier.state.categories, isNull);
      expect(notifier.state.equipmentAnswers, isEmpty);
      expect(notifier.state.precautionChecks, isEmpty);
      expect(notifier.state.startTime, isNotNull);
      expect(notifier.state.isLoading, false);
    });

    test('fetchItems 성공 시 카테고리가 로딩된다', () async {
      mockRepository.categoriesToReturn = testCategories;

      await notifier.fetchItems();

      expect(notifier.state.categories, testCategories);
      expect(notifier.state.isLoaded, true);
      expect(notifier.state.equipmentAnswers, isEmpty);
      expect(notifier.state.precautionChecks, isEmpty);
    });

    test('fetchItems 실패 시 에러 상태', () async {
      mockRepository.exceptionToThrow = Exception('네트워크 오류');

      await notifier.fetchItems();

      expect(notifier.state.isError, true);
      expect(notifier.state.errorMessage, contains('네트워크 오류'));
    });

    test('setEquipmentAnswer로 라디오 응답 설정', () async {
      mockRepository.categoriesToReturn = testCategories;
      await notifier.fetchItems();

      notifier.setEquipmentAnswer(1, '예');
      notifier.setEquipmentAnswer(2, '해당없음');

      expect(notifier.state.equipmentAnswers[1], '예');
      expect(notifier.state.equipmentAnswers[2], '해당없음');
      expect(notifier.state.allRequiredChecked, false); // 3개 중 2개만
    });

    test('togglePrecaution으로 체크박스 토글', () async {
      mockRepository.categoriesToReturn = testCategories;
      await notifier.fetchItems();

      notifier.togglePrecaution(1);
      expect(notifier.state.precautionChecks[1], true);

      notifier.togglePrecaution(1);
      expect(notifier.state.precautionChecks[1], false);
    });

    test('submit 성공 시 제출 완료 상태', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      // 모든 RADIO 항목 응답
      notifier.setEquipmentAnswer(1, '예');
      notifier.setEquipmentAnswer(2, '해당없음');
      notifier.setEquipmentAnswer(3, '예');

      expect(notifier.state.allRequiredChecked, true);

      // 예방사항 체크
      notifier.togglePrecaution(1);

      await notifier.submit();

      expect(notifier.state.isSubmitted, true);
      expect(notifier.state.isSubmitting, false);
      expect(mockRepository.submitCallCount, 1);
      expect(mockRepository.lastEquipments!.length, 3);
      expect(mockRepository.lastPrecautions, ['예방사항 항목 1']);
    });

    test('submit은 allRequiredChecked가 false이면 무시', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      notifier.setEquipmentAnswer(1, '예');
      // seqNum 2, 3 미응답

      await notifier.submit();

      expect(mockRepository.submitCallCount, 0);
      expect(notifier.state.isSubmitted, false);
    });

    test('submit 실패 시 에러 상태', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitExceptionToThrow = Exception('서버 오류');

      await notifier.fetchItems();

      notifier.setEquipmentAnswer(1, '예');
      notifier.setEquipmentAnswer(2, '해당없음');
      notifier.setEquipmentAnswer(3, '예');

      await notifier.submit();

      expect(notifier.state.isError, true);
      expect(notifier.state.errorMessage, contains('서버 오류'));
    });

    test('예방사항 없이 제출 가능', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      await notifier.fetchItems();

      notifier.setEquipmentAnswer(1, '예');
      notifier.setEquipmentAnswer(2, '해당없음');
      notifier.setEquipmentAnswer(3, '예');

      await notifier.submit();

      expect(notifier.state.isSubmitted, true);
      expect(mockRepository.lastPrecautions, isNull);
    });

    test('전체 흐름: fetchItems → 응답 → submit', () async {
      mockRepository.categoriesToReturn = testCategories;
      mockRepository.submitResultToReturn = testSubmitResult;

      expect(notifier.state.categories, isNull);

      await notifier.fetchItems();
      expect(notifier.state.isLoaded, true);

      notifier.setEquipmentAnswer(1, '예');
      notifier.setEquipmentAnswer(2, '해당없음');
      notifier.setEquipmentAnswer(3, '예');
      notifier.togglePrecaution(1);
      notifier.togglePrecaution(2);

      expect(notifier.state.allRequiredChecked, true);

      await notifier.submit();

      expect(notifier.state.isSubmitted, true);
      expect(mockRepository.submitCallCount, 1);
      expect(mockRepository.lastEquipments!.length, 3);
      expect(mockRepository.lastPrecautions!.length, 2);
    });
  });
}
