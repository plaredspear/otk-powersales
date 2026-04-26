import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/alternative_holiday_model.dart';

void main() {
  group('AlternativeHolidayModel', () {
    test('fromJson으로 정상 파싱되어야 한다', () {
      final json = {
        'id': 1,
        'actual_work_date': '2026-03-07',
        'target_alt_holiday_date': '2026-03-09',
        'confirm_alt_holiday_date': null,
        'status': '신규',
        'change_reason': null,
        'created_at': '2026-03-09T10:30:00',
      };

      final model = AlternativeHolidayModel.fromJson(json);

      expect(model.id, 1);
      expect(model.actualWorkDate, '2026-03-07');
      expect(model.targetAltHolidayDate, '2026-03-09');
      expect(model.confirmAltHolidayDate, isNull);
      expect(model.status, '신규');
    });

    test('toEntity 변환이 정상적이어야 한다', () {
      final model = AlternativeHolidayModel(
        id: 2,
        actualWorkDate: '2026-03-14',
        targetAltHolidayDate: '2026-03-16',
        confirmAltHolidayDate: '2026-03-16',
        status: '승인',
        changeReason: null,
        createdAt: '2026-03-16T09:00:00',
      );

      final entity = model.toEntity();

      expect(entity.id, 2);
      expect(entity.actualWorkDate, DateTime(2026, 3, 14));
      expect(entity.confirmAltHolidayDate, DateTime(2026, 3, 16));
      expect(entity.status, '승인');
    });

    test('승인 건의 confirm_alt_holiday_date가 정상 파싱되어야 한다', () {
      final json = {
        'id': 3,
        'actual_work_date': '2026-03-08',
        'target_alt_holiday_date': '2026-03-10',
        'confirm_alt_holiday_date': '2026-03-11',
        'status': '조정',
        'change_reason': '관리자 조정',
        'created_at': '2026-03-09T10:00:00',
      };

      final entity = AlternativeHolidayModel.fromJson(json).toEntity();

      expect(entity.confirmAltHolidayDate, DateTime(2026, 3, 11));
      expect(entity.changeReason, '관리자 조정');
      expect(entity.status, '조정');
    });
  });
}
