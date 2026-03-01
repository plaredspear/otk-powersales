import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/attendance_summary_model.dart';

void main() {
  group('AttendanceSummaryModel', () {
    group('fromJson', () {
      test('JSON을 올바르게 파싱해야 한다', () {
        final json = {
          'total_count': 5,
          'registered_count': 3,
        };

        final model = AttendanceSummaryModel.fromJson(json);

        expect(model.totalCount, 5);
        expect(model.registeredCount, 3);
      });
    });

    group('toEntity', () {
      test('AttendanceSummary 엔티티로 올바르게 변환해야 한다', () {
        const model = AttendanceSummaryModel(
          totalCount: 5,
          registeredCount: 3,
        );

        final entity = model.toEntity();

        expect(entity.totalCount, 5);
        expect(entity.registeredCount, 3);
      });
    });

    group('toJson', () {
      test('snake_case 키로 변환해야 한다', () {
        const model = AttendanceSummaryModel(
          totalCount: 5,
          registeredCount: 3,
        );

        final json = model.toJson();

        expect(json['total_count'], 5);
        expect(json['registered_count'], 3);
      });
    });

    group('equality', () {
      test('동일 필드의 두 모델은 같아야 한다', () {
        const model1 = AttendanceSummaryModel(totalCount: 5, registeredCount: 3);
        const model2 = AttendanceSummaryModel(totalCount: 5, registeredCount: 3);

        expect(model1, equals(model2));
        expect(model1.hashCode, equals(model2.hashCode));
      });
    });
  });
}
