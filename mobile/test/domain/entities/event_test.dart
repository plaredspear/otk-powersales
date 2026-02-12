import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/event.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  const testProduct1 = EventProduct(
    productCode: 'P001',
    productName: '진라면 매운맛',
    isMainProduct: true,
  );

  const testProduct2 = EventProduct(
    productCode: 'P002',
    productName: '진라면 순한맛',
    isMainProduct: false,
  );

  final testEvent = Event(
    id: 'EVT001',
    eventType: '[시식]',
    eventName: '상온(오뚜기카레_매운맛100G)',
    startDate: DateTime(2026, 2, 1),
    endDate: DateTime(2026, 2, 28),
    customerId: 'C001',
    customerName: '이마트 부산점',
    assigneeId: '20010585',
    mainProduct: testProduct1,
    subProducts: const [testProduct2],
  );

  group('EventProduct Entity 생성 테스트', () {
    test('EventProduct 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testProduct1.productCode, 'P001');
      expect(testProduct1.productName, '진라면 매운맛');
      expect(testProduct1.isMainProduct, true);
    });
  });

  group('EventProduct copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testProduct1.copyWith(
        productName: '진라면 짬뽕맛',
      );

      expect(updated.productCode, testProduct1.productCode);
      expect(updated.productName, '진라면 짬뽕맛');
      expect(updated.isMainProduct, testProduct1.isMainProduct);
    });

    test('모든 필드 변경', () {
      final updated = testProduct1.copyWith(
        productCode: 'P999',
        productName: '새로운 제품',
        isMainProduct: false,
      );

      expect(updated.productCode, 'P999');
      expect(updated.productName, '새로운 제품');
      expect(updated.isMainProduct, false);
    });
  });

  group('EventProduct toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testProduct1.toJson();

      expect(json['productCode'], 'P001');
      expect(json['productName'], '진라면 매운맛');
      expect(json['isMainProduct'], true);
    });

    test('fromJson 역직렬화', () {
      final json = {
        'productCode': 'P001',
        'productName': '진라면 매운맛',
        'isMainProduct': true,
      };

      final product = EventProduct.fromJson(json);

      expect(product.productCode, 'P001');
      expect(product.productName, '진라면 매운맛');
      expect(product.isMainProduct, true);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testProduct1.toJson();
      final product = EventProduct.fromJson(json);

      expect(product, testProduct1);
    });
  });

  group('EventProduct equality 테스트', () {
    test('같은 값을 가진 EventProduct는 같은 객체', () {
      const product1 = EventProduct(
        productCode: 'P001',
        productName: '진라면 매운맛',
        isMainProduct: true,
      );

      const product2 = EventProduct(
        productCode: 'P001',
        productName: '진라면 매운맛',
        isMainProduct: true,
      );

      expect(product1, product2);
    });

    test('다른 값을 가진 EventProduct는 다른 객체', () {
      const product1 = EventProduct(
        productCode: 'P001',
        productName: '진라면 매운맛',
        isMainProduct: true,
      );

      const product2 = EventProduct(
        productCode: 'P002',
        productName: '진라면 순한맛',
        isMainProduct: false,
      );

      expect(product1, isNot(product2));
    });
  });

  group('EventProduct hashCode 테스트', () {
    test('같은 값을 가진 EventProduct는 같은 hashCode', () {
      const product1 = EventProduct(
        productCode: 'P001',
        productName: '진라면 매운맛',
        isMainProduct: true,
      );

      const product2 = EventProduct(
        productCode: 'P001',
        productName: '진라면 매운맛',
        isMainProduct: true,
      );

      expect(product1.hashCode, product2.hashCode);
    });
  });

  group('EventProduct toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testProduct1.toString();

      expect(result, contains('EventProduct'));
      expect(result, contains('productCode: P001'));
      expect(result, contains('productName: 진라면 매운맛'));
      expect(result, contains('isMainProduct: true'));
    });
  });

  group('Event Entity 생성 테스트', () {
    test('Event 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testEvent.id, 'EVT001');
      expect(testEvent.eventType, '[시식]');
      expect(testEvent.eventName, '상온(오뚜기카레_매운맛100G)');
      expect(testEvent.startDate, DateTime(2026, 2, 1));
      expect(testEvent.endDate, DateTime(2026, 2, 28));
      expect(testEvent.customerId, 'C001');
      expect(testEvent.customerName, '이마트 부산점');
      expect(testEvent.assigneeId, '20010585');
      expect(testEvent.mainProduct, testProduct1);
      expect(testEvent.subProducts.length, 1);
      expect(testEvent.subProducts[0], testProduct2);
    });

    test('mainProduct가 null인 Event 생성', () {
      final event = Event(
        id: 'EVT002',
        eventType: '[판촉]',
        eventName: '테스트 행사',
        startDate: DateTime(2026, 3, 1),
        endDate: DateTime(2026, 3, 31),
        customerId: 'C002',
        customerName: '홈플러스',
        assigneeId: '20010586',
      );

      expect(event.mainProduct, null);
      expect(event.subProducts, isEmpty);
    });
  });

  group('Event copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updated = testEvent.copyWith(
        eventName: '신규 행사명',
        customerName: '롯데마트',
      );

      expect(updated.id, testEvent.id);
      expect(updated.eventType, testEvent.eventType);
      expect(updated.eventName, '신규 행사명');
      expect(updated.startDate, testEvent.startDate);
      expect(updated.endDate, testEvent.endDate);
      expect(updated.customerId, testEvent.customerId);
      expect(updated.customerName, '롯데마트');
      expect(updated.assigneeId, testEvent.assigneeId);
      expect(updated.mainProduct, testEvent.mainProduct);
      expect(updated.subProducts, testEvent.subProducts);
    });

    test('날짜 필드 변경', () {
      final newStartDate = DateTime(2026, 3, 1);
      final newEndDate = DateTime(2026, 3, 31);

      final updated = testEvent.copyWith(
        startDate: newStartDate,
        endDate: newEndDate,
      );

      expect(updated.startDate, newStartDate);
      expect(updated.endDate, newEndDate);
      expect(updated.id, testEvent.id);
    });

    test('제품 목록 변경', () {
      const newProduct = EventProduct(
        productCode: 'P003',
        productName: '새 제품',
        isMainProduct: false,
      );

      final updated = testEvent.copyWith(
        mainProduct: newProduct,
        subProducts: const [],
      );

      expect(updated.mainProduct, newProduct);
      expect(updated.subProducts, isEmpty);
    });
  });

  group('Event toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testEvent.toJson();

      expect(json['id'], 'EVT001');
      expect(json['eventType'], '[시식]');
      expect(json['eventName'], '상온(오뚜기카레_매운맛100G)');
      expect(json['startDate'], '2026-02-01T00:00:00.000');
      expect(json['endDate'], '2026-02-28T00:00:00.000');
      expect(json['customerId'], 'C001');
      expect(json['customerName'], '이마트 부산점');
      expect(json['assigneeId'], '20010585');
      expect(json['mainProduct'], isA<Map<String, dynamic>>());
      expect(json['subProducts'], isA<List>());
      expect(json['subProducts'].length, 1);
    });

    test('fromJson 역직렬화', () {
      final json = {
        'id': 'EVT001',
        'eventType': '[시식]',
        'eventName': '상온(오뚜기카레_매운맛100G)',
        'startDate': '2026-02-01T00:00:00.000',
        'endDate': '2026-02-28T00:00:00.000',
        'customerId': 'C001',
        'customerName': '이마트 부산점',
        'assigneeId': '20010585',
        'mainProduct': {
          'productCode': 'P001',
          'productName': '진라면 매운맛',
          'isMainProduct': true,
        },
        'subProducts': [
          {
            'productCode': 'P002',
            'productName': '진라면 순한맛',
            'isMainProduct': false,
          }
        ],
      };

      final event = Event.fromJson(json);

      expect(event.id, 'EVT001');
      expect(event.eventType, '[시식]');
      expect(event.eventName, '상온(오뚜기카레_매운맛100G)');
      expect(event.startDate, DateTime(2026, 2, 1));
      expect(event.endDate, DateTime(2026, 2, 28));
      expect(event.customerId, 'C001');
      expect(event.customerName, '이마트 부산점');
      expect(event.assigneeId, '20010585');
      expect(event.mainProduct?.productCode, 'P001');
      expect(event.subProducts.length, 1);
      expect(event.subProducts[0].productCode, 'P002');
    });

    test('fromJson null mainProduct 처리', () {
      final json = {
        'id': 'EVT002',
        'eventType': '[판촉]',
        'eventName': '테스트 행사',
        'startDate': '2026-03-01T00:00:00.000',
        'endDate': '2026-03-31T00:00:00.000',
        'customerId': 'C002',
        'customerName': '홈플러스',
        'assigneeId': '20010586',
        'mainProduct': null,
        'subProducts': null,
      };

      final event = Event.fromJson(json);

      expect(event.mainProduct, null);
      expect(event.subProducts, isEmpty);
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testEvent.toJson();
      final event = Event.fromJson(json);

      expect(event, testEvent);
    });
  });

  group('Event equality 테스트', () {
    test('같은 값을 가진 Event는 같은 객체', () {
      final event1 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '상온(오뚜기카레_매운맛100G)',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트 부산점',
        assigneeId: '20010585',
        mainProduct: testProduct1,
        subProducts: const [testProduct2],
      );

      final event2 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '상온(오뚜기카레_매운맛100G)',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트 부산점',
        assigneeId: '20010585',
        mainProduct: testProduct1,
        subProducts: const [testProduct2],
      );

      expect(event1, event2);
    });

    test('다른 값을 가진 Event는 다른 객체', () {
      final event1 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '행사1',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트',
        assigneeId: '20010585',
      );

      final event2 = Event(
        id: 'EVT002',
        eventType: '[판촉]',
        eventName: '행사2',
        startDate: DateTime(2026, 3, 1),
        endDate: DateTime(2026, 3, 31),
        customerId: 'C002',
        customerName: '홈플러스',
        assigneeId: '20010586',
      );

      expect(event1, isNot(event2));
    });

    test('제품 목록이 다른 Event는 다른 객체', () {
      final event1 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '행사1',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트',
        assigneeId: '20010585',
        subProducts: const [testProduct1],
      );

      final event2 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '행사1',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트',
        assigneeId: '20010585',
        subProducts: const [testProduct2],
      );

      expect(event1, isNot(event2));
    });
  });

  group('Event hashCode 테스트', () {
    test('같은 값을 가진 Event는 같은 hashCode', () {
      final event1 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '상온(오뚜기카레_매운맛100G)',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트 부산점',
        assigneeId: '20010585',
        mainProduct: testProduct1,
        subProducts: const [testProduct2],
      );

      final event2 = Event(
        id: 'EVT001',
        eventType: '[시식]',
        eventName: '상온(오뚜기카레_매운맛100G)',
        startDate: DateTime(2026, 2, 1),
        endDate: DateTime(2026, 2, 28),
        customerId: 'C001',
        customerName: '이마트 부산점',
        assigneeId: '20010585',
        mainProduct: testProduct1,
        subProducts: const [testProduct2],
      );

      expect(event1.hashCode, event2.hashCode);
    });
  });

  group('Event toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testEvent.toString();

      expect(result, contains('Event'));
      expect(result, contains('id: EVT001'));
      expect(result, contains('eventType: [시식]'));
      expect(result, contains('eventName: 상온(오뚜기카레_매운맛100G)'));
      expect(result, contains('customerId: C001'));
      expect(result, contains('customerName: 이마트 부산점'));
      expect(result, contains('assigneeId: 20010585'));
    });
  });
}
