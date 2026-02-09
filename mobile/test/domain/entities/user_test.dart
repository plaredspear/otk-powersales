import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/domain/entities/user.dart';
import '../../test_helper.dart';

void main() {
  setUpAll(() async {
    await TestHelper.initialize();
  });

  const testUser = User(
    id: 1,
    employeeId: '20010585',
    name: '홍길동',
    department: '영업1팀',
    branchName: '부산1지점',
    role: 'USER',
  );

  group('User Entity 생성 테스트', () {
    test('User 인스턴스가 올바르게 생성되는지 확인', () {
      expect(testUser.id, 1);
      expect(testUser.employeeId, '20010585');
      expect(testUser.name, '홍길동');
      expect(testUser.department, '영업1팀');
      expect(testUser.branchName, '부산1지점');
      expect(testUser.role, 'USER');
    });
  });

  group('User copyWith 테스트', () {
    test('일부 필드만 변경', () {
      final updatedUser = testUser.copyWith(
        name: '김철수',
        department: '영업2팀',
      );

      expect(updatedUser.id, testUser.id);
      expect(updatedUser.employeeId, testUser.employeeId);
      expect(updatedUser.name, '김철수');
      expect(updatedUser.department, '영업2팀');
      expect(updatedUser.branchName, testUser.branchName);
      expect(updatedUser.role, testUser.role);
    });

    test('변경하지 않은 필드 유지', () {
      final updatedUser = testUser.copyWith(name: '이영희');

      expect(updatedUser.id, testUser.id);
      expect(updatedUser.employeeId, testUser.employeeId);
      expect(updatedUser.name, '이영희');
      expect(updatedUser.department, testUser.department);
      expect(updatedUser.branchName, testUser.branchName);
      expect(updatedUser.role, testUser.role);
    });

    test('모든 필드 변경', () {
      final updatedUser = testUser.copyWith(
        id: 2,
        employeeId: '20010586',
        name: '박영수',
        department: '영업3팀',
        branchName: '서울1지점',
        role: 'ADMIN',
      );

      expect(updatedUser.id, 2);
      expect(updatedUser.employeeId, '20010586');
      expect(updatedUser.name, '박영수');
      expect(updatedUser.department, '영업3팀');
      expect(updatedUser.branchName, '서울1지점');
      expect(updatedUser.role, 'ADMIN');
    });
  });

  group('User toJson/fromJson 테스트', () {
    test('toJson 직렬화', () {
      final json = testUser.toJson();

      expect(json['id'], 1);
      expect(json['employeeId'], '20010585');
      expect(json['name'], '홍길동');
      expect(json['department'], '영업1팀');
      expect(json['branchName'], '부산1지점');
      expect(json['role'], 'USER');
    });

    test('fromJson 역직렬화', () {
      final json = {
        'id': 1,
        'employeeId': '20010585',
        'name': '홍길동',
        'department': '영업1팀',
        'branchName': '부산1지점',
        'role': 'USER',
      };

      final user = User.fromJson(json);

      expect(user.id, 1);
      expect(user.employeeId, '20010585');
      expect(user.name, '홍길동');
      expect(user.department, '영업1팀');
      expect(user.branchName, '부산1지점');
      expect(user.role, 'USER');
    });

    test('toJson/fromJson 라운드트립', () {
      final json = testUser.toJson();
      final user = User.fromJson(json);

      expect(user, testUser);
    });
  });

  group('User equality 테스트', () {
    test('같은 값을 가진 User는 같은 객체', () {
      const user1 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      const user2 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      expect(user1, user2);
    });

    test('다른 값을 가진 User는 다른 객체', () {
      const user1 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      const user2 = User(
        id: 2,
        employeeId: '20010586',
        name: '김철수',
        department: '영업2팀',
        branchName: '서울1지점',
        role: 'ADMIN',
      );

      expect(user1, isNot(user2));
    });

    test('일부 필드만 다른 User는 다른 객체', () {
      const user1 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      const user2 = User(
        id: 1,
        employeeId: '20010585',
        name: '김철수',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      expect(user1, isNot(user2));
    });
  });

  group('User hashCode 테스트', () {
    test('같은 값을 가진 User는 같은 hashCode', () {
      const user1 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      const user2 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      expect(user1.hashCode, user2.hashCode);
    });

    test('다른 값을 가진 User는 다른 hashCode', () {
      const user1 = User(
        id: 1,
        employeeId: '20010585',
        name: '홍길동',
        department: '영업1팀',
        branchName: '부산1지점',
        role: 'USER',
      );

      const user2 = User(
        id: 2,
        employeeId: '20010586',
        name: '김철수',
        department: '영업2팀',
        branchName: '서울1지점',
        role: 'ADMIN',
      );

      expect(user1.hashCode, isNot(user2.hashCode));
    });
  });

  group('User toString 테스트', () {
    test('toString 포맷 확인', () {
      final result = testUser.toString();

      expect(result, contains('User'));
      expect(result, contains('id: 1'));
      expect(result, contains('employeeId: 20010585'));
      expect(result, contains('name: 홍길동'));
      expect(result, contains('department: 영업1팀'));
      expect(result, contains('branchName: 부산1지점'));
      expect(result, contains('role: USER'));
    });
  });
}
