import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';
import 'package:mobile/presentation/providers/safety_check_state.dart';

class TestData {
  static const section1 = SafetyCheckCategory(
    questionNum: 1,
    title: '안전예방 장비 착용',
    inputType: 'RADIO',
    required: true,
    options: ['예', '해당없음'],
    items: [
      SafetyCheckItem(seqNum: 1, contents: '손목보호대를 착용했습니다'),
      SafetyCheckItem(seqNum: 2, contents: '안전화를 착용했습니다'),
    ],
  );

  static const section2 = SafetyCheckCategory(
    questionNum: 2,
    title: '안전사고 예방사항',
    inputType: 'CHECKBOX',
    required: false,
    items: [
      SafetyCheckItem(seqNum: 1, contents: '예방사항 1'),
      SafetyCheckItem(seqNum: 2, contents: '예방사항 2'),
    ],
  );

  static const testCategories = [section1, section2];
}

void main() {
  group('SafetyCheckState (V1)', () {
    group('초기 상태', () {
      test('initial() factory가 올바른 기본값을 생성한다', () {
        final state = SafetyCheckState.initial();

        expect(state.categories, isNull);
        expect(state.equipmentAnswers, isEmpty);
        expect(state.precautionChecks, isEmpty);
        expect(state.expandedSeqNums, isEmpty);
        expect(state.startTime, isNotNull);
        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.isSubmitted, false);
        expect(state.errorMessage, isNull);
      });
    });

    group('toLoading', () {
      test('isLoading을 true로 설정한다', () {
        final state = SafetyCheckState.initial().toLoading();
        expect(state.isLoading, true);
        expect(state.errorMessage, isNull);
      });

      test('기존 categories를 유지한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toLoading();
        expect(state.categories, TestData.testCategories);
      });
    });

    group('toLoaded', () {
      test('categories를 설정하고 응답을 초기화한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);

        expect(state.categories, TestData.testCategories);
        expect(state.equipmentAnswers, isEmpty);
        expect(state.precautionChecks, isEmpty);
        expect(state.isLoading, false);
      });
    });

    group('setEquipmentAnswer', () {
      test('장비 항목 응답을 설정한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .setEquipmentAnswer(1, '예');

        expect(state.equipmentAnswers[1], '예');
        expect(state.equipmentAnswers.containsKey(2), false);
      });

      test('기존 응답을 변경한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .setEquipmentAnswer(1, '예')
            .setEquipmentAnswer(1, '해당없음');

        expect(state.equipmentAnswers[1], '해당없음');
      });
    });

    group('togglePrecaution', () {
      test('예방사항 체크를 토글한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .togglePrecaution(1);

        expect(state.precautionChecks[1], true);
      });

      test('두 번 토글하면 false', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .togglePrecaution(1)
            .togglePrecaution(1);

        expect(state.precautionChecks[1], false);
      });
    });

    group('expandedSeqNums (아코디언)', () {
      test('toLoaded는 RADIO 항목 전체를 펼친 상태로 초기화한다', () {
        final state =
            SafetyCheckState.initial().toLoaded(TestData.testCategories);
        // section1(RADIO) 항목 seqNum 1, 2 만 포함, section2(CHECKBOX)는 제외
        expect(state.expandedSeqNums, {1, 2});
      });

      test('copyWith으로 expandedSeqNums를 설정한다', () {
        final state =
            SafetyCheckState.initial().copyWith(expandedSeqNums: {3});
        expect(state.expandedSeqNums, {3});
      });

      test('다른 상태 전환 시 expandedSeqNums가 유지된다', () {
        final state = SafetyCheckState.initial()
            .copyWith(expandedSeqNums: {5})
            .setEquipmentAnswer(1, '예');
        expect(state.expandedSeqNums, {5});
      });
    });

    group('allRequiredChecked', () {
      test('categories가 null이면 false', () {
        expect(SafetyCheckState.initial().allRequiredChecked, false);
      });

      test('RADIO 섹션의 모든 항목에 응답하면 true', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .setEquipmentAnswer(1, '예')
            .setEquipmentAnswer(2, '해당없음');

        expect(state.allRequiredChecked, true);
      });

      test('일부만 응답하면 false', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .setEquipmentAnswer(1, '예');

        expect(state.allRequiredChecked, false);
      });

      test('CHECKBOX만 있는 경우 항상 true', () {
        final state = SafetyCheckState.initial()
            .toLoaded([TestData.section2]);

        expect(state.allRequiredChecked, true);
      });
    });

    group('상태 전환 시나리오', () {
      test('Loading → Loaded → SetAnswer → Submitting → Submitted', () {
        var state = SafetyCheckState.initial()
            .toLoading()
            .toLoaded(TestData.testCategories)
            .setEquipmentAnswer(1, '예')
            .setEquipmentAnswer(2, '해당없음');

        expect(state.allRequiredChecked, true);

        state = state.toSubmitting();
        expect(state.isSubmitting, true);
        expect(state.equipmentAnswers[1], '예');

        state = state.toSubmitted();
        expect(state.isSubmitted, true);
        expect(state.isSubmitting, false);
      });

      test('Error → Loading 재시도', () {
        final error = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toError('오류');
        final retrying = error.toLoading();

        expect(retrying.isLoading, true);
        expect(retrying.isError, false);
        expect(retrying.categories, TestData.testCategories);
      });
    });
  });
}
