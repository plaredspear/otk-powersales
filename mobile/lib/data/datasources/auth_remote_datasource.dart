import '../models/login_response_model.dart';
import '../models/auth_token_model.dart';
import '../models/user_model.dart';

/// 인증 원격 데이터소스 인터페이스
///
/// API 서버와의 인증 관련 통신을 추상화합니다.
abstract class AuthRemoteDataSource {
  /// 로그인 API 호출
  ///
  /// [autoLogin] 자동로그인 선택 여부. true 면 서버가 장수명(60일) refresh token 을
  /// 발급해 오래 방치해도 세션이 유지된다. false 면 기본 7일 롤링 세션.
  Future<LoginResponseModel> login(
      String employeeCode, String password, String deviceId, bool autoLogin);

  /// 토큰 갱신 API 호출
  Future<AuthTokenModel> refreshToken(String refreshToken);

  /// 현재 사용자 정보 조회 API 호출 (자동로그인 후 사용자 복원용)
  Future<UserModel> getMe();

  /// 현재 비밀번호 검증 API 호출
  Future<bool> verifyCurrentPassword(String currentPassword);

  /// 비밀번호 변경 API 호출 (Spec #584).
  ///
  /// 강제 변경 시 [currentPassword] 는 null 로 호출 가능. 응답에 새 토큰 페어 포함.
  /// [autoLogin] 자동로그인 선택 여부. 변경 시 새 refresh family 로 재발급되므로,
  /// 저장된 선호를 전달해 ON 세션의 장수명(60일)을 유지한다.
  Future<AuthTokenModel> changePassword({
    String? currentPassword,
    required String newPassword,
    bool autoLogin = false,
  });

  /// 로그아웃 API 호출
  Future<void> logout();

  /// GPS 동의 약관 조회 API 호출
  Future<Map<String, dynamic>> getGpsConsentTerms();

  /// GPS 동의 상태 조회 API 호출
  Future<Map<String, dynamic>> getGpsConsentStatus();

  /// GPS 동의 기록 API 호출
  Future<Map<String, dynamic>> recordGpsConsent({String? agreementNumber});
}
