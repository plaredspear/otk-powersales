import '../../domain/entities/auth_token.dart';
import '../../domain/entities/user.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../datasources/auth_local_datasource.dart';

/// 인증 Repository 구현체
///
/// 실제 API DataSource를 사용하여 인증 기능을 구현합니다.
class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _localDataSource;

  AuthRepositoryImpl({
    required AuthRemoteDataSource remoteDataSource,
    required AuthLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

  @override
  Future<LoginResult> login(String employeeCode, String password,
      {bool autoLogin = false}) async {
    final deviceId = await _localDataSource.getDeviceId();
    final response = await _remoteDataSource.login(
        employeeCode, password, deviceId, autoLogin);
    final result = response.toLoginResult();

    // 토큰을 로컬 저장소에 저장
    await _localDataSource.saveAccessToken(result.token.accessToken);
    await _localDataSource.saveRefreshToken(result.token.refreshToken);

    return result;
  }

  @override
  Future<AuthToken> refreshToken(String refreshToken) async {
    final tokenModel = await _remoteDataSource.refreshToken(refreshToken);
    final token = tokenModel.toEntity();

    // 새 Access Token 저장
    await _localDataSource.saveAccessToken(token.accessToken);

    return token;
  }

  @override
  Future<User> getMe() async {
    final userModel = await _remoteDataSource.getMe();
    return userModel.toEntity();
  }

  @override
  Future<AuthToken> changePassword({
    String? currentPassword,
    required String newPassword,
  }) async {
    // 저장된 자동로그인 선호를 전달 → 변경 후에도 ON 세션의 장수명(60일)을 유지.
    final autoLogin = await _localDataSource.isAutoLoginEnabled();
    final tokenModel = await _remoteDataSource.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
      autoLogin: autoLogin,
    );
    final token = tokenModel.toEntity();
    // 새 토큰 페어 저장 (클레임 passwordChangeRequired=false 반영)
    await _localDataSource.saveAccessToken(token.accessToken);
    await _localDataSource.saveRefreshToken(token.refreshToken);
    return token;
  }

  @override
  Future<void> logout() async {
    await _remoteDataSource.logout();
    await _localDataSource.clearTokens();
  }

  @override
  Future<GpsConsentTerms> getGpsConsentTerms() async {
    final data = await _remoteDataSource.getGpsConsentTerms();
    return GpsConsentTerms(
      agreementNumber: data['agreementNumber'] as String,
      contents: data['contents'] as String,
    );
  }

  @override
  Future<GpsConsentStatus> getGpsConsentStatus() async {
    final data = await _remoteDataSource.getGpsConsentStatus();
    return GpsConsentStatus(
      requiresGpsConsent: data['requiresGpsConsent'] as bool,
    );
  }

  @override
  Future<GpsConsentRecordResult> recordGpsConsent({String? agreementNumber}) async {
    final data = await _remoteDataSource.recordGpsConsent(
      agreementNumber: agreementNumber,
    );
    final result = GpsConsentRecordResult(
      accessToken: data['accessToken'] as String,
      expiresIn: data['expiresIn'] as int,
    );
    await _localDataSource.saveAccessToken(result.accessToken);
    return result;
  }
}
