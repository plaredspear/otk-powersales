import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/register_shelf_life_usecase.dart';

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
  late RegisterShelfLife useCase;

  setUp(() {
    repository = _MockShelfLifeRepository();
    useCase = RegisterShelfLife(repository);
  });

  group('RegisterShelfLife', () {
    test('유통기한을 성공적으로 등록한다', () async {
      // Given
      final form = ShelfLifeRegisterForm(
        storeId: 1025,
        productCode: '30310009',
        expiryDate: DateTime(2026, 12, 31),
        alertDate: DateTime(2026, 12, 1),
        description: '냉장 보관',
      );
      final expectedItem = _createTestItem(
        id: 1,
        storeId: 1025,
        productCode: '30310009',
      );
      repository.itemResult = expectedItem;

      // When
      final result = await useCase(form);

      // Then
      expect(result, expectedItem);
      expect(result.id, 1);
      expect(result.storeId, 1025);
      expect(result.productCode, '30310009');
    });

    test('필수 항목 미입력 시 예외를 발생시킨다 (storeId=0)', () async {
      // Given
      final form = ShelfLifeRegisterForm(
        storeId: 0, // 잘못된 storeId
        productCode: '30310009',
        expiryDate: DateTime(2026, 12, 31),
        alertDate: DateTime(2026, 12, 1),
        description: '',
      );

      // When & Then
      expect(
        () => useCase(form),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('필수 항목을 입력해주세요'),
        )),
      );
    });

    test('필수 항목 미입력 시 예외를 발생시킨다 (empty productCode)', () async {
      // Given
      final form = ShelfLifeRegisterForm(
        storeId: 1025,
        productCode: '', // 빈 productCode
        expiryDate: DateTime(2026, 12, 31),
        alertDate: DateTime(2026, 12, 1),
        description: '',
      );

      // When & Then
      expect(
        () => useCase(form),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('필수 항목을 입력해주세요'),
        )),
      );
    });

    test('Repository 에러를 전파한다', () async {
      // Given
      final form = ShelfLifeRegisterForm(
        storeId: 1025,
        productCode: '30310009',
        expiryDate: DateTime(2026, 12, 31),
        alertDate: DateTime(2026, 12, 1),
        description: '',
      );
      repository.error = Exception('서버 오류');

      // When & Then
      expect(
        () => useCase(form),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('서버 오류'),
        )),
      );
    });
  });
}
