import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/shelf_life_register_request.dart';
import 'package:mobile/domain/entities/shelf_life_form.dart';

void main() {
  group('ShelfLifeRegisterRequest', () {
    const testRequest = ShelfLifeRegisterRequest(
      storeId: 100,
      productCode: 'P001',
      expiryDate: '2026-03-15',
      alertDate: '2026-03-08',
      description: '3층 선반',
    );

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        final result = testRequest.toJson();

        expect(result['store_id'], 100);
        expect(result['product_code'], 'P001');
        expect(result['expiry_date'], '2026-03-15');
        expect(result['alert_date'], '2026-03-08');
        expect(result['description'], '3층 선반');
      });

      test('description이 빈 문자열일 때도 직렬화해야 한다', () {
        const request = ShelfLifeRegisterRequest(
          storeId: 100,
          productCode: 'P001',
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
        );

        final result = request.toJson();

        expect(result['description'], '');
      });
    });

    group('fromForm', () {
      test('ShelfLifeRegisterForm에서 올바르게 생성해야 한다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 100,
          productCode: 'P001',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        final result = ShelfLifeRegisterRequest.fromForm(form);

        expect(result.storeId, 100);
        expect(result.productCode, 'P001');
        expect(result.expiryDate, '2026-03-15');
        expect(result.alertDate, '2026-03-08');
        expect(result.description, '3층 선반');
      });

      test('DateTime을 YYYY-MM-DD 형식으로 변환해야 한다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 1,
          productCode: 'P002',
          expiryDate: DateTime(2026, 1, 5),
          alertDate: DateTime(2025, 12, 29),
          description: '',
        );

        final result = ShelfLifeRegisterRequest.fromForm(form);

        expect(result.expiryDate, '2026-01-05');
        expect(result.alertDate, '2025-12-29');
      });

      test('description이 빈 문자열인 Form도 처리해야 한다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 1,
          productCode: 'P001',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
        );

        final result = ShelfLifeRegisterRequest.fromForm(form);

        expect(result.description, '');
      });
    });

    group('round trip', () {
      test('fromForm -> toJson 변환이 올바른 JSON을 생성해야 한다', () {
        final form = ShelfLifeRegisterForm(
          storeId: 100,
          productCode: 'P001',
          expiryDate: DateTime(2026, 3, 15),
          alertDate: DateTime(2026, 3, 8),
          description: '3층 선반',
        );

        final request = ShelfLifeRegisterRequest.fromForm(form);
        final json = request.toJson();

        expect(json, {
          'store_id': 100,
          'product_code': 'P001',
          'expiry_date': '2026-03-15',
          'alert_date': '2026-03-08',
          'description': '3층 선반',
        });
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 요청은 동일해야 한다', () {
        const request1 = ShelfLifeRegisterRequest(
          storeId: 100,
          productCode: 'P001',
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          description: '3층 선반',
        );
        const request2 = ShelfLifeRegisterRequest(
          storeId: 100,
          productCode: 'P001',
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          description: '3층 선반',
        );

        expect(request1, request2);
      });

      test('다른 값을 가진 두 요청은 동일하지 않아야 한다', () {
        const request2 = ShelfLifeRegisterRequest(
          storeId: 200,
          productCode: 'P002',
          expiryDate: '2026-04-01',
          alertDate: '2026-03-25',
        );

        expect(testRequest, isNot(request2));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 요청은 같은 hashCode를 가져야 한다', () {
        const request1 = ShelfLifeRegisterRequest(
          storeId: 100,
          productCode: 'P001',
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          description: '3층 선반',
        );
        const request2 = ShelfLifeRegisterRequest(
          storeId: 100,
          productCode: 'P001',
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
          description: '3층 선반',
        );

        expect(request1.hashCode, request2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        final result = testRequest.toString();

        expect(result, contains('ShelfLifeRegisterRequest'));
        expect(result, contains('storeId: 100'));
        expect(result, contains('productCode: P001'));
        expect(result, contains('expiryDate: 2026-03-15'));
        expect(result, contains('alertDate: 2026-03-08'));
        expect(result, contains('description: 3층 선반'));
      });
    });

    group('기본값', () {
      test('description 기본값은 빈 문자열이어야 한다', () {
        const request = ShelfLifeRegisterRequest(
          storeId: 100,
          productCode: 'P001',
          expiryDate: '2026-03-15',
          alertDate: '2026-03-08',
        );

        expect(request.description, '');
      });
    });
  });
}
