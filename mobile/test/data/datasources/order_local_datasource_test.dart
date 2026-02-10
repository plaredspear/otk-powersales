import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:hive/hive.dart';
import 'package:mobile/data/datasources/order_local_datasource.dart';

void main() {
  late OrderLocalDataSource dataSource;
  late Directory tempDir;

  setUp(() async {
    tempDir = await Directory.systemTemp.createTemp('hive_test_');
    Hive.init(tempDir.path);
    dataSource = OrderLocalDataSource();
  });

  tearDown(() async {
    await Hive.close();
    if (tempDir.existsSync()) {
      tempDir.deleteSync(recursive: true);
    }
  });

  group('OrderLocalDataSource', () {
    test('임시저장 후 불러오기: JSON 데이터를 저장하고 불러오면 내용이 일치한다', () async {
      // Given: 임시저장할 주문서 데이터
      final draftData = {
        'client_id': 100,
        'client_name': '테스트거래처',
        'delivery_date': '2026-03-01',
        'items': [
          {
            'product_code': '01234567',
            'product_name': '오뚜기 진라면',
            'quantity_boxes': 5.0,
            'quantity_pieces': 10,
            'unit_price': 1200,
            'box_size': 20,
            'total_price': 72000,
            'is_selected': false,
          }
        ],
        'total_amount': 72000,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00.000Z',
      };

      // When: 임시저장하고 불러온다
      await dataSource.saveDraft(draftData);
      final loaded = await dataSource.loadDraft();

      // Then: 저장된 데이터와 불러온 데이터가 일치한다
      expect(loaded, isNotNull);
      expect(loaded, equals(draftData));
      expect(loaded!['client_id'], 100);
      expect(loaded['client_name'], '테스트거래처');
      expect(loaded['items'], isList);
      expect((loaded['items'] as List).length, 1);
    });

    test('임시저장이 없을 때 불러오기: null을 반환한다', () async {
      // When: 임시저장 없이 불러온다
      final loaded = await dataSource.loadDraft();

      // Then: null이 반환된다
      expect(loaded, isNull);
    });

    test('임시저장 삭제: 저장된 주문서를 삭제한다', () async {
      // Given: 임시저장된 주문서가 있다
      final draftData = {
        'client_id': 100,
        'items': [],
        'total_amount': 0,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00.000Z',
      };
      await dataSource.saveDraft(draftData);

      // When: 임시저장을 삭제한다
      await dataSource.deleteDraft();

      // Then: 불러올 수 없다
      final loaded = await dataSource.loadDraft();
      expect(loaded, isNull);
    });

    test('임시저장 존재 여부: 저장 전에는 false를 반환한다', () async {
      // When: 임시저장 없이 존재 여부를 확인한다
      final hasDraft = await dataSource.hasDraft();

      // Then: false를 반환한다
      expect(hasDraft, isFalse);
    });

    test('임시저장 존재 여부: 저장 후에는 true를 반환한다', () async {
      // Given: 임시저장된 주문서가 있다
      final draftData = {
        'client_id': 100,
        'items': [],
        'total_amount': 0,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00.000Z',
      };
      await dataSource.saveDraft(draftData);

      // When: 존재 여부를 확인한다
      final hasDraft = await dataSource.hasDraft();

      // Then: true를 반환한다
      expect(hasDraft, isTrue);
    });

    test('임시저장 존재 여부: 삭제 후에는 false를 반환한다', () async {
      // Given: 임시저장된 주문서가 있다
      final draftData = {
        'client_id': 100,
        'items': [],
        'total_amount': 0,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00.000Z',
      };
      await dataSource.saveDraft(draftData);

      // When: 임시저장을 삭제한다
      await dataSource.deleteDraft();

      // Then: 존재하지 않는다
      final hasDraft = await dataSource.hasDraft();
      expect(hasDraft, isFalse);
    });

    test('임시저장 덮어쓰기: 새로운 임시저장이 이전 데이터를 덮어쓴다', () async {
      // Given: 첫 번째 임시저장
      final firstDraft = {
        'client_id': 100,
        'client_name': '첫번째거래처',
        'items': [],
        'total_amount': 10000,
        'is_draft': true,
        'last_modified': '2026-02-10T10:00:00.000Z',
      };
      await dataSource.saveDraft(firstDraft);

      // When: 두 번째 임시저장으로 덮어쓴다
      final secondDraft = {
        'client_id': 200,
        'client_name': '두번째거래처',
        'items': [],
        'total_amount': 20000,
        'is_draft': true,
        'last_modified': '2026-02-10T11:00:00.000Z',
      };
      await dataSource.saveDraft(secondDraft);

      // Then: 두 번째 임시저장만 남아있다
      final loaded = await dataSource.loadDraft();
      expect(loaded, isNotNull);
      expect(loaded!['client_id'], 200);
      expect(loaded['client_name'], '두번째거래처');
      expect(loaded['total_amount'], 20000);
    });

    test('손상된 데이터 불러오기: JSON 파싱 실패 시 null을 반환한다', () async {
      // Given: Hive box에 직접 잘못된 데이터를 저장한다
      final box = await Hive.openBox('order_draft_box');
      await box.put('draft_data', 'this is not a valid JSON');

      // When: 임시저장을 불러온다
      final loaded = await dataSource.loadDraft();

      // Then: null이 반환된다 (파싱 실패를 처리함)
      expect(loaded, isNull);

      await box.close();
    });

    test('복잡한 중첩 구조 저장 및 불러오기: 여러 제품 항목이 있는 주문서를 처리한다', () async {
      // Given: 여러 제품 항목이 있는 복잡한 주문서
      final complexDraft = {
        'client_id': 100,
        'client_name': '테스트거래처',
        'credit_balance': 50000000,
        'delivery_date': '2026-03-15',
        'items': [
          {
            'product_code': '01234567',
            'product_name': '오뚜기 진라면',
            'quantity_boxes': 5.0,
            'quantity_pieces': 10,
            'unit_price': 1200,
            'box_size': 20,
            'total_price': 72000,
            'is_selected': true,
          },
          {
            'product_code': '01234568',
            'product_name': '오뚜기 참깨라면',
            'quantity_boxes': 3.0,
            'quantity_pieces': 0,
            'unit_price': 1300,
            'box_size': 20,
            'total_price': 78000,
            'is_selected': false,
          },
          {
            'product_code': '01234569',
            'product_name': '오뚜기 케첩',
            'quantity_boxes': 10.5,
            'quantity_pieces': 5,
            'unit_price': 2000,
            'box_size': 12,
            'total_price': 252000,
            'is_selected': true,
          },
        ],
        'total_amount': 402000,
        'is_draft': true,
        'last_modified': '2026-02-10T10:30:00.000Z',
      };

      // When: 임시저장하고 불러온다
      await dataSource.saveDraft(complexDraft);
      final loaded = await dataSource.loadDraft();

      // Then: 모든 중첩된 데이터가 정확하게 복원된다
      expect(loaded, isNotNull);
      expect(loaded!['items'], isList);
      expect((loaded['items'] as List).length, 3);
      expect(loaded['total_amount'], 402000);

      final items = loaded['items'] as List;
      expect(items[0]['product_code'], '01234567');
      expect(items[0]['is_selected'], true);
      expect(items[1]['product_code'], '01234568');
      expect(items[1]['is_selected'], false);
      expect(items[2]['product_code'], '01234569');
      expect(items[2]['quantity_boxes'], 10.5);
    });
  });
}
