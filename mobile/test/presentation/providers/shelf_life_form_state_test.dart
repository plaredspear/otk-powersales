import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/presentation/providers/shelf_life_form_state.dart';

void main() {
  group('ShelfLifeFormState', () {
    group('initial', () {
      test('기본값이 올바르게 설정되어야 한다', () {
        final state = ShelfLifeFormState.initial();

        expect(state.isLoading, false);
        expect(state.errorMessage, isNull);
        expect(state.selectedStoreId, isNull);
        expect(state.selectedStoreName, isNull);
        expect(state.selectedProductCode, isNull);
        expect(state.selectedProductName, isNull);
        expect(state.description, '');
        expect(state.editId, isNull);
        expect(state.isSaved, false);
        expect(state.isDeleted, false);
        expect(state.stores, isEmpty);
      });

      test('기본 유통기한은 오늘+2일이어야 한다', () {
        final state = ShelfLifeFormState.initial();
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final expectedExpiry = today.add(const Duration(days: 2));

        expect(state.expiryDate, expectedExpiry);
      });

      test('기본 알림일은 유통기한-1일이어야 한다', () {
        final state = ShelfLifeFormState.initial();
        final now = DateTime.now();
        final today = DateTime(now.year, now.month, now.day);
        final expectedAlert = today.add(const Duration(days: 1));

        expect(state.alertDate, expectedAlert);
      });
    });

    group('computed getters', () {
      test('isRegisterMode는 editId가 null이면 true여야 한다', () {
        final state = ShelfLifeFormState.initial();

        expect(state.isRegisterMode, true);
        expect(state.isEditMode, false);
      });

      test('isEditMode는 editId가 있으면 true여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(editId: 1);

        expect(state.isEditMode, true);
        expect(state.isRegisterMode, false);
      });

      test('hasStore는 거래처가 선택되었을 때 true여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedStoreId: 100,
        );

        expect(state.hasStore, true);
      });

      test('hasStore는 거래처가 없을 때 false여야 한다', () {
        final state = ShelfLifeFormState.initial();

        expect(state.hasStore, false);
      });

      test('hasProduct는 제품이 선택되었을 때 true여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedProductCode: 'P001',
        );

        expect(state.hasProduct, true);
      });

      test('hasProduct는 제품이 없을 때 false여야 한다', () {
        final state = ShelfLifeFormState.initial();

        expect(state.hasProduct, false);
      });

      test('isValid는 등록 모드에서 거래처+제품이 있으면 true여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedStoreId: 100,
          selectedProductCode: 'P001',
        );

        expect(state.isValid, true);
      });

      test('isValid는 등록 모드에서 거래처만 있으면 false여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedStoreId: 100,
        );

        expect(state.isValid, false);
      });

      test('isValid는 등록 모드에서 제품만 있으면 false여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedProductCode: 'P001',
        );

        expect(state.isValid, false);
      });

      test('isValid는 수정 모드에서 항상 true여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(editId: 1);

        expect(state.isValid, true);
      });

      test('canSave는 유효하고 로딩 중이 아닐 때 true여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedStoreId: 100,
          selectedProductCode: 'P001',
        );

        expect(state.canSave, true);
      });

      test('canSave는 로딩 중이면 false여야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedStoreId: 100,
          selectedProductCode: 'P001',
          isLoading: true,
        );

        expect(state.canSave, false);
      });

      test('canSave는 유효하지 않으면 false여야 한다', () {
        final state = ShelfLifeFormState.initial();

        expect(state.canSave, false);
      });
    });

    group('toLoading', () {
      test('로딩 상태로 전환하고 에러를 초기화해야 한다', () {
        final state = ShelfLifeFormState.initial()
            .copyWith(errorMessage: '이전 에러');

        final result = state.toLoading();

        expect(result.isLoading, true);
        expect(result.errorMessage, isNull);
      });
    });

    group('toError', () {
      test('에러 상태로 전환하고 로딩을 해제해야 한다', () {
        final state = ShelfLifeFormState.initial().toLoading();

        final result = state.toError('등록 실패');

        expect(result.isLoading, false);
        expect(result.errorMessage, '등록 실패');
      });
    });

    group('copyWith', () {
      test('거래처 정보를 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();

        final result = state.copyWith(
          selectedStoreId: 100,
          selectedStoreName: '이마트',
        );

        expect(result.selectedStoreId, 100);
        expect(result.selectedStoreName, '이마트');
      });

      test('clearStore가 true이면 거래처를 초기화해야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedStoreId: 100,
          selectedStoreName: '이마트',
        );

        final result = state.copyWith(clearStore: true);

        expect(result.selectedStoreId, isNull);
        expect(result.selectedStoreName, isNull);
      });

      test('제품 정보를 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();

        final result = state.copyWith(
          selectedProductCode: 'P001',
          selectedProductName: '진라면',
        );

        expect(result.selectedProductCode, 'P001');
        expect(result.selectedProductName, '진라면');
      });

      test('clearProduct가 true이면 제품을 초기화해야 한다', () {
        final state = ShelfLifeFormState.initial().copyWith(
          selectedProductCode: 'P001',
          selectedProductName: '진라면',
        );

        final result = state.copyWith(clearProduct: true);

        expect(result.selectedProductCode, isNull);
        expect(result.selectedProductName, isNull);
      });

      test('clearError가 true이면 에러를 초기화해야 한다', () {
        final state = ShelfLifeFormState.initial()
            .copyWith(errorMessage: '에러');

        final result = state.copyWith(clearError: true);

        expect(result.errorMessage, isNull);
      });

      test('날짜 정보를 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();
        final newExpiry = DateTime(2026, 5, 1);
        final newAlert = DateTime(2026, 4, 30);

        final result = state.copyWith(
          expiryDate: newExpiry,
          alertDate: newAlert,
        );

        expect(result.expiryDate, newExpiry);
        expect(result.alertDate, newAlert);
      });

      test('설명을 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();

        final result = state.copyWith(description: '3층 선반');

        expect(result.description, '3층 선반');
      });

      test('isSaved를 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();

        final result = state.copyWith(isSaved: true);

        expect(result.isSaved, true);
      });

      test('isDeleted를 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();

        final result = state.copyWith(isDeleted: true);

        expect(result.isDeleted, true);
      });

      test('stores를 업데이트해야 한다', () {
        final state = ShelfLifeFormState.initial();

        final result = state.copyWith(stores: {100: '이마트', 200: '롯데마트'});

        expect(result.stores.length, 2);
        expect(result.stores[100], '이마트');
      });
    });
  });
}
