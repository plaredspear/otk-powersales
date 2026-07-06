import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/models/user_model.dart';

void main() {
  group('UserModel', () {
    Map<String, dynamic> baseJson({dynamic role}) => {
          'id': 1,
          'employeeCode': '00000009',
          'name': '최금주',
          'orgName': '강서1지점',
          'role': role,
        };

    group('fromJson + toEntity - role 번역 (SF picklist → 도메인)', () {
      test('여사원 → USER', () {
        final entity = UserModel.fromJson(baseJson(role: '여사원')).toEntity();
        expect(entity.role, 'USER');
      });

      test('조장 → LEADER', () {
        final entity = UserModel.fromJson(baseJson(role: '조장')).toEntity();
        expect(entity.role, 'LEADER');
      });

      test('지점장 → ADMIN', () {
        final entity = UserModel.fromJson(baseJson(role: '지점장')).toEntity();
        expect(entity.role, 'ADMIN');
      });

      test('role null → 기본 USER', () {
        final entity = UserModel.fromJson(baseJson(role: null)).toEntity();
        expect(entity.role, 'USER');
      });

      test('그 외 권한(AccountViewAll 등) → 기본 USER', () {
        final entity =
            UserModel.fromJson(baseJson(role: 'AccountViewAll')).toEntity();
        expect(entity.role, 'USER');
      });

      test('이미 도메인 값(영문)이면 그대로 통과 (멱등)', () {
        expect(UserModel.fromJson(baseJson(role: 'LEADER')).toEntity().role,
            'LEADER');
        expect(UserModel.fromJson(baseJson(role: 'ADMIN')).toEntity().role,
            'ADMIN');
        expect(
            UserModel.fromJson(baseJson(role: 'USER')).toEntity().role, 'USER');
      });
    });

    test('fromJson - 원본 role 값은 모델에 보존된다', () {
      final model = UserModel.fromJson(baseJson(role: '조장'));
      expect(model.role, '조장');
    });

    group('toEntity - rawRole (원본 권한 보존, "내 정보" 미지정 구분용)', () {
      test('원본 값이 있으면 그대로 보존', () {
        expect(UserModel.fromJson(baseJson(role: '여사원')).toEntity().rawRole,
            '여사원');
        expect(UserModel.fromJson(baseJson(role: '조장')).toEntity().rawRole,
            '조장');
        expect(
            UserModel.fromJson(baseJson(role: 'AccountViewAll'))
                .toEntity()
                .rawRole,
            'AccountViewAll');
      });

      test('role null → rawRole null (실제 여사원과 구분)', () {
        final entity = UserModel.fromJson(baseJson(role: null)).toEntity();
        // role 은 여전히 USER 로 뭉개지지만, rawRole 은 null 로 미지정을 구분한다.
        expect(entity.role, 'USER');
        expect(entity.rawRole, isNull);
      });

      test('role 빈 문자열 → rawRole null', () {
        final entity = UserModel.fromJson(baseJson(role: '')).toEntity();
        expect(entity.rawRole, isNull);
      });
    });
  });
}
