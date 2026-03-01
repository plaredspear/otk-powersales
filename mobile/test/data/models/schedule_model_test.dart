import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/schedule_model.dart';

void main() {
  group('ScheduleModel', () {
    group('fromJson', () {
      test('모든 필드가 있는 JSON을 올바르게 파싱해야 한다', () {
        final json = {
          'schedule_id': 'a0Xxxxxxxxxxx',
          'employee_name': '홍길동',
          'employee_sfid': '005xxxxxxxxx',
          'store_name': '이마트 부산점',
          'store_sfid': '001xxxxxxxxx',
          'work_category': '방판',
          'work_type': '정기',
          'is_commute_registered': true,
          'commute_registered_at': '2026-03-01T09:00:00',
        };

        final model = ScheduleModel.fromJson(json);

        expect(model.scheduleId, 'a0Xxxxxxxxxxx');
        expect(model.employeeName, '홍길동');
        expect(model.employeeSfid, '005xxxxxxxxx');
        expect(model.storeName, '이마트 부산점');
        expect(model.storeSfid, '001xxxxxxxxx');
        expect(model.workCategory, '방판');
        expect(model.workType, '정기');
        expect(model.isCommuteRegistered, true);
        expect(model.commuteRegisteredAt, DateTime.parse('2026-03-01T09:00:00'));
      });

      test('nullable 필드가 null인 JSON을 올바르게 파싱해야 한다', () {
        final json = {
          'schedule_id': 'a0Xxxxxxxxxxx',
          'employee_name': '홍길동',
          'employee_sfid': '005xxxxxxxxx',
          'store_name': null,
          'store_sfid': null,
          'work_category': '방판',
          'work_type': null,
          'is_commute_registered': false,
          'commute_registered_at': null,
        };

        final model = ScheduleModel.fromJson(json);

        expect(model.storeName, isNull);
        expect(model.storeSfid, isNull);
        expect(model.workType, isNull);
        expect(model.isCommuteRegistered, false);
        expect(model.commuteRegisteredAt, isNull);
      });
    });

    group('toEntity', () {
      test('ScheduleModel을 Schedule 엔티티로 올바르게 변환해야 한다', () {
        const model = ScheduleModel(
          scheduleId: 'a0X1',
          employeeName: '홍길동',
          employeeSfid: '005x',
          storeName: '이마트 부산점',
          storeSfid: '001x',
          workCategory: '방판',
          workType: '정기',
          isCommuteRegistered: false,
          commuteRegisteredAt: null,
        );

        final entity = model.toEntity();

        expect(entity.scheduleId, 'a0X1');
        expect(entity.employeeName, '홍길동');
        expect(entity.storeName, '이마트 부산점');
        expect(entity.workCategory, '방판');
        expect(entity.isCommuteRegistered, false);
        expect(entity.commuteRegisteredAt, isNull);
      });
    });

    group('toJson', () {
      test('snake_case 키로 변환해야 한다', () {
        const model = ScheduleModel(
          scheduleId: 'a0X1',
          employeeName: '홍길동',
          employeeSfid: '005x',
          workCategory: '방판',
          isCommuteRegistered: true,
        );

        final json = model.toJson();

        expect(json['schedule_id'], 'a0X1');
        expect(json['employee_name'], '홍길동');
        expect(json['work_category'], '방판');
        expect(json['is_commute_registered'], true);
      });
    });

    group('equality', () {
      test('동일 필드의 두 모델은 같아야 한다', () {
        const model1 = ScheduleModel(
          scheduleId: 'a0X1',
          employeeName: '홍길동',
          employeeSfid: '005x',
          workCategory: '방판',
          isCommuteRegistered: false,
        );
        const model2 = ScheduleModel(
          scheduleId: 'a0X1',
          employeeName: '홍길동',
          employeeSfid: '005x',
          workCategory: '방판',
          isCommuteRegistered: false,
        );

        expect(model1, equals(model2));
        expect(model1.hashCode, equals(model2.hashCode));
      });
    });
  });
}
