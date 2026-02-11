import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/delete_shelf_life_usecase.dart';
import 'package:mobile/domain/usecases/register_shelf_life_usecase.dart';
import 'package:mobile/domain/usecases/update_shelf_life_usecase.dart';
import 'package:mobile/presentation/providers/shelf_life_form_provider.dart';

void main() {
  group('ShelfLifeFormNotifier', () {
    late ShelfLifeFormNotifier notifier;
    late FakeShelfLifeRepository fakeRepository;
    late RegisterShelfLife registerUseCase;
    late UpdateShelfLife updateUseCase;
    late DeleteShelfLife deleteUseCase;

    setUp(() {
      fakeRepository = FakeShelfLifeRepository();
      registerUseCase = RegisterShelfLife(fakeRepository);
      updateUseCase = UpdateShelfLife(fakeRepository);
      deleteUseCase = DeleteShelfLife(fakeRepository);
      notifier = ShelfLifeFormNotifier(
        registerShelfLife: registerUseCase,
        updateShelfLife: updateUseCase,
        deleteShelfLife: deleteUseCase,
      );
    });

    test('초기 상태가 올바르게 설정되어야 한다', () {
      expect(notifier.state.isLoading, false);
      expect(notifier.state.isRegisterMode, true);
      expect(notifier.state.selectedStoreId, isNull);
      expect(notifier.state.selectedProductCode, isNull);
      expect(notifier.state.isSaved, false);
      expect(notifier.state.isDeleted, false);
    });

    group('initializeForEdit', () {
      test('수정 모드로 초기화하면 기존 데이터가 로드되어야 한다', () {
        notifier.initializeForEdit(_sampleItem);

        expect(notifier.state.isEditMode, true);
        expect(notifier.state.editId, 1);
        expect(notifier.state.selectedStoreId, 100);
        expect(notifier.state.selectedStoreName, '이마트');
        expect(notifier.state.selectedProductCode, 'P001');
        expect(notifier.state.selectedProductName, '진라면');
        expect(notifier.state.expiryDate, DateTime(2026, 3, 15));
        expect(notifier.state.alertDate, DateTime(2026, 3, 14));
        expect(notifier.state.description, '3층 선반');
      });
    });

    group('selectStore', () {
      test('거래처를 선택하면 state에 반영되어야 한다', () {
        notifier.selectStore(100, '이마트');

        expect(notifier.state.selectedStoreId, 100);
        expect(notifier.state.selectedStoreName, '이마트');
      });
    });

    group('selectProduct', () {
      test('제품을 선택하면 state에 반영되어야 한다', () {
        notifier.selectProduct('P001', '진라면');

        expect(notifier.state.selectedProductCode, 'P001');
        expect(notifier.state.selectedProductName, '진라면');
      });
    });

    group('updateExpiryDate', () {
      test('유통기한을 변경하면 알림일도 자동 변경되어야 한다', () {
        final newExpiry = DateTime(2026, 5, 10);

        notifier.updateExpiryDate(newExpiry);

        expect(notifier.state.expiryDate, DateTime(2026, 5, 10));
        expect(notifier.state.alertDate, DateTime(2026, 5, 9));
      });
    });

    group('updateAlertDate', () {
      test('알림일을 독립적으로 변경할 수 있어야 한다', () {
        final newAlert = DateTime(2026, 4, 1);

        notifier.updateAlertDate(newAlert);

        expect(notifier.state.alertDate, DateTime(2026, 4, 1));
      });
    });

    group('updateDescription', () {
      test('설명을 변경할 수 있어야 한다', () {
        notifier.updateDescription('2층 냉장고');

        expect(notifier.state.description, '2층 냉장고');
      });
    });

    group('register', () {
      test('등록 성공 시 isSaved가 true여야 한다', () async {
        fakeRepository.registerResult = _sampleItem;
        notifier.selectStore(100, '이마트');
        notifier.selectProduct('P001', '진라면');

        await notifier.register();

        expect(notifier.state.isSaved, true);
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, isNull);
      });

      test('유효하지 않으면 등록하지 않아야 한다', () async {
        // 거래처/제품 미선택
        await notifier.register();

        expect(notifier.state.isSaved, false);
        expect(fakeRepository.registerCalls, 0);
      });

      test('등록 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('이미 등록된 유통기한입니다');
        notifier.selectStore(100, '이마트');
        notifier.selectProduct('P001', '진라면');

        await notifier.register();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '이미 등록된 유통기한입니다');
        expect(notifier.state.isSaved, false);
      });

      test('등록 시 올바른 폼 데이터가 전달되어야 한다', () async {
        fakeRepository.registerResult = _sampleItem;
        notifier.selectStore(100, '이마트');
        notifier.selectProduct('P001', '진라면');
        notifier.updateDescription('3층 선반');

        await notifier.register();

        expect(fakeRepository.lastRegisterForm, isNotNull);
        expect(fakeRepository.lastRegisterForm!.storeId, 100);
        expect(fakeRepository.lastRegisterForm!.productCode, 'P001');
        expect(fakeRepository.lastRegisterForm!.description, '3층 선반');
      });
    });

    group('update', () {
      test('수정 성공 시 isSaved가 true여야 한다', () async {
        fakeRepository.updateResult = _sampleItem;
        notifier.initializeForEdit(_sampleItem);

        await notifier.update();

        expect(notifier.state.isSaved, true);
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, isNull);
      });

      test('editId가 없으면 수정하지 않아야 한다', () async {
        // 등록 모드 (editId = null)
        await notifier.update();

        expect(notifier.state.isSaved, false);
        expect(fakeRepository.updateCalls, 0);
      });

      test('수정 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('수정 실패');
        notifier.initializeForEdit(_sampleItem);

        await notifier.update();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '수정 실패');
        expect(notifier.state.isSaved, false);
      });

      test('수정 시 올바른 폼 데이터가 전달되어야 한다', () async {
        fakeRepository.updateResult = _sampleItem;
        notifier.initializeForEdit(_sampleItem);
        notifier.updateExpiryDate(DateTime(2026, 4, 1));
        notifier.updateDescription('수정된 설명');

        await notifier.update();

        expect(fakeRepository.lastUpdateId, 1);
        expect(fakeRepository.lastUpdateForm, isNotNull);
        expect(
          fakeRepository.lastUpdateForm!.expiryDate,
          DateTime(2026, 4, 1),
        );
        expect(
          fakeRepository.lastUpdateForm!.alertDate,
          DateTime(2026, 3, 31),
        );
        expect(fakeRepository.lastUpdateForm!.description, '수정된 설명');
      });
    });

    group('delete', () {
      test('삭제 성공 시 isDeleted가 true여야 한다', () async {
        notifier.initializeForEdit(_sampleItem);

        await notifier.delete();

        expect(notifier.state.isDeleted, true);
        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, isNull);
      });

      test('editId가 없으면 삭제하지 않아야 한다', () async {
        // 등록 모드 (editId = null)
        await notifier.delete();

        expect(notifier.state.isDeleted, false);
        expect(fakeRepository.deleteCalls, 0);
      });

      test('삭제 실패 시 에러 메시지를 설정해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('삭제 실패');
        notifier.initializeForEdit(_sampleItem);

        await notifier.delete();

        expect(notifier.state.isLoading, false);
        expect(notifier.state.errorMessage, '삭제 실패');
        expect(notifier.state.isDeleted, false);
      });

      test('삭제 시 올바른 ID가 전달되어야 한다', () async {
        notifier.initializeForEdit(_sampleItem);

        await notifier.delete();

        expect(fakeRepository.lastDeleteId, 1);
      });
    });

    group('clearError', () {
      test('에러 메시지를 초기화해야 한다', () async {
        fakeRepository.exceptionToThrow = Exception('에러');
        notifier.selectStore(100, '이마트');
        notifier.selectProduct('P001', '진라면');
        await notifier.register();

        expect(notifier.state.errorMessage, isNotNull);

        notifier.clearError();

        expect(notifier.state.errorMessage, isNull);
      });
    });
  });
}

// ──────────────────────────────────────────────────────────────────
// Fake Repository
// ──────────────────────────────────────────────────────────────────

class FakeShelfLifeRepository implements ShelfLifeRepository {
  ShelfLifeItem? registerResult;
  ShelfLifeItem? updateResult;
  int registerCalls = 0;
  int updateCalls = 0;
  int deleteCalls = 0;
  ShelfLifeRegisterForm? lastRegisterForm;
  int? lastUpdateId;
  ShelfLifeUpdateForm? lastUpdateForm;
  int? lastDeleteId;
  Exception? exceptionToThrow;

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    return [];
  }

  @override
  Future<ShelfLifeItem> registerShelfLife(dynamic form) async {
    registerCalls++;
    lastRegisterForm = form as ShelfLifeRegisterForm;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return registerResult!;
  }

  @override
  Future<ShelfLifeItem> updateShelfLife(int id, dynamic form) async {
    updateCalls++;
    lastUpdateId = id;
    lastUpdateForm = form as ShelfLifeUpdateForm;
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return updateResult!;
  }

  @override
  Future<void> deleteShelfLife(int id) async {
    deleteCalls++;
    lastDeleteId = id;
    if (exceptionToThrow != null) throw exceptionToThrow!;
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> ids) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return ids.length;
  }
}

// ──────────────────────────────────────────────────────────────────
// Sample test data
// ──────────────────────────────────────────────────────────────────

final _sampleItem = ShelfLifeItem(
  id: 1,
  productCode: 'P001',
  productName: '진라면',
  storeName: '이마트',
  storeId: 100,
  expiryDate: DateTime(2026, 3, 15),
  alertDate: DateTime(2026, 3, 14),
  dDay: 5,
  description: '3층 선반',
  isExpired: false,
);
