import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/repositories/mock/auth_mock_repository.dart';

void main() {
  group('AuthMockRepository', () {
    late AuthMockRepository repository;

    setUp(() {
      repository = AuthMockRepository();
    });

    group('login', () {
      test('정상 로그인 시 LoginResult 반환', () async {
        final result = await repository.login('20010585', 'test1234');

        expect(result.user.employeeId, equals('20010585'));
        expect(result.user.name, equals('홍길동'));
        expect(result.token.accessToken, isNotEmpty);
      });

      test('잘못된 사번이면 Exception 발생', () async {
        expect(
          () => repository.login('99999999', 'test1234'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('사번 또는 비밀번호가 올바르지 않습니다'),
            ),
          ),
        );
      });

      test('잘못된 비밀번호이면 Exception 발생', () async {
        expect(
          () => repository.login('20010585', 'wrong'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('사번 또는 비밀번호가 올바르지 않습니다'),
            ),
          ),
        );
      });

      test('단말기 바인딩된 계정으로 로그인 시 DEVICE_MISMATCH 에러', () async {
        expect(
          () => repository.login('20040001', 'test1234'),
          throwsA(
            isA<Exception>().having(
              (e) => e.toString(),
              'message',
              contains('등록된 단말기와 다른 기기입니다'),
            ),
          ),
        );
      });

      test('비밀번호 변경 필요 계정의 requiresPasswordChange가 true', () async {
        final result = await repository.login('20020001', 'otg1');

        expect(result.requiresPasswordChange, isTrue);
      });
    });
  });
}
