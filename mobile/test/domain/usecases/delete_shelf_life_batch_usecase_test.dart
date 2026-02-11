import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/shelf_life_item.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';
import 'package:mobile/domain/repositories/shelf_life_repository.dart';
import 'package:mobile/domain/usecases/delete_shelf_life_usecase.dart';
import 'package:mobile/domain/usecases/delete_shelf_life_batch_usecase.dart';

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

void main() {
  late _MockShelfLifeRepository repository;

  setUp(() {
    repository = _MockShelfLifeRepository();
  });

  group('DeleteShelfLife', () {
    late DeleteShelfLife useCase;

    setUp(() {
      useCase = DeleteShelfLife(repository);
    });

    test('유통기한을 성공적으로 삭제한다', () async {
      // Given
      const itemId = 1;

      // When
      await useCase(itemId);

      // Then
      // 예외가 발생하지 않으면 성공
    });

    test('ID가 0 이하일 때 예외를 발생시킨다', () async {
      // Given
      const invalidId = 0;

      // When & Then
      expect(
        () => useCase(invalidId),
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
      repository.error = Exception('삭제 실패');

      // When & Then
      expect(
        () => useCase(itemId),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('삭제 실패'),
        )),
      );
    });
  });

  group('DeleteShelfLifeBatch', () {
    late DeleteShelfLifeBatch useCase;

    setUp(() {
      useCase = DeleteShelfLifeBatch(repository);
    });

    test('유통기한을 성공적으로 일괄 삭제한다', () async {
      // Given
      final ids = [1, 2, 3];
      repository.deleteCount = 3;

      // When
      final result = await useCase(ids);

      // Then
      expect(result, 3);
    });

    test('빈 ID 목록일 때 예외를 발생시킨다', () async {
      // Given
      final emptyIds = <int>[];

      // When & Then
      expect(
        () => useCase(emptyIds),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('삭제할 항목을 선택해주세요'),
        )),
      );
    });

    test('Repository 에러를 전파한다', () async {
      // Given
      final ids = [1, 2, 3];
      repository.error = Exception('일괄 삭제 실패');

      // When & Then
      expect(
        () => useCase(ids),
        throwsA(isA<Exception>().having(
          (e) => e.toString(),
          'message',
          contains('일괄 삭제 실패'),
        )),
      );
    });
  });
}
