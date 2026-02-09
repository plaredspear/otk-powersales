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

  /// 비밀번호 변경 필요 여부
  final bool requiresPasswordChange;

  /// GPS 동의 필요 여부
  final bool requiresGpsConsent;

  const LoginResult({
    required this.user,
    required this.token,
    required this.requiresPasswordChange,
    required this.requiresGpsConsent,
  });

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is LoginResult &&
        other.user == user &&
        other.token == token &&
        other.requiresPasswordChange == requiresPasswordChange &&
        other.requiresGpsConsent == requiresGpsConsent;
  }

  @override
  int get hashCode {
    return Object.hash(
      user,
      token,
      requiresPasswordChange,
      requiresGpsConsent,
    );
  }

  @override
  String toString() {
    return 'LoginResult(user: $user, token: $token, requiresPasswordChange: $requiresPasswordChange, requiresGpsConsent: $requiresGpsConsent)';
  }
}

/// 인증 Repository 인터페이스
///
/// 사용자 인증, 토큰 관리, 비밀번호 변경 등의 기능을 추상화합니다.
/// 구현체는 Mock Repository 또는 실제 API Repository가 될 수 있습니다.
abstract class AuthRepository {
  /// 로그인
  ///
  /// [employeeId]: 사번 (8자리 숫자)
  /// [password]: 비밀번호
  ///
  /// Returns: 로그인 결과 (사용자 정보, 토큰, 비밀번호 변경 필요 여부, GPS 동의 필요 여부)
  Future<LoginResult> login(String employeeId, String password);

  /// 토큰 갱신
  ///
  /// Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.
  ///
  /// [refreshToken]: Refresh Token
  ///
  /// Returns: 새로운 인증 토큰
  Future<AuthToken> refreshToken(String refreshToken);

  /// 비밀번호 변경
  ///
  /// [currentPassword]: 현재 비밀번호
  /// [newPassword]: 새 비밀번호
  Future<void> changePassword(String currentPassword, String newPassword);

  /// 로그아웃
  ///
  /// 서버에 로그아웃을 알리고 로컬 토큰을 삭제합니다.
  Future<void> logout();

  /// GPS 위치정보 동의 기록
  ///
  /// 사용자의 GPS 위치정보 수집 동의를 서버에 기록합니다.
  Future<void> recordGpsConsent();
}
