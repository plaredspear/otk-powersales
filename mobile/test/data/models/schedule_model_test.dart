import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/schedule_model.dart';

void main() {
  group('ScheduleModel', () {
    group('fromJson', () {
      test('모든 필드가 있는 JSON을 올바르게 파싱해야 한다', () {
        final json = {
          'scheduleId': 12345,
          'employeeName': '홍길동',
          'employeeCode': '005xxxxxxxxx',
          'accountName': '이마트 부산점',
          'accountId': 456,
          'workCategory': '진열',
          'workCategory2': '상시',
          'workType': '고정',
          'isCommuteRegistered': true,
          'commuteRegisteredAt': '2026-03-01T09:00:00',
        };

        final model = ScheduleModel.fromJson(json);

        expect(model.scheduleId, 12345);
        expect(model.employeeName, '홍길동');
        expect(model.employeeCode, '005xxxxxxxxx');
        expect(model.accountName, '이마트 부산점');
        expect(model.accountId, 456);
        expect(model.workCategory, '진열');
        expect(model.workCategory2, '상시');
        expect(model.workType, '고정');
        expect(model.isCommuteRegistered, true);
        expect(model.commuteRegisteredAt, DateTime.parse('2026-03-01T09:00:00'));
      });

      test('nullable 필드가 null인 JSON을 올바르게 파싱해야 한다', () {
        final json = {
          'scheduleId': 12345,
          'employeeName': '홍길동',
          'employeeCode': '005xxxxxxxxx',
          'accountName': null,
          'accountId': null,
          'workCategory': '방판',
          'workCategory2': null,
          'workType': null,
          'isCommuteRegistered': false,
          'commuteRegisteredAt': null,
        };

        final model = ScheduleModel.fromJson(json);

        expect(model.accountName, isNull);
        expect(model.accountId, isNull);
        expect(model.workCategory2, isNull);
        expect(model.workType, isNull);
        expect(model.isCommuteRegistered, false);
        expect(model.commuteRegisteredAt, isNull);
      });
    });

    group('toEntity', () {
      test('ScheduleModel을 Schedule 엔티티로 올바르게 변환해야 한다', () {
        const model = ScheduleModel(
          scheduleId: 12345,
          employeeName: '홍길동',
          employeeCode: '005x',
          accountName: '이마트 부산점',
          accountId: 456,
          workCategory: '방판',
          workType: '정기',
          isCommuteRegistered: false,
          commuteRegisteredAt: null,
        );

        final entity = model.toEntity();

        expect(entity.scheduleId, 12345);
        expect(entity.employeeName, '홍길동');
        expect(entity.accountName, '이마트 부산점');
        expect(entity.workCategory, '방판');
        expect(entity.isCommuteRegistered, false);
        expect(entity.commuteRegisteredAt, isNull);
      });
    });

    group('toJson', () {
      test('snake_case 키로 변환해야 한다', () {
        const model = ScheduleModel(
          scheduleId: 12345,
          employeeName: '홍길동',
          employeeCode: '005x',
          workCategory: '진열',
          workCategory2: '상시',
          isCommuteRegistered: true,
        );

        final json = model.toJson();

        expect(json['scheduleId'], 12345);
        expect(json['employeeName'], '홍길동');
        expect(json['workCategory'], '진열');
        expect(json['workCategory2'], '상시');
        expect(json['isCommuteRegistered'], true);
      });
    });

    group('equality', () {
      test('동일 필드의 두 모델은 같아야 한다', () {
        const model1 = ScheduleModel(
          scheduleId: 12345,
          employeeName: '홍길동',
          employeeCode: '005x',
          workCategory: '방판',
          isCommuteRegistered: false,
        );
        const model2 = ScheduleModel(
          scheduleId: 12345,
          employeeName: '홍길동',
          employeeCode: '005x',
          workCategory: '방판',
          isCommuteRegistered: false,
        );

        expect(model1, equals(model2));
        expect(model1.hashCode, equals(model2.hashCode));
      });
    });
  });
}
