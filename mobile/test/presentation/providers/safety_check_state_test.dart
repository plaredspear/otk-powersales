import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/safety_check_category.dart';
import 'package:mobile/domain/entities/safety_check_item.dart';
import 'package:mobile/presentation/providers/safety_check_state.dart';

/// 테스트용 데이터 헬퍼
class TestData {
  /// 테스트용 SafetyCheckItem - 필수 항목
  static const requiredItem1 = SafetyCheckItem(
    id: 1,
    label: '차량 점검',
    sortOrder: 1,
    required: true,
  );

  static const requiredItem2 = SafetyCheckItem(
    id: 2,
    label: '안전벨트 확인',
    sortOrder: 2,
    required: true,
  );

  /// 테스트용 SafetyCheckItem - 선택 항목
  static const optionalItem1 = SafetyCheckItem(
    id: 3,
    label: '세차 필요 여부',
    sortOrder: 3,
    required: false,
  );

  static const optionalItem2 = SafetyCheckItem(
    id: 4,
    label: '주유 필요 여부',
    sortOrder: 4,
    required: false,
  );

  /// 테스트용 SafetyCheckCategory - 필수 항목만
  static const categoryWithRequiredOnly = SafetyCheckCategory(
    id: 1,
    name: '차량 안전 점검',
    description: '운행 전 필수 점검 사항',
    items: [requiredItem1, requiredItem2],
  );

  /// 테스트용 SafetyCheckCategory - 필수 + 선택 항목
  static const categoryWithMixed = SafetyCheckCategory(
    id: 2,
    name: '차량 관리',
    description: '차량 유지보수 점검',
    items: [requiredItem1, optionalItem1],
  );

  /// 테스트용 SafetyCheckCategory - 선택 항목만
  static const categoryWithOptionalOnly = SafetyCheckCategory(
    id: 3,
    name: '기타 점검',
    description: '선택 점검 사항',
    items: [optionalItem1, optionalItem2],
  );

  /// 테스트용 SafetyCheckCategory 목록
  static const testCategories = [
    categoryWithRequiredOnly,
    categoryWithMixed,
  ];
}

void main() {
  group('SafetyCheckState', () {
    group('초기 상태', () {
      test('initial() factory가 올바른 기본값을 생성한다', () {
        final state = SafetyCheckState.initial();

        expect(state.categories, isNull);
        expect(state.checkedItems, isEmpty);
        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.isSubmitted, false);
        expect(state.errorMessage, isNull);
        expect(state.isLoaded, false);
        expect(state.isError, false);
      });
    });

    group('toLoading', () {
      test('isLoading을 true로 설정하고 error를 초기화한다', () {
        final state = SafetyCheckState.initial().toLoading();

        expect(state.isLoading, true);
        expect(state.errorMessage, isNull);
        expect(state.isSubmitting, false);
        expect(state.isSubmitted, false);
      });

      test('기존 categories를 유지한다', () {
        final initialState = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);
        final loadingState = initialState.toLoading();

        expect(loadingState.categories, TestData.testCategories);
        expect(loadingState.isLoading, true);
      });

      test('기존 checkedItems를 유지한다', () {
        final initialState = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1);
        final loadingState = initialState.toLoading();

        expect(loadingState.checkedItems[1], true);
        expect(loadingState.isLoading, true);
      });
    });

    group('toLoaded', () {
      test('categories를 설정하고 모든 항목을 unchecked로 초기화한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);

        expect(state.categories, TestData.testCategories);
        expect(state.checkedItems.length, 3); // 총 3개 항목
        expect(state.checkedItems[1], false); // requiredItem1
        expect(state.checkedItems[2], false); // requiredItem2
        expect(state.checkedItems[3], false); // optionalItem1
        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
      });

      test('loading 상태를 초기화한다', () {
        final state = SafetyCheckState.initial()
            .toLoading()
            .toLoaded(TestData.testCategories);

        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.isSubmitted, false);
      });

      test('빈 카테고리 목록도 처리할 수 있다', () {
        final state = SafetyCheckState.initial().toLoaded([]);

        expect(state.categories, isEmpty);
        expect(state.checkedItems, isEmpty);
        expect(state.isLoaded, true);
      });
    });

    group('toError', () {
      test('errorMessage를 설정하고 loading 상태를 초기화한다', () {
        final state = SafetyCheckState.initial()
            .toLoading()
            .toError('네트워크 오류');

        expect(state.errorMessage, '네트워크 오류');
        expect(state.isLoading, false);
        expect(state.isSubmitting, false);
        expect(state.isSubmitted, false);
        expect(state.isError, true);
      });

      test('기존 categories를 유지한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toError('서버 오류');

        expect(state.categories, TestData.testCategories);
        expect(state.errorMessage, '서버 오류');
      });

      test('기존 checkedItems를 유지한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toError('네트워크 오류');

        expect(state.checkedItems[1], true);
        expect(state.errorMessage, '네트워크 오류');
      });
    });

    group('toSubmitting', () {
      test('isSubmitting을 true로 설정하고 error를 초기화한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toSubmitting();

        expect(state.isSubmitting, true);
        expect(state.errorMessage, isNull);
        expect(state.isLoading, false);
        expect(state.isSubmitted, false);
      });

      test('기존 categories와 checkedItems를 유지한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toSubmitting();

        expect(state.categories, TestData.testCategories);
        expect(state.checkedItems[1], true);
        expect(state.isSubmitting, true);
      });
    });

    group('toSubmitted', () {
      test('isSubmitted를 true로 설정하고 isSubmitting을 false로 변경한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toSubmitting()
            .toSubmitted();

        expect(state.isSubmitted, true);
        expect(state.isSubmitting, false);
        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
      });

      test('기존 categories와 checkedItems를 유지한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(2)
            .toSubmitting()
            .toSubmitted();

        expect(state.categories, TestData.testCategories);
        expect(state.checkedItems[1], true);
        expect(state.checkedItems[2], true);
        expect(state.isSubmitted, true);
      });
    });

    group('toggleItem', () {
      test('단일 항목을 토글한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1);

        expect(state.checkedItems[1], true);
        expect(state.checkedItems[2], false);
        expect(state.checkedItems[3], false);
      });

      test('다른 항목에 영향을 주지 않는다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(2);

        expect(state.checkedItems[1], true);
        expect(state.checkedItems[2], true);
        expect(state.checkedItems[3], false);
      });

      test('두 번 토글하면 원래 상태로 돌아온다', () {
        final initialState = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);
        final toggledOnce = initialState.toggleItem(1);
        final toggledTwice = toggledOnce.toggleItem(1);

        expect(initialState.checkedItems[1], false);
        expect(toggledOnce.checkedItems[1], true);
        expect(toggledTwice.checkedItems[1], false);
      });

      test('알 수 없는 항목을 토글하면 true로 추가된다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(999);

        expect(state.checkedItems[999], true);
      });

      test('다른 상태 값들을 유지한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toSubmitting()
            .toggleItem(1);

        expect(state.isSubmitting, true);
        expect(state.categories, TestData.testCategories);
      });
    });

    group('allRequiredChecked', () {
      test('categories가 null이면 false를 반환한다', () {
        final state = SafetyCheckState.initial();

        expect(state.allRequiredChecked, false);
      });

      test('필수 항목이 체크되지 않았으면 false를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);

        expect(state.allRequiredChecked, false);
      });

      test('일부 필수 항목만 체크되었으면 false를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1); // requiredItem1만 체크

        expect(state.allRequiredChecked, false);
      });

      test('모든 필수 항목이 체크되면 true를 반환한다 (선택 항목은 미체크)', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1) // categoryWithRequiredOnly의 requiredItem1
            .toggleItem(2); // categoryWithRequiredOnly의 requiredItem2
        // optionalItem1 (id: 3)은 체크 안 함

        expect(state.allRequiredChecked, true);
        expect(state.checkedItems[3], false); // 선택 항목은 체크 안 됨
      });

      test('필수 항목이 없으면 true를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded([TestData.categoryWithOptionalOnly]);

        expect(state.allRequiredChecked, true);
      });

      test('모든 항목이 체크되면 true를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(2)
            .toggleItem(3);

        expect(state.allRequiredChecked, true);
      });
    });

    group('checkedItemIds', () {
      test('체크된 항목이 없으면 빈 리스트를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);

        expect(state.checkedItemIds, isEmpty);
      });

      test('체크된 항목의 ID만 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(3);

        expect(state.checkedItemIds, containsAll([1, 3]));
        expect(state.checkedItemIds.length, 2);
      });

      test('false인 항목은 포함하지 않는다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(2)
            .toggleItem(2); // 다시 토글하여 false로 변경

        expect(state.checkedItemIds, [1]);
      });

      test('순서는 보장하지 않지만 모든 체크된 ID를 포함한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(2)
            .toggleItem(3);

        expect(state.checkedItemIds, unorderedEquals([1, 2, 3]));
      });
    });

    group('isLoaded', () {
      test('categories가 null이면 false를 반환한다', () {
        final state = SafetyCheckState.initial();

        expect(state.isLoaded, false);
      });

      test('isLoading이 true면 false를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toLoading();

        expect(state.isLoaded, false);
      });

      test('isSubmitting이 true면 false를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toSubmitting();

        expect(state.isLoaded, false);
      });

      test('errorMessage가 있으면 false를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toError('오류 발생');

        expect(state.isLoaded, false);
      });

      test('categories가 있고 loading/submitting/error가 없으면 true를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);

        expect(state.isLoaded, true);
      });

      test('submitted 상태여도 categories가 있고 loading/submitting/error가 없으면 true를 반환한다', () {
        final state = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toSubmitting()
            .toSubmitted();

        expect(state.isLoaded, true);
        expect(state.isSubmitted, true);
      });
    });

    group('isError', () {
      test('errorMessage가 null이면 false를 반환한다', () {
        final state = SafetyCheckState.initial();

        expect(state.isError, false);
      });

      test('errorMessage가 있으면 true를 반환한다', () {
        final state = SafetyCheckState.initial().toError('네트워크 오류');

        expect(state.isError, true);
      });

      test('빈 문자열도 에러로 간주한다', () {
        final state = SafetyCheckState.initial().toError('');

        expect(state.isError, true);
      });
    });

    group('copyWith', () {
      test('모든 필드를 개별적으로 복사할 수 있다', () {
        final original = SafetyCheckState.initial();
        final copied = original.copyWith(
          categories: TestData.testCategories,
          checkedItems: {1: true, 2: false},
          isLoading: true,
          isSubmitting: true,
          isSubmitted: true,
          errorMessage: '오류',
        );

        expect(copied.categories, TestData.testCategories);
        expect(copied.checkedItems, {1: true, 2: false});
        expect(copied.isLoading, true);
        expect(copied.isSubmitting, true);
        expect(copied.isSubmitted, true);
        expect(copied.errorMessage, '오류');
      });

      test('일부 필드만 복사할 수 있다', () {
        final original = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);
        final copied = original.copyWith(isLoading: true);

        expect(copied.categories, TestData.testCategories);
        expect(copied.isLoading, true);
        expect(copied.errorMessage, isNull);
      });

      test('errorMessage를 null로 설정할 수 있다', () {
        final original = SafetyCheckState.initial().toError('오류');
        final copied = original.copyWith(errorMessage: null);

        expect(copied.errorMessage, isNull);
        expect(copied.isError, false);
      });

      test('파라미터를 제공하지 않으면 원본을 유지한다', () {
        final original = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);
        final copied = original.copyWith();

        expect(copied.categories, original.categories);
        expect(copied.checkedItems, original.checkedItems);
        expect(copied.isLoading, original.isLoading);
      });

      test('null이 아닌 errorMessage를 다른 값으로 변경할 수 있다', () {
        final original = SafetyCheckState.initial().toError('첫 번째 오류');
        final copied = original.copyWith(errorMessage: '두 번째 오류');

        expect(original.errorMessage, '첫 번째 오류');
        expect(copied.errorMessage, '두 번째 오류');
      });
    });

    group('상태 전환 시나리오', () {
      test('Loading → Loaded 전환', () {
        final state = SafetyCheckState.initial()
            .toLoading()
            .toLoaded(TestData.testCategories);

        expect(state.isLoaded, true);
        expect(state.isLoading, false);
        expect(state.categories, TestData.testCategories);
      });

      test('Loaded → Submitting → Submitted 전환', () {
        final loaded = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toggleItem(1)
            .toggleItem(2);
        final submitting = loaded.toSubmitting();
        final submitted = submitting.toSubmitted();

        expect(loaded.isLoaded, true);
        expect(submitting.isSubmitting, true);
        expect(submitting.isLoaded, false);
        expect(submitted.isSubmitted, true);
        expect(submitted.isLoaded, true);
        expect(submitted.checkedItems[1], true);
        expect(submitted.checkedItems[2], true);
      });

      test('Loaded → Error 전환', () {
        final loaded = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories);
        final error = loaded.toError('네트워크 오류');

        expect(loaded.isLoaded, true);
        expect(error.isError, true);
        expect(error.isLoaded, false);
        expect(error.categories, TestData.testCategories);
      });

      test('Error → Loading (재시도) 전환', () {
        final error = SafetyCheckState.initial()
            .toLoaded(TestData.testCategories)
            .toError('네트워크 오류');
        final retrying = error.toLoading();

        expect(error.isError, true);
        expect(retrying.isLoading, true);
        expect(retrying.isError, false);
        expect(retrying.categories, TestData.testCategories);
      });

      test('복잡한 시나리오: Initial → Loading → Loaded → Toggle → Submitting → Error → Loading → Loaded → Submitted', () {
        var state = SafetyCheckState.initial();
        expect(state.isLoaded, false);

        // 첫 번째 로딩
        state = state.toLoading();
        expect(state.isLoading, true);

        // 데이터 로드 성공
        state = state.toLoaded(TestData.testCategories);
        expect(state.isLoaded, true);
        expect(state.checkedItems.length, 3);

        // 항목 체크
        state = state.toggleItem(1).toggleItem(2);
        expect(state.checkedItemIds, unorderedEquals([1, 2]));

        // 제출 시도
        state = state.toSubmitting();
        expect(state.isSubmitting, true);

        // 제출 실패
        state = state.toError('제출 실패');
        expect(state.isError, true);
        expect(state.checkedItems[1], true); // 체크 상태 유지

        // 재시도
        state = state.toLoading();
        expect(state.isLoading, true);
        expect(state.isError, false);

        // 재로딩 성공 (기존 체크는 초기화됨)
        state = state.toLoaded(TestData.testCategories);
        expect(state.isLoaded, true);
        expect(state.checkedItems[1], false); // 재로딩 시 초기화

        // 다시 체크
        state = state.toggleItem(1).toggleItem(2);

        // 재제출 성공
        state = state.toSubmitting().toSubmitted();
        expect(state.isSubmitted, true);
        expect(state.isLoaded, true);
      });
    });
  });
}
