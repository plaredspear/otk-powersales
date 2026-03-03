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
  Future<ShelfLifeItem> updateShelfLife(int seq, ShelfLifeUpdateForm form) async {
    if (error != null) throw error!;
    return itemResult!;
  }

  @override
  Future<void> deleteShelfLife(int seq) async {
    if (error != null) throw error!;
  }

  @override
  Future<int> deleteShelfLifeBatch(List<int> seqs) async {
    if (error != null) throw error!;
    return deleteCount!;
  }
}

ShelfLifeItem _createTestItem({
  int seq = 1,
  String productCode = '30310009',
  String productName = '고등어김치&무조림(캔)280G',
  String accountName = '그린유통D',
  String accountCode = 'ACC1025',
  DateTime? expiryDate,
  DateTime? alertDate,
  int dDay = 0,
  String description = '',
  bool isExpired = true,
}) {
  return ShelfLifeItem(
    seq: seq,
    productCode: productCode,
    productName: productName,
    accountName: accountName,
    accountCode: accountCode,
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
        accountCode: 'ACC1025',
        accountName: '그린유통D',
        productCode: '30310009',
        productName: '고등어김치&무조림(캔)280G',
        expiryDate: DateTime(2026, 12, 31),
        alertDate: DateTime(2026, 12, 1),
        description: '냉장 보관',
      );
      final expectedItem = _createTestItem(
        seq: 1,
        accountCode: 'ACC1025',
        productCode: '30310009',
      );
      repository.itemResult = expectedItem;

      // When
      final result = await useCase(form);

      // Then
      expect(result, expectedItem);
      expect(result.seq, 1);
      expect(result.accountCode, 'ACC1025');
      expect(result.productCode, '30310009');
    });

    test('필수 항목 미입력 시 예외를 발생시킨다 (empty accountCode)', () async {
      // Given
      final form = ShelfLifeRegisterForm(
        accountCode: '', // 빈 accountCode
        accountName: '',
        productCode: '30310009',
        productName: '고등어김치&무조림(캔)280G',
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
        accountCode: 'ACC1025',
        accountName: '그린유통D',
        productCode: '', // 빈 productCode
        productName: '',
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
        accountCode: 'ACC1025',
        accountName: '그린유통D',
        productCode: '30310009',
        productName: '고등어김치&무조림(캔)280G',
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
