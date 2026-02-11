import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/claim_code.dart';

void main() {
  group('ClaimDateType Enum', () {
    test('유통기한 타입이 올바르게 생성된다', () {
      // Given & When
      const dateType = ClaimDateType.expiryDate;

      // Then
      expect(dateType.code, 'EXPIRY_DATE');
      expect(dateType.displayName, '유통기한');
    });

    test('제조일자 타입이 올바르게 생성된다', () {
      // Given & When
      const dateType = ClaimDateType.manufactureDate;

      // Then
      expect(dateType.code, 'MANUFACTURE_DATE');
      expect(dateType.displayName, '제조일자');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const dateType = ClaimDateType.expiryDate;

      // When
      final json = dateType.toJson();

      // Then
      expect(json, 'EXPIRY_DATE');
    });

    test('fromJson이 올바르게 동작한다', () {
      // Given
      const json = 'MANUFACTURE_DATE';

      // When
      final dateType = ClaimDateType.fromJson(json);

      // Then
      expect(dateType, ClaimDateType.manufactureDate);
    });

    test('fromCode가 올바르게 동작한다', () {
      // Given
      const code = 'EXPIRY_DATE';

      // When
      final dateType = ClaimDateType.fromCode(code);

      // Then
      expect(dateType, ClaimDateType.expiryDate);
    });

    test('존재하지 않는 코드는 기본값(유통기한)을 반환한다', () {
      // Given
      const invalidCode = 'INVALID_CODE';

      // When
      final dateType = ClaimDateType.fromJson(invalidCode);

      // Then
      expect(dateType, ClaimDateType.expiryDate);
    });
  });

  group('PurchaseMethod Entity', () {
    test('엔티티가 올바르게 생성된다', () {
      // Given & When
      const purchaseMethod = PurchaseMethod(
        code: 'PM01',
        name: '대형마트',
      );

      // Then
      expect(purchaseMethod.code, 'PM01');
      expect(purchaseMethod.name, '대형마트');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const purchaseMethod = PurchaseMethod(
        code: 'PM02',
        name: '편의점',
      );

      // When
      final json = purchaseMethod.toJson();

      // Then
      expect(json, {
        'code': 'PM02',
        'name': '편의점',
      });
    });

    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'code': 'PM03',
        'name': '온라인',
      };

      // When
      final purchaseMethod = PurchaseMethod.fromJson(json);

      // Then
      expect(purchaseMethod.code, 'PM03');
      expect(purchaseMethod.name, '온라인');
    });

    test('toJson과 fromJson이 정확히 왕복 변환된다', () {
      // Given
      const original = PurchaseMethod(
        code: 'PM04',
        name: '슈퍼마켓',
      );

      // When
      final json = original.toJson();
      final restored = PurchaseMethod.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('copyWith가 올바르게 동작한다', () {
      // Given
      const original = PurchaseMethod(
        code: 'PM01',
        name: '대형마트',
      );

      // When
      final copied = original.copyWith(name: '대형할인마트');

      // Then
      expect(copied.code, 'PM01');
      expect(copied.name, '대형할인마트');
      expect(original.name, '대형마트'); // 원본 불변성 확인
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      const method1 = PurchaseMethod(code: 'PM01', name: '대형마트');
      const method2 = PurchaseMethod(code: 'PM01', name: '대형마트');

      // Then
      expect(method1, method2);
      expect(method1.hashCode, method2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      // Given
      const method1 = PurchaseMethod(code: 'PM01', name: '대형마트');
      const method2 = PurchaseMethod(code: 'PM02', name: '편의점');

      // Then
      expect(method1, isNot(method2));
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      const purchaseMethod = PurchaseMethod(code: 'PM01', name: '대형마트');

      // When
      final str = purchaseMethod.toString();

      // Then
      expect(str, 'PurchaseMethod(code: PM01, name: 대형마트)');
    });
  });

  group('ClaimRequestType Entity', () {
    test('엔티티가 올바르게 생성된다', () {
      // Given & When
      const requestType = ClaimRequestType(
        code: 'RT01',
        name: '교환',
      );

      // Then
      expect(requestType.code, 'RT01');
      expect(requestType.name, '교환');
    });

    test('toJson이 올바르게 동작한다', () {
      // Given
      const requestType = ClaimRequestType(
        code: 'RT02',
        name: '환불',
      );

      // When
      final json = requestType.toJson();

      // Then
      expect(json, {
        'code': 'RT02',
        'name': '환불',
      });
    });

    test('fromJson이 올바르게 동작한다', () {
      // Given
      final json = {
        'code': 'RT03',
        'name': '원인 규명',
      };

      // When
      final requestType = ClaimRequestType.fromJson(json);

      // Then
      expect(requestType.code, 'RT03');
      expect(requestType.name, '원인 규명');
    });

    test('toJson과 fromJson이 정확히 왕복 변환된다', () {
      // Given
      const original = ClaimRequestType(
        code: 'RT99',
        name: '기타',
      );

      // When
      final json = original.toJson();
      final restored = ClaimRequestType.fromJson(json);

      // Then
      expect(restored, original);
    });

    test('copyWith가 올바르게 동작한다', () {
      // Given
      const original = ClaimRequestType(
        code: 'RT01',
        name: '교환',
      );

      // When
      final copied = original.copyWith(name: '제품 교환');

      // Then
      expect(copied.code, 'RT01');
      expect(copied.name, '제품 교환');
      expect(original.name, '교환'); // 원본 불변성 확인
    });

    test('같은 값을 가진 엔티티는 동일하게 비교된다', () {
      // Given
      const type1 = ClaimRequestType(code: 'RT01', name: '교환');
      const type2 = ClaimRequestType(code: 'RT01', name: '교환');

      // Then
      expect(type1, type2);
      expect(type1.hashCode, type2.hashCode);
    });

    test('다른 값을 가진 엔티티는 다르게 비교된다', () {
      // Given
      const type1 = ClaimRequestType(code: 'RT01', name: '교환');
      const type2 = ClaimRequestType(code: 'RT02', name: '환불');

      // Then
      expect(type1, isNot(type2));
    });

    test('toString이 올바른 형식으로 출력된다', () {
      // Given
      const requestType = ClaimRequestType(code: 'RT01', name: '교환');

      // When
      final str = requestType.toString();

      // Then
      expect(str, 'ClaimRequestType(code: RT01, name: 교환)');
    });
  });
}
