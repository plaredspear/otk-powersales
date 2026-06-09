import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/promotion.dart';

void main() {
  group('MyPromotionAssignment.fromJson', () {
    test('모든 필드 정상 파싱 (id → promotionEmployeeId)', () {
      final json = {
        'id': 10,
        'promotionId': 1,
        'promotionNumber': 'PM00000001',
        'promotionType': '시식',
        'accountName': '이마트 성수점',
        'scheduleDate': '2026-06-09',
        'standLocation': '엔드',
        'isClosed': false,
      };

      final a = MyPromotionAssignment.fromJson(json);

      expect(a.promotionEmployeeId, 10);
      expect(a.promotionId, 1);
      expect(a.promotionNumber, 'PM00000001');
      expect(a.promotionType, '시식');
      expect(a.accountName, '이마트 성수점');
      expect(a.scheduleDate, '2026-06-09');
      expect(a.standLocation, '엔드');
      expect(a.isClosed, false);
    });

    test('nullable 필드 null 및 isClosed 누락 시 기본값', () {
      final json = {
        'id': 11,
        'promotionId': 2,
        'promotionNumber': 'PM00000002',
        'promotionType': null,
        'accountName': null,
        'scheduleDate': null,
        'standLocation': null,
      };

      final a = MyPromotionAssignment.fromJson(json);

      expect(a.promotionEmployeeId, 11);
      expect(a.promotionId, 2);
      expect(a.promotionNumber, 'PM00000002');
      expect(a.promotionType, isNull);
      expect(a.accountName, isNull);
      expect(a.scheduleDate, isNull);
      expect(a.standLocation, isNull);
      expect(a.isClosed, false);
    });

    test('isClosed true 파싱', () {
      final json = {
        'id': 12,
        'promotionId': 3,
        'promotionNumber': 'PM00000003',
        'isClosed': true,
      };

      final a = MyPromotionAssignment.fromJson(json);

      expect(a.isClosed, true);
    });
  });
}
