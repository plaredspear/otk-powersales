import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/user_model.dart';
import 'package:mobile/domain/entities/user.dart';

void main() {
  group('UserModel', () {
    const testModel = UserModel(
      id: 1,
      employeeId: '20010585',
      name: '홍길동',
      department: '영업1팀',
      branchName: '부산1지점',
      role: 'USER',
    );

    final testJson = {
      'id': 1,
      'employee_id': '20010585',
      'name': '홍길동',
      'department': '영업1팀',
      'branch_name': '부산1지점',
      'role': 'USER',
    };

    final testEntity = User(
      id: 1,
      employeeId: '20010585',
      name: '홍길동',
      department: '영업1팀',
      branchName: '부산1지점',
      role: 'USER',
    );

    group('fromJson', () {
      test('snake_case JSON 키를 올바르게 파싱해야 한다', () {
        // Act
        final result = UserModel.fromJson(testJson);

        // Assert
        expect(result.id, 1);
        expect(result.employeeId, '20010585');
        expect(result.name, '홍길동');
        expect(result.department, '영업1팀');
        expect(result.branchName, '부산1지점');
        expect(result.role, 'USER');
      });
    });

    group('toJson', () {
      test('snake_case JSON 키로 올바르게 직렬화해야 한다', () {
        // Act
        final result = testModel.toJson();

        // Assert
        expect(result['id'], 1);
        expect(result['employee_id'], '20010585');
        expect(result['name'], '홍길동');
        expect(result['department'], '영업1팀');
        expect(result['branch_name'], '부산1지점');
        expect(result['role'], 'USER');
      });
    });

    group('toEntity', () {
      test('올바른 User 엔티티를 생성해야 한다', () {
        // Act
        final result = testModel.toEntity();

        // Assert
        expect(result.id, testModel.id);
        expect(result.employeeId, testModel.employeeId);
        expect(result.name, testModel.name);
        expect(result.department, testModel.department);
        expect(result.branchName, testModel.branchName);
        expect(result.role, testModel.role);
      });
    });

    group('fromEntity', () {
      test('User 엔티티로부터 올바른 UserModel을 생성해야 한다', () {
        // Act
        final result = UserModel.fromEntity(testEntity);

        // Assert
        expect(result.id, testEntity.id);
        expect(result.employeeId, testEntity.employeeId);
        expect(result.name, testEntity.name);
        expect(result.department, testEntity.department);
        expect(result.branchName, testEntity.branchName);
        expect(result.role, testEntity.role);
      });
    });

    group('round trip', () {
      test('fromJson -> toEntity -> fromEntity -> toJson 변환이 일관성 있어야 한다', () {
        // Arrange & Act
        final modelFromJson = UserModel.fromJson(testJson);
        final entity = modelFromJson.toEntity();
        final modelFromEntity = UserModel.fromEntity(entity);
        final jsonResult = modelFromEntity.toJson();

        // Assert
        expect(jsonResult, testJson);
      });
    });

    group('equality', () {
      test('같은 값을 가진 두 UserModel은 동일해야 한다', () {
        // Arrange
        const model1 = UserModel(
          id: 1,
          employeeId: '20010585',
          name: '홍길동',
          department: '영업1팀',
          branchName: '부산1지점',
          role: 'USER',
        );

        const model2 = UserModel(
          id: 1,
          employeeId: '20010585',
          name: '홍길동',
          department: '영업1팀',
          branchName: '부산1지점',
          role: 'USER',
        );

        // Assert
        expect(model1, model2);
      });

      test('다른 값을 가진 두 UserModel은 동일하지 않아야 한다', () {
        // Arrange
        const model1 = UserModel(
          id: 1,
          employeeId: '20010585',
          name: '홍길동',
          department: '영업1팀',
          branchName: '부산1지점',
          role: 'USER',
        );

        const model2 = UserModel(
          id: 2,
          employeeId: '20010586',
          name: '김철수',
          department: '영업2팀',
          branchName: '서울지점',
          role: 'ADMIN',
        );

        // Assert
        expect(model1, isNot(model2));
      });
    });

    group('hashCode', () {
      test('같은 값을 가진 두 UserModel은 같은 hashCode를 가져야 한다', () {
        // Arrange
        const model1 = UserModel(
          id: 1,
          employeeId: '20010585',
          name: '홍길동',
          department: '영업1팀',
          branchName: '부산1지점',
          role: 'USER',
        );

        const model2 = UserModel(
          id: 1,
          employeeId: '20010585',
          name: '홍길동',
          department: '영업1팀',
          branchName: '부산1지점',
          role: 'USER',
        );

        // Assert
        expect(model1.hashCode, model2.hashCode);
      });
    });

    group('toString', () {
      test('올바른 문자열 표현을 반환해야 한다', () {
        // Act
        final result = testModel.toString();

        // Assert
        expect(result, contains('UserModel'));
        expect(result, contains('id: 1'));
        expect(result, contains('employeeId: 20010585'));
        expect(result, contains('name: 홍길동'));
        expect(result, contains('department: 영업1팀'));
        expect(result, contains('branchName: 부산1지점'));
        expect(result, contains('role: USER'));
      });
    });
  });
}
