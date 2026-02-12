import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/auth_remote_datasource.dart';
import 'package:mobile/data/models/auth_token_model.dart';
import 'package:mobile/data/models/login_response_model.dart';
import 'package:mobile/data/repositories/password_repository_impl.dart';

void main() {
  late PasswordRepositoryImpl repository;
  late FakeAuthRemoteDataSource fakeRemoteDataSource;

  setUp(() {
    fakeRemoteDataSource = FakeAuthRemoteDataSource();
    repository = PasswordRepositoryImpl(
      remoteDataSource: fakeRemoteDataSource,
    );
  });

  group('PasswordRepositoryImpl', () {
    group('verifyCurrentPassword', () {
      test('비밀번호가 일치하면 true를 반환한다', () async {
        // Given: remote datasource가 true를 반환하도록 설정
        fakeRemoteDataSource.verifyPasswordResult = true;

        // When: 비밀번호를 검증한다
        final result = await repository.verifyCurrentPassword('1234');

        // Then: remote datasource가 호출되고 true가 반환된다
        expect(fakeRemoteDataSource.verifyCurrentPasswordCalls, 1);
        expect(fakeRemoteDataSource.lastCurrentPassword, '1234');
        expect(result, true);
      });

      test('비밀번호가 불일치하면 false를 반환한다', () async {
        // Given: remote datasource가 false를 반환하도록 설정
        fakeRemoteDataSource.verifyPasswordResult = false;

        // When: 비밀번호를 검증한다
        final result = await repository.verifyCurrentPassword('wrong');

        // Then: remote datasource가 호출되고 false가 반환된다
        expect(fakeRemoteDataSource.verifyCurrentPasswordCalls, 1);
        expect(fakeRemoteDataSource.lastCurrentPassword, 'wrong');
        expect(result, false);
      });
    });

    group('changePassword', () {
      test('비밀번호 변경을 remote datasource에 위임한다', () async {
        // When: 비밀번호를 변경한다
        await repository.changePassword('old123', 'new456');

        // Then: remote datasource가 올바른 파라미터로 호출된다
        expect(fakeRemoteDataSource.changePasswordCalls, 1);
        expect(fakeRemoteDataSource.lastOldPassword, 'old123');
        expect(fakeRemoteDataSource.lastNewPassword, 'new456');
      });
    });
  });
}

/// AuthRemoteDataSource Fake 구현
///
/// 테스트를 위해 네트워크 호출을 시뮬레이션합니다.
class FakeAuthRemoteDataSource implements AuthRemoteDataSource {
  // verifyCurrentPassword
  int verifyCurrentPasswordCalls = 0;
  String? lastCurrentPassword;
  bool verifyPasswordResult = true;

  // changePassword
  int changePasswordCalls = 0;
  String? lastOldPassword;
  String? lastNewPassword;

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    verifyCurrentPasswordCalls++;
    lastCurrentPassword = currentPassword;
    return verifyPasswordResult;
  }

  @override
  Future<void> changePassword(String currentPassword, String newPassword) async {
    changePasswordCalls++;
    lastOldPassword = currentPassword;
    lastNewPassword = newPassword;
  }

  // 나머지 메서드는 미구현 (이 테스트에서는 사용하지 않음)
  @override
  Future<LoginResponseModel> login(String employeeId, String password) {
    throw UnimplementedError();
  }

  @override
  Future<AuthTokenModel> refreshToken(String refreshToken) {
    throw UnimplementedError();
  }

  @override
  Future<void> logout() {
    throw UnimplementedError();
  }

  @override
  Future<void> recordGpsConsent() {
    throw UnimplementedError();
  }
}
