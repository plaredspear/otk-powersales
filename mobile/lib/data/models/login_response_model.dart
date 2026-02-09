import '../../domain/repositories/auth_repository.dart';
import 'user_model.dart';
import 'auth_token_model.dart';

/// 로그인 응답 모델 (DTO)
///
/// Backend API의 로그인 응답 JSON을 파싱하여 LoginResult로 변환합니다.
/// 응답 형식:
/// ```json
/// {
///   "success": true,
///   "data": {
///     "user": { ... },
///     "token": { ... },
///     "requires_password_change": true,
///     "requires_gps_consent": false
///   },
///   "message": "로그인 성공"
/// }
/// ```
class LoginResponseModel {
  final UserModel user;
  final AuthTokenModel token;
  final bool requiresPasswordChange;
  final bool requiresGpsConsent;

  const LoginResponseModel({
    required this.user,
    required this.token,
    required this.requiresPasswordChange,
    required this.requiresGpsConsent,
  });

  /// snake_case JSON에서 파싱 (data 객체를 파싱)
  factory LoginResponseModel.fromJson(Map<String, dynamic> json) {
    return LoginResponseModel(
      user: UserModel.fromJson(json['user'] as Map<String, dynamic>),
      token: AuthTokenModel.fromJson(json['token'] as Map<String, dynamic>),
      requiresPasswordChange: json['requires_password_change'] as bool,
      requiresGpsConsent: json['requires_gps_consent'] as bool,
    );
  }

  /// snake_case JSON으로 직렬화
  Map<String, dynamic> toJson() {
    return {
      'user': user.toJson(),
      'token': token.toJson(),
      'requires_password_change': requiresPasswordChange,
      'requires_gps_consent': requiresGpsConsent,
    };
  }

  /// Domain LoginResult로 변환
  LoginResult toLoginResult() {
    return LoginResult(
      user: user.toEntity(),
      token: token.toEntity(),
      requiresPasswordChange: requiresPasswordChange,
      requiresGpsConsent: requiresGpsConsent,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is LoginResponseModel &&
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
    return 'LoginResponseModel(user: $user, token: $token, requiresPasswordChange: $requiresPasswordChange, requiresGpsConsent: $requiresGpsConsent)';
  }
}
