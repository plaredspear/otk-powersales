import '../../domain/repositories/password_repository.dart';
import '../datasources/auth_remote_datasource.dart';

/// 비밀번호 Repository 구현체
///
/// AuthRemoteDataSource를 사용하여 비밀번호 검증 및 변경 기능을 구현합니다.
class PasswordRepositoryImpl implements PasswordRepository {
  final AuthRemoteDataSource _remoteDataSource;

  PasswordRepositoryImpl({
    required AuthRemoteDataSource remoteDataSource,
  }) : _remoteDataSource = remoteDataSource;

  @override
  Future<bool> verifyCurrentPassword(String currentPassword) async {
    return await _remoteDataSource.verifyCurrentPassword(currentPassword);
  }

  @override
  Future<void> changePassword(String currentPassword, String newPassword) async {
    await _remoteDataSource.changePassword(currentPassword, newPassword);
  }
}
