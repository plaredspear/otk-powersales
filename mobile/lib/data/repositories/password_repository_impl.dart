import '../../domain/entities/auth_token.dart';
import '../../domain/repositories/password_repository.dart';
import '../datasources/auth_local_datasource.dart';
import '../datasources/auth_remote_datasource.dart';

/// 비밀번호 Repository 구현체.
///
/// AuthRemoteDataSource 를 사용해 비밀번호 검증/변경을 수행하며, 변경 후 새 토큰 페어를
/// 로컬 저장한다.
class PasswordRepositoryImpl implements PasswordRepository {
  final AuthRemoteDataSource _remoteDataSource;
  final AuthLocalDataSource _localDataSource;

  PasswordRepositoryImpl({
    required AuthRemoteDataSource remoteDataSource,
    required AuthLocalDataSource localDataSource,
  })  : _remoteDataSource = remoteDataSource,
        _localDataSource = localDataSource;

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    return await _remoteDataSource.verifyCurrentPassword(currentPassword);
  }

  @override
  Future<AuthToken> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    final tokenModel = await _remoteDataSource.changePassword(
      currentPassword: currentPassword,
      newPassword: newPassword,
    );
    final token = tokenModel.toEntity();
    await _localDataSource.saveAccessToken(token.accessToken);
    await _localDataSource.saveRefreshToken(token.refreshToken);
    return token;
  }
}
