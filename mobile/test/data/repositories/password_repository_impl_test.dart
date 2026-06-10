import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/data/datasources/auth_local_datasource.dart';
import 'package:mobile/data/datasources/auth_remote_datasource.dart';
import 'package:mobile/data/models/auth_token_model.dart';
import 'package:mobile/data/models/login_response_model.dart';
import 'package:mobile/data/models/user_model.dart';
import 'package:mobile/data/repositories/password_repository_impl.dart';

void main() {
  late PasswordRepositoryImpl repository;
  late FakeAuthRemoteDataSource fakeRemoteDataSource;
  late _FakeAuthLocalDataSource fakeLocalDataSource;

  setUp(() {
    fakeRemoteDataSource = FakeAuthRemoteDataSource();
    fakeLocalDataSource = _FakeAuthLocalDataSource();
    repository = PasswordRepositoryImpl(
      remoteDataSource: fakeRemoteDataSource,
      localDataSource: fakeLocalDataSource,
    );
  });

  group('PasswordRepositoryImpl', () {
    group('verifyCurrentPassword', () {
      test('비밀번호가 일치하면 true를 반환한다', () async {
        fakeRemoteDataSource.verifyPasswordResult = true;
        final result = await repository.verifyCurrentPassword('1234');
        expect(fakeRemoteDataSource.verifyCurrentPasswordCalls, 1);
        expect(fakeRemoteDataSource.lastCurrentPassword, '1234');
        expect(result, true);
      });

      test('비밀번호가 불일치하면 false를 반환한다', () async {
        fakeRemoteDataSource.verifyPasswordResult = false;
        final result = await repository.verifyCurrentPassword('wrong');
        expect(result, false);
      });
    });

    group('changePassword', () {
      test('자발 변경: remote 호출 + 새 토큰 저장', () async {
        fakeRemoteDataSource.changePasswordResult = const AuthTokenModel(
          accessToken: 'new-access',
          refreshToken: 'new-refresh',
          expiresIn: 3600,
        );

        final token = await repository.changePassword(
          currentPassword: 'old123',
          newPassword: 'newpass1',
        );

        expect(fakeRemoteDataSource.changePasswordCalls, 1);
        expect(fakeRemoteDataSource.lastOldPassword, 'old123');
        expect(fakeRemoteDataSource.lastNewPassword, 'newpass1');
        expect(token.accessToken, 'new-access');
        expect(token.refreshToken, 'new-refresh');
        expect(fakeLocalDataSource.savedAccessToken, 'new-access');
        expect(fakeLocalDataSource.savedRefreshToken, 'new-refresh');
      });
    });
  });
}

class FakeAuthRemoteDataSource implements AuthRemoteDataSource {
  int verifyCurrentPasswordCalls = 0;
  String? lastCurrentPassword;
  bool verifyPasswordResult = true;

  int changePasswordCalls = 0;
  String? lastOldPassword;
  String? lastNewPassword;
  AuthTokenModel changePasswordResult = const AuthTokenModel(
    accessToken: 'mock',
    refreshToken: 'mock',
    expiresIn: 3600,
  );

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    verifyCurrentPasswordCalls++;
    lastCurrentPassword = currentPassword;
    return verifyPasswordResult;
  }

  @override
  Future<AuthTokenModel> changePassword({
    String? currentPassword,
    required String newPassword,
  }) async {
    changePasswordCalls++;
    lastOldPassword = currentPassword;
    lastNewPassword = newPassword;
    return changePasswordResult;
  }

  @override
  Future<LoginResponseModel> login(
      String employeeCode, String password, String deviceId) {
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
  Future<UserModel> getMe() {
    throw UnimplementedError();
  }

  @override
  Future<Map<String, dynamic>> getGpsConsentTerms() {
    throw UnimplementedError();
  }

  @override
  Future<Map<String, dynamic>> getGpsConsentStatus() {
    throw UnimplementedError();
  }

  @override
  Future<Map<String, dynamic>> recordGpsConsent({String? agreementNumber}) {
    throw UnimplementedError();
  }
}

class _FakeAuthLocalDataSource extends AuthLocalDataSource {
  String? savedAccessToken;
  String? savedRefreshToken;

  @override
  Future<void> saveAccessToken(String token) async {
    savedAccessToken = token;
  }

  @override
  Future<void> saveRefreshToken(String token) async {
    savedRefreshToken = token;
  }
}
