import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/expiry_alert.dart';

void main() {
  group('ExpiryAlert', () {
    const testAlert = ExpiryAlert(
      branchName: '부산1지점',
      employeeName: '최금주',
      employeeId: '20030117',
      expiryCount: 1,
    );

    final testJson = {
      'branchName': '부산1지점',
      'employeeName': '최금주',
      'employeeId': '20030117',
      'expiryCount': 1,
    };

    group('생성', () {
      test('ExpiryAlert 엔티티가 올바르게 생성된다', () {
        expect(testAlert.branchName, '부산1지점');
        expect(testAlert.employeeName, '최금주');
        expect(testAlert.employeeId, '20030117');
        expect(testAlert.expiryCount, 1);
      });
    });

    group('copyWith', () {
      test('일부 필드만 변경하여 복사할 수 있다', () {
        final copied = testAlert.copyWith(expiryCount: 5);

        expect(copied.branchName, testAlert.branchName);
        expect(copied.employeeName, testAlert.employeeName);
        expect(copied.employeeId, testAlert.employeeId);
        expect(copied.expiryCount, 5);
      });

      test('모든 필드를 변경하여 복사할 수 있다', () {
        final copied = testAlert.copyWith(
          branchName: '서울2지점',
          employeeName: '홍길동',
          employeeId: '20010585',
          expiryCount: 3,
        );

        expect(copied.branchName, '서울2지점');
        expect(copied.employeeName, '홍길동');
        expect(copied.employeeId, '20010585');
        expect(copied.expiryCount, 3);
      });

      test('아무 필드도 변경하지 않으면 동일한 값의 새 인스턴스를 반환한다', () {
        final copied = testAlert.copyWith();

        expect(copied, testAlert);
        expect(identical(copied, testAlert), isFalse);
      });
    });

    group('toJson', () {
      test('올바른 JSON Map을 반환한다', () {
        final result = testAlert.toJson();

        expect(result['branchName'], '부산1지점');
        expect(result['employeeName'], '최금주');
        expect(result['employeeId'], '20030117');
        expect(result['expiryCount'], 1);
      });
    });

    group('fromJson', () {
      test('JSON Map에서 올바르게 생성된다', () {
        final result = ExpiryAlert.fromJson(testJson);

        expect(result.branchName, '부산1지점');
        expect(result.employeeName, '최금주');
        expect(result.employeeId, '20030117');
        expect(result.expiryCount, 1);
      });
    });

    group('round trip', () {
      test('toJson -> fromJson 변환이 일관성 있다', () {
        final json = testAlert.toJson();
        final restored = ExpiryAlert.fromJson(json);

        expect(restored, testAlert);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 ExpiryAlert은 동일하다', () {
        const alert1 = ExpiryAlert(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );
        const alert2 = ExpiryAlert(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );

        expect(alert1, alert2);
      });

      test('다른 값을 가진 두 ExpiryAlert은 동일하지 않다', () {
        const other = ExpiryAlert(
          branchName: '서울2지점',
          employeeName: '홍길동',
          employeeId: '20010585',
          expiryCount: 3,
        );

        expect(testAlert, isNot(other));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 ExpiryAlert은 같은 hashCode를 가진다', () {
        const alert1 = ExpiryAlert(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );
        const alert2 = ExpiryAlert(
          branchName: '부산1지점',
          employeeName: '최금주',
          employeeId: '20030117',
          expiryCount: 1,
        );

        expect(alert1.hashCode, alert2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환한다', () {
        final result = testAlert.toString();

        expect(result, contains('ExpiryAlert'));
        expect(result, contains('branchName: 부산1지점'));
        expect(result, contains('employeeName: 최금주'));
        expect(result, contains('employeeId: 20030117'));
        expect(result, contains('expiryCount: 1'));
      });
    });
  });
}
