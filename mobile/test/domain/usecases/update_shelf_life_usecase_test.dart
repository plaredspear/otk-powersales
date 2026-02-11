import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/update_shelf_life_usecase.dart';

class _MockShelfLifeRepository implements ShelfLifeRepository {
  List<ShelfLifeItem>? listResult;
  ShelfLifeItem? itemResult;
  int? deleteCount;
  Exception? error;

  @override
  Future<List<ShelfLifeItem>> getShelfLifeList(ShelfLifeFilter filter) async {
    if (error != null) throw error!;
    return listResult!;
  }

  @override
  Future<ShelfLifeItem> registerShelfLife(ShelfLifeRegisterForm form) async {
    if (error != null) throw error!;
    return itemResult!;
  }

  @override
  Future<ShelfLifeItem> updateShelfLife(int id, ShelfLifeUpdateForm form) async {
    if (error != null) throw error!;
    return itemResult!;
  }

  @override
  Future<void> deleteShelfLife(int id) async {
    if (error != null) throw error!;
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> ids) async {
    if (error != null) throw error!;
    return deleteCount!;
  }
}

ShelfLifeItem _createTestItem({
  int id = 1,
  String productCode = '30310009',
  String productName = '고등어김치&무조림(캔)280G',
  String storeName = '그린유통D',
  int storeId = 1025,
  DateTime? expiryDate,
  DateTime? alertDate,
  int dDay = 0,
  String description = '',
  bool isExpired = true,
}) {
  return ShelfLifeItem(
    id: id,
    productCode: productCode,
    productName: productName,
    storeName: storeName,
    storeId: storeId,
    expiryDate: expiryDate ?? DateTime(2026, 2, 15),
    alertDate: alertDate ?? DateTime(2026, 2, 14),
    dDay: dDay,
    description: description,
    isExpired: isExpired,
  );
}

void main() {
  late _MockShelfLifeRepository repository;
  late UpdateShelfLife useCase;

  setUp(() {
    repository = _MockShelfLifeRepository();
    useCase = UpdateShelfLife(repository);
  });

  group('UpdateShelfLife', () {
    test('유통기한을 성공적으로 수정한다', () async {
      // Given
      const itemId = 1;
      final form = ShelfLifeUpdateForm(
        expiryDate: DateTime(2027, 1, 31),
        alertDate: DateTime(2027, 1, 1),
        description: '수정된 설명',
      );
      final expectedItem = _createTestItem(
        id: itemId,
        expiryDate: DateTime(2027, 1, 31),
        alertDate: DateTime(2027, 1, 1),
        description: '수정된 설명',
      );
      repository.itemResult = expectedItem;

      // When
      final result = await useCase(itemId, form);

      // Then
      expect(result, expectedItem);
      expect(result.id, itemId);
      expect(result.expiryDate, DateTime(2027, 1, 31));
      expect(result.alertDate, DateTime(2027, 1, 1));
      expect(result.description, '수정된 설명');
    });

    test('ID가 0 이하일 때 예외를 발생시킨다', () async {
      // Given
      const invalidId = 0;
      final form = ShelfLifeUpdateForm(
        expiryDate: DateTime(2027, 1, 31),
        alertDate: DateTime(2027, 1, 1),
        description: '설명',
      );

      // When & Then
      expect(
        () => useCase(invalidId, form),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('유효하지 않은 유통기한 ID입니다'),
        )),
      );
    });

    test('Repository 에러를 전파한다', () async {
      // Given
      const itemId = 1;
      final form = ShelfLifeUpdateForm(
        expiryDate: DateTime(2027, 1, 31),
        alertDate: DateTime(2027, 1, 1),
        description: '설명',
      );
      repository.error = Exception('데이터베이스 오류');

      // When & Then
      expect(
        () => useCase(itemId, form),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('데이터베이스 오류'),
        )),
      );
    });
  });
}
