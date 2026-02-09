import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/favorite_product.dart';

void main() {
  group('FavoriteProduct 엔티티', () {
    test('엔티티가 올바르게 생성된다', () {
      final now = DateTime(2026, 2, 1, 12, 0);
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: now,
      );

      expect(favorite.id, 'prod_001');
      expect(favorite.productName, '진라면');
      expect(favorite.addedAt, now);
    });

    test('copyWith 메서드가 올바르게 동작한다', () {
      final original = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime(2026, 2, 1),
      );

      final copied = original.copyWith(productName: '진라면 순한맛');

      expect(copied.id, original.id);
      expect(copied.productName, '진라면 순한맛');
      expect(copied.addedAt, original.addedAt);
    });

    test('toJson이 올바르게 동작한다', () {
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime(2026, 2, 1, 12, 30),
      );

      final json = favorite.toJson();

      expect(json['id'], 'prod_001');
      expect(json['productName'], '진라면');
      expect(json['addedAt'], '2026-02-01T12:30:00.000');
    });

    test('fromJson이 올바르게 동작한다', () {
      final json = {
        'id': 'prod_001',
        'productName': '진라면',
        'addedAt': '2026-02-01T12:30:00.000',
      };

      final favorite = FavoriteProduct.fromJson(json);

      expect(favorite.id, 'prod_001');
      expect(favorite.productName, '진라면');
      expect(favorite.addedAt, DateTime(2026, 2, 1, 12, 30));
    });

    test('toJson과 fromJson이 정확히 동작한다 (왕복 테스트)', () {
      final original = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime(2026, 2, 1, 12, 30),
      );

      final json = original.toJson();
      final restored = FavoriteProduct.fromJson(json);

      expect(restored, original);
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      final now = DateTime(2026, 2, 1, 12, 0);
      final favorite1 = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: now,
      );
      final favorite2 = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: now,
      );

      expect(favorite1, favorite2);
      expect(favorite1.hashCode, favorite2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      final now = DateTime(2026, 2, 1, 12, 0);
      final favorite1 = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: now,
      );
      final favorite2 = FavoriteProduct(
        id: 'prod_002',
        productName: '진라면 매운맛',
        addedAt: now,
      );

      expect(favorite1, isNot(favorite2));
    });

    test('toString이 올바른 형식으로 출력된다', () {
      final favorite = FavoriteProduct(
        id: 'prod_001',
        productName: '진라면',
        addedAt: DateTime(2026, 2, 1, 12, 0),
      );

      final str = favorite.toString();

      expect(str, contains('FavoriteProduct'));
      expect(str, contains('prod_001'));
      expect(str, contains('진라면'));
    });
  });
}
