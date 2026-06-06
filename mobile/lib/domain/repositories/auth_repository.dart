import '../entities/user.dart';
import '../entities/auth_token.dart';

/// 로그인 결과 값 객체
///
/// 로그인 성공 시 반환되는 정보를 담는 도메인 레벨 값 객체입니다.
class LoginResult {
  /// 사용자 정보
  final User user;

  /// 인증 토큰
  final AuthToken token;

  /// 비밀번호 변경 필요 여부 (강제 변경 플래그 — Spec #584)
  final bool passwordChangeRequired;

  /// GPS 동의 필요 여부
  final bool requiresGpsConsent;

  const LoginResult({
    required this.user,
    required this.token,
    required this.passwordChangeRequired,
    required this.requiresGpsConsent,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is LoginResult &&
        other.user == user &&
        other.token == token &&
        other.passwordChangeRequired == passwordChangeRequired &&
        other.requiresGpsConsent == requiresGpsConsent;
  }

  @override
  int get hashCode {
    return Object.hash(
      user,
      token,
      passwordChangeRequired,
      requiresGpsConsent,
    );
  }

  @override
  String toString() {
    return 'LoginResult(user: $user, token: $token, passwordChangeRequired: $passwordChangeRequired, requiresGpsConsent: $requiresGpsConsent)';
  }
}

/// 인증 Repository 인터페이스
///
/// 사용자 인증, 토큰 관리, 비밀번호 변경 등의 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class AuthRepository {
  /// 로그인
  ///
  /// [employeeCode]: 사번 (8자리 숫자)
  /// [password]: 비밀번호
  ///
  /// Returns: 로그인 결과 (사용자 정보, 토큰, 비밀번호 변경 필요 여부, GPS 동의 필요 여부)
  Future<LoginResult> login(String employeeCode, String password);

  /// 토큰 갱신
  ///
  /// Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.
  ///
  /// [refreshToken]: Refresh Token
  ///
  /// Returns: 새로운 인증 토큰
  Future<AuthToken> refreshToken(String refreshToken);

  /// 현재 사용자 정보 조회
  ///
  /// Access Token 으로 서버에서 현재 사용자 정보를 조회합니다.
  /// 자동로그인 직후 토큰만으로는 복원할 수 없는 사용자 정보를 확보하는 데 사용합니다.
  ///
  /// Returns: 사용자 정보
  Future<User> getMe();

  /// 비밀번호 변경 (강제/자발 통합 — Spec #584).
  ///
  /// 강제 변경 (토큰 클레임 `passwordChangeRequired=true`) 시 [currentPassword] 는 무시되므로
  /// `null` 또는 빈 문자열로 호출 가능. 자발 변경 시는 필수.
  ///
  /// 응답에 새 토큰 페어(클레임 `passwordChangeRequired=false` 반영) 가 포함되며 호출자는
  /// 반환된 [AuthToken] 으로 자동 로그인 상태를 갱신해야 한다.
  Future<AuthToken> changePassword({
    String? currentPassword,
    required String newPassword,
  });

  /// 로그아웃
  ///
  /// 서버에 로그아웃을 알리고 로컬 토큰을 삭제합니다.
  Future<void> logout();

  /// GPS 동의 약관 조회
  Future<GpsConsentTerms> getGpsConsentTerms();

  /// GPS 동의 상태 조회
  Future<GpsConsentStatus> getGpsConsentStatus();

  /// GPS 위치정보 동의 기록
  ///
  /// [agreementNumber]: 동의한 약관 번호 (선택)
  /// Returns: 새 access_token 포함 결과
  Future<GpsConsentRecordResult> recordGpsConsent({String? agreementNumber});
}

/// GPS 동의 약관 값 객체
class GpsConsentTerms {
  final String agreementNumber;
  final String contents;

  const GpsConsentTerms({
    required this.agreementNumber,
    required this.contents,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is GpsConsentTerms &&
        other.agreementNumber == agreementNumber &&
        other.contents == contents;
  }

  @override
  int get hashCode => Object.hash(agreementNumber, contents);
}

/// GPS 동의 상태 값 객체
class GpsConsentStatus {
  final bool requiresGpsConsent;

  const GpsConsentStatus({required this.requiresGpsConsent});

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is GpsConsentStatus &&
        other.requiresGpsConsent == requiresGpsConsent;
  }

  @override
  int get hashCode => requiresGpsConsent.hashCode;
}

/// GPS 동의 기록 결과 값 객체
class GpsConsentRecordResult {
  final String accessToken;
  final int expiresIn;

  const GpsConsentRecordResult({
    required this.accessToken,
    required this.expiresIn,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is GpsConsentRecordResult &&
        other.accessToken == accessToken &&
        other.expiresIn == expiresIn;
  }

  @override
  int get hashCode => Object.hash(accessToken, expiresIn);
}
