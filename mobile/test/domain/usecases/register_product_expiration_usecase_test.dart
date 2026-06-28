import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/product_expiration_item.dart';
import 'package:mobile/domain/entities/product_expiration_form.dart';
import 'package:mobile/domain/repositories/product_expiration_repository.dart';
import 'package:mobile/domain/usecases/register_product_expiration_usecase.dart';

class _MockProductExpirationRepository implements ProductExpirationRepository {
  List<ProductExpirationItem>? listResult;
  ProductExpirationItem? itemResult;
  int? deleteCount;
  Exception? error;

  @override
  Future<List<ProductExpirationItem>> getProductExpirationList(ProductExpirationFilter filter) async {
    if (error != null) throw error!;
    return listResult!;
  }

  @override
  Future<ProductExpirationItem> registerProductExpiration(ProductExpirationRegisterForm form) async {
    if (error != null) throw error!;
    return itemResult!;
  }

  @override
  Future<ProductExpirationItem> updateProductExpiration(int seq, ProductExpirationUpdateForm form) async {
    if (error != null) throw error!;
    return itemResult!;
  }

  @override
  Future<void> deleteProductExpiration(int seq) async {
    if (error != null) throw error!;
  }

  @override
  Future<int> deleteProductExpirationBatch(List<int> seqs) async {
    if (error != null) throw error!;
    return deleteCount!;
  }
}

ProductExpirationItem _createTestItem({
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
  return ProductExpirationItem(
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
  late _MockProductExpirationRepository repository;
  late RegisterProductExpiration useCase;

  setUp(() {
    repository = _MockProductExpirationRepository();
    useCase = RegisterProductExpiration(repository);
  });

  group('RegisterProductExpiration', () {
    test('소비기한을 성공적으로 등록한다', () async {
      // Given
      final form = ProductExpirationRegisterForm(
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
      final form = ProductExpirationRegisterForm(
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
      final form = ProductExpirationRegisterForm(
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
      final form = ProductExpirationRegisterForm(
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
