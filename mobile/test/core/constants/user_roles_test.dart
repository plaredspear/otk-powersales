import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/constants/user_roles.dart';

void main() {
  group('UserRoles.canManageTeam', () {
    test('조장(LEADER)은 팀 관리 권한이다', () {
      expect(UserRoles.canManageTeam(UserRoles.leader), isTrue);
    });

    test('지점장(ADMIN)도 조장과 동일하게 팀 관리 권한이다', () {
      expect(UserRoles.canManageTeam(UserRoles.branchManager), isTrue);
    });

    test('여사원(USER)은 팀 관리 권한이 아니다', () {
      expect(UserRoles.canManageTeam(UserRoles.user), isFalse);
    });

    test('부서장(AccountViewAll)은 도메인 role 이 USER 로 번역되므로 팀 관리 권한이 아니다', () {
      expect(UserRoles.canManageTeam('USER'), isFalse);
    });

    test('null / 미지정 / 알 수 없는 값은 팀 관리 권한이 아니다', () {
      expect(UserRoles.canManageTeam(null), isFalse);
      expect(UserRoles.canManageTeam(''), isFalse);
      expect(UserRoles.canManageTeam('지점장'), isFalse,
          reason: 'SF picklist 원문이 아니라 번역된 도메인 role 을 입력받는다');
    });
  });
}
