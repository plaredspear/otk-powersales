import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/expiry_alert_model.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';

void main() {
  group('ExpiryAlertModel', () {
    const testModel = ExpiryAlertModel(
      branchName: '부산1지점',
      employeeName: '최금주',
      employeeId: '20030117',
      expiryCount: 1,
    );

    final testJson = {
      'branch_name': '부산1지점',
      'employee_name': '최금주',
      'employee_id': '20030117',
      'expiry_count': 1,
    };

    const testEntity = ExpiryAlert(
      branchName: '부산1지점',
      employeeName: '최금주',
      employeeId: '20030117',
      expiryCount: 1,
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        final result = ExpiryAlertModel.fromJson(testJson);

        expect(result.branchName, '부산1지점');
        expect(result.employeeName, '최금주');
        expect(result.employeeId, '20030117');
        expect(result.expiryCount, 1);
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        final result = testModel.toJson();

        expect(result['branch_name'], '부산1지점');
        expect(result['employee_name'], '최금주');
        expect(result['employee_id'], '20030117');
        expect(result['expiry_count'], 1);
      });
    });

    group('toEntity', () {
      test('올바른 ExpiryAlert 엔티티를 생성해야 한다', () {
        final result = testModel.toEntity();

        expect(result.branchName, testModel.branchName);
        expect(result.employeeName, testModel.employeeName);
        expect(result.employeeId, testModel.employeeId);
        expect(result.expiryCount, testModel.expiryCount);
      });
    });

    group('fromEntity', () {
      test('ExpiryAlert 엔티티로부터 올바른 ExpiryAlertModel을 생성해야 한다', () {
        final result = ExpiryAlertModel.fromEntity(testEntity);

        expect(result.branchName, testEntity.branchName);
        expect(result.employeeName, testEntity.employeeName);
        expect(result.employeeId, testEntity.employeeId);
        expect(result.expiryCount, testEntity.expiryCount);
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        final modelFromJson = ExpiryAlertModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = ExpiryAlertModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        expect(jsonResult, testJson);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ExpiryAlertModel은 동일해야 한다', () {
        const model1 = ExpiryAlertModel(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );
        const model2 = ExpiryAlertModel(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );

        expect(model1, model2);
      });

      test('다른 값을 가진 두 ExpiryAlertModel은 동일하지 않아야 한다', () {
        const other = ExpiryAlertModel(
          branchName: '서울2지점',
          employeeName: '홍길동',
          employeeId: '20010585',
          expiryCount: 3,
        );

        expect(testModel, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ExpiryAlertModel은 같은 hashCode를 가져야 한다', () {
        const model1 = ExpiryAlertModel(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );
        const model2 = ExpiryAlertModel(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );

        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        final result = testModel.toString();

        expect(result, contains('ExpiryAlertModel'));
        expect(result, contains('branchName: 부산1지점'));
        expect(result, contains('employeeName: 최금주'));
      });
    });
  });
}
