import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/get_shelf_life_list_usecase.dart';

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
  late GetShelfLifeList useCase;

  setUp(() {
    repository = _MockShelfLifeRepository();
    useCase = GetShelfLifeList(repository);
  });

  group('GetShelfLifeList', () {
    test('기본 필터로 유통기한 목록을 조회한다', () async {
      // Given
      final filter = ShelfLifeFilter(
        fromDate: DateTime(2026, 1, 1),
        toDate: DateTime(2026, 2, 28),
      );
      final expectedItems = [
        _createTestItem(id: 1, dDay: -5, isExpired: true),
        _createTestItem(id: 2, dDay: 10, isExpired: false),
      ];
      repository.listResult = expectedItems;

      // When
      final result = await useCase(filter);

      // Then
      expect(result, expectedItems);
      expect(result.length, 2);
    });

    test('빈 목록을 반환한다', () async {
      // Given
      final filter = ShelfLifeFilter(
        fromDate: DateTime(2026, 1, 1),
        toDate: DateTime(2026, 2, 28),
      );
      repository.listResult = [];

      // When
      final result = await useCase(filter);

      // Then
      expect(result, isEmpty);
    });

    test('검색 기간 초과 시 예외를 발생시킨다', () async {
      // Given
      final filter = ShelfLifeFilter(
        fromDate: DateTime(2026, 1, 1),
        toDate: DateTime(2026, 8, 1), // 7개월 초과
      );

      // When & Then
      expect(
        () => useCase(filter),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('유통기한 검색 기간은 최대 6개월입니다'),
        )),
      );
    });

    test('Repository 에러를 전파한다', () async {
      // Given
      final filter = ShelfLifeFilter(
        fromDate: DateTime(2026, 1, 1),
        toDate: DateTime(2026, 2, 28),
      );
      repository.error = Exception('네트워크 오류');

      // When & Then
      expect(
        () => useCase(filter),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('네트워크 오류'),
        )),
      );
    });

    test('groupByExpiry: 유통기한 지남/전 그룹으로 분리한다', () {
      // Given
      final items = [
        _createTestItem(id: 1, dDay: -5, isExpired: true),
        _createTestItem(id: 2, dDay: -3, isExpired: true),
        _createTestItem(id: 3, dDay: 10, isExpired: false),
        _createTestItem(id: 4, dDay: 5, isExpired: false),
      ];

      // When
      final result = GetShelfLifeList.groupByExpiry(items);

      // Then
      expect(result.keys, containsAll(['expired', 'notExpired']));
      expect(result['expired']!.length, 2);
      expect(result['notExpired']!.length, 2);
      expect(result['expired']!.every((item) => item.isExpired), isTrue);
      expect(result['notExpired']!.every((item) => !item.isExpired), isTrue);
    });

    test('groupByExpiry: 각 그룹 내 D-DAY 오름차순 정렬', () {
      // Given
      final items = [
        _createTestItem(id: 1, dDay: -3, isExpired: true),
        _createTestItem(id: 2, dDay: -10, isExpired: true),
        _createTestItem(id: 3, dDay: -5, isExpired: true),
        _createTestItem(id: 4, dDay: 15, isExpired: false),
        _createTestItem(id: 5, dDay: 5, isExpired: false),
        _createTestItem(id: 6, dDay: 10, isExpired: false),
      ];

      // When
      final result = GetShelfLifeList.groupByExpiry(items);

      // Then
      final expiredDDays = result['expired']!.map((e) => e.dDay).toList();
      final notExpiredDDays = result['notExpired']!.map((e) => e.dDay).toList();

      expect(expiredDDays, [-10, -5, -3]); // 오름차순
      expect(notExpiredDDays, [5, 10, 15]); // 오름차순
    });
  });
}
