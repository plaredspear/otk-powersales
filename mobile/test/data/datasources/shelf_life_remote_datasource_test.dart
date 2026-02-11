import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/shelf_life_remote_datasource.dart';

void main() {
  group('ShelfLifeBatchDeleteResponse', () {
    group('fromJson', () {
      test('data.deleted_count를 올바르게 파싱해야 한다', () {
        final json = {
          'data': {
            'deleted_count': 5,
          },
        };

        final result = ShelfLifeBatchDeleteResponse.fromJson(json);

        expect(result.deletedCount, 5);
      });

      test('삭제 건수가 0일 때도 올바르게 파싱해야 한다', () {
        final json = {
          'data': {
            'deleted_count': 0,
          },
        };

        final result = ShelfLifeBatchDeleteResponse.fromJson(json);

        expect(result.deletedCount, 0);
      });

      test('삭제 건수가 1일 때도 올바르게 파싱해야 한다', () {
        final json = {
          'data': {
            'deleted_count': 1,
          },
        };

        final result = ShelfLifeBatchDeleteResponse.fromJson(json);

        expect(result.deletedCount, 1);
      });
    });
  });
}
