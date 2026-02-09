import '../../domain/entities/auth_token.dart';
import '../../domain/repositories/auth_repository.dart';
import '../datasources/auth_remote_datasource.dart';
import '../datasources/auth_local_datasource.dart';

/// 인증 Repository 구현체
///
/// 실제 API DataSource를 사용하여 인증 기능을 구현합니다.
/// Backend API 연동 시점에 AuthMockRepository를 대체합니다.
class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _localDataSource;

  AuthRepositoryImpl({
    required AuthRemoteDataSource remoteDataSource,
    required AuthLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

  @override
  Future<LoginResult> login(String employeeId, String password) async {
    final response = await _remoteDataSource.login(employeeId, password);
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
  Future<void> changePassword(String currentPassword, String newPassword) async {
    await _remoteDataSource.changePassword(currentPassword, newPassword);
  }

  @override
  Future<void> logout() async {
    await _remoteDataSource.logout();
    await _localDataSource.clearTokens();
  }

  @override
  Future<void> recordGpsConsent() async {
    await _remoteDataSource.recordGpsConsent();
  }
}
