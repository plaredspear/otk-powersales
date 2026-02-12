import '../models/login_response_model.dart';
import '../models/auth_token_model.dart';

/// 인증 원격 데이터소스 인터페이스
///
/// API 서버와의 인증 관련 통신을 추상화합니다.
abstract class AuthRemoteDataSource {
  /// 로그인 API 호출
  Future<LoginResponseModel> login(String employeeId, String password);

  /// 토큰 갱신 API 호출
  Future<AuthTokenModel> refreshToken(String refreshToken);

  /// 현재 비밀번호 검증 API 호출
  Future<bool> verifyCurrentPassword(String currentPassword);

  /// 비밀번호 변경 API 호출
  Future<void> changePassword(String currentPassword, String newPassword);

  /// 로그아웃 API 호출
  Future<void> logout();

  /// GPS 동의 기록 API 호출
  Future<void> recordGpsConsent();
}
